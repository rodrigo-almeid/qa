package com.loantrack.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.remainingBalance
import com.loantrack.app.domain.usecase.GetLoansUseCase
import com.loantrack.app.domain.usecase.MarkPaidUseCase
import com.loantrack.app.util.DateUtils
import com.loantrack.app.worker.LoanStatusWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class LoanFilter {
    ALL, PENDING, OVERDUE, PARTIAL, PAID
}

enum class SortOrder {
    BY_DUE_DATE, BY_AMOUNT, BY_NAME
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val loans: List<Loan> = emptyList(),
    val filteredLoans: List<Loan> = emptyList(),
    val totalOutstanding: Double = 0.0,
    val receivedThisMonth: Double = 0.0,
    val selectedFilter: LoanFilter = LoanFilter.ALL,
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.BY_DUE_DATE,
    val selectedPeriodDate: java.util.Date? = null,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getLoansUseCase: GetLoansUseCase,
    private val markPaidUseCase: MarkPaidUseCase,
    private val firebaseAuth: FirebaseAuth,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = firebaseAuth.currentUser?.uid

    init {
        loadLoans()
        scheduleWorker()
    }

    private fun loadLoans() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            getLoansUseCase(userId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
                .collect { loans ->
                    val now = java.util.Date()
                    val outstanding = loans
                        .filter { it.status != LoanStatus.PAID.name }
                        .sumOf { it.remainingBalance() }

                    val receivedThisMonth = loans
                        .filter { loan ->
                            loan.paymentDate != null && DateUtils.isSameMonth(loan.paymentDate.toDate(), now)
                        }
                        .sumOf { it.amountPaid }

                    val currentState = _uiState.value
                    val filtered = applyFilters(
                        loans,
                        currentState.selectedFilter,
                        currentState.searchQuery,
                        currentState.sortOrder,
                        currentState.selectedPeriodDate
                    )

                    _uiState.value = currentState.copy(
                        isLoading = false,
                        loans = loans,
                        filteredLoans = filtered,
                        totalOutstanding = outstanding,
                        receivedThisMonth = receivedThisMonth
                    )
                }
        }
    }

    fun setFilter(filter: LoanFilter) {
        val state = _uiState.value
        val filtered = applyFilters(state.loans, filter, state.searchQuery, state.sortOrder, state.selectedPeriodDate)
        _uiState.value = state.copy(selectedFilter = filter, filteredLoans = filtered)
    }

    fun setSearchQuery(query: String) {
        val state = _uiState.value
        val filtered = applyFilters(state.loans, state.selectedFilter, query, state.sortOrder, state.selectedPeriodDate)
        _uiState.value = state.copy(searchQuery = query, filteredLoans = filtered)
    }

    fun setSortOrder(order: SortOrder) {
        val state = _uiState.value
        val filtered = applyFilters(state.loans, state.selectedFilter, state.searchQuery, order, state.selectedPeriodDate)
        _uiState.value = state.copy(sortOrder = order, filteredLoans = filtered)
    }

    fun setPeriodFilter(date: java.util.Date?) {
        val state = _uiState.value
        val filtered = applyFilters(state.loans, state.selectedFilter, state.searchQuery, state.sortOrder, date)
        _uiState.value = state.copy(selectedPeriodDate = date, filteredLoans = filtered)
    }

    fun markPaid(loan: Loan) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            markPaidUseCase(userId, loan)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun applyFilters(
        loans: List<Loan>,
        filter: LoanFilter,
        query: String,
        sort: SortOrder,
        periodDate: java.util.Date?
    ): List<Loan> {
        var result = loans

        result = when (filter) {
            LoanFilter.ALL -> result
            LoanFilter.PENDING -> result.filter { it.status == LoanStatus.PENDING.name }
            LoanFilter.OVERDUE -> result.filter { it.status == LoanStatus.OVERDUE.name }
            LoanFilter.PARTIAL -> result.filter { it.status == LoanStatus.PARTIAL.name }
            LoanFilter.PAID -> result.filter { it.status == LoanStatus.PAID.name }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.debtorName.contains(query, ignoreCase = true) ||
                        it.contact.contains(query, ignoreCase = true)
            }
        }

        if (periodDate != null) {
            result = result.filter { loan ->
                DateUtils.isSameMonth(loan.dueDate.toDate(), periodDate)
            }
        }

        result = when (sort) {
            SortOrder.BY_DUE_DATE -> result.sortedBy { it.dueDate.toDate() }
            SortOrder.BY_AMOUNT -> result.sortedByDescending { it.amountToReceive }
            SortOrder.BY_NAME -> result.sortedBy { it.debtorName }
        }

        return result
    }

    private fun scheduleWorker() {
        val request = PeriodicWorkRequestBuilder<LoanStatusWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "loan_status_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
