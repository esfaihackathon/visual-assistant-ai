package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import com.saral.app.domain.models.TransactionType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SearchTransactionsUseCaseTest {
    private lateinit var useCase: SearchTransactionsUseCase

    @Before
    fun setup() {
        useCase = SearchTransactionsUseCase(MockBankingRepository())
    }

    @Test
    fun search_salary_returnsMatch() = runTest {
        val results = useCase("salary")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.description.contains("salary", ignoreCase = true) })
    }

    @Test
    fun search_electricity_returnsMatch() = runTest {
        val results = useCase("electricity")
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.description.contains("electricity", ignoreCase = true) })
    }

    @Test
    fun search_amazon_returnsMatch() = runTest {
        val results = useCase("amazon")
        assertTrue(results.isNotEmpty())
        assertEquals("Amazon Purchase", results.first().description)
        assertEquals(TransactionType.DEBIT, results.first().type)
    }

    @Test
    fun search_netflix_returnsMatch() = runTest {
        val results = useCase("netflix")
        assertTrue(results.isNotEmpty())
        assertEquals(649.0, results.first().amount, 0.001)
    }

    @Test
    fun search_caseInsensitive_salary() = runTest {
        val lower = useCase("SALARY")
        val upper = useCase("salary")
        assertEquals(lower.size, upper.size)
    }

    @Test
    fun search_unknownKeyword_returnsEmpty() = runTest {
        val results = useCase("rent")
        assertTrue(results.isEmpty())
    }

    @Test
    fun search_partialMatch_works() = runTest {
        // "Salary Credit" contains "redit" — partial match should NOT return it
        val results = useCase("redit")
        // "Salary Credit" and "Dividend Credit" and "UPI from Amit" — "Credit" contains "redit"
        assertTrue(results.all { it.description.contains("redit", ignoreCase = true) })
    }

    @Test
    fun search_salary_isCreditType() = runTest {
        val results = useCase("salary")
        assertEquals(TransactionType.CREDIT, results.first().type)
        assertEquals(45000.0, results.first().amount, 0.001)
    }

    @Test
    fun search_electricity_isDebitType() = runTest {
        val results = useCase("electricity")
        assertEquals(TransactionType.DEBIT, results.first().type)
        assertEquals(2350.0, results.first().amount, 0.001)
    }
}
