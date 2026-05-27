package com.saral.app.domain.models

import org.junit.Assert.*
import org.junit.Test

class BankAccountTest {

    @Test
    fun creation_withValidData() {
        val account = BankAccount(
            bankName = "SBI",
            accountLast4 = "7845",
            accountType = "Savings",
            balance = 25000.0
        )
        assertEquals("SBI", account.bankName)
        assertEquals("7845", account.accountLast4)
        assertEquals("Savings", account.accountType)
        assertEquals(25000.0, account.balance, 0.001)
    }

    @Test
    fun equality_sameValues() {
        val account1 = BankAccount("HDFC", "1234", "Current", 10000.0)
        val account2 = BankAccount("HDFC", "1234", "Current", 10000.0)
        assertEquals(account1, account2)
    }

    @Test
    fun inequality_differentValues() {
        val account1 = BankAccount("SBI", "7845", "Savings", 25000.0)
        val account2 = BankAccount("HDFC", "1234", "Current", 10000.0)
        assertNotEquals(account1, account2)
    }

    @Test
    fun copy_preservesValues() {
        val account = BankAccount("ICICI", "5678", "Savings", 50000.0)
        val copied = account.copy(balance = 60000.0)
        assertEquals("ICICI", copied.bankName)
        assertEquals("5678", copied.accountLast4)
        assertEquals(60000.0, copied.balance, 0.001)
    }

    @Test
    fun toString_containsValues() {
        val account = BankAccount("Axis", "9999", "Savings", 15000.0)
        val str = account.toString()
        assertTrue(str.contains("Axis"))
        assertTrue(str.contains("9999"))
    }
}
