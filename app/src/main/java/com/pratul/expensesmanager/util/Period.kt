package com.pratul.expensesmanager.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class PeriodPreset(val label: String) {
    Today("Today"),
    Yesterday("Yesterday"),
    ThisWeek("This week"),
    Last7Days("Last 7 days"),
    ThisMonth("This month"),
    LastMonth("Last month"),
    ThisYear("This year"),
    AllTime("All time"),
    Custom("Custom…")
}

/** A span of local time: [startMillis] inclusive, [endMillis] exclusive. */
data class DateRange(val startMillis: Long, val endMillis: Long) {
    operator fun contains(millis: Long) = millis in startMillis until endMillis

    /** Whole days covered, at least 1 (for daily averages). */
    fun days(): Int = (((endMillis - startMillis) + 43_200_000) / 86_400_000).toInt().coerceAtLeast(1)

    /** e.g. "5 Oct 2026", "29 Sep – 5 Oct 2026", "Jan 2025 – Oct 2026" style labels. */
    fun label(): String {
        val lastDay = endMillis - 1
        val start = Calendar.getInstance().apply { timeInMillis = startMillis }
        val end = Calendar.getInstance().apply { timeInMillis = lastDay }
        val sameYear = start.get(Calendar.YEAR) == end.get(Calendar.YEAR)
        val full = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val noYear = SimpleDateFormat("d MMM", Locale.getDefault())
        return when {
            sameDay(start, end) -> full.format(Date(startMillis))
            sameYear -> noYear.format(Date(startMillis)) + " – " + full.format(Date(lastDay))
            else -> full.format(Date(startMillis)) + " – " + full.format(Date(lastDay))
        }
    }

    private fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}

private fun startOfToday(): Calendar = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}

/**
 * The date range for a preset. Weeks start on the phone's usual first day of the week.
 * [earliestMillis] is used for "All time" (the oldest expense).
 */
fun rangeFor(preset: PeriodPreset, earliestMillis: Long?): DateRange {
    val today = startOfToday()
    val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    fun shifted(field: Int, amount: Int, from: Calendar = today) =
        (from.clone() as Calendar).apply { add(field, amount) }.timeInMillis

    return when (preset) {
        PeriodPreset.Today -> DateRange(today.timeInMillis, tomorrow)
        PeriodPreset.Yesterday -> DateRange(shifted(Calendar.DAY_OF_MONTH, -1), today.timeInMillis)
        PeriodPreset.ThisWeek -> {
            val start = (today.clone() as Calendar).apply {
                while (get(Calendar.DAY_OF_WEEK) != firstDayOfWeek) add(Calendar.DAY_OF_MONTH, -1)
            }
            DateRange(start.timeInMillis, tomorrow)
        }
        PeriodPreset.Last7Days -> DateRange(shifted(Calendar.DAY_OF_MONTH, -6), tomorrow)
        PeriodPreset.ThisMonth -> {
            val start = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            DateRange(start.timeInMillis, tomorrow)
        }
        PeriodPreset.LastMonth -> {
            val thisMonth = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
            DateRange(shifted(Calendar.MONTH, -1, thisMonth), thisMonth.timeInMillis)
        }
        PeriodPreset.ThisYear -> {
            val start = (today.clone() as Calendar).apply { set(Calendar.DAY_OF_YEAR, 1) }
            DateRange(start.timeInMillis, tomorrow)
        }
        PeriodPreset.AllTime -> {
            val start = earliestMillis?.let { startOfDay(it) } ?: today.timeInMillis
            DateRange(minOf(start, today.timeInMillis), tomorrow)
        }
        // Custom ranges are chosen in a date picker; this is only a fallback
        PeriodPreset.Custom -> DateRange(today.timeInMillis, tomorrow)
    }
}

fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** A range picked in the Material date range picker (UTC midnights, end day included) as local time. */
fun rangeFromPicker(startUtc: Long, endUtc: Long): DateRange {
    fun localMidnight(utc: Long, addDays: Int): Long {
        val u = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
        return Calendar.getInstance().apply {
            clear()
            set(u.get(Calendar.YEAR), u.get(Calendar.MONTH), u.get(Calendar.DAY_OF_MONTH))
            add(Calendar.DAY_OF_MONTH, addDays)
        }.timeInMillis
    }
    return DateRange(localMidnight(startUtc, 0), localMidnight(endUtc, 1))
}
