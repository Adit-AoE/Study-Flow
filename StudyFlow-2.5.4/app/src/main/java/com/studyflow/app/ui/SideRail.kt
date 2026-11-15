package com.studyflow.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MenuOpen
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class Screen(val title: String, val icon: ImageVector) {
    Dashboard("Dashboard", Icons.Rounded.Home),
    Tasks("Assignments", Icons.Rounded.CheckBox),
    Courses("Courses", Icons.Rounded.Book),
    Attendance("Attendance", Icons.Rounded.PieChart),
    Routine("Routine", Icons.Rounded.Schedule),
    Calendar("Calendar", Icons.Rounded.DateRange),
    Timer("Focus Timer", Icons.Rounded.Timer),
    Settings("Settings", Icons.Rounded.Settings),
}

val RailCollapsedWidth = 64.dp
private val RailExpandedWidth = 252.dp

/**
 * Icon-only navigation rail. The menu button expands it to show labels; picking a section (or
 * tapping outside / pressing back) collapses it again.
 */
@Composable
fun SideRail(
    current: Screen,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelect: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width by animateDpAsState(if (expanded) RailExpandedWidth else RailCollapsedWidth, label = "railWidth")

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .width(width)
            .shadow(if (expanded) 16.dp else 0.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical))
                    .padding(vertical = 8.dp),
            ) {
                // Header: menu button (+ app name when expanded)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggle, modifier = Modifier.padding(start = 8.dp).size(48.dp)) {
                        Icon(
                            if (expanded) Icons.Rounded.MenuOpen else Icons.Rounded.Menu,
                            contentDescription = if (expanded) "Collapse menu" else "Expand menu",
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Study Flow",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Screen.entries.forEach { screen ->
                        RailItem(
                            screen = screen,
                            selected = screen == current,
                            expanded = expanded,
                            onClick = { onSelect(screen) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RailItem(screen: Screen, selected: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            screen.icon,
            contentDescription = screen.title,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        if (expanded) {
            Spacer(Modifier.width(16.dp))
            Text(
                screen.title,
                color = tint,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

/** Dimmed layer behind the expanded rail; tapping it collapses the rail. */
@Composable
fun RailScrim(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
        )
    }
}
