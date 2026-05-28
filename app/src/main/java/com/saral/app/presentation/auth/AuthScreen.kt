package com.saral.app.presentation.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentGreen
import com.saral.app.ui.theme.ErrorRed
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextWhite

@Composable
fun AuthScreen(
    authStep: AuthStep,
    isListening: Boolean,
    onAuthenticate: () -> Unit,
    onOtpMicClick: () -> Unit,
    onRegenerate: () -> Unit,
    onCancel: () -> Unit
) {
    LaunchedEffect(authStep) {
        if (authStep is AuthStep.BiometricPending) {
            onAuthenticate()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = authStep, label = "auth_step") { step ->
            when (step) {
                is AuthStep.BiometricPending -> BiometricView()
                is AuthStep.OtpCalling -> OtpCallingView()
                is AuthStep.OtpInput -> OtpInputView(
                    attemptsLeft = step.attemptsLeft,
                    isListening = isListening,
                    onMicClick = onOtpMicClick
                )
                is AuthStep.OtpExhausted -> OtpExhaustedView(
                    isListening = isListening,
                    onMicClick = onOtpMicClick,
                    onRegenerate = onRegenerate,
                    onCancel = onCancel
                )
                is AuthStep.Success -> SuccessView()
            }
        }
    }
}

@Composable
private fun BiometricView() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "pulse_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .padding(32.dp)
            .semantics {
                contentDescription = "Biometric authentication screen. Place your finger on the sensor."
            }
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = null,
            modifier = Modifier.size(120.dp).scale(scale),
            tint = AccentBlue
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Touch the fingerprint sensor",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Authenticate to access your banking",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OtpCallingView() {
    val infiniteTransition = rememberInfiniteTransition(label = "call_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(animation = tween(600), repeatMode = RepeatMode.Reverse),
        label = "call_scale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Phone,
            contentDescription = null,
            modifier = Modifier.size(100.dp).scale(scale),
            tint = AccentGreen
        )
        Spacer(Modifier.height(32.dp))
        Text(
            text = "Calling your registered number...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Listen to the automated call for your OTP",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        CircularProgressIndicator(color = AccentGreen)
    }
}

@Composable
private fun OtpInputView(
    attemptsLeft: Int,
    isListening: Boolean,
    onMicClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "Enter OTP",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Say your 4-digit one-time password",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} remaining",
            style = MaterialTheme.typography.bodyMedium,
            color = if (attemptsLeft == 1) ErrorRed else TextLight,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
        MicButton(isListening = isListening, onMicClick = onMicClick, hint = "Tap to say OTP")
    }
}

@Composable
private fun OtpExhaustedView(
    isListening: Boolean,
    onMicClick: () -> Unit,
    onRegenerate: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = ErrorRed
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Too many incorrect attempts",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Say \"yes\" to regenerate OTP or \"no\" to cancel",
            style = MaterialTheme.typography.bodyLarge,
            color = TextLight,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        MicButton(isListening = isListening, onMicClick = onMicClick, hint = "Say yes or no")
        Spacer(Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(onClick = onRegenerate, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Regenerate")
            }
        }
    }
}

@Composable
private fun SuccessView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.displayLarge,
            color = AccentGreen
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Authentication Successful",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MicButton(isListening: Boolean, onMicClick: () -> Unit, hint: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(animation = tween(500), repeatMode = RepeatMode.Reverse),
        label = "mic_scale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(if (isListening) scale else 1f)
                .background(color = if (isListening) AccentGreen else AccentBlue, shape = CircleShape)
                .clickable(onClick = onMicClick)
                .semantics {
                    contentDescription = if (isListening) "Listening. Tap to stop." else hint
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = TextWhite
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (isListening) "Listening..." else hint,
            style = MaterialTheme.typography.bodyMedium,
            color = TextLight
        )
    }
}
