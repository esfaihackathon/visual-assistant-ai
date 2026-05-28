package com.saral.app.domain.usecases

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.Beneficiary
import javax.inject.Inject

class GetBeneficiariesUseCase @Inject constructor(
    private val repository: BankingRepository
) {
    suspend operator fun invoke(): List<Beneficiary> = repository.getBeneficiaries()
}
