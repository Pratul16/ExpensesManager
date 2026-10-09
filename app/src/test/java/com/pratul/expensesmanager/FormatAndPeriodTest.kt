package com.pratul.expensesmanager

import com.pratul.expensesmanager.util.DateRange
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatMoneyCompact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatAndPeriodTest {

    @Test
    fun testFormatMoneyCompact() {
        assertEquals("₹950", formatMoneyCompact(950.0))
        assertEquals("₹1.2K", formatMoneyCompact(1200.0))
        assertEquals("₹3.2L", formatMoneyCompact(320000.0))
        assertEquals("₹1.5Cr", formatMoneyCompact(15000000.0))
    }

    @Test
    fun testDateRangeContains() {
        val range = DateRange(1000L, 2000L)
        assertTrue(1000L in range)
        assertTrue(1500L in range)
        assertFalse(2000L in range)
        assertFalse(500L in range)
    }

    @Test
    fun testMonthKeyNavigation() {
        val jan2026 = MonthKey(2026, 0)
        val dec2025 = jan2026.previous()
        val feb2026 = jan2026.next()

        assertEquals(MonthKey(2025, 11), dec2025)
        assertEquals(MonthKey(2026, 1), feb2026)
    }
}
