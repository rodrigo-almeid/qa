package com.loantrack.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.loantrack.app.R
import com.loantrack.app.ui.dashboard.components.FilterTabs
import com.loantrack.app.ui.dashboard.components.LoanCard
import com.loantrack.app.ui.dashboard.components.SummaryCards

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddLoan: () -> Unit,
    onNavigateToLoanDetail: (String) -> Unit,
    onSignOut: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }
    var searchActive by remember { mutableStateOf(false) }
    var confirmPayLoan by remember { mutableStateOf<com.loantrack.app.data.model.Loan?>(null) }

    if (confirmPayLoan != null) {
        val loan = confirmPayLoan!!
        AlertDialog(
            onDismissRequest = { confirmPayLoan = null },
            title = { Text(stringResource(R.string.dar_baixa)) },
            text = { Text(stringResource(R.string.confirm_dar_baixa, loan.debtorName)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markPaid(loan)
                    confirmPayLoan = null
                }) { Text(stringResource(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPayLoan = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            Text(
                                text = stringResource(R.string.sort_options),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_due_date)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.BY_DUE_DATE)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_amount)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.BY_AMOUNT)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_name)) },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.BY_NAME)
                                    showSortMenu = false
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sign_out)) },
                                leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
                                onClick = {
                                    showSortMenu = false
                                    onSignOut()
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddLoan) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_loan))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SummaryCards(
                totalOutstanding = uiState.totalOutstanding,
                receivedThisMonth = uiState.receivedThisMonth,
                modifier = Modifier.padding(16.dp)
            )

            FilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            if (searchActive) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    singleLine = true
                )
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredLoans.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_loans),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredLoans, key = { it.id }) { loan ->
                        LoanCard(
                            loan = loan,
                            onClick = { onNavigateToLoanDetail(loan.id) },
                            onDarBaixa = { confirmPayLoan = loan }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        uiState.error?.let { error ->
            LaunchedEffect(error) {
                viewModel.clearError()
            }
        }
    }
}
