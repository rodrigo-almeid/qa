package com.loantrack.app.ui.loanform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.PaymentType
import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.domain.usecase.SaveLoanUseCase
import com.loantrack.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class LoanFormUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val debtorName: String = "",
    val contact: String = "",
    val loanDate: Date = DateUtils.today(),
    val dueDate: Date = DateUtils.today(),
    val amountLent: String = "",
    val amountToReceive: String = "",
    val paymentType: PaymentType = PaymentType.SINGLE,
    val installmentsTotal: String = "",
    val notes: String = "",
    val isEditMode: Boolean = false,
    val showAmountAlert: Boolean = false,
    val showPaidEditConfirm: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val amountError: String? = null,
    val dueDateError: String? = null
)

@HiltViewModel
class LoanFormViewModel @Inject constructor(
    private val saveLoanUseCase: SaveLoanUseCase,
    private val loanRepository: LoanRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanFormUiState())
    val uiState: StateFlow<LoanFormUiState> = _uiState.asStateFlow()

    private var existingLoan: Loan? = null
    private val userId get() = firebaseAuth.currentUser?.uid ?: ""

    fun loadLoan(loanId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val loan = loanRepository.getLoanById(userId, loanId)
            if (loan != null) {
                existingLoan = loan
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEditMode = true,
                    debtorName = loan.debtorName,
                    contact = loan.contact,
                    loanDate = loan.loanDate.toDate(),
                    dueDate = loan.dueDate.toDate(),
                    amountLent = loan.amountLent.toString(),
                    amountToReceive = loan.amountToReceive.toString(),
                    paymentType = PaymentType.valueOf(loan.paymentType),
                    installmentsTotal = loan.installmentsTotal?.toString() ?: "",
                    notes = loan.notes ?: ""
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateDebtorName(name: String) = updateState { copy(debtorName = name, nameError = null) }
    fun updateContact(contact: String) = updateState { copy(contact = contact) }
    fun updateLoanDate(date: Date) {
        updateState {
            copy(
                loanDate = date,
                dueDateError = if (dueDate.before(date)) "Data de vencimento inválida" else null
            )
        }
    }
    fun updateDueDate(date: Date) {
        val state = _uiState.value
        val error = if (date.before(state.loanDate)) "A data de vencimento deve ser igual ou posterior à data do empréstimo" else null
        updateState { copy(dueDate = date, dueDateError = error) }
    }
    fun updateAmountLent(amount: String) {
        updateState { copy(amountLent = amount) }
        checkAmountAlert()
    }
    fun updateAmountToReceive(amount: String) {
        updateState { copy(amountToReceive = amount, amountError = null) }
        checkAmountAlert()
    }
    fun updatePaymentType(type: PaymentType) = updateState { copy(paymentType = type) }
    fun updateInstallmentsTotal(count: String) = updateState { copy(installmentsTotal = count) }
    fun updateNotes(notes: String) = updateState { copy(notes = notes) }
    fun clearError() = updateState { copy(error = null) }

    private fun checkAmountAlert() {
        val state = _uiState.value
        val lent = state.amountLent.toDoubleOrNull() ?: return
        val toReceive = state.amountToReceive.toDoubleOrNull() ?: return
        updateState { copy(showAmountAlert = toReceive < lent) }
    }

    fun saveLoan() {
        val state = _uiState.value
        var hasError = false

        if (state.debtorName.isBlank()) {
            updateState { copy(nameError = "Campo obrigatório") }
            hasError = true
        }

        val amountToReceive = state.amountToReceive.toDoubleOrNull()
        if (amountToReceive == null || amountToReceive <= 0) {
            updateState { copy(amountError = "O valor a receber deve ser maior que zero") }
            hasError = true
        }

        if (state.dueDate.before(state.loanDate)) {
            updateState { copy(dueDateError = "A data de vencimento deve ser igual ou posterior à data do empréstimo") }
            hasError = true
        }

        if (hasError) return

        val existingLoanNow = existingLoan
        if (existingLoanNow?.status == LoanStatus.PAID.name && !state.showPaidEditConfirm) {
            updateState { copy(showPaidEditConfirm = true) }
            return
        }

        viewModelScope.launch {
            updateState { copy(isLoading = true, showPaidEditConfirm = false) }
            val amountLent = state.amountLent.toDoubleOrNull() ?: 0.0
            val now = Timestamp.now()

            val loan = Loan(
                id = existingLoanNow?.id ?: "",
                userId = userId,
                debtorName = state.debtorName.trim(),
                contact = state.contact.trim(),
                loanDate = DateUtils.toTimestamp(state.loanDate),
                dueDate = DateUtils.toTimestamp(state.dueDate),
                originalDueDate = existingLoanNow?.originalDueDate ?: DateUtils.toTimestamp(state.dueDate),
                amountLent = amountLent,
                amountToReceive = amountToReceive!!,
                amountPaid = existingLoanNow?.amountPaid ?: 0.0,
                paymentType = state.paymentType.name,
                installmentsTotal = if (state.paymentType == PaymentType.INSTALLMENT)
                    state.installmentsTotal.toIntOrNull() else null,
                installmentsPaid = existingLoanNow?.installmentsPaid,
                status = existingLoanNow?.status ?: LoanStatus.PENDING.name,
                notes = state.notes.trim().ifBlank { null },
                createdAt = existingLoanNow?.createdAt ?: now,
                updatedAt = now
            )

            val result = saveLoanUseCase(userId, loan)
            if (result.isSuccess) {
                updateState { copy(isLoading = false, isSaved = true) }
            } else {
                updateState {
                    copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message ?: "Erro ao salvar"
                    )
                }
            }
        }
    }

    fun dismissPaidEditConfirm() = updateState { copy(showPaidEditConfirm = false) }

    fun confirmPaidEdit() {
        updateState { copy(showPaidEditConfirm = false) }
        saveLoan()
    }

    private fun updateState(update: LoanFormUiState.() -> LoanFormUiState) {
        _uiState.value = _uiState.value.update()
    }
}
