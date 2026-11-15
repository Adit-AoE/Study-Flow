@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studyflow.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.studyflow.app.data.AppData
import com.studyflow.app.data.Storage
import com.studyflow.app.data.Subject
import com.studyflow.app.data.Subtask
import com.studyflow.app.data.Task
import com.studyflow.app.data.newId
import com.studyflow.app.ui.PickedFile
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.DateField
import com.studyflow.app.ui.components.DropdownField
import com.studyflow.app.ui.components.EmptyScreen
import com.studyflow.app.ui.components.FormSheet
import com.studyflow.app.ui.components.MiniCheck
import com.studyflow.app.ui.components.Pill
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.components.SfTextField
import com.studyflow.app.ui.components.contrastOn
import com.studyflow.app.ui.components.formatDateCustom
import com.studyflow.app.ui.components.parseColorOrNull
import java.io.File

private val TaskTypes = listOf("Assignment" to "Assignment", "Quiz" to "Test/Quiz", "Task" to "General Task")

@Composable
fun TasksScreen(data: AppData, vm: StudyFlowViewModel, padding: PaddingValues) {
    val sem = data.active
    val context = LocalContext.current

    var sheetOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Task?>(null) }
    var deleting by remember { mutableStateOf<Task?>(null) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (sem.tasks.isEmpty()) {
            EmptyScreen(Icons.Rounded.CheckCircle, "No tasks yet", "Tap “New Task” to add your first assignment.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(sem.tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        subjects = sem.subjects,
                        onToggle = { vm.toggleTask(task.id) },
                        onToggleSubtask = { idx -> vm.toggleSubtask(task.id, idx) },
                        onEdit = {
                            editing = task
                            sheetOpen = true
                        },
                        onDelete = { deleting = task },
                        onOpenFile = { openAttachment(context, task) },
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
            text = { Text("New Task", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (sheetOpen) {
        TaskSheet(
            initial = editing,
            subjects = sem.subjects,
            onDismiss = { sheetOpen = false },
            onSave = { draft, picked, remove -> vm.saveTask(editing?.id, draft, picked, remove) },
        )
    }

    deleting?.let { task ->
        ConfirmDialog(
            title = "Delete task?",
            text = "“${task.title}” will be removed.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteTask(task.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

private fun openAttachment(context: android.content.Context, task: Task) {
    val stored = task.filePath
    if (stored.isNullOrEmpty()) {
        Toast.makeText(context, "This file was only attached on another device.", Toast.LENGTH_SHORT).show()
        return
    }
    val file: File = Storage.attachmentFile(context, stored)
    if (!file.exists()) {
        Toast.makeText(context, "The attached file is no longer available.", Toast.LENGTH_SHORT).show()
        return
    }
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val ext = (task.file ?: stored).substringAfterLast('.', "").lowercase()
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app can open this file.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun TaskCard(
    task: Task,
    subjects: List<Subject>,
    onToggle: () -> Unit,
    onToggleSubtask: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenFile: () -> Unit,
) {
    val subject = subjects.firstOrNull { it.name == task.subject }
    var menuOpen by remember { mutableStateOf(false) }

    SfCard(
        modifier = Modifier.alpha(if (task.completed) 0.55f else 1f),
        contentPadding = PaddingValues(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f).padding(top = 12.dp)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (task.subject.isNotEmpty()) {
                        val bg = parseColorOrNull(subject?.color)
                        Pill(
                            text = task.subject,
                            container = bg ?: MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            content = if (bg != null) contrastOn(bg) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${task.type} • ${formatDateCustom(task.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 3.dp),
                    )
                    if (!task.file.isNullOrEmpty()) {
                        Pill(
                            text = task.file,
                            container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            content = MaterialTheme.colorScheme.onSurface,
                            icon = Icons.Rounded.AttachFile,
                            uppercase = false,
                            onClick = onOpenFile,
                        )
                    }
                }
                if (task.description.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Task options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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

        if (task.subtasks.isNotEmpty()) {
            Column(Modifier.padding(start = 52.dp, end = 12.dp, top = 4.dp)) {
                task.subtasks.forEachIndexed { index, st ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSubtask(index) }
                            .padding(vertical = 8.dp)
                            .alpha(if (st.completed) 0.5f else 1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MiniCheck(checked = st.completed, size = 18.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            st.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (st.completed) TextDecoration.LineThrough else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskSheet(
    initial: Task?,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (draft: Task, picked: PickedFile?, removeFile: Boolean) -> Unit,
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: "Assignment") }
    var subject by remember { mutableStateOf(initial?.subject ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: "") }
    var subtasks by remember { mutableStateOf(initial?.subtasks ?: emptyList<Subtask>()) }
    var newSubtask by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf<PickedFile?>(null) }
    var removeFile by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            picked = PickedFile(uri, Storage.displayName(context, uri))
            removeFile = false
        }
    }

    val existingFile = if (!removeFile) initial?.file else null
    val attachmentName = picked?.name ?: existingFile

    FormSheet(
        title = if (initial == null) "Add Task" else "Edit Task",
        confirmText = if (initial == null) "Save Task" else "Save Changes",
        onDismiss = onDismiss,
        error = error,
        onConfirm = {
            if (title.isBlank()) {
                error = "Please enter a task title"
                false
            } else {
                val pendingSub = newSubtask.trim()
                val finalSubtasks = if (pendingSub.isNotEmpty()) subtasks + Subtask(pendingSub) else subtasks
                val draft = Task(
                    id = initial?.id ?: newId(),
                    title = title.trim(),
                    description = description.trim(),
                    type = type,
                    date = date,
                    subject = subject,
                    subtasks = finalSubtasks,
                    file = initial?.file,
                    filePath = initial?.filePath,
                    completed = initial?.completed ?: false,
                )
                onSave(draft, picked, removeFile)
                true
            }
        },
    ) {
        SfTextField(title, { title = it; error = null }, "Task Title", placeholder = "e.g., Math Assignment")
        SfTextField(description, { description = it }, "Description (Optional)", placeholder = "Brief description")
        DropdownField(
            label = "Task Type",
            selectedText = TaskTypes.firstOrNull { it.first == type }?.second ?: type,
            options = TaskTypes,
            onSelect = { type = it },
        )
        DropdownField(
            label = "Course / Subject",
            selectedText = if (subject.isEmpty()) "None / General" else subject,
            options = listOf("" to "None / General") + subjects.map { it.name to it.name },
            onSelect = { subject = it },
        )
        DateField(label = "Deadline (Optional)", value = date, onChange = { date = it })

        // Subtasks
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Subtasks", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            subtasks.forEachIndexed { index, st ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(st.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { subtasks = subtasks.filterIndexed { i, _ -> i != index } }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove subtask", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SfTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    label = "New subtask",
                    modifier = Modifier.weight(1f),
                    imeAction = ImeAction.Done,
                )
                OutlinedButton(
                    onClick = {
                        val text = newSubtask.trim()
                        if (text.isNotEmpty()) {
                            subtasks = subtasks + Subtask(text)
                            newSubtask = ""
                        }
                    },
                ) { Text("Add") }
            }
        }

        // Attachment
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Attach File", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (attachmentName != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        attachmentName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    IconButton(onClick = {
                        if (picked != null) picked = null else removeFile = true
                    }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove attachment", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                OutlinedButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Choose a file")
                }
            }
        }
    }
}
