package com.loantrack.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.data.repository.LoanRepository
import com.loantrack.app.util.CurrencyUtils
import com.loantrack.app.util.DateUtils
import com.loantrack.app.util.EmailConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Properties
import javax.mail.Message
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

@HiltWorker
class EmailReportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: LoanRepository,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure()
            val userEmail = auth.currentUser?.email ?: return@withContext Result.failure()
            val password = EmailConfig.getPassword(applicationContext)
            if (password.isBlank()) return@withContext Result.failure()

            val loans = repository.getLoansOnce(userId)
            val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.time
            val now = Date()

            val pending = loans.filter { it.status == LoanStatus.PENDING.name || it.status == LoanStatus.OVERDUE.name }
            val paidLast30 = loans.filter { loan ->
                loan.status == LoanStatus.PAID.name &&
                loan.paymentDate != null &&
                loan.paymentDate.toDate().after(thirtyDaysAgo)
            }

            val html = buildHtmlReport(pending, paidLast30, now)
            sendEmail(userEmail, password, html, now)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun buildHtmlReport(
        pending: List<com.loantrack.app.data.model.Loan>,
        paid: List<com.loantrack.app.data.model.Loan>,
        date: Date
    ): String {
        val totalPending = pending.sumOf { it.amountToReceive }
        val totalPaid = paid.sumOf { it.amountToReceive }

        val pendingRows = if (pending.isEmpty()) "<tr><td colspan='4' style='text-align:center;color:#888'>Nenhum empréstimo pendente</td></tr>"
        else pending.joinToString("") { loan ->
            val statusColor = if (loan.status == LoanStatus.OVERDUE.name) "#d32f2f" else "#1565C0"
            val statusLabel = if (loan.status == LoanStatus.OVERDUE.name) "Atrasado" else "Pendente"
            """<tr>
                <td>${loan.debtorName}</td>
                <td>${loan.contact}</td>
                <td>${DateUtils.formatDate(loan.dueDate)}</td>
                <td>${CurrencyUtils.format(loan.amountToReceive)}</td>
                <td style='color:$statusColor;font-weight:bold'>$statusLabel</td>
            </tr>"""
        }

        val paidRows = if (paid.isEmpty()) "<tr><td colspan='4' style='text-align:center;color:#888'>Nenhum pagamento nos últimos 30 dias</td></tr>"
        else paid.joinToString("") { loan ->
            """<tr>
                <td>${loan.debtorName}</td>
                <td>${loan.contact}</td>
                <td>${loan.paymentDate?.let { DateUtils.formatDate(it) } ?: "-"}</td>
                <td>${CurrencyUtils.format(loan.amountToReceive)}</td>
                <td style='color:#2e7d32;font-weight:bold'>Pago</td>
            </tr>"""
        }

        return """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8">
        <style>
            body { font-family: Arial, sans-serif; color: #333; max-width: 800px; margin: 0 auto; padding: 20px; }
            h1 { color: #1565C0; }
            h2 { color: #444; border-bottom: 2px solid #1565C0; padding-bottom: 8px; }
            table { width: 100%; border-collapse: collapse; margin-bottom: 24px; }
            th { background: #1565C0; color: white; padding: 10px; text-align: left; }
            td { padding: 8px 10px; border-bottom: 1px solid #eee; }
            tr:nth-child(even) { background: #f5f5f5; }
            .summary { display: flex; gap: 20px; margin-bottom: 24px; }
            .card { background: #f0f4ff; border-left: 4px solid #1565C0; padding: 12px 16px; border-radius: 4px; flex: 1; }
            .card.paid { background: #f0fff4; border-color: #2e7d32; }
            .amount { font-size: 22px; font-weight: bold; color: #1565C0; }
            .card.paid .amount { color: #2e7d32; }
        </style>
        </head>
        <body>
        <h1>Relatorio LoanTrack</h1>
        <p>Gerado em: ${DateUtils.formatDate(com.google.firebase.Timestamp(date))}</p>

        <div class="summary">
            <div class="card">
                <div>Total na rua (${pending.size} emprestimos)</div>
                <div class="amount">${CurrencyUtils.format(totalPending)}</div>
            </div>
            <div class="card paid">
                <div>Recebido nos ultimos 30 dias (${paid.size} pagamentos)</div>
                <div class="amount">${CurrencyUtils.format(totalPaid)}</div>
            </div>
        </div>

        <h2>Pendentes e Atrasados</h2>
        <table>
            <tr><th>Devedor</th><th>Contato</th><th>Vencimento</th><th>Valor</th><th>Status</th></tr>
            $pendingRows
        </table>

        <h2>Pagos nos ultimos 30 dias</h2>
        <table>
            <tr><th>Devedor</th><th>Contato</th><th>Data Pagamento</th><th>Valor</th><th>Status</th></tr>
            $paidRows
        </table>

        <p style="color:#888;font-size:12px">Relatorio automatico enviado pelo LoanTrack</p>
        </body></html>
        """.trimIndent()
    }

    private fun sendEmail(toEmail: String, password: String, html: String, date: Date) {
        val props = Properties().apply {
            put("mail.smtp.host", EmailConfig.getSmtpHost())
            put("mail.smtp.port", EmailConfig.getSmtpPort().toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.ssl.trust", EmailConfig.getSmtpHost())
        }

        val session = Session.getInstance(props, object : javax.mail.Authenticator() {
            override fun getPasswordAuthentication() =
                javax.mail.PasswordAuthentication(EmailConfig.getSmtpFrom(), password)
        })

        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(EmailConfig.getSmtpFrom(), "LoanTrack"))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
            subject = "LoanTrack - Relatorio ${DateUtils.formatDate(com.google.firebase.Timestamp(date))}"
            setContent(html, "text/html; charset=utf-8")
        }

        Transport.send(message)
    }
}
