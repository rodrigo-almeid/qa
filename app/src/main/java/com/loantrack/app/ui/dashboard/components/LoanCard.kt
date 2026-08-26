package com.loantrack.app.ui.dashboard.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loantrack.app.R
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.model.remainingBalance
import com.loantrack.app.ui.theme.StatusOverdue
import com.loantrack.app.ui.theme.StatusPaid
import com.loantrack.app.ui.theme.StatusPartial
import com.loantrack.app.ui.theme.StatusPending
import com.loantrack.app.util.CurrencyUtils
import com.loantrack.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanCard(
    loan: Loan,
    onClick: () -> Unit,
    onDarBaixa: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = loan.debtorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                StatusBadge(status = loan.status)
            }

            if (loan.contact.isNotBlank()) {
                Text(
                    text = loan.contact,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Valor",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = CurrencyUtils.format(loan.amountToReceive),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vencimento",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = DateUtils.formatDate(loan.dueDate),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (loan.amountPaid > 0 && loan.status != LoanStatus.PAID.name) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Restante: ${CurrencyUtils.format(loan.remainingBalance())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = StatusPartial
                )
            }

            if (loan.status != LoanStatus.PAID.name) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDarBaixa,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.dar_baixa))
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        LoanStatus.PENDING.name -> stringResource(R.string.status_pending) to StatusPending
        LoanStatus.PAID.name -> stringResource(R.string.status_paid) to StatusPaid
        LoanStatus.OVERDUE.name -> stringResource(R.string.status_overdue) to StatusOverdue
        LoanStatus.PARTIAL.name -> stringResource(R.string.status_partial) to StatusPartial
        else -> status to MaterialTheme.colorScheme.outline
    }

    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.12f)
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(
            enabled = true,
            borderColor = color.copy(alpha = 0.3f)
        )
    )
}
