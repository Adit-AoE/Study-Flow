package com.studyflow.app.data

import java.util.UUID

const val DEFAULT_COLOR = "#dc2626"
const val NEUTRAL_COLOR = "#64748b"

fun newId(): String = UUID.randomUUID().toString()

data class Subtask(
    val title: String,
    val completed: Boolean = false,
)

data class Task(
    val id: String = newId(),
    val title: String,
    val description: String = "",
    /** "Assignment", "Quiz" or "Task" (same values as the web version). */
    val type: String = "Assignment",
    /** ISO date (yyyy-MM-dd) or empty when there is no deadline. */
    val date: String = "",
    /** Course name, or empty for "None / General". */
    val subject: String = "",
    val subtasks: List<Subtask> = emptyList(),
    /** Display name of the attached file (this is all the web version stores). */
    val file: String? = null,
    /** Name of the copy kept in app storage (only exists for files attached on this device). */
    val filePath: String? = null,
    val completed: Boolean = false,
)

data class Subject(
    val id: String = newId(),
    val name: String,
    val present: Int = 0,
    val total: Int = 0,
    val prof: String = "",
    val room: String = "",
    val color: String = DEFAULT_COLOR,
    val duration: Int = 60,
)

data class RoutineItem(
    val id: String = newId(),
    /** 0 = Sunday ... 6 = Saturday (JavaScript getDay(), as in the web version). */
    val day: Int,
    /** 24h "HH:mm". */
    val time: String,
    val name: String,
    /** "Class" or "Self-Study". */
    val type: String = "Class",
    val prof: String = "",
    val room: String = "",
    val color: String = NEUTRAL_COLOR,
    val duration: Int = 60,
)

data class Semester(
    val id: String,
    val name: String,
    val startDate: String = "",
    val endDate: String = "",
    val tasks: List<Task> = emptyList(),
    val subjects: List<Subject> = emptyList(),
    val routine: List<RoutineItem> = emptyList(),
)

data class Settings(
    val darkMode: Boolean = true,
    val syncId: String = "",
    val lastSync: String? = null,
)

data class AppData(
    val semesters: List<Semester>,
    val currentSemesterId: String,
    val settings: Settings = Settings(),
) {
    val active: Semester
        get() = semesters.firstOrNull { it.id == currentSemesterId } ?: semesters.first()
}

fun initialData(): AppData {
    val id = "sem_1"
    return AppData(
        semesters = listOf(Semester(id = id, name = "Semester 1")),
        currentSemesterId = id,
        settings = Settings(),
    )
}

/** Returns an error message, or null when the semester details are valid. */
fun semesterError(name: String, start: String, end: String): String? = when {
    name.isBlank() -> "Please enter a semester name."
    start.isNotEmpty() && end.isNotEmpty() && end < start -> "End date cannot be earlier than start date."
    else -> null
}

/** Applies [transform] to the active semester. */
fun AppData.updateActive(transform: (Semester) -> Semester): AppData {
    val activeId = active.id
    return copy(semesters = semesters.map { if (it.id == activeId) transform(it) else it })
}

/** Imported data can't point at attachment files that live on another device. */
fun AppData.withoutLocalFiles(): AppData = copy(
    semesters = semesters.map { sem ->
        sem.copy(tasks = sem.tasks.map { it.copy(filePath = null) })
    }
)

/** Port of mergeSemesterArrays() from the web version. */
fun mergeSemester(target: Semester, source: Semester): Semester {
    val start = if (target.startDate.isEmpty() && source.startDate.isNotEmpty()) source.startDate else target.startDate
    val end = if (target.endDate.isEmpty() && source.endDate.isNotEmpty()) source.endDate else target.endDate

    val subjects = target.subjects.toMutableList()
    source.subjects.forEach { sub ->
        if (subjects.none { it.name.equals(sub.name, ignoreCase = true) }) subjects += sub
    }

    val tasks = target.tasks.toMutableList()
    source.tasks.forEach { task ->
        val exists = tasks.any { it.title == task.title && it.date == task.date && it.type == task.type }
        if (!exists) tasks += task
    }

    val routine = target.routine.toMutableList()
    source.routine.forEach { rt ->
        val exists = routine.any { it.day == rt.day && it.time == rt.time && it.name == rt.name }
        if (!exists) routine += rt
    }

    return target.copy(startDate = start, endDate = end, subjects = subjects, tasks = tasks, routine = routine)
}
