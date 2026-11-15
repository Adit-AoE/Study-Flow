@file:OptIn(ExperimentalMaterial3Api::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.studyflow.app.data.AppData
import com.studyflow.app.data.DEFAULT_COLOR
import com.studyflow.app.data.Subject
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.ColorPicker
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.EmptyScreen
import com.studyflow.app.ui.components.FormSheet
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.components.SfTextField
import com.studyflow.app.ui.components.parseColorOr
import com.studyflow.app.ui.components.parseColorOrNull

@Composable
fun CoursesScreen(data: AppData, vm: StudyFlowViewModel, padding: PaddingValues) {
    val subjects = data.active.subjects

    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Subject?>(null) }
    var deleting by remember { mutableStateOf<Subject?>(null) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (subjects.isEmpty()) {
            EmptyScreen(Icons.Rounded.Book, "No courses yet", "Tap “Add Course” to create your first one.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(subjects, key = { it.id }) { subject ->
                    CourseCard(
                        subject = subject,
                        onEdit = {
                            editing = subject
                            sheetOpen = true
                        },
                        onDelete = { deleting = subject },
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                editing = null
                sheetOpen = true
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add Course", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (sheetOpen) {
        CourseSheet(
            initial = editing,
            onDismiss = { sheetOpen = false },
            onSave = { name, prof, room, color, duration ->
                val current = editing
                if (current == null) {
                    vm.addSubject(name, prof, room, color, duration)
                } else {
                    vm.editSubject(current.id, name, prof, room, color, duration)
                }
            },
        )
    }

    deleting?.let { subject ->
        ConfirmDialog(
            title = "Delete subject?",
            text = "“${subject.name}” and its attendance record will be removed.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteSubject(subject.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun CourseCard(subject: Subject, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val accent = parseColorOr(subject.color, MaterialTheme.colorScheme.primary)

    SfCard(contentPadding = PaddingValues(0.dp)) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent)
            )
            Column(Modifier.weight(1f).padding(start = 16.dp, top = 14.dp, bottom = 16.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                CourseLine(Icons.Rounded.Person, subject.prof.ifEmpty { "No Professor" })
                CourseLine(Icons.Rounded.MeetingRoom, subject.room.ifEmpty { "No Room" })
                CourseLine(Icons.Rounded.AccessTime, "${subject.duration} mins")
            }
            Box(Modifier.padding(top = 4.dp, end = 4.dp)) {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Course options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseLine(icon: ImageVector, text: String) {
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CourseSheet(
    initial: Subject?,
    onDismiss: () -> Unit,
    onSave: (name: String, prof: String, room: String, color: String, duration: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var prof by remember { mutableStateOf(initial?.prof ?: "") }
    var room by remember { mutableStateOf(initial?.room ?: "") }
    var duration by remember { mutableStateOf(initial?.duration?.toString() ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: DEFAULT_COLOR) }
    var error by remember { mutableStateOf<String?>(null) }

    FormSheet(
        title = if (initial == null) "Add Course" else "Edit Course",
        confirmText = if (initial == null) "Save Course" else "Save Changes",
        onDismiss = onDismiss,
        error = error,
        onConfirm = {
            val parsed = parseColorOrNull(color)
            when {
                name.isBlank() -> {
                    error = "Please enter a course name"
                    false
                }
                parsed == null -> {
                    error = "Enter a valid color, like #dc2626"
                    false
                }
                else -> {
                    val minutes = duration.trim().toIntOrNull()?.takeIf { it > 0 } ?: 60
                    onSave(name.trim(), prof.trim(), room.trim(), color.trim().lowercase(), minutes)
                    true
                }
            }
        },
    ) {
        SfTextField(name, { name = it; error = null }, "Course Name", placeholder = "e.g., Data Structures")
        SfTextField(prof, { prof = it }, "Professor Name", placeholder = "e.g., Dr. Smith")
        SfTextField(room, { room = it }, "Room Number", placeholder = "e.g., Room 402")
        SfTextField(
            duration,
            { duration = it.filter { ch -> ch.isDigit() }.take(3) },
            "Lecture Duration (in minutes)",
            placeholder = "e.g., 50 or 90",
            keyboardType = KeyboardType.Number,
        )
        ColorPicker(hex = color, onChange = { color = it; error = null })
    }
}
