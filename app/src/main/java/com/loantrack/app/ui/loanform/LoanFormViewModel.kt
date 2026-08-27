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
import com.loantrack.app.util.BrazilianPhoneTransformation
import com.loantrack.app.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject
import kotlin.math.roundToLong

data class LoanFormUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val debtorName: String = "",
    val contact: String = "",
    val loanDate: Date = DateUtils.today(),
    val dueDate: Date = DateUtils.addMonths(DateUtils.today(), 1),
    val amountLent: String = "",
    val amountToReceive: String = "",
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
                    contact = loan.contact.filter { it.isDigit() },
                    loanDate = loan.loanDate.toDate(),
                    dueDate = loan.dueDate.toDate(),
                    amountLent = (loan.amountLent * 100).roundToLong().toString(),
                    amountToReceive = (loan.amountToReceive * 100).roundToLong().toString(),
                    notes = loan.notes ?: ""
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateDebtorName(name: String) = updateState { copy(debtorName = name, nameError = null) }

    fun updateContact(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(11)
        updateState { copy(contact = digits) }
    }

    fun updateLoanDate(date: Date) {
        val state = _uiState.value
        val newDueDate = if (!state.isEditMode) DateUtils.addMonths(date, 1) else state.dueDate
        updateState {
            copy(
                loanDate = date,
                dueDate = newDueDate,
                dueDateError = if (newDueDate.before(date)) "Data de vencimento inválida" else null
            )
        }
    }

    fun updateDueDate(date: Date) {
        val state = _uiState.value
        val error = if (date.before(state.loanDate)) "A data de vencimento deve ser igual ou posterior à data do empréstimo" else null
        updateState { copy(dueDate = date, dueDateError = error) }
    }

    fun updateAmountLent(input: String) {
        val digits = input.filter { it.isDigit() }.trimStart('0').ifEmpty { "" }
        updateState { copy(amountLent = digits) }
        checkAmountAlert()
    }

    fun updateAmountToReceive(input: String) {
        val digits = input.filter { it.isDigit() }.trimStart('0').ifEmpty { "" }
        updateState { copy(amountToReceive = digits, amountError = null) }
        checkAmountAlert()
    }

    fun updateNotes(notes: String) = updateState { copy(notes = notes) }
    fun clearError() = updateState { copy(error = null) }

    private fun checkAmountAlert() {
        val state = _uiState.value
        val lent = state.amountLent.toLongOrNull() ?: return
        val toReceive = state.amountToReceive.toLongOrNull() ?: return
        if (lent > 0 && toReceive > 0) updateState { copy(showAmountAlert = toReceive < lent) }
    }

    fun saveLoan() {
        val state = _uiState.value
        var hasError = false

        if (state.debtorName.isBlank()) {
            updateState { copy(nameError = "Campo obrigatório") }
            hasError = true
        }

        val amountToReceiveLong = state.amountToReceive.toLongOrNull() ?: 0L
        if (amountToReceiveLong <= 0) {
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
            val amountLent = (state.amountLent.toLongOrNull() ?: 0L) / 100.0
            val amountToReceive = amountToReceiveLong / 100.0
            val now = Timestamp.now()

            val loan = Loan(
                id = existingLoanNow?.id ?: "",
                userId = userId,
                debtorName = state.debtorName.trim(),
                contact = BrazilianPhoneTransformation.format(state.contact.trim()),
                loanDate = DateUtils.toTimestamp(state.loanDate),
                dueDate = DateUtils.toTimestamp(state.dueDate),
                originalDueDate = existingLoanNow?.originalDueDate ?: DateUtils.toTimestamp(state.dueDate),
                amountLent = amountLent,
                amountToReceive = amountToReceive,
                amountPaid = existingLoanNow?.amountPaid ?: 0.0,
                paymentType = PaymentType.SINGLE.name,
                installmentsTotal = null,
                installmentsPaid = null,
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
                    copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Erro ao salvar")
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
