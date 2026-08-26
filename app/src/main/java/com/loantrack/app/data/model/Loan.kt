package com.loantrack.app.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Loan(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val debtorName: String = "",
    val contact: String = "",
    val loanDate: Timestamp = Timestamp.now(),
    val dueDate: Timestamp = Timestamp.now(),
    val originalDueDate: Timestamp = Timestamp.now(),
    val paymentDate: Timestamp? = null,
    val amountLent: Double = 0.0,
    val amountToReceive: Double = 0.0,
    val amountPaid: Double = 0.0,
    val paymentType: String = PaymentType.SINGLE.name,
    val installmentsTotal: Int? = null,
    val installmentsPaid: Int? = null,
    val status: String = LoanStatus.PENDING.name,
    val notes: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

enum class LoanStatus {
    PENDING, PAID, OVERDUE, PARTIAL
}

enum class PaymentType {
    SINGLE, INSTALLMENT
}

fun Loan.computeStatus(now: java.util.Date = java.util.Date()): LoanStatus {
    return when {
        amountPaid >= amountToReceive && amountToReceive > 0 -> LoanStatus.PAID
        amountPaid > 0 && amountPaid < amountToReceive -> LoanStatus.PARTIAL
        amountPaid == 0.0 && dueDate.toDate().before(now) && !isSameDay(dueDate.toDate(), now) -> LoanStatus.OVERDUE
        amountPaid < amountToReceive && dueDate.toDate().before(now) && !isSameDay(dueDate.toDate(), now) -> LoanStatus.OVERDUE
        else -> LoanStatus.PENDING
    }
}

private fun isSameDay(d1: java.util.Date, d2: java.util.Date): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
    val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
            cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

fun Loan.remainingBalance(): Double = amountToReceive - amountPaid
