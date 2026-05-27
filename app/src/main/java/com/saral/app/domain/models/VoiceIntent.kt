package com.saral.app.domain.models

sealed class VoiceIntent {
    data object CheckBalance : VoiceIntent()
    data class TransferMoney(val amount: Double?, val recipient: String?) : VoiceIntent()
    data object RequestChequeBook : VoiceIntent()
    data object RecentTransactions : VoiceIntent()
    data object Help : VoiceIntent()
    data object Unknown : VoiceIntent()
    data object ConfirmYes : VoiceIntent()
    data object ConfirmNo : VoiceIntent()
}
