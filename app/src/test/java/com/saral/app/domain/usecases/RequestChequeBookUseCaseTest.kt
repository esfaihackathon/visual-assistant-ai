package com.saral.app.domain.usecases

import com.saral.app.data.mock.MockBankingRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RequestChequeBookUseCaseTest {
    private lateinit var repo: MockBankingRepository
    private lateinit var useCase: RequestChequeBookUseCase

    @Before
    fun setup() {
        repo = MockBankingRepository()
        useCase = RequestChequeBookUseCase(repo)
    }

    @Test
    fun invoke_requestsChequeBook() = runBlocking {
        val result = useCase.invoke()
        assertNotNull(result)
        assertEquals("REQUESTED", result.status)
    }

    @Test
    fun invoke_providesEstimatedDelivery() = runBlocking {
        val result = useCase.invoke()
        assertTrue(result.estimatedDeliveryDays > 0)
        assertEquals(5, result.estimatedDeliveryDays)
    }
}
