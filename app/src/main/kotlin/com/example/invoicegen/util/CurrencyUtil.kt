package com.example.invoicegen.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToLong

object CurrencyUtil {
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    /** Format cents as a US currency string, e.g. 1099 -> "$10.99" */
    fun formatCents(cents: Long): String = formatter.format(cents / 100.0)

    /** Parse a dollar string like "$10.99" or "10.99" into cents */
    fun parseToCents(input: String): Long {
        val cleaned = input.replace(Regex("[^\\d.]"), "")
        return if (cleaned.isEmpty()) 0L
        else (cleaned.toDoubleOrNull() ?: 0.0).times(100).roundToLong()
    }

    /** Calculate line total: quantity × unitPrice (cents) */
    fun calcLineTotal(quantity: Double, unitPriceCents: Long): Long =
        (quantity * unitPriceCents).roundToLong()

    /** Calculate tax amount in cents */
    fun calcTax(subtotalCents: Long, taxPercent: Double): Long =
        (subtotalCents * taxPercent / 100.0).roundToLong()

    /** Format a Double as a plain dollar string for display in price fields */
    fun centsToDisplayString(cents: Long): String =
        String.format(Locale.US, "%.2f", cents / 100.0)
}
