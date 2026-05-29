package com.saral.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SaralColorScheme = darkColorScheme(
    primary          = PrimaryBtn,     // button fills – white text AAA
    onPrimary        = TextWhite,
    secondary        = AccentGreen,    // teal success (deuteranopia-safe)
    onSecondary      = TextWhite,
    tertiary         = AccentYellow,   // amber focus/highlight
    onTertiary       = NavyDark,
    background       = NavyDark,
    onBackground     = TextWhite,
    surface          = NavyMedium,
    onSurface        = TextWhite,
    surfaceVariant   = SurfaceCard,
    onSurfaceVariant = TextLight,
    error            = ErrorRed,       // deep orange (protanopia-safe)
    onError          = TextWhite
)

@Composable
fun SaralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SaralColorScheme,
        typography  = SaralTypography,
        content     = content
    )
}
