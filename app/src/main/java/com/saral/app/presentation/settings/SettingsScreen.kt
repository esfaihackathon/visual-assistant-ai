package com.saral.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.NavyLight
import com.saral.app.ui.theme.SurfaceCard
import com.saral.app.ui.theme.TextLight

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var hapticEnabled by remember { mutableStateOf(true) }
    var speechRate by remember { mutableFloatStateOf(0.9f) }
    var selectedLanguage by remember { mutableStateOf("English") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .padding(24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.semantics {
                    contentDescription = "Go back to home"
                }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AccentBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Accessibility Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Language selection
        SettingCard(
            icon = Icons.Filled.Language,
            title = "Language",
            description = "Current: $selectedLanguage"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("English", "Hindi").forEach { lang ->
                    Card(
                        modifier = Modifier
                            .semantics { contentDescription = "Select $lang language" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (lang == selectedLanguage) AccentBlue else NavyLight
                        ),
                        shape = RoundedCornerShape(8.dp),
                        onClick = { selectedLanguage = lang }
                    ) {
                        Text(
                            text = lang,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Speech rate
        SettingCard(
            icon = Icons.Filled.Speed,
            title = "Speech Speed",
            description = "Adjust how fast the assistant speaks"
        ) {
            Slider(
                value = speechRate,
                onValueChange = { speechRate = it },
                valueRange = 0.5f..1.5f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Speech speed slider. Current speed: ${String.format("%.1f", speechRate)}x"
                }
            )
            Text(
                text = "${String.format("%.1f", speechRate)}x",
                style = MaterialTheme.typography.bodyMedium,
                color = AccentBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Haptic feedback toggle
        SettingCard(
            icon = Icons.Filled.Vibration,
            title = "Haptic Feedback",
            description = "Vibrate on actions"
        ) {
            Switch(
                checked = hapticEnabled,
                onCheckedChange = { hapticEnabled = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = AccentBlue,
                    checkedTrackColor = AccentBlue.copy(alpha = 0.4f)
                ),
                modifier = Modifier.semantics {
                    contentDescription = "Haptic feedback ${if (hapticEnabled) "enabled" else "disabled"}"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Font size info
        SettingCard(
            icon = Icons.Filled.TextFields,
            title = "Large Text",
            description = "Always enabled for accessibility. Minimum 18sp fonts."
        ) {}
    }
}

@Composable
private fun SettingCard(
    icon: ImageVector,
    title: String,
    description: String,
    action: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            action()
        }
    }
}
