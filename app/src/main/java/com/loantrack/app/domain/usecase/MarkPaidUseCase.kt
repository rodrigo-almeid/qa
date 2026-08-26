package com.loantrack.app.domain.usecase

import com.google.firebase.Timestamp
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.repository.LoanRepository
import javax.inject.Inject

class MarkPaidUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    suspend operator fun invoke(userId: String, loan: Loan): Result<Unit> {
        if (loan.amountToReceive == 0.0) {
            return Result.failure(IllegalStateException("Cannot mark as paid when amountToReceive is 0"))
        }
        val now = Timestamp.now()
        val updatedLoan = loan.copy(
            amountPaid = loan.amountToReceive,
            paymentDate = now,
            status = LoanStatus.PAID.name,
            updatedAt = now
        )
        return repository.updateLoan(userId, updatedLoan)
    }
}
