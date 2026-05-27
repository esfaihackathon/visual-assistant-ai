package com.saral.app.data.mock

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MockBankingRepositoryTest {
    private lateinit var repo: MockBankingRepository

    @Before
    fun setup() {
        repo = MockBankingRepository()
    }

    @Test
    fun getBalance_returnsAccount() = runBlocking {
        val account = repo.getBalance()
        assertEquals("SBI", account.bankName)
        assertEquals("7845", account.accountLast4)
        assertTrue(account.balance > 0)
    }

    @Test
    fun transferMoney_reducesBalance_and_returnsTxnId() = runBlocking {
        val before = repo.getBalance().balance
        val result = repo.transferMoney(500.0, "Rahul")
        val after = repo.getBalance().balance
        assertTrue(result.success)
        assertTrue(result.txnId.startsWith("TXN"))
        assertEquals(before - 500.0, after, 0.001)
    }

    @Test
    fun requestChequeBook_returnsEstimate() = runBlocking {
        val req = repo.requestChequeBook()
        assertEquals("REQUESTED", req.status)
        assertTrue(req.estimatedDeliveryDays > 0)
    }

    @Test
    fun getRecentTransactions_returnsList() = runBlocking {
        val txns = repo.getRecentTransactions()
        assertTrue(txns.isNotEmpty())
    }
}
