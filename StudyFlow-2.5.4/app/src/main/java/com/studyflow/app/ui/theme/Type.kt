package com.studyflow.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.studyflow.app.R

val PlusJakartaSans = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
)

private val base = Typography()

val StudyFlowTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = PlusJakartaSans),
    displayMedium = base.displayMedium.copy(fontFamily = PlusJakartaSans),
    displaySmall = base.displaySmall.copy(fontFamily = PlusJakartaSans),
    headlineLarge = base.headlineLarge.copy(fontFamily = PlusJakartaSans),
    headlineMedium = base.headlineMedium.copy(fontFamily = PlusJakartaSans),
    headlineSmall = base.headlineSmall.copy(fontFamily = PlusJakartaSans),
    titleLarge = base.titleLarge.copy(fontFamily = PlusJakartaSans),
    titleMedium = base.titleMedium.copy(fontFamily = PlusJakartaSans),
    titleSmall = base.titleSmall.copy(fontFamily = PlusJakartaSans),
    bodyLarge = base.bodyLarge.copy(fontFamily = PlusJakartaSans),
    bodyMedium = base.bodyMedium.copy(fontFamily = PlusJakartaSans),
    bodySmall = base.bodySmall.copy(fontFamily = PlusJakartaSans),
    labelLarge = base.labelLarge.copy(fontFamily = PlusJakartaSans),
    labelMedium = base.labelMedium.copy(fontFamily = PlusJakartaSans),
    labelSmall = base.labelSmall.copy(fontFamily = PlusJakartaSans),
)
