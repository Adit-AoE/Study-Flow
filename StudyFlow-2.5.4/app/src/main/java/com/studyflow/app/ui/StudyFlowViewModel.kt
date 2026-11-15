package com.studyflow.app.ui

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.data.AppData
import com.studyflow.app.data.AppJson
import com.studyflow.app.data.RoutineItem
import com.studyflow.app.data.Semester
import com.studyflow.app.data.Storage
import com.studyflow.app.data.StorageDispatcher
import com.studyflow.app.data.Subject
import com.studyflow.app.data.Task
import com.studyflow.app.data.initialData
import com.studyflow.app.data.mergeSemester
import com.studyflow.app.data.updateActive
import com.studyflow.app.data.withoutLocalFiles
import com.studyflow.app.notify.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** A file chosen with the system picker, not yet copied into app storage. */
data class PickedFile(val uri: Uri, val name: String)

data class PendingImport(val raw: JSONObject, val fileName: String, val semesterCount: Int)

data class TimerUi(
    val durationSec: Int = 25 * 60,
    val remainingSec: Int = 25 * 60,
    val running: Boolean = false,
)

enum class ImportMode { Merge, Replace }

class StudyFlowViewModel(app: Application) : AndroidViewModel(app) {

    private val appCtx: Application get() = getApplication()

    /** Not tied to viewModelScope on purpose: a save must finish even if the screen closes right after. */
    private val ioScope = CoroutineScope(SupervisorJob() + StorageDispatcher)

    private val _data = MutableStateFlow(Storage.load(app))
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        persist(_data.value)
    }

    private fun say(text: String) {
        _messages.tryEmit(text)
    }

    private fun persist(snapshot: AppData) {
        ioScope.launch {
            Storage.save(appCtx, snapshot)
            Reminders.rescheduleAll(appCtx, snapshot)
        }
    }

    private fun mutate(transform: (AppData) -> AppData) {
        persist(_data.updateAndGet(transform))
    }

    // ---- theme -----------------------------------------------------------------------------------

    fun toggleTheme() = mutate { it.copy(settings = it.settings.copy(darkMode = !it.settings.darkMode)) }

    // ---- semesters ---------------------------------------------------------------------------------

    fun switchSemester(id: String) = mutate { d ->
        if (d.semesters.any { it.id == id }) d.copy(currentSemesterId = id) else d
    }

    fun addSemester(name: String, start: String, end: String) {
        val id = "sem_" + System.currentTimeMillis()
        mutate { d ->
            d.copy(
                semesters = d.semesters + Semester(id = id, name = name.trim(), startDate = start, endDate = end),
                currentSemesterId = id,
            )
        }
    }

    fun editSemester(id: String, name: String, start: String, end: String) = mutate { d ->
        d.copy(semesters = d.semesters.map {
            if (it.id == id) it.copy(name = name.trim(), startDate = start, endDate = end) else it
        })
    }

    /** Returns false when the last remaining semester can't be deleted. */
    fun deleteSemester(id: String): Boolean {
        if (_data.value.semesters.size <= 1) {
            say("You cannot delete the only remaining semester. Use 'Reset Current Semester' in the Danger Zone to clear its data.")
            return false
        }
        val doomed = _data.value.semesters.firstOrNull { it.id == id }
        mutate { d ->
            val remaining = d.semesters.filter { it.id != id }
            d.copy(
                semesters = remaining,
                currentSemesterId = if (d.currentSemesterId == id) remaining.first().id else d.currentSemesterId,
            )
        }
        doomed?.tasks?.forEach { t -> ioScope.launch { Storage.deleteAttachment(appCtx, t.filePath) } }
        return true
    }

    fun resetCurrentSemester() {
        val old = _data.value.active.tasks
        mutate { d -> d.updateActive { it.copy(tasks = emptyList(), subjects = emptyList(), routine = emptyList()) } }
        old.forEach { t -> ioScope.launch { Storage.deleteAttachment(appCtx, t.filePath) } }
    }

    fun resetAll() {
        mutate { initialData() }
        ioScope.launch { Storage.wipeAttachments(appCtx) }
        say("All data was reset.")
    }

    // ---- tasks --------------------------------------------------------------------------------------

    /**
     * Adds a task ([existingId] == null) or updates one. A newly picked file is copied into app
     * storage; [removeFile] drops the current attachment.
     */
    fun saveTask(existingId: String?, draft: Task, picked: PickedFile?, removeFile: Boolean) {
        ioScope.launch {
            val stored = picked?.let { Storage.copyAttachment(appCtx, it.uri) }
            if (picked != null && stored == null) say("Couldn't attach that file.")

            if (existingId == null) {
                mutate { d ->
                    d.updateActive { sem ->
                        sem.copy(tasks = sem.tasks + draft.copy(file = stored?.name, filePath = stored?.storedName, completed = false))
                    }
                }
            } else {
                val old = _data.value.active.tasks.firstOrNull { it.id == existingId } ?: return@launch
                val replaceFile = stored != null || removeFile
                if (replaceFile) Storage.deleteAttachment(appCtx, old.filePath)
                mutate { d ->
                    d.updateActive { sem ->
                        sem.copy(tasks = sem.tasks.map { t ->
                            if (t.id != existingId) t else draft.copy(
                                id = t.id,
                                completed = t.completed,
                                file = if (replaceFile) stored?.name else t.file,
                                filePath = if (replaceFile) stored?.storedName else t.filePath,
                            )
                        })
                    }
                }
            }
        }
    }

    fun toggleTask(id: String) = mutate { d ->
        d.updateActive { sem -> sem.copy(tasks = sem.tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it }) }
    }

    fun toggleSubtask(taskId: String, index: Int) = mutate { d ->
        d.updateActive { sem ->
            sem.copy(tasks = sem.tasks.map { t ->
                if (t.id != taskId) t else t.copy(subtasks = t.subtasks.mapIndexed { i, st ->
                    if (i == index) st.copy(completed = !st.completed) else st
                })
            })
        }
    }

    fun deleteTask(id: String) {
        val doomed = _data.value.active.tasks.firstOrNull { it.id == id }
        mutate { d -> d.updateActive { sem -> sem.copy(tasks = sem.tasks.filter { it.id != id }) } }
        ioScope.launch { Storage.deleteAttachment(appCtx, doomed?.filePath) }
    }

    // ---- courses ---------------------------------------------------------------------------------------

    fun addSubject(name: String, prof: String, room: String, color: String, duration: Int) = mutate { d ->
        d.updateActive { sem ->
            sem.copy(subjects = sem.subjects + Subject(name = name.trim(), prof = prof, room = room, color = color, duration = duration))
        }
    }

    /** Renaming a course also updates the routine sessions and tasks that used the old name. */
    fun editSubject(id: String, name: String, prof: String, room: String, color: String, duration: Int) = mutate { d ->
        d.updateActive { sem ->
            val old = sem.subjects.firstOrNull { it.id == id } ?: return@updateActive sem
            val newName = name.trim()
            sem.copy(
                subjects = sem.subjects.map {
                    if (it.id == id) it.copy(name = newName, prof = prof, room = room, color = color, duration = duration) else it
                },
                routine = sem.routine.map {
                    if (it.name == old.name) it.copy(name = newName, prof = prof, room = room, color = color, duration = duration) else it
                },
                tasks = sem.tasks.map { if (it.subject == old.name) it.copy(subject = newName) else it },
            )
        }
    }

    fun deleteSubject(id: String) = mutate { d ->
        d.updateActive { sem -> sem.copy(subjects = sem.subjects.filter { it.id != id }) }
    }

    fun markAttendance(id: String, present: Boolean) = mutate { d ->
        d.updateActive { sem ->
            sem.copy(subjects = sem.subjects.map {
                if (it.id == id) it.copy(total = it.total + 1, present = it.present + if (present) 1 else 0) else it
            })
        }
    }

    // ---- routine ----------------------------------------------------------------------------------------

    fun addRoutine(item: RoutineItem) = mutate { d ->
        d.updateActive { sem -> sem.copy(routine = sem.routine + item) }
    }

    fun updateRoutine(id: String, item: RoutineItem) = mutate { d ->
        d.updateActive { sem -> sem.copy(routine = sem.routine.map { if (it.id == id) item.copy(id = id) else it }) }
    }

    fun deleteRoutine(id: String) = mutate { d ->
        d.updateActive { sem -> sem.copy(routine = sem.routine.filter { it.id != id }) }
    }

    // ---- sync (simulated, exactly like the web version) -------------------------------------------------------

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun sync(syncId: String) {
        val id = syncId.trim()
        if (id.isEmpty()) {
            say("Please enter a Sync ID to connect your devices.")
            return
        }
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            delay(1500)
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
            mutate { it.copy(settings = it.settings.copy(syncId = id, lastSync = stamp)) }
            _syncing.value = false
        }
    }

    // ---- import / export -----------------------------------------------------------------------------------

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    fun exportTo(uri: Uri) {
        val snapshot = _data.value
        ioScope.launch {
            try {
                appCtx.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                    out.write(AppJson.toJsonString(snapshot).toByteArray(Charsets.UTF_8))
                } ?: throw IllegalStateException("No output stream")
                say("Backup saved.")
            } catch (e: Exception) {
                say("Couldn't save the backup file.")
            }
        }
    }

    fun prepareImport(uri: Uri) {
        ioScope.launch {
            try {
                val text = appCtx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalStateException("No input stream")
                val raw = JSONObject(text)
                val count = AppJson.normalize(raw).semesters.size
                _pendingImport.value = PendingImport(raw, Storage.displayName(appCtx, uri), count)
            } catch (e: Exception) {
                say("Invalid file format")
            }
        }
    }

    fun cancelImport() {
        _pendingImport.value = null
    }

    fun confirmImport(mode: ImportMode) {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        val incoming = AppJson.normalize(pending.raw).withoutLocalFiles()

        when (mode) {
            ImportMode.Replace -> {
                mutate { incoming }
                ioScope.launch { Storage.wipeAttachments(appCtx) }
                say("Data replaced successfully!")
            }
            ImportMode.Merge -> {
                val isLegacyFlat = pending.raw.optJSONArray("semesters") == null
                mutate { current ->
                    if (isLegacyFlat) {
                        // Old single-semester backup: merge into the active semester.
                        val src = incoming.semesters.first()
                        current.updateActive { mergeSemester(it, src) }
                    } else {
                        var semesters = current.semesters
                        incoming.semesters.forEach { inc ->
                            val match = semesters.firstOrNull { it.id == inc.id || it.name.equals(inc.name, ignoreCase = true) }
                            semesters = if (match != null) {
                                semesters.map { if (it.id == match.id) mergeSemester(it, inc) else it }
                            } else {
                                semesters + inc
                            }
                        }
                        current.copy(semesters = semesters)
                    }
                }
                say("Data merged successfully!")
            }
        }
    }

    // ---- focus timer ---------------------------------------------------------------------------------------

    private val _timer = MutableStateFlow(TimerUi())
    val timer: StateFlow<TimerUi> = _timer.asStateFlow()

    private val _timerDone = MutableStateFlow(false)
    val timerDone: StateFlow<Boolean> = _timerDone.asStateFlow()

    private var tickJob: Job? = null
    private var endsAt = 0L

    fun setTimer(minutes: Int) {
        pauseTimer()
        val seconds = minutes.coerceIn(1, 999) * 60
        _timer.value = TimerUi(durationSec = seconds, remainingSec = seconds, running = false)
    }

    fun startTimer() {
        val t = _timer.value
        if (t.running) return
        val remaining = if (t.remainingSec <= 0) t.durationSec else t.remainingSec
        endsAt = SystemClock.elapsedRealtime() + remaining * 1000L
        _timer.value = t.copy(remainingSec = remaining, running = true)
        Reminders.scheduleTimer(appCtx, endsAt)

        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                val left = endsAt - SystemClock.elapsedRealtime()
                if (left <= 0L) {
                    _timer.update { it.copy(remainingSec = 0, running = false) }
                    _timerDone.value = true
                    break
                }
                val shown = ((left + 999L) / 1000L).toInt()
                _timer.update { it.copy(remainingSec = shown) }
                delay(200)
            }
        }
    }

    fun pauseTimer() {
        val t = _timer.value
        tickJob?.cancel()
        tickJob = null
        Reminders.cancelTimer(appCtx)
        if (!t.running) return
        val left = endsAt - SystemClock.elapsedRealtime()
        val remaining = ((left + 999L) / 1000L).toInt().coerceAtLeast(0)
        _timer.value = t.copy(remainingSec = remaining, running = false)
    }

    fun resetTimer() {
        tickJob?.cancel()
        tickJob = null
        Reminders.cancelTimer(appCtx)
        _timer.update { it.copy(remainingSec = it.durationSec, running = false) }
    }

    fun acknowledgeTimerDone() {
        _timerDone.value = false
    }
}
