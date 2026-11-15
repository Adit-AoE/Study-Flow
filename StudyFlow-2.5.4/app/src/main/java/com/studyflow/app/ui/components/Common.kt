@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.studyflow.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

// ---- cards & small pieces --------------------------------------------------------------------------

@Composable
fun SfCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** Rounded "badge" like the ones on the web version. */
@Composable
fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    uppercase: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = content)
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = if (uppercase) text.uppercase() else text,
            color = content,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionTitle(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun EmptyScreen(icon: ImageVector, title: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Visual-only check box (the parent row handles the click). */
@Composable
fun MiniCheck(checked: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val primary = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(if (checked) primary else Color.Transparent)
            .border(2.dp, if (checked) primary else MaterialTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(size - 6.dp), tint = Color.White)
        }
    }
}

// ---- bottom-sheet form ---------------------------------------------------------------------------------

/**
 * Native bottom sheet used for every add / edit form. [onConfirm] returns true when the input was
 * valid and the sheet can close (the caller shows problems through [error]).
 */
@Composable
fun FormSheet(
    title: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Boolean,
    modifier: Modifier = Modifier,
    error: String? = null,
    extraAction: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            content()
            if (error != null) {
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            if (extraAction != null) extraAction()
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = { close() }, modifier = Modifier.weight(1f).height(48.dp)) { Text("Cancel") }
                Button(onClick = { if (onConfirm()) close() }, modifier = Modifier.weight(1f).height(48.dp)) { Text(confirmText) }
            }
        }
    }
}

// ---- fields --------------------------------------------------------------------------------------------------

@Composable
fun SfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) ({ Text(placeholder) }) else null,
        singleLine = singleLine,
        isError = isError,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = keyboardActions,
    )
}

/** Read-only text field that opens a menu: a version-independent stand-in for an exposed dropdown. */
@Composable
fun <T> DropdownField(
    label: String,
    selectedText: String,
    options: List<Pair<T, String>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            singleLine = true,
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .clickable { expanded = true }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        expanded = false
                        onSelect(value)
                    },
                )
            }
        }
    }
}

/** Date field storing an ISO "yyyy-MM-dd" string ("" when empty). */
@Composable
fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Not set",
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = if (value.isEmpty()) "" else formatDateCustom(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = {
                Icon(if (value.isEmpty()) Icons.Rounded.DateRange else Icons.Rounded.Close, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            singleLine = true,
        )
        Row(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .clickable { pickerOpen = true }
            )
            if (value.isNotEmpty()) {
                Box(
                    Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .clip(shape)
                        .clickable { onChange("") }
                )
            }
        }
    }

    if (pickerOpen) {
        val initialMillis = try {
            if (value.isEmpty()) null else LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { pickerOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    pickerOpen = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickerOpen = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Time field storing a 24h "HH:mm" string. */
@Composable
fun TimeField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val shape = MaterialTheme.shapes.medium
    Box(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = formatTimeAmPm(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text("Select time") },
            trailingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            singleLine = true,
        )
        Box(
            Modifier
                .matchParentSize()
                .clip(shape)
                .clickable { pickerOpen = true }
        )
    }

    if (pickerOpen) {
        val parts = value.split(":")
        val state = rememberTimePickerState(
            initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
            initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
            is24Hour = false,
        )
        Dialog(
            onDismissRequest = { pickerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.padding(horizontal = 16.dp).widthIn(max = 360.dp),
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Select time",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    )
                    TimePicker(state = state)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { pickerOpen = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onChange(String.format(Locale.US, "%02d:%02d", state.hour, state.minute))
                            pickerOpen = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
}

// ---- colour picker --------------------------------------------------------------------------------------------

private val CoursePalette = listOf(
    "#dc2626", "#ea580c", "#d97706", "#16a34a", "#0d9488", "#0284c7",
    "#2563eb", "#7c3aed", "#c026d3", "#db2777", "#64748b", "#334155",
)

@Composable
fun ColorPicker(hex: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val valid = parseColorOrNull(hex) != null
    Column(modifier.fillMaxWidth()) {
        Text("Course Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 10.dp),
        ) {
            CoursePalette.forEach { swatch ->
                val color = parseColorOr(swatch, Color.Gray)
                val selected = swatch.equals(hex.trim(), ignoreCase = true)
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(color)
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier
                        )
                        .clickable { onChange(swatch) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = contrastOn(color), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        SfTextField(
            value = hex,
            onValueChange = onChange,
            label = "Custom color (hex)",
            placeholder = "#dc2626",
            isError = !valid,
            imeAction = ImeAction.Done,
        )
    }
}

// ---- dialogs ----------------------------------------------------------------------------------------------------

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
