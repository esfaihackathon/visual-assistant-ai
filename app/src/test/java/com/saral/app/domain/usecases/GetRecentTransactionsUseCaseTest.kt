package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetRecentTransactionsUseCaseTest {
    private lateinit var repo: MockBankingRepository
    private lateinit var useCase: GetRecentTransactionsUseCase

    @Before
    fun setup() {
        repo = MockBankingRepository()
        useCase = GetRecentTransactionsUseCase(repo)
    }

    @Test
    fun invoke_returnsList() = runBlocking {
        val transactions = useCase.invoke()
        assertNotNull(transactions)
        assertTrue(transactions.isNotEmpty())
    }

    @Test
    fun invoke_returnsMultipleTransactions() = runBlocking {
        val transactions = useCase.invoke()
        assertTrue(transactions.size >= 5)
    }

    @Test
    fun invoke_includesTransactionDetails() = runBlocking {
        val transactions = useCase.invoke()
        val firstTxn = transactions.first()
        assertNotNull(firstTxn.id)
        assertNotNull(firstTxn.description)
        assertTrue(firstTxn.amount > 0)
        assertNotNull(firstTxn.date)
        assertNotNull(firstTxn.type)
    }

    @Test
    fun invoke_hasMixedCreditAndDebit() = runBlocking {
        val transactions = useCase.invoke()
        val creditExists = transactions.any { it.type.name == "CREDIT" }
        val debitExists = transactions.any { it.type.name == "DEBIT" }
        assertTrue(creditExists && debitExists)
    }
}
