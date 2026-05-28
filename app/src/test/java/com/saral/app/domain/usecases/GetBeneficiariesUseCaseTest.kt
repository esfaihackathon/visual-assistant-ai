package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GetBeneficiariesUseCaseTest {
    private lateinit var useCase: GetBeneficiariesUseCase

    @Before
    fun setup() {
        useCase = GetBeneficiariesUseCase(MockBankingRepository())
    }

    @Test
    fun invoke_returnsNonEmptyList() = runBlocking {
        val result = useCase()
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun invoke_returnsFiveBeneficiaries() = runBlocking {
        val result = useCase()
        assertEquals(5, result.size)
    }

    @Test
    fun invoke_eachBeneficiaryHasName() = runBlocking {
        val result = useCase()
        result.forEach { assertTrue(it.name.isNotBlank()) }
    }

    @Test
    fun invoke_eachBeneficiaryHasAccountLast4() = runBlocking {
        val result = useCase()
        result.forEach {
            assertTrue("accountLast4 must be 4 digits", it.accountLast4.matches(Regex("\\d{4}")))
        }
    }

    @Test
    fun invoke_eachBeneficiaryHasBankName() = runBlocking {
        val result = useCase()
        result.forEach { assertTrue(it.bankName.isNotBlank()) }
    }

    @Test
    fun invoke_eachBeneficiaryHasUniqueId() = runBlocking {
        val result = useCase()
        val ids = result.map { it.id }.toSet()
        assertEquals(result.size, ids.size)
    }

    @Test
    fun invoke_containsRahulSharma() = runBlocking {
        val result = useCase()
        assertTrue(result.any { it.name == "Rahul Sharma" })
    }

    @Test
    fun invoke_containsPriyaGupta() = runBlocking {
        val result = useCase()
        assertTrue(result.any { it.name == "Priya Gupta" })
    }

    @Test
    fun invoke_containsAmitKumar() = runBlocking {
        val result = useCase()
        assertTrue(result.any { it.name == "Amit Kumar" })
    }
}
