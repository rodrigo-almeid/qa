package com.loantrack.app.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val displayFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))

    fun formatDate(timestamp: Timestamp): String = displayFormat.format(timestamp.toDate())

    fun formatDate(date: Date): String = displayFormat.format(date)

    fun formatMonthYear(date: Date): String = monthYearFormat.format(date).replaceFirstChar { it.uppercase() }

    fun today(): Date = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    fun isSameMonth(date: Date, reference: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date }
        val cal2 = Calendar.getInstance().apply { time = reference }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
    }

    fun tomorrow(): Date = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    fun isSameDay(d1: Date, d2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = d1 }
        val cal2 = Calendar.getInstance().apply { time = d2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun startOfMonth(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    fun endOfMonth(date: Date): Date = Calendar.getInstance().apply {
        time = date
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
    }.time

    fun addMonths(date: Date, months: Int): Date = Calendar.getInstance().apply {
        time = date
        add(Calendar.MONTH, months)
    }.time

    fun toTimestamp(date: Date): Timestamp = Timestamp(date)
}
