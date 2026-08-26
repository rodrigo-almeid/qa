package com.loantrack.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.loantrack.app.data.model.LoanStatus
import com.loantrack.app.domain.usecase.UpdateOverdueStatusUseCase
import com.loantrack.app.util.CurrencyUtils
import com.loantrack.app.util.DateUtils
import com.loantrack.app.util.NotificationUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LoanStatusWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseAuth: FirebaseAuth,
    private val updateOverdueStatusUseCase: UpdateOverdueStatusUseCase
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userId = firebaseAuth.currentUser?.uid ?: return Result.success()

        val result = updateOverdueStatusUseCase(userId)
        if (result.isFailure) return Result.retry()

        val loans = result.getOrNull() ?: return Result.success()

        NotificationUtils.createNotificationChannel(applicationContext)

        var notificationId = 1000
        val today = DateUtils.today()
        val tomorrow = DateUtils.tomorrow()

        for (loan in loans) {
            if (loan.status == LoanStatus.PAID.name) continue
            if (loan.amountPaid >= loan.amountToReceive) continue

            val remaining = loan.amountToReceive - loan.amountPaid
            val formattedAmount = CurrencyUtils.format(remaining)
            val dueDate = loan.dueDate.toDate()

            when {
                DateUtils.isSameDay(dueDate, tomorrow) -> {
                    NotificationUtils.sendNotification(
                        context = applicationContext,
                        title = "Lembrete de Vencimento",
                        message = "Lembrete: ${loan.debtorName} deve pagar amanhã - $formattedAmount",
                        notificationId = notificationId++
                    )
                }
                DateUtils.isSameDay(dueDate, today) -> {
                    NotificationUtils.sendNotification(
                        context = applicationContext,
                        title = "Vencimento Hoje",
                        message = "Vencimento hoje: ${loan.debtorName} - $formattedAmount",
                        notificationId = notificationId++
                    )
                }
            }
        }

        return Result.success()
    }
}
