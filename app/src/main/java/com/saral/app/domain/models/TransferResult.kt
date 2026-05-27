package com.saral.app.domain.models

data class TransferResult(
    val success: Boolean,
    val txnId: String,
    val amount: Double,
    val recipientName: String
)
