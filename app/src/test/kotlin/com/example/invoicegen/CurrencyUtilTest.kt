package com.example.invoicegen

import com.example.invoicegen.util.CurrencyUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyUtilTest {

    @Test
    fun `formatCents formats correctly`() {
        assertEquals("$10.99", CurrencyUtil.formatCents(1099))
        assertEquals("$0.00", CurrencyUtil.formatCents(0))
        assertEquals("$1,000.00", CurrencyUtil.formatCents(100000))
    }

    @Test
    fun `parseToCents parses dollar strings`() {
        assertEquals(1099L, CurrencyUtil.parseToCents("10.99"))
        assertEquals(1099L, CurrencyUtil.parseToCents("$10.99"))
        assertEquals(0L, CurrencyUtil.parseToCents(""))
        assertEquals(100000L, CurrencyUtil.parseToCents("1000.00"))
    }

    @Test
    fun `calcLineTotal multiplies quantity and unit price`() {
        assertEquals(2198L, CurrencyUtil.calcLineTotal(2.0, 1099L))
        assertEquals(0L, CurrencyUtil.calcLineTotal(0.0, 1099L))
    }

    @Test
    fun `calcTax computes correct tax amount`() {
        assertEquals(825L, CurrencyUtil.calcTax(10000L, 8.25))
        assertEquals(0L, CurrencyUtil.calcTax(10000L, 0.0))
    }
}
