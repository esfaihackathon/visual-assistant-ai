package com.saral.app.domain.models

data class BankAccount(
    val bankName: String,
    val accountLast4: String,
    val accountType: String,
    val balance: Double
)
