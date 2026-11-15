@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.studyflow.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.app.data.AppData
import com.studyflow.app.data.NEUTRAL_COLOR
import com.studyflow.app.data.RoutineItem
import com.studyflow.app.data.Subject
import com.studyflow.app.data.newId
import com.studyflow.app.ui.StudyFlowViewModel
import com.studyflow.app.ui.components.ConfirmDialog
import com.studyflow.app.ui.components.DropdownField
import com.studyflow.app.ui.components.FormSheet
import com.studyflow.app.ui.components.SfTextField
import com.studyflow.app.ui.components.TimeField
import com.studyflow.app.ui.components.NeutralColor
import com.studyflow.app.ui.components.addMinutes
import com.studyflow.app.ui.components.contrastOn
import com.studyflow.app.ui.components.formatTimeAmPm
import com.studyflow.app.ui.components.parseColorOr
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

private val DayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val DayFullNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

private const val FIRST_HOUR = 8
private const val LAST_HOUR = 18
private const val OTHER = "__other__"

/** Column index 0..6 (Mon..Sun) -> web day number (1..6, 0 = Sunday). */
private fun dayNumber(index: Int): Int = (index + 1) % 7

private val TimeColumnWidth = 44.dp

private data class RoutineTarget(val item: RoutineItem?, val day: Int, val time: String)

@Composable
fun RoutineScreen(data: AppData, vm: StudyFlowViewModel, padding: PaddingValues) {
    val sem = data.active
    val today = todayDayNumber()

    var weekView by rememberSaveable { mutableStateOf(false) }
    var target by remember { mutableStateOf<RoutineTarget?>(null) }
    var deleting by remember { mutableStateOf<RoutineItem?>(null) }

    val pagerState = rememberPagerState(initialPage = (today + 6) % 7, pageCount = { 7 })

    val onAdd: (Int, Int) -> Unit = { day, hour ->
        target = RoutineTarget(null, day, hour.toString().padStart(2, '0') + ":00")
    }
    val onEdit: (RoutineItem) -> Unit = { item -> target = RoutineTarget(item, item.day, item.time) }

    Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = Color.White,
                )
                SegmentedButton(
                    selected = !weekView,
                    onClick = { weekView = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = colors,
                ) { Text("Day") }
                SegmentedButton(
                    selected = weekView,
                    onClick = { weekView = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = colors,
                ) { Text("Week") }
            }

            if (weekView) {
                WeekView(
                    routine = sem.routine,
                    onAdd = onAdd,
                    onEdit = onEdit,
                    modifier = Modifier.weight(1f),
                )
            } else {
                DayView(
                    routine = sem.routine,
                    pagerState = pagerState,
                    onAdd = onAdd,
                    onEdit = onEdit,
                    onDelete = { deleting = it },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                val day = if (weekView) today else dayNumber(pagerState.currentPage)
                target = RoutineTarget(null, day, "")
            },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add Session", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    target?.let { t ->
        RoutineSheet(
            initial = t.item,
            presetDay = t.day,
            presetTime = t.time,
            subjects = sem.subjects,
            onDismiss = { target = null },
            onSave = { item ->
                if (t.item == null) vm.addRoutine(item) else vm.updateRoutine(t.item.id, item)
            },
            onRequestDelete = t.item?.let { existing -> { deleting = existing } },
        )
    }

    deleting?.let { item ->
        ConfirmDialog(
            title = "Delete session?",
            text = "“${item.name}” will be removed from your routine.",
            confirmText = "Delete",
            onConfirm = {
                vm.deleteRoutine(item.id)
                deleting = null
                if (target?.item?.id == item.id) target = null
            },
            onDismiss = { deleting = null },
        )
    }
}

// ---- day view ---------------------------------------------------------------------------------------------

@Composable
private fun DayView(
    routine: List<RoutineItem>,
    pagerState: PagerState,
    onAdd: (day: Int, hour: Int) -> Unit,
    onEdit: (RoutineItem) -> Unit,
    onDelete: (RoutineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val today = todayDayNumber()
    val primary = MaterialTheme.colorScheme.primary
    val chipShape = RoundedCornerShape(14.dp)

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DayNames.forEachIndexed { index, label ->
                val selected = pagerState.currentPage == index
                val isToday = dayNumber(index) == today
                val hasItems = routine.any { it.day == dayNumber(index) }
                Column(
                    Modifier
                        .weight(1f)
                        .clip(chipShape)
                        .background(if (selected) primary else Color.Transparent)
                        .then(if (!selected && isToday) Modifier.border(1.dp, primary, chipShape) else Modifier)
                        .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !hasItems -> Color.Transparent
                                    selected -> Color.White
                                    else -> primary
                                }
                            )
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { page ->
            val dayNum = dayNumber(page)
            val hourH = 72.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(end = 12.dp, bottom = 96.dp)
            ) {
                HourLabels(hourH)
                DayColumn(
                    dayNum = dayNum,
                    items = routine.filter { it.day == dayNum },
                    hourH = hourH,
                    compact = false,
                    isToday = dayNum == today,
                    showDelete = true,
                    onAdd = { hour -> onAdd(dayNum, hour) },
                    onEdit = onEdit,
                    onDelete = onDelete,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ---- week view ----------------------------------------------------------------------------------------------

@Composable
private fun WeekView(
    routine: List<RoutineItem>,
    onAdd: (day: Int, hour: Int) -> Unit,
    onEdit: (RoutineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val today = todayDayNumber()
    val columnWidth = 108.dp
    val hourH = 64.dp

    Column(modifier.fillMaxWidth()) {
        // Sticky header: scrolls sideways together with the grid, never vertically.
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(TimeColumnWidth))
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                DayNames.forEachIndexed { index, label ->
                    val isToday = dayNumber(index) == today
                    Box(
                        Modifier
                            .width(columnWidth)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(vertical)
                .padding(bottom = 96.dp)
        ) {
            HourLabels(hourH)
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                DayNames.indices.forEach { index ->
                    val dayNum = dayNumber(index)
                    DayColumn(
                        dayNum = dayNum,
                        items = routine.filter { it.day == dayNum },
                        hourH = hourH,
                        compact = true,
                        isToday = dayNum == today,
                        showDelete = false,
                        onAdd = { hour -> onAdd(dayNum, hour) },
                        onEdit = onEdit,
                        onDelete = {},
                        modifier = Modifier.width(columnWidth),
                    )
                }
            }
        }
    }
}

// ---- shared timeline pieces ---------------------------------------------------------------------------------

@Composable
private fun HourLabels(hourH: Dp) {
    Column(Modifier.width(TimeColumnWidth)) {
        for (hour in FIRST_HOUR..LAST_HOUR) {
            val ampm = if (hour >= 12) "pm" else "am"
            val h12 = if (hour > 12) hour - 12 else hour
            Box(Modifier.height(hourH).fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                Text(
                    "$h12 $ampm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 6.dp, top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    dayNum: Int,
    items: List<RoutineItem>,
    hourH: Dp,
    compact: Boolean,
    isToday: Boolean,
    showDelete: Boolean,
    onAdd: (hour: Int) -> Unit,
    onEdit: (RoutineItem) -> Unit,
    onDelete: (RoutineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = LAST_HOUR - FIRST_HOUR + 1
    val perMinute = hourH / 60
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    // Same placement rules as the web grid: the hour is clamped to the visible range and the
    // minutes / duration are scaled to the row height.
    val placed = items.sortedBy { it.time }.map { item ->
        val parts = item.time.split(":")
        val hour = (parts.getOrNull(0)?.toIntOrNull() ?: FIRST_HOUR).coerceIn(FIRST_HOUR, LAST_HOUR)
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val top = hourH * (hour - FIRST_HOUR) + perMinute * minute
        val height = maxOf(perMinute * item.duration, 28.dp)
        Triple(item, top, height)
    }
    val rowsHeight = hourH * hours
    val eventsBottom = placed.maxOfOrNull { it.second + it.third } ?: 0.dp
    val totalHeight = maxOf(rowsHeight, eventsBottom) + 16.dp

    Box(
        modifier
            .height(totalHeight)
            .then(if (isToday) Modifier.background(primary.copy(alpha = 0.05f)) else Modifier)
    ) {
        Column {
            for (hour in FIRST_HOUR..LAST_HOUR) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(hourH)
                        .drawBehind {
                            val w = 1.dp.toPx()
                            drawLine(lineColor, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = w)
                            if (compact) {
                                drawLine(lineColor, Offset(size.width, 0f), Offset(size.width, size.height), strokeWidth = w)
                            }
                        }
                        .clickable { onAdd(hour) }
                )
            }
        }

        placed.forEach { (item, top, height) ->
            RoutineEvent(
                item = item,
                top = top,
                height = height,
                compact = compact,
                showDelete = showDelete,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }

        if (isToday && !compact) {
            NowLine(hourH)
        }
    }
}

@Composable
private fun RoutineEvent(
    item: RoutineItem,
    top: Dp,
    height: Dp,
    compact: Boolean,
    showDelete: Boolean,
    onEdit: (RoutineItem) -> Unit,
    onDelete: (RoutineItem) -> Unit,
) {
    val bg = parseColorOr(item.color, NeutralColor)
    val fg = contrastOn(bg)
    val shape = RoundedCornerShape(if (compact) 8.dp else 12.dp)

    Box(
        Modifier
            .offset(y = top)
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 1.dp)
            .height(height - 2.dp)
            .clip(shape)
            .background(bg)
            .clickable { onEdit(item) }
            .padding(if (compact) 6.dp else 10.dp)
    ) {
        Column(Modifier.padding(end = if (showDelete) 22.dp else 0.dp)) {
            Text(
                item.name,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 12.sp else 14.sp,
                maxLines = if (compact) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (height >= 44.dp) {
                Text(
                    "${formatTimeAmPm(item.time)} – ${formatTimeAmPm(addMinutes(item.time, item.duration))}",
                    color = fg.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val details = listOf(item.room, item.prof).filter { it.isNotBlank() }.joinToString(" | ")
            if (details.isNotEmpty() && height >= 62.dp) {
                Text(
                    details,
                    color = fg.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showDelete) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onDelete(item) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Delete session", tint = fg, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** Thin red line marking the current time on today's column. */
@Composable
private fun NowLine(hourH: Dp) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = LocalTime.now()
        }
    }
    val minutesFromStart = (now.hour - FIRST_HOUR) * 60 + now.minute
    if (minutesFromStart < 0 || now.hour > LAST_HOUR) return

    val primary = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .offset(y = hourH / 60 * minutesFromStart - 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(primary))
        Box(Modifier.weight(1f).height(2.dp).background(primary))
    }
}

// ---- add / edit sheet ----------------------------------------------------------------------------------------

@Composable
private fun RoutineSheet(
    initial: RoutineItem?,
    presetDay: Int,
    presetTime: String,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (RoutineItem) -> Unit,
    onRequestDelete: (() -> Unit)?,
) {
    val initialSelection = when {
        initial == null -> ""
        subjects.any { it.name == initial.name } -> initial.name
        else -> OTHER
    }

    var day by remember { mutableStateOf(initial?.day ?: presetDay) }
    var time by remember { mutableStateOf(initial?.time ?: presetTime) }
    var selection by remember { mutableStateOf(initialSelection) }
    var custom by remember { mutableStateOf(if (initial != null && initialSelection == OTHER) initial.name else "") }
    var type by remember { mutableStateOf(initial?.type ?: "Class") }
    var error by remember { mutableStateOf<String?>(null) }

    val selectedSubject = subjects.firstOrNull { it.name == selection }

    FormSheet(
        title = if (initial == null) "Add Routine Item" else "Edit Routine Item",
        confirmText = if (initial == null) "Save Routine" else "Save Changes",
        onDismiss = onDismiss,
        error = error,
        extraAction = if (onRequestDelete != null) {
            {
                TextButton(onClick = onRequestDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete session", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        } else null,
        onConfirm = {
            val name = if (selection == OTHER) custom.trim() else selection
            if (name.isEmpty() || time.isEmpty()) {
                error = "Please select a course or enter a name, and choose a time."
                false
            } else {
                val subject = if (selection == OTHER) null else subjects.firstOrNull { it.name == name }
                var prof = subject?.prof ?: ""
                var room = subject?.room ?: ""
                if (type == "Self-Study") {
                    prof = ""
                    room = ""
                }
                onSave(
                    RoutineItem(
                        id = initial?.id ?: newId(),
                        day = day,
                        time = time,
                        name = name,
                        type = type,
                        prof = prof,
                        room = room,
                        color = subject?.color ?: NEUTRAL_COLOR,
                        duration = subject?.duration ?: 60,
                    )
                )
                true
            }
        },
    ) {
        DropdownField(
            label = "Day of the Week",
            selectedText = DayFullNames[(day + 6) % 7],
            options = DayNames.indices.map { dayNumber(it) to DayFullNames[it] },
            onSelect = { day = it },
        )
        TimeField(label = "Start Time", value = time, onChange = { time = it; error = null })
        DropdownField(
            label = "Course / Subject",
            selectedText = when (selection) {
                "" -> ""
                OTHER -> "Other (Custom Activity)"
                else -> selection
            },
            options = subjects.map { it.name to it.name } + (OTHER to "Other (Custom Activity)"),
            onSelect = {
                selection = it
                error = null
            },
        )
        if (selection == OTHER) {
            SfTextField(custom, { custom = it; error = null }, "Custom Class/Activity Name")
        }
        if (selectedSubject != null) {
            Text(
                "Auto-filling: ${selectedSubject.prof.ifBlank { "No Prof" }} | Room: ${selectedSubject.room.ifBlank { "No Room" }} | Duration: ${selectedSubject.duration} min(s)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(10.dp),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Session Type", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = Color.White,
                )
                SegmentedButton(
                    selected = type == "Class",
                    onClick = { type = "Class" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = colors,
                ) { Text("Class") }
                SegmentedButton(
                    selected = type == "Self-Study",
                    onClick = { type = "Self-Study" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = colors,
                ) { Text("Self-Study") }
            }
        }
    }
}
