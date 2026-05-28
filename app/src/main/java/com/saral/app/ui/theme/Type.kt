package com.saral.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val SaralTypography = Typography(
        displayLarge = TextStyle(
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            lineHeight = 44.sp
        ),
displayMedium = TextStyle(
fontSize = 28.sp,
fontWeight = FontWeight.Bold,
color = TextWhite,
lineHeight = 36.sp
),
    headlineLarge = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextWhite,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextWhite,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = TextWhite,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        color = TextWhite,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.Normal,
        color = TextLight,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        color = TextLight,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = TextWhite,
        lineHeight = 22.sp
    )
)
