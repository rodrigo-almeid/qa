package com.loantrack.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.NumberFormat
import java.util.Locale

class CurrencyVisualTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = format(digits)

        // Cursor always at the end — currency input works right-to-left
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = formatted.length
            override fun transformedToOriginal(offset: Int) = digits.length
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }

    companion object {
        fun format(rawDigits: String): String {
            if (rawDigits.isEmpty()) return ""
            val cents = rawDigits.toLongOrNull() ?: 0L
            return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(cents / 100.0)
        }
    }
}
