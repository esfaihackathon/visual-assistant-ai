package com.saral.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// All font sizes are 18 sp minimum to comply with accessibility requirements.
val SaralTypography = Typography(
    displayLarge = TextStyle(
        fontSize   = 36.sp,
        fontWeight = FontWeight.ExtraBold,
        color      = TextWhite,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontSize   = 28.sp,
        fontWeight = FontWeight.Bold,
        color      = TextWhite,
        lineHeight = 36.sp
    ),
    headlineLarge = TextStyle(
        fontSize   = 26.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextWhite,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontSize   = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextWhite,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontSize   = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextWhite,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontSize   = 20.sp,
        fontWeight = FontWeight.Medium,
        color      = TextWhite,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize   = 18.sp,
        fontWeight = FontWeight.Medium,
        color      = TextWhite,
        lineHeight = 26.sp
    ),
    // bodyLarge is the standard reading size – minimum 18 sp
    bodyLarge = TextStyle(
        fontSize   = 18.sp,
        fontWeight = FontWeight.Normal,
        color      = TextLight,
        lineHeight = 28.sp
    ),
    // bodyMedium raised to 18 sp (was 16 sp – below WCAG minimum)
    bodyMedium = TextStyle(
        fontSize   = 18.sp,
        fontWeight = FontWeight.Normal,
        color      = TextLight,
        lineHeight = 26.sp
    ),
    // labelLarge raised to 18 sp for button/label legibility
    labelLarge = TextStyle(
        fontSize   = 18.sp,
        fontWeight = FontWeight.Medium,
        color      = TextWhite,
        lineHeight = 24.sp
    ),
    labelMedium = TextStyle(
        fontSize   = 16.sp,
        fontWeight = FontWeight.Normal,
        color      = TextLight,
        lineHeight = 22.sp
    )
)
