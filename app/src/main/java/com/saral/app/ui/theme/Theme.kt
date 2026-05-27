package com.saral.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SaralColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = TextWhite,
    secondary = AccentGreen,
    onSecondary = TextWhite,
    background = NavyDark,
    onBackground = TextWhite,
    surface = NavyMedium,
    onSurface = TextWhite,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextLight,
    error = ErrorRed,
    onError = TextWhite
)

@Composable
fun SaralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SaralColorScheme,
        typography = SaralTypography,
        content = content
    )
}
