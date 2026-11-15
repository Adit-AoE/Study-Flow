@file:OptIn(ExperimentalMaterial3Api::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.HowToReg
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyflow.app.data.AppData
import com.studyflow.app.ui.components.EmptyHint
import com.studyflow.app.ui.components.Pill
import com.studyflow.app.ui.components.SectionTitle
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.components.formatDateCustom
import com.studyflow.app.ui.components.formatTimeAmPm
import com.studyflow.app.ui.components.parseColorOr
import com.studyflow.app.ui.theme.SfColors
import java.time.LocalDate

/** Web "day of week" numbers: 0 = Sunday ... 6 = Saturday. */
fun todayDayNumber(): Int = LocalDate.now().dayOfWeek.value % 7

@Composable
fun DashboardScreen(data: AppData, padding: PaddingValues) {
    val sem = data.active

    val pending = sem.tasks.count { !it.completed }
    val totalClasses = sem.subjects.sumOf { it.total }
    val attended = sem.subjects.sumOf { it.present }
    val attendancePct = if (totalClasses == 0) 100 else Math.round(attended * 100f / totalClasses)
    val today = todayDayNumber()
    val todayRoutine = sem.routine.filter { it.day == today }.sortedBy { it.time }
    val deadlines = sem.tasks.filter { !it.completed && it.date.isNotEmpty() }.sortedBy { it.date }.take(3)

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                icon = Icons.Rounded.CheckCircle,
                value = pending.toString(),
                label = "Pending Tasks",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Rounded.HowToReg,
                value = "$attendancePct%",
                label = "Avg. Attendance",
                valueColor = if (attendancePct < 75) MaterialTheme.colorScheme.primary else SfColors.Green,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = Icons.Rounded.Today,
                value = todayRoutine.size.toString(),
                label = "Classes Today",
                modifier = Modifier.weight(1f),
            )
        }

        SfCard {
            SectionTitle(Icons.Rounded.HourglassEmpty, "Upcoming Deadlines")
            Spacer(Modifier.size(8.dp))
            if (deadlines.isEmpty()) {
                EmptyHint("No upcoming deadlines.")
            }
            deadlines.forEach { task ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Due: ${formatDateCustom(task.date)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Pill(
                        text = task.type,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        content = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        SfCard {
            SectionTitle(Icons.Rounded.Timer, "Today's Schedule")
            Spacer(Modifier.size(8.dp))
            if (todayRoutine.isEmpty()) {
                EmptyHint("No classes today.")
            }
            todayRoutine.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(parseColorOr(item.color, MaterialTheme.colorScheme.primary))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            item.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${formatTimeAmPm(item.time)} • ${item.type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    SfCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = valueColor, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
