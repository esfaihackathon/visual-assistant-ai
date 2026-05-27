package com.saral.app.domain.usecases

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.TransferResult
import javax.inject.Inject

class TransferMoneyUseCase @Inject constructor(
    private val repository: BankingRepository
) {
    suspend operator fun invoke(amount: Double, recipientName: String): TransferResult =
        repository.transferMoney(amount, recipientName)
}
