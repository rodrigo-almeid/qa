package com.loantrack.app.ui.loandetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.domain.usecase.DeleteLoanUseCase
import com.loantrack.app.domain.usecase.MarkPaidUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoanDetailUiState(
    val loan: Loan? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
    val error: String? = null,
    val showDeleteDialog: Boolean = false,
    val showReopenDialog: Boolean = false
)

@HiltViewModel
class LoanDetailViewModel @Inject constructor(
    private val repository: LoanRepository,
    private val markPaidUseCase: MarkPaidUseCase,
    private val deleteLoanUseCase: DeleteLoanUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanDetailUiState())
    val uiState: StateFlow<LoanDetailUiState> = _uiState.asStateFlow()

    private val userId get() = auth.currentUser?.uid ?: ""

    fun loadLoan(loanId: String) {
        viewModelScope.launch {
            val loan = repository.getLoanById(userId, loanId)
            _uiState.value = _uiState.value.copy(loan = loan, isLoading = false)
        }
    }

    fun showDeleteDialog() = _uiState.value.let { _uiState.value = it.copy(showDeleteDialog = true) }
    fun dismissDeleteDialog() = _uiState.value.let { _uiState.value = it.copy(showDeleteDialog = false) }
    fun showReopenDialog() = _uiState.value.let { _uiState.value = it.copy(showReopenDialog = true) }
    fun dismissReopenDialog() = _uiState.value.let { _uiState.value = it.copy(showReopenDialog = false) }
    fun clearError() = _uiState.value.let { _uiState.value = it.copy(error = null) }

    fun markAsPaid() {
        val loan = _uiState.value.loan ?: return
        viewModelScope.launch {
            val result = markPaidUseCase(userId, loan)
            if (result.isSuccess) {
                val updated = repository.getLoanById(userId, loan.id)
                _uiState.value = _uiState.value.copy(loan = updated)
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteLoan() {
        val loan = _uiState.value.loan ?: return
        viewModelScope.launch {
            val result = deleteLoanUseCase(userId, loan.id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isDeleted = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    error = result.exceptionOrNull()?.message,
                    showDeleteDialog = false
                )
            }
        }
    }

    fun reopenLoan() {
        val loan = _uiState.value.loan ?: return
        viewModelScope.launch {
            val reopened = loan.copy(
                status = LoanStatus.PENDING.name,
                amountPaid = 0.0,
                paymentDate = null
            )
            val result = repository.updateLoan(userId, reopened)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(loan = reopened, showReopenDialog = false)
            }
        }
    }
}
