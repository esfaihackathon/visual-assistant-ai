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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saral.app.domain.models.TransactionType
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentGreen
import com.saral.app.ui.theme.ErrorRed
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.NavyLight
import com.saral.app.ui.theme.SurfaceCard
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextWhite

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMicClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
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
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.semantics {
                        contentDescription = "Open accessibility settings"
                    }
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = TextLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = uiState.responseText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite,
                            lineHeight = 28.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Transaction list
            AnimatedVisibility(visible = uiState.showTransactions && uiState.transactions.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Recent Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(uiState.transactions) { txn ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = txn.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextWhite
                                        )
                                        Text(
                                            text = txn.date,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextLight,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = if (txn.type == TransactionType.DEBIT) "-₹${txn.amount.toLong()}" else "+₹${txn.amount.toLong()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (txn.type == TransactionType.DEBIT) ErrorRed else AccentGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Listening indicator
            if (uiState.isListening) {
                Text(
                    text = "Listening…",
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isProcessing) {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Mic button
            MicButton(
                isListening = uiState.isListening,
                onClick = onMicClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tap to speak",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Recent commands
            if (uiState.recentCommands.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextLight
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.recentCommands.take(3).forEach { cmd ->
                        Text(
                            text = cmd,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(NavyLight)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLight,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                }
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
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Box(
        modifier = Modifier
            .size(88.dp)
            .scale(if (isListening) pulseScale else 1f)
            .clip(CircleShape)
            .background(if (isListening) ErrorRed else AccentBlue)
            .then(
                if (isListening) Modifier.border(3.dp, ErrorRed.copy(alpha = 0.4f), CircleShape)
                else Modifier
            )
            .clickable(
                role = Role.Button,
                onClickLabel = if (isListening) "Stop listening" else "Start listening"
            ) { onClick() }
            .semantics {
                contentDescription = if (isListening) "Microphone active. Tap to stop listening." else "Tap to start speaking a command."
                role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = TextWhite
        )
    }
}
