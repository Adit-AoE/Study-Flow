package com.studyflow.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Accent colours shared by the whole app (same values as the web version). */
object SfColors {
    val Red = Color(0xFFDC2626)
    val Green = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
}

private val DarkColors = darkColorScheme(
    primary = SfColors.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3F0D0D),
    onPrimaryContainer = Color(0xFFFFC9C4),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1E1E1E),
    onSecondaryContainer = Color(0xFFF8FAFC),
    tertiary = SfColors.Green,
    onTertiary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceTint = SfColors.Red,
    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF1E1E1E),
    error = Color(0xFFF87171),
    onError = Color.Black,
    errorContainer = Color(0xFF3F0D0D),
    onErrorContainer = Color(0xFFFFC9C4),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF1C1C1C),
)

private val LightColors = lightColorScheme(
    primary = SfColors.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1DE),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF0F172A),
    tertiary = SfColors.Green,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceTint = SfColors.Red,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1DE),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE9EEF4),
)

private val SfShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun StudyFlowTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = StudyFlowTypography,
        shapes = SfShapes,
        content = content,
    )
}
