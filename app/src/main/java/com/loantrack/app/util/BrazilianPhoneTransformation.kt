package com.loantrack.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class BrazilianPhoneTransformation : VisualTransformation {

    // Formatted: (XX) XXXXX-XXXX  (15 chars for 11 digits)
    // digit pos → formatted pos (cursor after n digits)
    // 0→0, 1→2, 2→3, 3→6, 4→7, 5→8, 6→9, 7→10, 8→12, 9→13, 10→14, 11→15
    private val origToTrans = intArrayOf(0, 2, 3, 6, 7, 8, 9, 10, 12, 13, 14, 15)

    // formatted pos → digit count to the left of cursor
    // indices 0-15: how many digits precede that position
    private val transToOrig = intArrayOf(0, 0, 1, 2, 2, 2, 3, 4, 5, 6, 7, 7, 8, 9, 10, 11)

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.take(11)
        val formatted = format(digits)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                origToTrans[offset.coerceIn(0, origToTrans.size - 1)]
                    .coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int =
                transToOrig[offset.coerceIn(0, transToOrig.size - 1)]
                    .coerceAtMost(digits.length)
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }

    companion object {
        fun format(digits: String): String = buildString {
            digits.forEachIndexed { i, c ->
                when (i) {
                    0 -> append("($c")
                    1 -> append("$c) ")
                    7 -> append("-$c")
                    else -> append(c)
                }
            }
        }
    }
}
