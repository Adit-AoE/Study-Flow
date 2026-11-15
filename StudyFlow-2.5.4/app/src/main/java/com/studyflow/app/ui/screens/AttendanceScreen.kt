@file:OptIn(ExperimentalMaterial3Api::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.studyflow.app.data.AppData
import com.studyflow.app.data.Subject
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.EmptyScreen
import com.studyflow.app.ui.components.SfCard
import com.studyflow.app.ui.theme.SfColors

@Composable
fun AttendanceScreen(data: AppData, vm: StudyFlowViewModel, padding: PaddingValues) {
    val subjects = data.active.subjects

    Box(Modifier.fillMaxSize().padding(padding)) {
        if (subjects.isEmpty()) {
            EmptyScreen(Icons.Rounded.PieChart, "No courses to track", "Add a course first, then mark attendance here.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(subjects, key = { it.id }) { subject ->
                    AttendanceCard(
                        subject = subject,
                        onPresent = { vm.markAttendance(subject.id, true) },
                        onAbsent = { vm.markAttendance(subject.id, false) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AttendanceCard(subject: Subject, onPresent: () -> Unit, onAbsent: () -> Unit) {
    val pct = if (subject.total == 0) 100 else Math.round(subject.present * 100f / subject.total)
    val statusColor = if (pct < 75) MaterialTheme.colorScheme.primary else SfColors.Green

    SfCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                subject.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text("$pct%", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = statusColor)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { pct.coerceIn(0, 100) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round,
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Present: ${subject.present}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Total: ${subject.total}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onPresent,
                modifier = Modifier.weight(1f).height(48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SfColors.Green),
                border = androidx.compose.foundation.BorderStroke(1.dp, SfColors.Green),
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Present", fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(
                onClick = onAbsent,
                modifier = Modifier.weight(1f).height(48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Absent", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
