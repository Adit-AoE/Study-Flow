@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.SfTextField

private val Presets = listOf(
    Triple(25, "Pomodoro", "25m"),
    Triple(5, "Short Break", "5m"),
    Triple(15, "Long Break", "15m"),
)

@Composable
fun TimerScreen(vm: StudyFlowViewModel, padding: PaddingValues) {
    val timer by vm.timer.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var custom by remember { mutableStateOf("") }

    // Keep the screen awake while a session is running.
    val view = LocalView.current
    DisposableEffect(timer.running) {
        view.keepScreenOn = timer.running
        onDispose { view.keepScreenOn = false }
    }

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val fraction = if (timer.durationSec == 0) 0f else timer.remainingSec.toFloat() / timer.durationSec

    fun applyCustom() {
        val minutes = custom.trim().toIntOrNull()
        if (minutes != null && minutes > 0) {
            vm.setTimer(minutes)
            custom = ""
            focus.clearFocus()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Presets.forEach { (minutes, name, short) ->
                FilterChip(
                    selected = timer.durationSec == minutes * 60,
                    onClick = { vm.setTimer(minutes) },
                    label = { Text("$name ($short)") },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SfTextField(
                value = custom,
                onValueChange = { custom = it.filter { ch -> ch.isDigit() }.take(3) },
                label = "Custom (min)",
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { applyCustom() }),
            )
            FilledTonalButton(onClick = { applyCustom() }, modifier = Modifier.height(56.dp)) { Text("Set Custom") }
        }

        // Progress ring with the countdown in the middle
        Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            val m = (timer.remainingSec / 60).toString().padStart(2, '0')
            val s = (timer.remainingSec % 60).toString().padStart(2, '0')
            Text(
                "$m:$s",
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = primary,
                maxLines = 1,
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { vm.startTimer() },
                enabled = !timer.running,
                modifier = Modifier.weight(1f).height(52.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Start", fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(
                onClick = { vm.pauseTimer() },
                enabled = timer.running,
                modifier = Modifier.weight(1f).height(52.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Pause", fontWeight = FontWeight.Bold, maxLines = 1)
            }
            OutlinedButton(
                onClick = { vm.resetTimer() },
                modifier = Modifier.weight(1f).height(52.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reset", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
