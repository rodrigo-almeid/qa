package com.loantrack.app.domain.usecase

import com.google.firebase.Timestamp
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.computeStatus
import com.loantrack.app.data.repository.LoanRepository
import javax.inject.Inject

class SaveLoanUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    suspend operator fun invoke(userId: String, loan: Loan): Result<Unit> {
        val now = Timestamp.now()
        val status = loan.computeStatus()
        val loanToSave = loan.copy(
            userId = userId,
            status = status.name,
            createdAt = if (loan.id.isBlank()) now else loan.createdAt,
            updatedAt = now
        )
        return repository.saveLoan(userId, loanToSave)
    }
}
