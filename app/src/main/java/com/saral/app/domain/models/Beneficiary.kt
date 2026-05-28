package com.saral.app.domain.models

data class Beneficiary(
    val id: String,
    val name: String,
    val accountLast4: String,
    val bankName: String,
    val ifscCode: String
)
