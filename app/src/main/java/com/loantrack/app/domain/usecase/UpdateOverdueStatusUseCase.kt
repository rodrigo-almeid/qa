package com.loantrack.app.domain.usecase

import com.google.firebase.Timestamp
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.computeStatus
import com.loantrack.app.data.repository.LoanRepository
import javax.inject.Inject

class UpdateOverdueStatusUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    suspend operator fun invoke(userId: String): Result<List<Loan>> {
        val loansResult = repository.getAllLoansOnce(userId)
        if (loansResult.isFailure) return Result.failure(loansResult.exceptionOrNull()!!)

        val loans = loansResult.getOrNull() ?: emptyList()
        val now = Timestamp.now()
        val loansToUpdate = mutableListOf<Loan>()

        for (loan in loans) {
            if (loan.status == LoanStatus.PAID.name) continue
            val computedStatus = loan.computeStatus()
            if (computedStatus.name != loan.status) {
                val updatedLoan = loan.copy(
                    status = computedStatus.name,
                    updatedAt = now
                )
                loansToUpdate.add(updatedLoan)
                repository.updateLoan(userId, updatedLoan)
            }
        }

        return Result.success(loans)
    }
}
