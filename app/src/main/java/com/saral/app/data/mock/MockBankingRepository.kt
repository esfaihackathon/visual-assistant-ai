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

    // All transactions are in May 2026 (current month) for demo purposes
    private val allTransactions = listOf(
        Transaction(
            id = "TXN001",
            description = "Reliance Fresh",
            amount = 1200.0,
            date = "27 May",
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
        ),
        Transaction(
            id = "TXN006",
            description = "Netflix Subscription",
            amount = 649.0,
            date = "18 May",
            type = TransactionType.DEBIT
        ),
        Transaction(
            id = "TXN007",
            description = "Petrol Pump",
            amount = 1800.0,
            date = "15 May",
            type = TransactionType.DEBIT
        ),
        Transaction(
            id = "TXN008",
            description = "Insurance Premium",
            amount = 3500.0,
            date = "10 May",
            type = TransactionType.DEBIT
        ),
        Transaction(
            id = "TXN009",
            description = "ATM Withdrawal",
            amount = 5000.0,
            date = "8 May",
            type = TransactionType.DEBIT
        ),
        Transaction(
            id = "TXN010",
            description = "Dividend Credit",
            amount = 1200.0,
            date = "5 May",
            type = TransactionType.CREDIT
        )
    )

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
        return allTransactions
    }

    override suspend fun searchTransactions(keyword: String): List<Transaction> {
        delay(400)
        return allTransactions.filter { txn ->
            txn.description.contains(keyword, ignoreCase = true)
        }
    }
}
