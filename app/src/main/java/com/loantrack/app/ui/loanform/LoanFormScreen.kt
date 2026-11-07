package com.loantrack.app.ui.loanform

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loantrack.app.R
import com.loantrack.app.util.BrazilianPhoneTransformation
import com.loantrack.app.util.CurrencyVisualTransformation
import com.loantrack.app.util.DateUtils
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanFormScreen(
    loanId: String?,
    onNavigateBack: () -> Unit,
    viewModel: LoanFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(loanId) {
        if (loanId != null) viewModel.loadLoan(loanId)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    fun showDatePicker(initialDate: Date, onDateSelected: (Date) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initialDate }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                onDateSelected(selected)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode)
                            stringResource(R.string.edit_loan_title)
                        else
                            stringResource(R.string.add_loan_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isReadOnly) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Empréstimos com status PAGO não podem ser editados. Reabra o empréstimo para editar.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.debtorName,
                    onValueChange = { if (!uiState.isReadOnly) viewModel.updateDebtorName(it) },
                    label = { Text(stringResource(R.string.debtor_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadOnly,
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.contact,
                    onValueChange = { if (!uiState.isReadOnly) viewModel.updateContact(it) },
                    label = { Text(stringResource(R.string.contact)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadOnly,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    visualTransformation = remember { BrazilianPhoneTransformation() },
                    singleLine = true
                )

                OutlinedTextField(
                    value = DateUtils.formatDate(uiState.loanDate),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.loan_date)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        if (!uiState.isReadOnly) {
                            IconButton(onClick = {
                                showDatePicker(uiState.loanDate) { viewModel.updateLoanDate(it) }
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = DateUtils.formatDate(uiState.dueDate),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.due_date)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    isError = uiState.dueDateError != null,
                    supportingText = uiState.dueDateError?.let { { Text(it) } },
                    trailingIcon = {
                        if (!uiState.isReadOnly) {
                            IconButton(onClick = {
                                showDatePicker(uiState.dueDate) { viewModel.updateDueDate(it) }
                            }) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                            }
                        }
                    }
                )

                OutlinedTextField(
                    value = uiState.amountLent,
                    onValueChange = { if (!uiState.isReadOnly) viewModel.updateAmountLent(it) },
                    label = { Text(stringResource(R.string.amount_lent)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadOnly,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = remember { CurrencyVisualTransformation() },
                    singleLine = true
                )

                OutlinedTextField(
                    value = uiState.amountToReceive,
                    onValueChange = { if (!uiState.isReadOnly) viewModel.updateAmountToReceive(it) },
                    label = { Text(stringResource(R.string.amount_to_receive)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadOnly,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = remember { CurrencyVisualTransformation() },
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    singleLine = true
                )

                if (uiState.showAmountAlert) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.amount_to_receive_less_than_lent),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                OutlinedTextField(
                    value = uiState.notes,
                    onValueChange = { if (!uiState.isReadOnly) viewModel.updateNotes(it) },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = uiState.isReadOnly,
                    minLines = 3,
                    maxLines = 5
                )

                if (!uiState.isReadOnly) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.saveLoan() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
