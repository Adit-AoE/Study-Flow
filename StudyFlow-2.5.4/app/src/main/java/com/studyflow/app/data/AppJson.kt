package com.studyflow.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Reads and writes the same JSON structure the web version stores / exports, so backups made on
 * the website can be imported here and the other way round. Extra fields (`id`, `filePath`) are
 * simply ignored by the web version.
 */
object AppJson {

    // ---- helpers -------------------------------------------------------------------------

    private fun JSONObject.str(name: String, def: String = ""): String =
        if (!has(name) || isNull(name)) def else optString(name, def)

    private fun JSONObject.strOrNull(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotEmpty() }

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = ArrayList<T>(length())
        for (i in 0 until length()) {
            val obj = optJSONObject(i) ?: continue
            out += transform(obj)
        }
        return out
    }

    private fun parseDay(value: Any?): Int {
        val n = when (value) {
            is Number -> value.toInt()
            is String -> value.trim().toDoubleOrNull()?.toInt()
            else -> null
        } ?: return 1
        return if (n in 0..6) n else 1
    }

    // ---- parsing -------------------------------------------------------------------------

    fun parse(text: String): AppData? = try {
        normalize(JSONObject(text))
    } catch (e: Exception) {
        null
    }

    private fun parseTasks(arr: JSONArray?): List<Task> = arr.mapObjects { o ->
        Task(
            id = o.strOrNull("id") ?: newId(),
            title = o.str("title"),
            description = o.str("description"),
            type = o.str("type").ifBlank { "Assignment" },
            date = o.str("date"),
            subject = o.str("subject"),
            subtasks = o.optJSONArray("subtasks").mapObjects { s ->
                Subtask(title = s.str("title"), completed = s.optBoolean("completed", false))
            },
            file = o.strOrNull("file"),
            filePath = o.strOrNull("filePath"),
            completed = o.optBoolean("completed", false),
        )
    }

    private fun parseSubjects(arr: JSONArray?): List<Subject> = arr.mapObjects { o ->
        Subject(
            id = o.strOrNull("id") ?: newId(),
            name = o.str("name"),
            present = o.optInt("present", 0),
            total = o.optInt("total", 0),
            prof = o.str("prof"),
            room = o.str("room"),
            color = o.str("color").ifBlank { DEFAULT_COLOR },
            duration = o.optInt("duration", 60).let { if (it > 0) it else 60 },
        )
    }

    private fun parseRoutine(arr: JSONArray?): List<RoutineItem> = arr.mapObjects { o ->
        RoutineItem(
            id = o.strOrNull("id") ?: newId(),
            day = parseDay(o.opt("day")),
            time = o.str("time", "08:00").ifBlank { "08:00" },
            name = o.str("name"),
            type = o.str("type").ifBlank { "Class" },
            prof = o.str("prof"),
            room = o.str("room"),
            color = o.str("color").ifBlank { NEUTRAL_COLOR },
            duration = o.optInt("duration", 60).let { if (it > 0) it else 60 },
        )
    }

    /** Port of normalizeAppData() from the web version (including the legacy flat format). */
    fun normalize(raw: JSONObject): AppData {
        val s = raw.optJSONObject("settings")
        val settings = Settings(
            darkMode = if (s != null && s.has("darkMode") && !s.isNull("darkMode")) s.optBoolean("darkMode", true) else true,
            syncId = s?.str("syncId") ?: "",
            lastSync = s?.strOrNull("lastSync"),
        )

        val sems = raw.optJSONArray("semesters")
        if (sems == null || sems.length() == 0) {
            val migratedId = "sem_" + System.currentTimeMillis()
            return AppData(
                semesters = listOf(
                    Semester(
                        id = migratedId,
                        name = "Semester 1",
                        tasks = parseTasks(raw.optJSONArray("tasks")),
                        subjects = parseSubjects(raw.optJSONArray("subjects")),
                        routine = parseRoutine(raw.optJSONArray("routine")),
                    )
                ),
                currentSemesterId = migratedId,
                settings = settings,
            )
        }

        val semesters = ArrayList<Semester>()
        for (i in 0 until sems.length()) {
            val o = sems.optJSONObject(i) ?: continue
            semesters += Semester(
                id = o.strOrNull("id") ?: ("sem_" + System.currentTimeMillis() + "_" + i),
                name = o.str("name").ifBlank { "Semester ${i + 1}" },
                startDate = o.str("startDate"),
                endDate = o.str("endDate"),
                tasks = parseTasks(o.optJSONArray("tasks")),
                subjects = parseSubjects(o.optJSONArray("subjects")),
                routine = parseRoutine(o.optJSONArray("routine")),
            )
        }
        if (semesters.isEmpty()) return initialData().copy(settings = settings)

        val requested = raw.str("currentSemesterId")
        val current = if (semesters.any { it.id == requested }) requested else semesters.first().id
        return AppData(semesters, current, settings)
    }

    // ---- writing -------------------------------------------------------------------------

    private fun nullable(value: String?): Any = value ?: JSONObject.NULL

    fun toJson(data: AppData): JSONObject {
        val semesters = JSONArray()
        data.semesters.forEach { sem ->
            val tasks = JSONArray()
            sem.tasks.forEach { t ->
                val subtasks = JSONArray()
                t.subtasks.forEach { st ->
                    subtasks.put(JSONObject().put("title", st.title).put("completed", st.completed))
                }
                tasks.put(
                    JSONObject()
                        .put("id", t.id)
                        .put("title", t.title)
                        .put("description", t.description)
                        .put("type", t.type)
                        .put("date", t.date)
                        .put("subject", t.subject)
                        .put("subtasks", subtasks)
                        .put("file", nullable(t.file))
                        .put("filePath", nullable(t.filePath))
                        .put("completed", t.completed)
                )
            }

            val subjects = JSONArray()
            sem.subjects.forEach { s ->
                subjects.put(
                    JSONObject()
                        .put("id", s.id)
                        .put("name", s.name)
                        .put("present", s.present)
                        .put("total", s.total)
                        .put("prof", s.prof)
                        .put("room", s.room)
                        .put("color", s.color)
                        .put("duration", s.duration)
                )
            }

            val routine = JSONArray()
            sem.routine.forEach { r ->
                routine.put(
                    JSONObject()
                        .put("id", r.id)
                        .put("day", r.day.toString()) // the web version stores the day as a string
                        .put("time", r.time)
                        .put("name", r.name)
                        .put("type", r.type)
                        .put("prof", r.prof)
                        .put("room", r.room)
                        .put("color", r.color)
                        .put("duration", r.duration)
                )
            }

            semesters.put(
                JSONObject()
                    .put("id", sem.id)
                    .put("name", sem.name)
                    .put("startDate", sem.startDate)
                    .put("endDate", sem.endDate)
                    .put("tasks", tasks)
                    .put("subjects", subjects)
                    .put("routine", routine)
            )
        }

        val settings = JSONObject()
            .put("darkMode", data.settings.darkMode)
            .put("syncId", data.settings.syncId)
            .put("lastSync", nullable(data.settings.lastSync))

        return JSONObject()
            .put("semesters", semesters)
            .put("currentSemesterId", data.currentSemesterId)
            .put("settings", settings)
    }

    fun toJsonString(data: AppData): String = toJson(data).toString(2)
}
