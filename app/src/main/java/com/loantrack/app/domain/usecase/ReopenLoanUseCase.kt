package com.loantrack.app.domain.usecase

import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.UserTier
import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.data.repository.UserRepository
import javax.inject.Inject

class ReopenLoanUseCase @Inject constructor(
    private val repository: LoanRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(userId: String, loanId: String): Result<Unit> {
        val user = userRepository.getCurrentUser()
        val tier = user.userTier

        if (tier == UserTier.BRONZE) {
            return Result.failure(
                IllegalStateException("Perfil Bronze não permite reabrir empréstimos.")
            )
        }

        val activeCount = repository.getActiveLoansCount(userId)
        if (activeCount >= tier.maxActiveLoans) {
            return Result.failure(
                IllegalStateException(
                    "Limite de ${tier.maxActiveLoans} empréstimos ativos atingido para o perfil ${tier.name}."
                )
            )
        }

        val loan = repository.getLoanById(userId, loanId)
            ?: return Result.failure(IllegalStateException("Empréstimo não encontrado."))

        if (loan.status != LoanStatus.PAID.name) {
            return Result.failure(IllegalStateException("Apenas empréstimos PAGO podem ser reabertos."))
        }

        val reopened = loan.copy(
            status = LoanStatus.PENDING.name,
            amountPaid = 0.0,
            paymentDate = null
        )
        return repository.updateLoan(userId, reopened)
    }
}
