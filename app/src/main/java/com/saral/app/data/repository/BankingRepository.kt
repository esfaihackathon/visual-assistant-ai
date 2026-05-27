package com.saral.app.data.repository

import com.saral.app.domain.models.BankAccount
import com.saral.app.domain.models.ChequeBookRequest
import com.saral.app.domain.models.Transaction
import com.saral.app.domain.models.TransferResult

interface BankingRepository {
    suspend fun getBalance(): BankAccount
    suspend fun transferMoney(amount: Double, recipientName: String): TransferResult
    suspend fun requestChequeBook(): ChequeBookRequest
    suspend fun getRecentTransactions(): List<Transaction>
    suspend fun searchTransactions(keyword: String): List<Transaction>
}
