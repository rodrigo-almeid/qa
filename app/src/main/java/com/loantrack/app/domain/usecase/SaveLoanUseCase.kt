package com.loantrack.app.domain.usecase

import com.google.firebase.Timestamp
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.UserTier
import com.loantrack.app.data.model.computeStatus
import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.data.repository.UserRepository
import javax.inject.Inject

class SaveLoanUseCase @Inject constructor(
    private val repository: LoanRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, loan: Loan): Result<Unit> {
        val user = userRepository.getCurrentUser()
        val tier = user.userTier

        if (loan.id.isNotBlank()) {
            if (tier == UserTier.BRONZE) {
                return Result.failure(
                    IllegalStateException("Perfil Bronze não permite editar empréstimos.")
                )
            }
            val existing = repository.getLoanById(userId, loan.id)
            if (existing?.status == LoanStatus.PAID.name) {
                return Result.failure(
                    IllegalStateException("Não é permitido editar empréstimos com status PAGO.")
                )
            }
        } else {
            val activeCount = repository.getActiveLoansCount(userId)
            if (activeCount >= tier.maxActiveLoans) {
                return Result.failure(
                    IllegalStateException(
                        "Limite de ${tier.maxActiveLoans} empréstimos ativos atingido para o perfil ${tier.name}."
                    )
                )
            }
        }

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
