package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TransferMoneyUseCaseTest {
    private lateinit var repo: MockBankingRepository
    private lateinit var useCase: TransferMoneyUseCase

    @Before
    fun setup() {
        repo = MockBankingRepository()
        useCase = TransferMoneyUseCase(repo)
    }

    @Test
    fun invoke_transfersMoneySuccessfully() = runBlocking {
        val result = useCase.invoke(500.0, "Rahul")
        assertTrue(result.success)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("Rahul", result.recipientName)
    }

    @Test
    fun invoke_generatesTransactionId() = runBlocking {
        val result = useCase.invoke(1000.0, "Priya")
        assertTrue(result.txnId.startsWith("TXN"))
        assertTrue(result.txnId.length > 3)
    }

    @Test
    fun invoke_handlesVariousAmounts() = runBlocking {
        val result1 = useCase.invoke(100.0, "Amit")
        val result2 = useCase.invoke(50000.0, "Neha")
        assertTrue(result1.success)
        assertTrue(result2.success)
        assertEquals(100.0, result1.amount, 0.001)
        assertEquals(50000.0, result2.amount, 0.001)
    }
}
