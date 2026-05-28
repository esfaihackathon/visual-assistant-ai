package com.saral.app.presentation.transfer

import com.saral.app.domain.models.Beneficiary

sealed class TransferStep {
    object SelectingBeneficiary : TransferStep()
    data class EnterAmount(val beneficiary: Beneficiary) : TransferStep()
    data class ConfirmTransfer(val beneficiary: Beneficiary, val amount: Double) : TransferStep()
    data class AwaitingBiometric(val beneficiary: Beneficiary, val amount: Double) : TransferStep()
    data class Complete(
        val beneficiary: Beneficiary,
        val amount: Double,
        val remainingBalance: Double,
        val txnId: String
    ) : TransferStep()
    object Failed : TransferStep()
}
