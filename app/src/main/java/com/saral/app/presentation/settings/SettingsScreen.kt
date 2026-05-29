package com.saral.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentGreen
import com.saral.app.ui.theme.AccentYellow
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.NavyLight
import com.saral.app.ui.theme.PrimaryBtn
import com.saral.app.ui.theme.SurfaceCard
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextWhite

private val FONT_SCALE_VALUES = listOf(0.85f, 1.0f, 1.15f, 1.3f, 1.5f)
private val FONT_SCALE_LABELS = listOf("Smaller", "Normal", "Large", "Larger", "Largest")

private fun scaleToIdx(scale: Float): Int =
    FONT_SCALE_VALUES.indices.minByOrNull { kotlin.math.abs(FONT_SCALE_VALUES[it] - scale) } ?: 1

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    availableVoices: List<String>,
    selectedVoice: String?,
    selectedLanguage: String,
    speechRate: Float,
    hapticEnabled: Boolean,
    fontScale: Float,
    onLanguageSelected: (String) -> Unit,
    onSpeechRateChanged: (Float) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onHapticToggled: (Boolean) -> Unit,
    onFontScaleChanged: (Float) -> Unit
) {
    var voiceMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = "Go back to home screen" }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text       = "Accessibility Settings",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Language ─────────────────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.Language,
            title       = "Language",
            description = "Current: $selectedLanguage"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("English", "Hindi").forEach { lang ->
                    val selected = lang == selectedLanguage
                    Card(
                        modifier = Modifier.semantics {
                            contentDescription = "Select $lang language${if (selected) ", currently selected" else ""}"
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) PrimaryBtn else NavyLight
                        ),
                        shape   = RoundedCornerShape(8.dp),
                        onClick = { onLanguageSelected(lang) }
                    ) {
                        Text(
                            text     = lang,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            style    = MaterialTheme.typography.labelLarge,
                            color    = TextWhite,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Speech Speed ─────────────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.Speed,
            title       = "Speech Speed",
            description = "Adjust how fast the assistant speaks"
        ) {
            Slider(
                value         = speechRate,
                onValueChange = onSpeechRateChanged,
                valueRange    = 0.5f..1.5f,
                steps         = 4,
                colors        = SliderDefaults.colors(
                    thumbColor       = AccentBlue,
                    activeTrackColor = AccentBlue.copy(alpha = 0.45f)
                ),
                modifier = Modifier.semantics {
                    contentDescription =
                        "Speech speed slider. Current: ${String.format("%.1f", speechRate)} times normal speed"
                }
            )
            Text(
                text  = "${String.format("%.1f", speechRate)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Voice ────────────────────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.TextFields,
            title       = "Voice",
            description = "Choose available speaking voice"
        ) {
            Box {
                OutlinedTextField(
                    value       = selectedVoice ?: "Auto",
                    onValueChange = {},
                    readOnly    = true,
                    modifier    = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Speed,
                            contentDescription = "Open voice list"
                        )
                    }
                )
                DropdownMenu(
                    expanded         = voiceMenuExpanded,
                    onDismissRequest = { voiceMenuExpanded = false }
                ) {
                    if (availableVoices.isEmpty()) {
                        DropdownMenuItem(
                            text    = { Text("No voices available") },
                            onClick = {}
                        )
                    } else {
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text    = { Text(voice) },
                                onClick = {
                                    onVoiceSelected(voice)
                                    voiceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { voiceMenuExpanded = true },
                colors  = ButtonDefaults.buttonColors(containerColor = PrimaryBtn),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text  = if (availableVoices.isEmpty()) "Loading voices..." else "Select Voice",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Haptic Feedback ───────────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.Vibration,
            title       = "Haptic Feedback",
            description = "Vibrate on button presses and actions"
        ) {
            Switch(
                checked         = hapticEnabled,
                onCheckedChange = onHapticToggled,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = TextWhite,
                    checkedTrackColor  = PrimaryBtn,
                    uncheckedThumbColor = TextLight,
                    uncheckedTrackColor = NavyLight
                ),
                modifier = Modifier.semantics {
                    contentDescription =
                        "Haptic feedback ${if (hapticEnabled) "enabled" else "disabled"}. Tap to toggle."
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Text Size (zoom) ─────────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.FormatSize,
            title       = "Text Size",
            description = "Make all text and controls larger or smaller"
        ) {
            val idx = remember(fontScale) { scaleToIdx(fontScale) }

            Slider(
                value         = idx.toFloat(),
                onValueChange = { v ->
                    val i = v.toInt().coerceIn(0, FONT_SCALE_VALUES.lastIndex)
                    onFontScaleChanged(FONT_SCALE_VALUES[i])
                },
                valueRange = 0f..4f,
                steps      = 3,  // 5 discrete stops: 0,1,2,3,4
                colors     = SliderDefaults.colors(
                    thumbColor       = AccentYellow,
                    activeTrackColor = AccentYellow.copy(alpha = 0.45f)
                ),
                modifier = Modifier.semantics {
                    contentDescription =
                        "Text size slider. Current: ${FONT_SCALE_LABELS.getOrElse(idx) { "Normal" }}"
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "Smaller",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextLight
                )
                Text(
                    text       = FONT_SCALE_LABELS.getOrElse(idx) { "Normal" },
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = AccentYellow,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text  = "Largest",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextLight
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Large Text (always on) ────────────────────────────────────────────
        SettingCard(
            icon        = Icons.Filled.TextFields,
            title       = "Large Text",
            description = "Always enabled. Minimum 18 sp fonts throughout."
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = AccentGreen.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text       = "Active",
                    modifier   = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style      = MaterialTheme.typography.labelMedium,
                    color      = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingCard(
    icon        : ImageVector,
    title       : String,
    description : String,
    action      : @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape    = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint     = AccentBlue,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))   // ← fixed: was height(8.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text  = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            action()
        }
    }
}
