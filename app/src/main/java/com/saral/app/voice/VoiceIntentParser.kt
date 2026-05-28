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

    // Matches specific transaction queries like "check if salary credited" or "electricity bill this month"
    private val queryTxnPatterns = listOf(
        "check.*if.*credit",
        "check.*salary",
        "salary.*credit",
        "electricity.*bill",
        "check.*electricity",
        "is there.*transaction.*for",
        "any.*transaction.*for",
        ".*credit.*this month",
        ".*bill.*this month",
        "check.*transaction.*for"
    ).map { it.toRegex(RegexOption.IGNORE_CASE) }

    // Generic transaction trigger — must be checked AFTER queryTxnPatterns and count extraction
    private val genericTxnPatterns = listOf(
        "recent.*transaction",
        "last.*transaction",
        "transaction.*history",
        "recent.*activity",
        "show.*transaction",
        "my.*transaction",
        "\\btransaction\\b"
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

    // Keywords mapped to searchable transaction description terms
    private val knownTransactionKeywords = listOf(
        "salary", "electricity", "amazon", "netflix", "petrol",
        "insurance", "reliance", "upi", "dividend", "atm", "bill"
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

        // Specific transaction query (e.g. "check if salary credited", "electricity bill this month")
        if (queryTxnPatterns.any { it.containsMatchIn(trimmed) }) {
            val keyword = extractTransactionKeyword(trimmed)
            return VoiceIntent.QueryTransaction(keyword)
        }

        // Count-specific request (e.g. "last 5 transactions", "last ten")
        val count = extractTransactionCount(trimmed)
        if (count != null) {
            return VoiceIntent.TransactionCount(count)
        }

        // Generic transaction mention — ask user how many they want to hear
        if (genericTxnPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.AskTransactionCount
        }

        if (helpPatterns.any { it.containsMatchIn(trimmed) }) {
            return VoiceIntent.Help
        }

        return VoiceIntent.Unknown
    }

    private fun extractTransactionCount(input: String): Int? {
        val lower = input.lowercase()
        return when {
            lower.contains("last ten") || lower.contains("last 10") -> 10
            lower.contains("last five") || lower.contains("last 5") -> 5
            lower.contains("last one") || lower.contains("last 1") -> 1
            // "last transaction" / "last transactions" with no number = most recent one
            Regex("last\\s+transactions?\\b").containsMatchIn(lower) -> 1
            // Numeric "last N" catch-all
            else -> Regex("last\\s+(\\d+)", RegexOption.IGNORE_CASE)
                .find(input)?.groupValues?.get(1)?.toIntOrNull()
        }
    }

    private fun extractTransactionKeyword(input: String): String =
        knownTransactionKeywords.firstOrNull { input.contains(it, ignoreCase = true) }
            ?: input.trim()
}
