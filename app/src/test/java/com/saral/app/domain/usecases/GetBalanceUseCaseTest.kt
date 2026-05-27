package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetBalanceUseCaseTest {
    private lateinit var repo: MockBankingRepository
    private lateinit var useCase: GetBalanceUseCase

    @Before
    fun setup() {
        repo = MockBankingRepository()
        useCase = GetBalanceUseCase(repo)
    }

    @Test
    fun invoke_returnsAccountBalance() = runBlocking {
        val account = useCase.invoke()
        assertNotNull(account)
        assertEquals("SBI", account.bankName)
        assertEquals("7845", account.accountLast4)
        assertTrue(account.balance > 0)
    }

    @Test
    fun invoke_returnsSavingsAccount() = runBlocking {
        val account = useCase.invoke()
        assertEquals("Savings", account.accountType)
    }
}
