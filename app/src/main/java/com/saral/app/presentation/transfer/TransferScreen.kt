package com.saral.app.presentation.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Fingerprint
import com.saral.app.domain.models.Beneficiary
import com.saral.app.ui.theme.AccentBlue
import com.saral.app.ui.theme.AccentGreen
import com.saral.app.ui.theme.AccentYellow
import com.saral.app.ui.theme.ErrorRed
import com.saral.app.ui.theme.NavyDark
import com.saral.app.ui.theme.NavyLight
import com.saral.app.ui.theme.PrimaryBtn
import com.saral.app.ui.theme.SurfaceCard
import com.saral.app.ui.theme.TextLight
import com.saral.app.ui.theme.TextWhite

@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    isListening: Boolean,
    onMicClick: () -> Unit,
    onRequestBiometric: () -> Unit,
    onBack: () -> Unit
) {
    val step by viewModel.step.collectAsState()
    val assistantMessage by viewModel.assistantMessage.collectAsState()
    val recognizedText by viewModel.recognizedText.collectAsState()
    val beneficiaries by viewModel.beneficiaries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Speak intro once beneficiaries are loaded
    LaunchedEffect(beneficiaries) {
        if (beneficiaries.isNotEmpty()) {
            viewModel.onScreenLoad()
        }
    }

    // Feature 1: voice "Done / Main Menu" navigation from Complete or Failed screen
    LaunchedEffect(viewModel) {
        viewModel.setNavigateCallback { viewModel.reset(); onBack() }
    }

    // Auto-trigger fingerprint when transfer is confirmed — no button tap required
    LaunchedEffect(step) {
        if (step is TransferStep.AwaitingBiometric) {
            delay(500) // let the card animate in before system dialog appears
            onRequestBiometric()
        }
    }

    BackHandler {
        viewModel.reset()
        onBack()
    }

    // Mic is hidden only while fingerprint dialog is active (system UI takes focus)
    val showMic = step !is TransferStep.AwaitingBiometric

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyDark)
    ) {
        // ── Scrollable content ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp, bottom = if (showMic) 160.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.reset(); onBack() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = TextLight,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transfer Money",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Assistant response card
            AnimatedVisibility(visible = assistantMessage.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Assistant: $assistantMessage" },
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = assistantMessage, style = MaterialTheme.typography.bodyLarge, color = TextWhite, lineHeight = 28.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step-specific content
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(200)) },
                label = "transfer_step"
            ) { currentStep ->
                when (currentStep) {
                    is TransferStep.SelectingBeneficiary -> BeneficiaryListContent(
                        beneficiaries = beneficiaries,
                        onSelect = { viewModel.onBeneficiarySelected(it) }
                    )
                    is TransferStep.EnterAmount -> EnterAmountContent(currentStep.beneficiary)
                    is TransferStep.ConfirmTransfer -> ConfirmTransferContent(
                        beneficiary = currentStep.beneficiary,
                        amount = currentStep.amount,
                        onConfirm = { viewModel.onVoiceInput("yes") },
                        onCancel = { viewModel.onVoiceInput("no") }
                    )
                    is TransferStep.AwaitingBiometric -> AwaitingBiometricContent(
                        beneficiary = currentStep.beneficiary,
                        amount = currentStep.amount
                    )
                    is TransferStep.Complete -> TransferCompleteContent(
                        beneficiary = currentStep.beneficiary,
                        amount = currentStep.amount,
                        remainingBalance = currentStep.remainingBalance,
                        txnId = currentStep.txnId,
                        onDone = { viewModel.reset(); onBack() }
                    )
                    is TransferStep.Failed -> TransferFailedContent(
                        onRetry = { viewModel.reset() },
                        onCancel = { viewModel.reset(); onBack() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recognized text
            AnimatedVisibility(visible = recognizedText.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "\"$recognizedText\"",
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        color = AccentBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ── Fixed bottom mic bar (hidden during fingerprint auth) ─────────────────
        if (showMic) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(NavyDark)
                    .padding(horizontal = 24.dp)
                    .padding(top = 10.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Loading / listening status strip
                AnimatedVisibility(visible = isLoading || isListening) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Mic, contentDescription = null,
                                tint = if (isListening) AccentYellow else AccentBlue,
                                modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isListening) "Listening…" else "Processing…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextWhite
                            )
                            if (isLoading) {
                                Spacer(modifier = Modifier.weight(1f))
                                CircularProgressIndicator(color = AccentBlue, strokeWidth = 3.dp, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                TransferMicButton(isListening = isListening, onClick = onMicClick)

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isListening) "Listening… Tap to stop" else "Tap to speak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isListening) AccentYellow else TextLight
                )
            }
        }
    }
}

@Composable
private fun BeneficiaryListContent(
    beneficiaries: List<Beneficiary>,
    onSelect: (Beneficiary) -> Unit
) {
    if (beneficiaries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(32.dp))
        }
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Beneficiaries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextWhite
                )
                // Feature 3: "Tap or say a name" hint
                Text(
                    text = "Tap or say a name",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentBlue
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            beneficiaries.forEachIndexed { index, b ->
                BeneficiaryRow(beneficiary = b, onSelect = { onSelect(b) })
                if (index < beneficiaries.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = NavyLight
                    )
                }
            }
        }
    }
}

@Composable
private fun BeneficiaryRow(beneficiary: Beneficiary, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Select ${beneficiary.name}"
            ) { onSelect() }
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .semantics {
                contentDescription =
                    "Select ${beneficiary.name}, ${beneficiary.bankName}, account ending ${beneficiary.accountLast4}"
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(AccentBlue.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = beneficiary.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Text(
                text = "${beneficiary.bankName}  •  ****${beneficiary.accountLast4}",
                style = MaterialTheme.typography.labelMedium,
                color = TextLight
            )
        }
        // Chevron affordance — signals tappable row
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = AccentBlue.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EnterAmountContent(beneficiary: Beneficiary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sending to",
                style = MaterialTheme.typography.labelLarge,
                color = TextLight
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = beneficiary.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite
                    )
                    Text(
                        text = "${beneficiary.bankName}  •  ****${beneficiary.accountLast4}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyLight)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Say the amount to transfer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight
                )
            }
        }
    }
}

@Composable
private fun ConfirmTransferContent(
    beneficiary: Beneficiary,
    amount: Double,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val amountFormatted = if (amount == amount.toLong().toDouble())
        String.format("%,d", amount.toLong()) else String.format("%,.2f", amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Confirm Transfer",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(16.dp))

            TransferDetailRow(label = "To", value = beneficiary.name)
            Spacer(modifier = Modifier.height(8.dp))
            TransferDetailRow(label = "Bank", value = "${beneficiary.bankName}  •  ****${beneficiary.accountLast4}")
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = NavyLight)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextLight
                )
                Text(
                    text = "₹$amountFormatted",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                ) {
                    Text("No, Cancel")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Yes, Proceed", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TransferDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextLight
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextWhite
        )
    }
}

@Composable
private fun TransferCompleteContent(
    beneficiary: Beneficiary,
    amount: Double,
    remainingBalance: Double,
    txnId: String,
    onDone: () -> Unit
) {
    val amountFormatted = if (amount == amount.toLong().toDouble())
        String.format("%,d", amount.toLong()) else String.format("%,.2f", amount)
    val balanceFormatted = if (remainingBalance == remainingBalance.toLong().toDouble())
        String.format("%,d", remainingBalance.toLong()) else String.format("%,.2f", remainingBalance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Transfer successful",
                tint = AccentGreen,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Transfer Successful!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹$amountFormatted sent to ${beneficiary.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = NavyLight)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Bank", style = MaterialTheme.typography.bodySmall, color = TextLight)
                Text(
                    "${beneficiary.bankName}  •  ****${beneficiary.accountLast4}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ref. No.", style = MaterialTheme.typography.bodySmall, color = TextLight)
                Text(txnId, style = MaterialTheme.typography.bodySmall, color = TextWhite)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = NavyLight)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyLight)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remaining Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextLight
                )
                Text(
                    text = "₹$balanceFormatted",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AccentBlue
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feature 1: voice hint for returning to home
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors   = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.08f)),
                shape    = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = null,
                        tint     = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text  = "Say \"Done\" or \"Main Menu\" to go home",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick  = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = PrimaryBtn)
            ) {
                Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TransferFailedContent(onRetry: () -> Unit, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = "Transfer failed",
                tint = ErrorRed,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Transfer Failed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Something went wrong. Please try again.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TextLight)
                ) {
                    Text("Go Back")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Try Again", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun AwaitingBiometricContent(beneficiary: Beneficiary, amount: Double) {
    val amountFormatted = if (amount == amount.toLong().toDouble())
        String.format("%,d", amount.toLong()) else String.format("%,.2f", amount)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.14f,
                animationSpec = infiniteRepeatable(animation = tween(700), repeatMode = RepeatMode.Reverse),
                label = "fingerprint_scale"
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Fingerprint scanner",
                    tint = AccentBlue, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Scanning Fingerprint…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "₹$amountFormatted → ${beneficiary.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = AccentBlue, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please authenticate using your fingerprint",
                style = MaterialTheme.typography.labelMedium,
                color = TextLight,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TransferMicButton(isListening: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "transfer_mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "transfer_mic_scale"
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
                contentDescription =
                    if (isListening) "Microphone active. Tap to stop."
                    else "Tap to speak your response."
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
