package com.saral.app.domain.models

import org.junit.Assert.*
import org.junit.Test

class TransferResultTest {

    @Test
    fun creation_successfulTransfer() {
        val result = TransferResult(
            success = true,
            txnId = "TXN123456",
            amount = 500.0,
            recipientName = "Rahul"
        )
        assertTrue(result.success)
        assertEquals("TXN123456", result.txnId)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("Rahul", result.recipientName)
    }

    @Test
    fun creation_failedTransfer() {
        val result = TransferResult(
            success = false,
            txnId = "",
            amount = 0.0,
            recipientName = ""
        )
        assertFalse(result.success)
    }

    @Test
    fun equality_sameValues() {
        val result1 = TransferResult(true, "TXN001", 1000.0, "Priya")
        val result2 = TransferResult(true, "TXN001", 1000.0, "Priya")
        assertEquals(result1, result2)
    }

    @Test
    fun inequality_differentTxnId() {
        val result1 = TransferResult(true, "TXN001", 1000.0, "Priya")
        val result2 = TransferResult(true, "TXN002", 1000.0, "Priya")
        assertNotEquals(result1, result2)
    }

    @Test
    fun copy_preservesFields() {
        val result = TransferResult(true, "TXN999", 2500.0, "Amit")
        val copied = result.copy(amount = 3000.0)
        assertEquals("TXN999", copied.txnId)
        assertEquals(3000.0, copied.amount, 0.001)
        assertEquals("Amit", copied.recipientName)
        assertTrue(copied.success)
    }
}
