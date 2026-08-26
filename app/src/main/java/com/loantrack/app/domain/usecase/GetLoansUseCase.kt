package com.loantrack.app.domain.usecase

import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetLoansUseCase @Inject constructor(
    private val repository: LoanRepository
) {
    operator fun invoke(userId: String): Flow<List<Loan>> =
        repository.getLoans(userId)
}
