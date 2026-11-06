package com.loantrack.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.loantrack.app.data.model.Loan
import com.loantrack.app.data.model.LoanStatus
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File

object ExportUtils {

    fun createXls(context: Context, loans: List<Loan>, filterLabel: String): File {
        val workbook = HSSFWorkbook()
        val sheet = workbook.createSheet("Empréstimos")

        val headerRow = sheet.createRow(0)
        listOf(
            "Devedor", "Contato", "Valor Emprestado (R$)",
            "Valor a Receber (R$)", "Data Empréstimo", "Vencimento", "Status", "Observações"
        ).forEachIndexed { i, h -> headerRow.createCell(i).setCellValue(h) }

        loans.forEachIndexed { index, loan ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(loan.debtorName)
            row.createCell(1).setCellValue(loan.contact)
            row.createCell(2).setCellValue(loan.amountLent)
            row.createCell(3).setCellValue(loan.amountToReceive)
            row.createCell(4).setCellValue(DateUtils.formatDate(loan.loanDate))
            row.createCell(5).setCellValue(DateUtils.formatDate(loan.dueDate))
            row.createCell(6).setCellValue(translateStatus(loan.status))
            row.createCell(7).setCellValue(loan.notes ?: "")
        }

        for (i in 0..7) sheet.autoSizeColumn(i)

        val dir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val safeLabel = filterLabel.replace(" ", "_").lowercase()
        val file = File(dir, "emprestimos_${safeLabel}_${System.currentTimeMillis()}.xls")
        file.outputStream().use { workbook.write(it) }
        workbook.close()
        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.ms-excel"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun translateStatus(status: String): String = when (status) {
        LoanStatus.PENDING.name -> "Pendente"
        LoanStatus.PAID.name -> "Pago"
        LoanStatus.OVERDUE.name -> "Atrasado"
        LoanStatus.PARTIAL.name -> "Parcial"
        else -> status
    }
}
