package com.loantrack.app.data.repository

import com.loantrack.app.data.model.Loan
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getLoans(userId: String): Flow<List<Loan>>
    suspend fun getLoanById(userId: String, loanId: String): Loan?
    suspend fun saveLoan(userId: String, loan: Loan): Result<Unit>
    suspend fun deleteLoan(userId: String, loanId: String): Result<Unit>
    suspend fun updateLoan(userId: String, loan: Loan): Result<Unit>
    suspend fun getAllLoansOnce(userId: String): Result<List<Loan>>
}
