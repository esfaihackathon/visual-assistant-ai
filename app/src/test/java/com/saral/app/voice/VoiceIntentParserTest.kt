package com.saral.app.voice

import com.saral.app.domain.models.VoiceIntent
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class VoiceIntentParserTest {
    private lateinit var parser: VoiceIntentParser

    @Before
    fun setup() {
        parser = VoiceIntentParser()
    }

    // ──────────────────────────────────────────
    // Balance
    // ──────────────────────────────────────────

    @Test
    fun parsesCheckBalance() {
        assertTrue(parser.parse("How much money do I have in my account?") is VoiceIntent.CheckBalance)
    }

    @Test
    fun parsesCheckBalance_shortForm() {
        assertTrue(parser.parse("Check my balance") is VoiceIntent.CheckBalance)
    }

    @Test
    fun parsesCheckBalance_hindi() {
        assertTrue(parser.parse("mera balance") is VoiceIntent.CheckBalance)
    }

    // ──────────────────────────────────────────
    // Transfer
    // ──────────────────────────────────────────

    @Test
    fun parsesTransferWithAmountAndRecipient() {
        val intent = parser.parse("Transfer 500 rupees to Rahul") as VoiceIntent.TransferMoney
        assertEquals(500.0, intent.amount)
        assertEquals("Rahul", intent.recipient)
    }

    @Test
    fun parsesTransferWithCommasAndDecimals() {
        val intent = parser.parse("Send 1,250.50 rupees to Amit") as VoiceIntent.TransferMoney
        assertEquals(1250.50, intent.amount!!, 0.001)
        assertEquals("Amit", intent.recipient)
    }

    @Test
    fun parsesTransferMissingRecipient_recipientNull() {
        val intent = parser.parse("Transfer 500 rupees") as VoiceIntent.TransferMoney
        assertNull(intent.recipient)
    }

    @Test
    fun parsesTransferMissingAmount_amountNull() {
        val intent = parser.parse("Transfer money to Priya") as VoiceIntent.TransferMoney
        assertNull(intent.amount)
    }

    // ──────────────────────────────────────────
    // Cheque Book
    // ──────────────────────────────────────────

    @Test
    fun parsesChequeBookRequest() {
        assertTrue(parser.parse("Request cheque book") is VoiceIntent.RequestChequeBook)
    }

    @Test
    fun parsesChequeBookOrder() {
        assertTrue(parser.parse("I need a new cheque book") is VoiceIntent.RequestChequeBook)
    }

    // ──────────────────────────────────────────
    // Transaction — Ask Count (generic trigger)
    // ──────────────────────────────────────────

    @Test
    fun parsesGenericTransaction_asksForCount() {
        assertTrue(parser.parse("Show recent transactions") is VoiceIntent.AskTransactionCount)
    }

    @Test
    fun parsesTransaction_bareWord_asksForCount() {
        assertTrue(parser.parse("transaction") is VoiceIntent.AskTransactionCount)
    }

    @Test
    fun parsesTransactionHistory_asksForCount() {
        assertTrue(parser.parse("Show my transaction history") is VoiceIntent.AskTransactionCount)
    }

    @Test
    fun parsesMyTransactions_asksForCount() {
        assertTrue(parser.parse("My transactions") is VoiceIntent.AskTransactionCount)
    }

    @Test
    fun parsesRecentActivity_asksForCount() {
        assertTrue(parser.parse("Show recent activity") is VoiceIntent.AskTransactionCount)
    }

    // ──────────────────────────────────────────
    // Transaction — Count (specific number)
    // ──────────────────────────────────────────

    @Test
    fun parsesLastFiveTransactions() {
        val intent = parser.parse("last 5 transactions") as VoiceIntent.TransactionCount
        assertEquals(5, intent.count)
    }

    @Test
    fun parsesLastTenTransactions() {
        val intent = parser.parse("last 10 transactions") as VoiceIntent.TransactionCount
        assertEquals(10, intent.count)
    }

    @Test
    fun parsesLastOneTransaction() {
        val intent = parser.parse("last 1 transaction") as VoiceIntent.TransactionCount
        assertEquals(1, intent.count)
    }

    @Test
    fun parsesLastTransaction_bare_returnsCount1() {
        val intent = parser.parse("last transaction") as VoiceIntent.TransactionCount
        assertEquals(1, intent.count)
    }

    @Test
    fun parsesLastTransactions_plural_bare_returnsCount1() {
        val intent = parser.parse("last transactions") as VoiceIntent.TransactionCount
        assertEquals(1, intent.count)
    }

    @Test
    fun parsesShowLastTransaction_returnsCount1() {
        val intent = parser.parse("show last transaction") as VoiceIntent.TransactionCount
        assertEquals(1, intent.count)
    }

    @Test
    fun parsesLastFiveTransactions_notAffectedByBareRule() {
        // "last 5 transactions" must still return 5, not 1
        val intent = parser.parse("last 5 transactions") as VoiceIntent.TransactionCount
        assertEquals(5, intent.count)
    }

    @Test
    fun parsesLastFive_wordForm() {
        val intent = parser.parse("last five transactions") as VoiceIntent.TransactionCount
        assertEquals(5, intent.count)
    }

    @Test
    fun parsesLastTen_wordForm() {
        val intent = parser.parse("last ten transactions") as VoiceIntent.TransactionCount
        assertEquals(10, intent.count)
    }

    @Test
    fun parsesLastOne_wordForm() {
        val intent = parser.parse("last one transaction") as VoiceIntent.TransactionCount
        assertEquals(1, intent.count)
    }

    @Test
    fun parsesLastFive_noSuffix() {
        val intent = parser.parse("last five") as VoiceIntent.TransactionCount
        assertEquals(5, intent.count)
    }

    @Test
    fun parsesLastTen_noSuffix() {
        val intent = parser.parse("last ten") as VoiceIntent.TransactionCount
        assertEquals(10, intent.count)
    }

    // ──────────────────────────────────────────
    // Transaction — Query (keyword search)
    // ──────────────────────────────────────────

    @Test
    fun parsesQueryTransaction_salaryLong() {
        val intent = parser.parse("Please check if the salary is credited this month") as VoiceIntent.QueryTransaction
        assertEquals("salary", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_salaryShort() {
        val intent = parser.parse("Check salary credit") as VoiceIntent.QueryTransaction
        assertEquals("salary", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_electricityBill() {
        val intent = parser.parse("Is there any transaction for the electricity bill this month") as VoiceIntent.QueryTransaction
        assertEquals("electricity", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_electricityShort() {
        val intent = parser.parse("Check electricity bill") as VoiceIntent.QueryTransaction
        assertEquals("electricity", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_amazon() {
        val intent = parser.parse("Any transaction for Amazon this month") as VoiceIntent.QueryTransaction
        assertEquals("amazon", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_netflix() {
        val intent = parser.parse("Is there a Netflix transaction this month") as VoiceIntent.QueryTransaction
        assertEquals("netflix", intent.keyword)
    }

    @Test
    fun parsesQueryTransaction_salaryCredit() {
        val intent = parser.parse("Has salary been credited this month") as VoiceIntent.QueryTransaction
        assertEquals("salary", intent.keyword)
    }

    // ──────────────────────────────────────────
    // Confirmation
    // ──────────────────────────────────────────

    @Test
    fun parsesConfirmYes() {
        assertTrue(parser.parse("Yes") is VoiceIntent.ConfirmYes)
    }

    @Test
    fun parsesConfirmYes_haan() {
        assertTrue(parser.parse("Haan") is VoiceIntent.ConfirmYes)
    }

    @Test
    fun parsesConfirmNo() {
        assertTrue(parser.parse("No") is VoiceIntent.ConfirmNo)
    }

    @Test
    fun parsesConfirmNo_nahi() {
        assertTrue(parser.parse("Nahi") is VoiceIntent.ConfirmNo)
    }

    // ──────────────────────────────────────────
    // Help & Unknown
    // ──────────────────────────────────────────

    @Test
    fun parsesHelp() {
        assertTrue(parser.parse("Help") is VoiceIntent.Help)
    }

    @Test
    fun unknownWhenNoPatternMatches() {
        assertTrue(parser.parse("Blah blah gibberish") is VoiceIntent.Unknown)
    }

    @Test
    fun blankInput_returnsUnknown() {
        assertTrue(parser.parse("   ") is VoiceIntent.Unknown)
    }

    // ──────────────────────────────────────────
    // Priority ordering — query beats generic
    // ──────────────────────────────────────────

    @Test
    fun queryTransactionTakesPriorityOverGenericTransaction() {
        // "salary" query must NOT fall through to AskTransactionCount
        val intent = parser.parse("Check if salary is credited this month")
        assertTrue("Expected QueryTransaction but got $intent", intent is VoiceIntent.QueryTransaction)
    }

    @Test
    fun countTakesPriorityOverGenericTransaction() {
        // "last 5 transactions" must NOT fall through to AskTransactionCount
        val intent = parser.parse("last 5 transactions")
        assertTrue("Expected TransactionCount but got $intent", intent is VoiceIntent.TransactionCount)
    }
}
