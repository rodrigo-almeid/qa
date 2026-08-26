package com.loantrack.app.domain.usecase

import com.google.firebase.Timestamp
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.computeStatus
import com.loantrack.app.data.repository.LoanRepository
import javax.inject.Inject

class PartialPaymentUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    suspend operator fun invoke(userId: String, loan: Loan, paymentAmount: Double): Result<Unit> {
        if (paymentAmount <= 0) {
            return Result.failure(IllegalArgumentException("Payment amount must be greater than zero"))
        }
        val now = Timestamp.now()
        val newAmountPaid = loan.amountPaid + paymentAmount
        val isPaid = newAmountPaid >= loan.amountToReceive

        val updatedLoan = loan.copy(
            amountPaid = newAmountPaid,
            paymentDate = if (isPaid) now else loan.paymentDate,
            status = if (isPaid) LoanStatus.PAID.name else loan.computeStatus().let { currentStatus ->
                if (newAmountPaid > 0) LoanStatus.PARTIAL.name else currentStatus.name
            },
            updatedAt = now
        )
        return repository.updateLoan(userId, updatedLoan)
    }
}
