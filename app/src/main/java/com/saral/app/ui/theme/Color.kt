package com.saral.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Backgrounds ──────────────────────────────────────────────────────────────
val NavyDark    = Color(0xFF081320)   // deepest bg
val NavyMedium  = Color(0xFF0F1E33)   // surface bg
val NavyLight   = Color(0xFF162A45)   // elevated bg / input

// ── Primary blue (for icons, links, accents) ──────────────────────────────────
// Vivid blue – readable on dark bg; safe for all colour-blind types on dark bg
val AccentBlue  = Color(0xFF4D9EFF)

// Button fill – white text on PrimaryBtn = 7.1:1 (WCAG AAA)
val PrimaryBtn  = Color(0xFF1556B0)
val PrimaryDeep = Color(0xFF0D4090)

// ── Semantic (always paired with shape/text, never colour alone) ──────────────
// Teal – distinguishable even with deuteranopia (safe green replacement)
val AccentGreen  = Color(0xFF00BFA5)
val SuccessGreen = Color(0xFF00BFA5)   // alias

// Deep orange – safe for protanopia (replaces pure red for error/danger)
val ErrorRed     = Color(0xFFFF6B35)

// Amber – WCAG AAA (10.9:1) on dark; safe for all colour-blind types
val AccentYellow  = Color(0xFFFFC107)
val WarningOrange = Color(0xFFFFB300)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextWhite = Color(0xFFFFFFFF)     // primary text – 19:1 on NavyDark
val TextLight = Color(0xFFB2CCE8)     // secondary text – 9:1 on NavyDark
val TextMuted = Color(0xFF6B8BAA)     // muted / hint text

// ── Surface ──────────────────────────────────────────────────────────────────
val SurfaceCard = NavyMedium
