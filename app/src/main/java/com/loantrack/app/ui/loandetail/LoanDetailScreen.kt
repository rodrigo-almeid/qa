package com.loantrack.app.ui.loandetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loantrack.app.R
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.remainingBalance
import com.loantrack.app.ui.dashboard.components.StatusBadge
import com.loantrack.app.util.CurrencyUtils
import com.loantrack.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanDetailScreen(
    loanId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: LoanDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(loanId) { viewModel.loadLoan(loanId) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDeleteDialog,
            title = { Text(stringResource(R.string.delete_loan)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(
                    onClick = viewModel::deleteLoan,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteDialog) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (uiState.showReopenDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissReopenDialog,
            title = { Text(stringResource(R.string.reopen_loan)) },
            text = { Text(stringResource(R.string.confirm_reopen)) },
            confirmButton = {
                TextButton(onClick = viewModel::reopenLoan) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissReopenDialog) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.loan_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { uiState.loan?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = viewModel::showDeleteDialog) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_loan),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.loan == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) { Text("Empréstimo não encontrado") }
            }
            else -> {
                val loan = uiState.loan!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = loan.debtorName,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (loan.contact.isNotBlank()) {
                                Text(
                                    text = loan.contact,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        StatusBadge(status = loan.status)
                    }

                    HorizontalDivider()

                    DetailRow(stringResource(R.string.amount_lent), CurrencyUtils.format(loan.amountLent))
                    DetailRow(stringResource(R.string.amount_to_receive), CurrencyUtils.format(loan.amountToReceive))
                    if (loan.amountPaid > 0) {
                        DetailRow(stringResource(R.string.amount_paid), CurrencyUtils.format(loan.amountPaid))
                        DetailRow(
                            stringResource(R.string.remaining_balance),
                            CurrencyUtils.format(loan.remainingBalance()),
                            highlight = true
                        )
                    }

                    HorizontalDivider()

                    DetailRow(stringResource(R.string.loan_date), DateUtils.formatDate(loan.loanDate))
                    DetailRow(stringResource(R.string.due_date), DateUtils.formatDate(loan.dueDate))
                    if (loan.originalDueDate.seconds != loan.dueDate.seconds) {
                        DetailRow(stringResource(R.string.original_due_date), DateUtils.formatDate(loan.originalDueDate))
                    }
                    loan.paymentDate?.let {
                        DetailRow(stringResource(R.string.payment_date), DateUtils.formatDate(it))
                    }

                    if (loan.paymentType == com.loantrack.app.data.model.PaymentType.INSTALLMENT.name) {
                        HorizontalDivider()
                        DetailRow(
                            stringResource(R.string.installments_paid),
                            "${loan.installmentsPaid ?: 0}/${loan.installmentsTotal ?: 0}"
                        )
                    }

                    if (!loan.notes.isNullOrBlank()) {
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.notes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(text = loan.notes, style = MaterialTheme.typography.bodyMedium)
                    }

                    HorizontalDivider()

                    if (loan.status != LoanStatus.PAID.name) {
                        Button(
                            onClick = viewModel::markAsPaid,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.dar_baixa_total)) }
                    } else {
                        OutlinedButton(
                            onClick = viewModel::showReopenDialog,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(stringResource(R.string.reopen_loan)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
