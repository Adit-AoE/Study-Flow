@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studyflow.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.AppData
import com.studyflow.app.data.Semester
import com.studyflow.app.data.semesterError
import com.studyflow.app.notify.Reminders
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.DateField
import com.studyflow.app.ui.components.EmptyHint
import com.studyflow.app.ui.components.FormSheet
import com.studyflow.app.ui.components.Pill
import com.studyflow.app.ui.components.SectionTitle
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.components.SfTextField
import com.studyflow.app.ui.components.formatDateCustom
import com.studyflow.app.ui.theme.SfColors
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(data: AppData, vm: StudyFlowViewModel, padding: PaddingValues) {
    val context = LocalContext.current
    val syncing by vm.syncing.collectAsStateWithLifecycle()

    var semesterSheet by remember { mutableStateOf(false) }
    var editingSemester by remember { mutableStateOf<Semester?>(null) }
    var deletingSemester by remember { mutableStateOf<Semester?>(null) }
    var confirmResetSemester by remember { mutableStateOf(false) }
    var confirmResetAll by remember { mutableStateOf(false) }

    var syncId by remember(data.settings.syncId) { mutableStateOf(data.settings.syncId) }
    var alertsOn by remember { mutableStateOf(Reminders.notificationsEnabled(context)) }
    var permissionDenied by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) vm.exportTo(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.prepareImport(uri)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        alertsOn = Reminders.notificationsEnabled(context)
        if (granted && alertsOn) {
            permissionDenied = false
            Reminders.notifyEnabled(context)
        } else {
            permissionDenied = true
            Toast.makeText(context, "Alerts are off. You can turn them on any time from this button.", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ---- semesters ----
        SfCard {
            SectionTitle(Icons.Rounded.School, "Semester Management")
            Spacer(Modifier.height(8.dp))
            Text(
                "Switch between semesters or create a new blank semester with custom start and end dates.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    editingSemester = null
                    semesterSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Semester", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                data.semesters.forEach { sem ->
                    SemesterRow(
                        sem = sem,
                        isActive = sem.id == data.currentSemesterId,
                        onSwitch = { vm.switchSemester(sem.id) },
                        onEdit = {
                            editingSemester = sem
                            semesterSheet = true
                        },
                        onDelete = {
                            if (data.semesters.size <= 1) vm.deleteSemester(sem.id) else deletingSemester = sem
                        },
                    )
                }
            }
        }

        // ---- sync (simulated, same as the web version) ----
        SfCard {
            SectionTitle(Icons.Rounded.Cloud, "Cross-Platform Sync")
            Spacer(Modifier.height(8.dp))
            Text(
                "Sync your data across mobile and desktop devices securely.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            SfTextField(
                value = syncId,
                onValueChange = { syncId = it },
                label = "Sync ID",
                placeholder = "Enter Sync ID",
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { vm.sync(syncId) }),
            )
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = { vm.sync(syncId) },
                enabled = !syncing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                if (syncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Syncing...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sync Now", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                data.settings.lastSync?.let { "Last synced: $it" } ?: "No data synced yet.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (data.settings.lastSync != null) SfColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---- local data ----
        SfCard {
            SectionTitle(Icons.Rounded.FileDownload, "Local Data Management")
            Spacer(Modifier.height(8.dp))
            Text(
                "Backup your data to a file or restore it on another device (with merge or replace options).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
                        exportLauncher.launch("StudyFlow_backup_$stamp.json")
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Export", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import", fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Backups use the same format as the web version. Attached files are not included in a backup.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---- notifications ----
        SfCard {
            SectionTitle(Icons.Rounded.Notifications, "Notifications")
            Spacer(Modifier.height(8.dp))
            Text(
                "Get alerts for due assignments and 15-minute reminders before classes start.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (alertsOn) "Alerts are on." else "Alerts are off.",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (alertsOn) SfColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    alertsOn = Reminders.notificationsEnabled(context)
                    when {
                        alertsOn -> Reminders.notifyEnabled(context)
                        permissionDenied -> openNotificationSettings(context)
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else -> openNotificationSettings(context)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (alertsOn) "Send a Test Alert" else "Enable Alerts", fontWeight = FontWeight.Bold)
            }
            if (!Reminders.canScheduleExact(context)) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { openExactAlarmSettings(context) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Allow Exact Reminder Timing", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Without this, Android may deliver class reminders a few minutes late.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---- danger zone ----
        SfCard {
            Text(
                "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Reset only the currently active semester's data or wipe everything across the entire app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { confirmResetSemester = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset Current Semester", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { confirmResetAll = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reset All Data", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // ---- sheets & dialogs ----
    if (semesterSheet) {
        SemesterSheet(
            initial = editingSemester,
            onDismiss = { semesterSheet = false },
            onSave = { name, start, end ->
                val current = editingSemester
                if (current == null) vm.addSemester(name, start, end) else vm.editSemester(current.id, name, start, end)
            },
        )
    }

    deletingSemester?.let { sem ->
        ConfirmDialog(
            title = "Delete semester?",
            text = "Delete “${sem.name}” and all of its courses, tasks, and routines?",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteSemester(sem.id)
                deletingSemester = null
            },
            onDismiss = { deletingSemester = null },
        )
    }

    if (confirmResetSemester) {
        ConfirmDialog(
            title = "Reset current semester?",
            text = "Are you sure you want to reset all courses, assignments, attendance, and routines in “${data.active.name}”? Other semesters will not be affected.",
            confirmText = "Reset",
            onConfirm = {
                vm.resetCurrentSemester()
                confirmResetSemester = false
            },
            onDismiss = { confirmResetSemester = false },
        )
    }

    if (confirmResetAll) {
        ConfirmDialog(
            title = "Reset all data?",
            text = "Are you sure you want to wipe ALL semesters and settings? This cannot be undone.",
            confirmText = "Wipe everything",
            onConfirm = {
                vm.resetAll()
                confirmResetAll = false
            },
            onDismiss = { confirmResetAll = false },
        )
    }
}

@Composable
private fun SemesterRow(
    sem: Semester,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val dates = if (sem.startDate.isNotEmpty() || sem.endDate.isNotEmpty()) {
        val s = if (sem.startDate.isNotEmpty()) formatDateCustom(sem.startDate) else "Unset"
        val e = if (sem.endDate.isNotEmpty()) formatDateCustom(sem.endDate) else "Unset"
        "$s → $e"
    } else {
        "No dates set"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                sem.name,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (isActive) {
                Spacer(Modifier.width(8.dp))
                Pill("Active", SfColors.Green.copy(alpha = 0.15f), SfColors.Green)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "$dates  •  ${sem.subjects.size} Courses  •  ${sem.tasks.size} Tasks",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (!isActive) {
                OutlinedButton(onClick = onSwitch, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("Switch", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(4.dp))
            }
            IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit semester") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete semester", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SemesterSheet(
    initial: Semester?,
    onDismiss: () -> Unit,
    onSave: (name: String, start: String, end: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var start by remember { mutableStateOf(initial?.startDate ?: "") }
    var end by remember { mutableStateOf(initial?.endDate ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    FormSheet(
        title = if (initial == null) "Create New Semester" else "Edit Semester",
        confirmText = if (initial == null) "Create Semester" else "Save Changes",
        onDismiss = onDismiss,
        error = error,
        onConfirm = {
            val problem = semesterError(name, start, end)
            if (problem != null) {
                error = problem
                false
            } else {
                onSave(name, start, end)
                true
            }
        },
    ) {
        SfTextField(name, { name = it; error = null }, "Semester Name", placeholder = "e.g., Semester 6")
        DateField(label = "Start Date (Optional)", value = start, onChange = { start = it; error = null })
        DateField(label = "End Date (Optional)", value = end, onChange = { end = it; error = null })
    }
}

private fun openNotificationSettings(context: Context) {
    val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open notification settings.", Toast.LENGTH_SHORT).show()
    }
}

private fun openExactAlarmSettings(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open alarm settings.", Toast.LENGTH_SHORT).show()
    }
}
