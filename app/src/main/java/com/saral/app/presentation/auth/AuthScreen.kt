package com.saral.app.presentation.auth

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.TextLight

@Composable
fun AuthScreen(
    authStep: AuthStep,
    onAuthenticate: () -> Unit
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
private fun SuccessView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(32.dp)
    ) {
        Text(
            text = "\u2713",
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
