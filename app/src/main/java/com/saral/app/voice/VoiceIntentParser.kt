package com.saral.app.voice

import com.saral.app.domain.models.VoiceIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceIntentParser @Inject constructor() {

    private val balancePatterns = listOf(
        "check.*balance",
        "what.*balance",
        "how much.*money",
        "my balance",
        "account balance",
        "balance check",
        "show.*balance",
        "tell.*balance"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val transferPatterns = listOf(
        "transfer",
        "send.*money",
        "pay\\b",
        "send.*rupees",
        "transfer.*rupees"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val chequeBookPatterns = listOf(
        "cheque.*book",
        "check.*book",
        "chequebook",
        "new cheque",
        "issue.*cheque",
        "request.*cheque"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val recentTxnPatterns = listOf(
        "recent.*transaction",
        "last.*transaction",
        "transaction.*history",
        "recent.*activity",
        "show.*transaction",
        "my.*transaction"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val helpPatterns = listOf(
        "\\bhelp\\b",
        "assist",
        "support",
        "call.*support",
        "customer.*care",
        "emergency"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val confirmYesPatterns = listOf(
        "^yes$", "^yeah$", "^yep$", "^confirm$", "^ok$", "^okay$",
        "^sure$", "^go ahead$", "^proceed$", "^haan$", "^ha$", "^ji$"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val confirmNoPatterns = listOf(
        "^no$", "^nope$", "^cancel$", "^stop$", "^nahi$", "^mat karo$", "^don't$"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    private val amountRegex = Regex("(\\d+(?:,\\d+)*(?:\\.\\d+)?)")
    private val recipientRegex = Regex(
        "(?:to|for)\\s+([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)?)",
        RegexOption.IGNORE_CASE
    )

    fun parse(input: String): VoiceIntent {
        val trimmed = input.trim()

        if (confirmYesPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.ConfirmYes
        }
        if (confirmNoPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.ConfirmNo
        }

        if (balancePatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.CheckBalance
        }

        if (transferPatterns.any { it.containsMatchIn(trimmed) }) {
            val amount = amountRegex.find(trimmed)?.groupValues?.get(1)
                ?.replace(",", "")?.toDoubleOrNull()
            val recipient = recipientRegex.find(trimmed)?.groupValues?.get(1)
            return VoiceIntent.TransferMoney(amount = amount, recipient = recipient)
        }

        if (chequeBookPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.RequestChequeBook
        }

        if (recentTxnPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.RecentTransactions
        }

        if (helpPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.Help
        }

        return VoiceIntent.Unknown
    }
}
