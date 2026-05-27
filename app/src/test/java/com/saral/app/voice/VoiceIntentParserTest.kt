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

    @Test
    fun parsesCheckBalance() {
        val intent = parser.parse("How much money do I have in my account?")
        assertTrue(intent is VoiceIntent.CheckBalance)
    }

    @Test
    fun parsesTransferWithAmountAndRecipient() {
        val intent = parser.parse("Transfer 500 rupees to Rahul")
        assertTrue(intent is VoiceIntent.TransferMoney)
        intent as VoiceIntent.TransferMoney
        assertEquals(500.0, intent.amount)
        assertEquals("Rahul", intent.recipient)
    }

    @Test
    fun parsesTransferWithCommasAndDecimals() {
        val intent = parser.parse("Send 1,250.50 rupees to Amit")
        assertTrue(intent is VoiceIntent.TransferMoney)
        intent as VoiceIntent.TransferMoney
        assertEquals(1250.50, intent.amount!!, 0.001)
        assertEquals("Amit", intent.recipient)
    }

    @Test
    fun parsesConfirmYesAndNo() {
        val yes = parser.parse("Yes")
        val no = parser.parse("No")
        assertTrue(yes is VoiceIntent.ConfirmYes)
        assertTrue(no is VoiceIntent.ConfirmNo)
    }

    @Test
    fun parsesChequeBookRequest() {
        val intent = parser.parse("Request cheque book")
        assertTrue(intent is VoiceIntent.RequestChequeBook)
    }

    @Test
    fun parsesRecentTransactions() {
        val intent = parser.parse("Show recent transactions")
        assertTrue(intent is VoiceIntent.RecentTransactions)
    }

    @Test
    fun parsesHelp() {
        val intent = parser.parse("Help")
        assertTrue(intent is VoiceIntent.Help)
    }

    @Test
    fun unknownWhenNoPatternMatches() {
        val intent = parser.parse("Blah blah gibberish")
        assertTrue(intent is VoiceIntent.Unknown)
    }
}
