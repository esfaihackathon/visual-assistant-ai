package com.saral.app.di

import com.saral.app.data.mock.MockBankingRepository
import com.saral.app.data.repository.BankingRepository
import org.junit.Assert.*
import org.junit.Test

class AppModuleTest {

    @Test
    fun mockBankingRepository_implementsBankingRepository() {
        val repo = MockBankingRepository()
        assertTrue(repo is BankingRepository)
    }

    @Test
    fun mockBankingRepository_canBeUsedAsInterface() {
        val repo: BankingRepository = MockBankingRepository()
        assertNotNull(repo)
    }

    @Test
    fun bindingStrategy_mockRepoHasAllMethods() {
        val repo = MockBankingRepository()
        assertTrue(repo.javaClass.methods.any { it.name == "getBalance" })
        assertTrue(repo.javaClass.methods.any { it.name == "transferMoney" })
        assertTrue(repo.javaClass.methods.any { it.name == "requestChequeBook" })
        assertTrue(repo.javaClass.methods.any { it.name == "getRecentTransactions" })
    }
}
