package com.saral.app.domain.models

data class Transaction(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String,
    val type: TransactionType
)

enum class TransactionType {
    CREDIT, DEBIT
}
