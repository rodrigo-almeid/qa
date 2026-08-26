package com.loantrack.app.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val format = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    fun format(value: Double): String = format.format(value)

    fun parse(text: String): Double? {
        return try {
            val cleaned = text
                .replace(Regex("[R$\\s]"), "")
                .replace(".", "")
                .replace(",", ".")
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
}
