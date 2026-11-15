package com.studyflow.app.ui.components

import androidx.compose.ui.graphics.Color
import com.studyflow.app.data.NEUTRAL_COLOR
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Port of formatTimeAMPM(): "14:05" -> "2:05 PM". */
fun formatTimeAmPm(time24: String): String {
    if (time24.isBlank()) return ""
    return try {
        val t = LocalTime.parse(time24)
        val h = if (t.hour % 12 == 0) 12 else t.hour % 12
        val ampm = if (t.hour >= 12) "PM" else "AM"
        "$h:${t.minute.toString().padStart(2, '0')} $ampm"
    } catch (e: Exception) {
        time24
    }
}

fun addMinutes(time24: String, minutes: Int): String = try {
    LocalTime.parse(time24).plusMinutes(minutes.toLong()).format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
} catch (e: Exception) {
    time24
}

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

/** Port of formatDateCustom(): "2026-10-05" -> "5 Oct 2026". */
fun formatDateCustom(date: String): String {
    if (date.isBlank()) return "No Deadline"
    val parts = date.split("-")
    if (parts.size != 3) return date
    val month = (parts[1].toIntOrNull() ?: return date) - 1
    val day = parts[2].toIntOrNull() ?: return date
    if (month !in 0..11) return date
    return "$day ${MONTHS[month]} ${parts[0]}"
}

fun todayIso(): String = LocalDate.now().toString()

// ---- colours -----------------------------------------------------------------------------------------

fun parseColorOrNull(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    val text = hex.trim()
    if (!text.startsWith("#")) return null
    return try {
        Color(android.graphics.Color.parseColor(text))
    } catch (e: IllegalArgumentException) {
        null
    }
}

fun parseColorOr(hex: String?, fallback: Color): Color = parseColorOrNull(hex) ?: fallback

/** Port of getContrastYIQ(): black text on light backgrounds, white on dark ones. */
fun contrastOn(background: Color): Color {
    val r = background.red * 255f
    val g = background.green * 255f
    val b = background.blue * 255f
    val yiq = (r * 299f + g * 587f + b * 114f) / 1000f
    return if (yiq >= 128f) Color.Black else Color.White
}

val NeutralColor: Color = Color(android.graphics.Color.parseColor(NEUTRAL_COLOR))
