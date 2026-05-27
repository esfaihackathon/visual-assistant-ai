package com.saral.app.domain.models

import org.junit.Assert.*
import org.junit.Test

class TransactionTest {

    @Test
    fun creation_creditTransaction() {
        val txn = Transaction(
            id = "TXN001",
            description = "Salary Credit",
            amount = 45000.0,
            date = "25 May",
            type = TransactionType.CREDIT
        )
        assertEquals("TXN001", txn.id)
        assertEquals("Salary Credit", txn.description)
        assertEquals(45000.0, txn.amount, 0.001)
        assertEquals("25 May", txn.date)
        assertEquals(TransactionType.CREDIT, txn.type)
    }

    @Test
    fun creation_debitTransaction() {
        val txn = Transaction(
            id = "TXN002",
            description = "Amazon Purchase",
            amount = 899.0,
            date = "22 May",
            type = TransactionType.DEBIT
        )
        assertEquals(TransactionType.DEBIT, txn.type)
        assertEquals(899.0, txn.amount, 0.001)
    }

    @Test
    fun equality_sameValues() {
        val txn1 = Transaction("ID1", "Expense", 100.0, "Today", TransactionType.DEBIT)
        val txn2 = Transaction("ID1", "Expense", 100.0, "Today", TransactionType.DEBIT)
        assertEquals(txn1, txn2)
    }

    @Test
    fun inequality_differentIds() {
        val txn1 = Transaction("ID1", "Expense", 100.0, "Today", TransactionType.DEBIT)
        val txn2 = Transaction("ID2", "Expense", 100.0, "Today", TransactionType.DEBIT)
        assertNotEquals(txn1, txn2)
    }

    @Test
    fun copy_changesAmount() {
        val txn = Transaction("TXN001", "Bill", 500.0, "Today", TransactionType.DEBIT)
        val copied = txn.copy(amount = 750.0)
        assertEquals("TXN001", copied.id)
        assertEquals(750.0, copied.amount, 0.001)
        assertEquals(TransactionType.DEBIT, copied.type)
    }
}
