package com.loantrack.app.ui.dashboard.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loantrack.app.R
import com.loantrack.app.ui.dashboard.LoanFilter

@Composable
fun FilterTabs(
    selectedFilter: LoanFilter,
    onFilterSelected: (LoanFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val filters = listOf(
        LoanFilter.ALL to stringResource(R.string.filter_todos),
        LoanFilter.PENDING to stringResource(R.string.filter_pendentes),
        LoanFilter.OVERDUE to stringResource(R.string.filter_atrasados),
        LoanFilter.PAID to stringResource(R.string.filter_pagos)
    )

    val selectedIndex = filters.indexOfFirst { it.first == selectedFilter }.coerceAtLeast(0)

    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp
    ) {
        filters.forEachIndexed { index, (filter, label) ->
            Tab(
                selected = selectedIndex == index,
                onClick = { onFilterSelected(filter) },
                text = { Text(text = label) }
            )
        }
    }
}
