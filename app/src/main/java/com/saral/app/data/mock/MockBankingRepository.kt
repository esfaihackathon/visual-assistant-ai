package com.saral.app.data.mock

import com.saral.app.data.repository.BankingRepository
import com.saral.app.domain.models.BankAccount
import com.saral.app.domain.models.ChequeBookRequest
import com.saral.app.domain.models.Transaction
import com.saral.app.domain.models.TransactionType
import com.saral.app.domain.models.TransferResult
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockBankingRepository @Inject constructor() : BankingRepository {

    private var currentBalance = 25000.0

    override suspend fun getBalance(): BankAccount {
        delay(500)
        return BankAccount(
            bankName = "SBI",
            accountLast4 = "7845",
            accountType = "Savings",
            balance = currentBalance
        )
    }

    override suspend fun transferMoney(amount: Double, recipientName: String): TransferResult {
        delay(1000)
        currentBalance -= amount
        val txnId = "TXN${(100000..999999).random()}"
        return TransferResult(
            success = true,
            txnId = txnId,
            amount = amount,
            recipientName = recipientName
        )
    }

    override suspend fun requestChequeBook(): ChequeBookRequest {
        delay(800)
        return ChequeBookRequest(
            status = "REQUESTED",
            estimatedDeliveryDays = 5
        )
    }

    override suspend fun getRecentTransactions(): List<Transaction> {
        delay(600)
        return listOf(
            Transaction(
                id = "TXN001",
                description = "Reliance Fresh",
                amount = 1200.0,
                date = "Yesterday",
                type = TransactionType.DEBIT
            ),
            Transaction(
                id = "TXN002",
                description = "Salary Credit",
                amount = 45000.0,
                date = "25 May",
                type = TransactionType.CREDIT
            ),
            Transaction(
                id = "TXN003",
                description = "Electricity Bill",
                amount = 2350.0,
                date = "24 May",
                type = TransactionType.DEBIT
            ),
            Transaction(
                id = "TXN004",
                description = "Amazon Purchase",
                amount = 899.0,
                date = "22 May",
                type = TransactionType.DEBIT
            ),
            Transaction(
                id = "TXN005",
                description = "UPI from Amit",
                amount = 500.0,
                date = "20 May",
                type = TransactionType.CREDIT
            )
        )
    }
}
