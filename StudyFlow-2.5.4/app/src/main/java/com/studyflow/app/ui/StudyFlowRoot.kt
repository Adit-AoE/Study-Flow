@file:OptIn(ExperimentalMaterial3Api::class)

package com.studyflow.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.data.AppData
import com.studyflow.app.ui.screens.AttendanceScreen
import com.studyflow.app.ui.screens.CalendarScreen
import com.studyflow.app.ui.screens.CoursesScreen
import com.studyflow.app.ui.screens.DashboardScreen
import com.studyflow.app.ui.screens.RoutineScreen
import com.studyflow.app.ui.screens.SettingsScreen
import com.studyflow.app.ui.screens.TasksScreen
import com.studyflow.app.ui.screens.TimerScreen
import com.studyflow.app.ui.theme.StudyFlowTheme

@Composable
fun StudyFlowRoot(vm: StudyFlowViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val pendingImport by vm.pendingImport.collectAsStateWithLifecycle()
    val timerDone by vm.timerDone.collectAsStateWithLifecycle()

    var screen by rememberSaveable { mutableStateOf(Screen.Dashboard) }
    var railExpanded by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    BackHandler(enabled = railExpanded) { railExpanded = false }
    BackHandler(enabled = !railExpanded && screen != Screen.Dashboard) { screen = Screen.Dashboard }

    StudyFlowTheme(darkTheme = data.settings.darkMode) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxSize()) {
                // Room for the collapsed rail; the expanded rail floats above the content.
                Spacer(Modifier.width(RailCollapsedWidth))

                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        screen.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        data.active.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            actions = {
                                SemesterMenu(data = data, onSwitch = { vm.switchSemester(it) })
                                IconButton(onClick = { vm.toggleTheme() }) {
                                    Icon(
                                        if (data.settings.darkMode) Icons.Rounded.Brightness7 else Icons.Rounded.Brightness4,
                                        contentDescription = "Toggle theme",
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.background,
                                scrolledContainerColor = MaterialTheme.colorScheme.background,
                            ),
                        )
                    },
                    snackbarHost = { SnackbarHost(snackbar) },
                ) { padding ->
                    Crossfade(targetState = screen, label = "screen") { target ->
                        when (target) {
                            Screen.Dashboard -> DashboardScreen(data, padding)
                            Screen.Tasks -> TasksScreen(data, vm, padding)
                            Screen.Courses -> CoursesScreen(data, vm, padding)
                            Screen.Attendance -> AttendanceScreen(data, vm, padding)
                            Screen.Routine -> RoutineScreen(data, vm, padding)
                            Screen.Calendar -> CalendarScreen(data, padding)
                            Screen.Timer -> TimerScreen(vm, padding)
                            Screen.Settings -> SettingsScreen(data, vm, padding)
                        }
                    }
                }
            }

            RailScrim(visible = railExpanded, onDismiss = { railExpanded = false })

            SideRail(
                current = screen,
                expanded = railExpanded,
                onToggle = { railExpanded = !railExpanded },
                onSelect = {
                    screen = it
                    railExpanded = false
                },
                modifier = Modifier.align(Alignment.CenterStart),
            )
        }

        // Dialogs that can appear on top of any section.
        pendingImport?.let { pending ->
            AlertDialog(
                onDismissRequest = { vm.cancelImport() },
                title = { Text("Import Data Options", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Found ${pending.semesterCount} semester(s) in ${pending.fileName}.\n\n" +
                                "Would you like to merge this data with your existing semesters or replace all existing data?"
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { vm.confirmImport(ImportMode.Merge) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Merge with Old Data")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.confirmImport(ImportMode.Replace) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Replace Old Data")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = { TextButton(onClick = { vm.cancelImport() }) { Text("Cancel") } },
            )
        }

        if (timerDone) {
            AlertDialog(
                onDismissRequest = { vm.acknowledgeTimerDone() },
                title = { Text("Session completed!", fontWeight = FontWeight.Bold) },
                text = { Text("Time is up! Great focus.") },
                confirmButton = { TextButton(onClick = { vm.acknowledgeTimerDone() }) { Text("OK") } },
            )
        }
    }
}

@Composable
private fun SemesterMenu(data: AppData, onSwitch: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.School, contentDescription = "Switch semester", tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            data.semesters.forEach { sem ->
                DropdownMenuItem(
                    text = { Text(sem.name) },
                    onClick = {
                        menuOpen = false
                        onSwitch(sem.id)
                    },
                    trailingIcon = if (sem.id == data.currentSemesterId) {
                        { Icon(Icons.Rounded.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary) }
                    } else null,
                )
            }
        }
    }
}
