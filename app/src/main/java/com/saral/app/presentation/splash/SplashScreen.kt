package com.saral.app.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentYellow
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextMuted
import com.saral.app.ui.theme.TextWhite
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_scale"
    )

    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(900))
        onSpeak("Welcome to Saral. Your accessible banking assistant.")
        delay(3000)
        onSplashComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
            .semantics {
                contentDescription =
                    "Saral splash screen. Welcome to Saral, your accessible banking assistant."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Logo ring: bank building + mic badge ──────────────────────
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.12f))
                    .border(2.dp, AccentBlue.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Primary icon — bank building (banking/finance)
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(66.dp),
                    tint = AccentBlue
                )
                // Mic badge bottom-right — represents voice / accessibility
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-10).dp, y = (-10).dp)
                        .clip(CircleShape)
                        .background(AccentYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = NavyDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── App name ─────────────────────────────────────────────────────
            Text(
                text       = "Saral",
                fontSize   = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TextWhite,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text  = "Accessible Banking for Everyone",
                style = MaterialTheme.typography.bodyLarge,
                color = TextLight
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Tagline ──────────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Hearing,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "Voice · Vision · Inclusion",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Amber accent dot ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentYellow.copy(alpha = 0.7f))
            )
        }
    }
}
