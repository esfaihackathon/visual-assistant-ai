package com.saral.app.domain.usecases

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.Transaction
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val repository: BankingRepository
) {
    suspend operator fun invoke(): List<Transaction> = repository.getRecentTransactions()
}
