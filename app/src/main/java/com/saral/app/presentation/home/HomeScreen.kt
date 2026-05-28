package com.saral.app.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentGreen
import com.saral.app.ui.theme.AccentYellow
import com.saral.app.ui.theme.ErrorRed
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.NavyLight
import com.saral.app.ui.theme.SurfaceCard
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextWhite
import com.saral.app.ui.theme.WarningOrange

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMicClick: () -> Unit,
    onTextCommand: (String) -> Unit,
    onQuickCommand: (String) -> Unit,
    isListening: Boolean,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var typedCommand by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saral",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(52.dp)
                        .semantics {
                            contentDescription = "Open accessibility settings"
                        }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = AccentBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Voice status banner
            AnimatedVisibility(visible = uiState.isListening || uiState.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = if (uiState.isListening) AccentYellow else AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (uiState.isListening) "Listening..." else "Processing...",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Response card
            AnimatedVisibility(
                visible = uiState.responseText.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Assistant response: ${uiState.responseText}"
                        },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.responseText,
                            fontSize = 20.sp,
                            color = TextWhite,
                            lineHeight = 30.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Recognized text
            AnimatedVisibility(
                visible = uiState.recognizedText.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "\"${uiState.recognizedText}\"",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentYellow,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Mic button
            MicButton(
                isListening = uiState.isListening,
                onClick = onMicClick
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (uiState.isListening) "Listening... Tap to stop" else "Tap to speak",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = if (uiState.isListening) AccentYellow else TextLight
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Text input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = typedCommand,
                    onValueChange = { typedCommand = it },
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    placeholder = {
                        Text(
                            text = "Type a command...",
                            fontSize = 18.sp,
                            color = TextLight
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
                    singleLine = true,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedPlaceholderColor = TextLight,
                        unfocusedPlaceholderColor = TextLight,
                        cursorColor = AccentYellow,
                        focusedIndicatorColor = AccentBlue,
                        unfocusedIndicatorColor = NavyLight,
                        focusedContainerColor = NavyLight,
                        unfocusedContainerColor = NavyLight
                    )
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = {
                        val command = typedCommand.trim()
                        if (command.isNotBlank()) {
                            onTextCommand(command)
                            typedCommand = ""
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .semantics { contentDescription = "Send command" },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section label
            Text(
                text = "Quick Actions",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // Vertical quick-action buttons
            QuickActionButton(
                icon = Icons.Filled.AccountBalance,
                label = "Check My Balance",
                description = "Hear your current account balance",
                color = AccentBlue,
                onClick = { onQuickCommand("Check my balance") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionButton(
                icon = Icons.Filled.SwapHoriz,
                label = "Transfer Money",
                description = "Send money to someone",
                color = AccentGreen,
                onClick = { onQuickCommand("Transfer 500 rupees to Rahul") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionButton(
                icon = Icons.Filled.Book,
                label = "Request Cheque Book",
                description = "Order a new cheque book",
                color = WarningOrange,
                onClick = { onQuickCommand("Request cheque book") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionButton(
                icon = Icons.Filled.Receipt,
                label = "Recent Transactions",
                description = "Hear your latest transactions",
                color = AccentYellow,
                onClick = { onQuickCommand("Show recent transactions") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            QuickActionButton(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                label = "Help",
                description = "Learn what you can do",
                color = TextLight,
                onClick = { onQuickCommand("Help") }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = label
            ) { onClick() }
            .semantics {
                contentDescription = "$label. $description"
                role = Role.Button
            },
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = TextLight
                )
            }
        }
    }
}

@Composable
private fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .scale(if (isListening) pulseScale else 1f)
            .clip(CircleShape)
            .background(if (isListening) ErrorRed else AccentBlue)
            .then(
                if (isListening) Modifier.border(4.dp, AccentYellow.copy(alpha = 0.6f), CircleShape)
                else Modifier.border(3.dp, AccentBlue.copy(alpha = 0.3f), CircleShape)
            )
            .clickable(
                role = Role.Button,
                onClickLabel = if (isListening) "Stop listening" else "Start listening"
            ) { onClick() }
            .semantics {
                contentDescription =
                    if (isListening) "Microphone active. Tap to stop listening."
                    else "Tap to start speaking a command."
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = TextWhite
        )
    }
}
