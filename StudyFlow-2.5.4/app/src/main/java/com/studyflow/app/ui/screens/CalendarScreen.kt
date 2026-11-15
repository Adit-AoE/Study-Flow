@file:OptIn(ExperimentalMaterial3Api::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyflow.app.data.AppData
import com.studyflow.app.data.Subject
import com.studyflow.app.data.Task
import com.studyflow.app.ui.components.EmptyHint
import com.studyflow.app.ui.components.Pill
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.components.contrastOn
import com.studyflow.app.ui.components.formatDateCustom
import com.studyflow.app.ui.components.parseColorOrNull
import com.studyflow.app.ui.theme.SfColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val WeekdayHeads = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")

@Composable
fun CalendarScreen(data: AppData, padding: PaddingValues) {
    val sem = data.active
    val today = LocalDate.now()
    val primary = MaterialTheme.colorScheme.primary

    // Saved as "yyyy-MM" / "yyyy-MM-dd" strings so they survive rotation.
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    val month = YearMonth.parse(monthText)
    val selected = selectedText

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SfCard(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month")
                }
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                )
                IconButton(onClick = { monthText = month.plusMonths(1).toString() }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month")
                }
            }

            // Semester range info
            Spacer(Modifier.height(2.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (sem.startDate.isNotEmpty() || sem.endDate.isNotEmpty()) {
                    val start = if (sem.startDate.isNotEmpty()) formatDateCustom(sem.startDate) else "Not Set"
                    val end = if (sem.endDate.isNotEmpty()) formatDateCustom(sem.endDate) else "Not Set"
                    Pill(sem.name, SfColors.Green.copy(alpha = 0.15f), SfColors.Green)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$start → $end",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                } else {
                    Pill(sem.name, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "No semester dates set",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Weekday header
            Row(Modifier.fillMaxWidth()) {
                WeekdayHeads.forEach { head ->
                    Text(
                        head,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Month grid
            val leading = month.atDay(1).dayOfWeek.value - 1 // Monday = 0
            val cells: List<LocalDate?> = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map<Int, LocalDate?> { month.atDay(it) }
            val rows = cells.chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                rows.forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 0 until 7) {
                            val date = week.getOrNull(i)
                            if (date == null) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                DayCell(
                                    date = date,
                                    isToday = date == today,
                                    isSelected = date.toString() == selected,
                                    isWeekend = i >= 5,
                                    inSemester = sem.startDate.isNotEmpty() && sem.endDate.isNotEmpty() &&
                                        date.toString() >= sem.startDate && date.toString() <= sem.endDate,
                                    isSemStart = sem.startDate.isNotEmpty() && date.toString() == sem.startDate,
                                    isSemEnd = sem.endDate.isNotEmpty() && date.toString() == sem.endDate,
                                    tasks = sem.tasks.filter { it.date == date.toString() },
                                    subjects = sem.subjects,
                                    onClick = { selectedText = date.toString() },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Agenda for the selected day
        val dayTasks = sem.tasks.filter { it.date == selected }
        SfCard {
            Text(
                formatDateCustom(selected),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(8.dp))

            if (sem.startDate == selected) {
                MarkerRow(Icons.Rounded.PlayArrow, "Semester start", SfColors.Green, Color.White)
            }
            if (sem.endDate == selected) {
                MarkerRow(Icons.Rounded.Flag, "Semester end", SfColors.Amber, Color.Black)
            }
            if (dayTasks.isEmpty()) {
                if (sem.startDate != selected && sem.endDate != selected) EmptyHint("Nothing due on this day.")
            }
            dayTasks.forEach { task ->
                val subject = sem.subjects.firstOrNull { it.name == task.subject }
                val color = parseColorOrNull(subject?.color) ?: primary
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(color))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(task.type, task.subject).filter { it.isNotEmpty() }.joinToString(" • "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (task.completed) {
                        Pill("Done", SfColors.Green.copy(alpha = 0.15f), SfColors.Green)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkerRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, container: Color, content: Color) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Pill(text, container, content, icon = icon)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    isWeekend: Boolean,
    inSemester: Boolean,
    isSemStart: Boolean,
    isSemEnd: Boolean,
    tasks: List<Task>,
    subjects: List<Subject>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(10.dp)

    val background = when {
        isToday -> primary.copy(alpha = 0.14f)
        isSemStart -> SfColors.Green.copy(alpha = 0.14f)
        isSemEnd -> SfColors.Amber.copy(alpha = 0.14f)
        isWeekend -> primary.copy(alpha = 0.05f)
        inSemester -> SfColors.Green.copy(alpha = 0.04f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.onSurface
        isToday -> primary
        isSemStart -> SfColors.Green
        isSemEnd -> SfColors.Amber
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isSelected || isToday || isSemStart || isSemEnd) 2.dp else 1.dp
    val numberColor = when {
        isToday || isWeekend -> primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier
            .aspectRatio(0.78f)
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = numberColor,
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        // One dot per task (up to three), coloured like the course.
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            tasks.take(3).forEach { task ->
                val subject = subjects.firstOrNull { it.name == task.subject }
                val dot = parseColorOrNull(subject?.color) ?: primary
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(dot)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        if (tasks.size > 3) {
            Text("+${tasks.size - 3}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
