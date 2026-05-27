package com.saral.app.domain.usecases

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.BankAccount
import javax.inject.Inject

class GetBalanceUseCase @Inject constructor(
    private val repository: BankingRepository
) {
    suspend operator fun invoke(): BankAccount = repository.getBalance()
}
