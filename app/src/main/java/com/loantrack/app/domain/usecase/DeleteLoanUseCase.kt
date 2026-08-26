package com.loantrack.app.domain.usecase

import com.loantrack.app.data.repository.LoanRepository
import javax.inject.Inject

class DeleteLoanUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    suspend operator fun invoke(userId: String, loanId: String): Result<Unit> =
        repository.deleteLoan(userId, loanId)
}
