package com.saral.app.domain.usecases

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.ChequeBookRequest
import javax.inject.Inject

class RequestChequeBookUseCase @Inject constructor(
    private val repository: BankingRepository
) {
    suspend operator fun invoke(): ChequeBookRequest = repository.requestChequeBook()
}
