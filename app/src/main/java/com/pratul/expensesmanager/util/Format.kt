package com.pratul.expensesmanager.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatMoney(amount: Double): String =
    "₹" + String.format(Locale.getDefault(), "%,.2f", amount)

fun formatMoneyWhole(amount: Double): String =
    "₹" + String.format(Locale.getDefault(), "%,.0f", amount)

/** Short labels for charts: ₹950, ₹12.5K, ₹3.2L, ₹1.1Cr */
fun formatMoneyCompact(amount: Double): String {
    fun short(value: Double, suffix: String): String {
        val text = String.format(Locale.getDefault(), "%.1f", value).removeSuffix(".0").removeSuffix(",0")
        return "₹$text$suffix"
    }
    return when {
        amount >= 10_000_000 -> short(amount / 10_000_000, "Cr")
        amount >= 100_000 -> short(amount / 100_000, "L")
        amount >= 1_000 -> short(amount / 1_000, "K")
        else -> "₹" + String.format(Locale.getDefault(), "%.0f", amount)
    }
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))

/** Formats a date-picker value (UTC midnight) without shifting it by the phone's time zone. */
fun formatPickerDate(utcMillis: Long): String =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(utcMillis))

/** Age in whole years today for a birth date given as a date-picker value. */
fun ageFrom(dobUtcMillis: Long): Int {
    val dob = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dobUtcMillis }
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    val birthdayPassed = today.get(Calendar.MONTH) > dob.get(Calendar.MONTH) ||
            (today.get(Calendar.MONTH) == dob.get(Calendar.MONTH) &&
                    today.get(Calendar.DAY_OF_MONTH) >= dob.get(Calendar.DAY_OF_MONTH))
    if (!birthdayPassed) age--
    return age
}

fun yearOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)

/** A calendar month. `month` is 0 (January) to 11 (December), like java.util.Calendar. */
data class MonthKey(val year: Int, val month: Int) : Comparable<MonthKey> {
    fun previous() = if (month == 0) MonthKey(year - 1, 11) else MonthKey(year, month - 1)
    fun next() = if (month == 11) MonthKey(year + 1, 0) else MonthKey(year, month + 1)

    fun label(): String = format("MMMM yyyy")
    fun shortLabel(): String = format("MMM yyyy")
    fun monthName(): String = format("MMMM")

    /** A single number (year * 12 + month) so a month can be kept in rememberSaveable. */
    fun toIndex() = year * 12 + month

    private fun format(pattern: String): String {
        val c = Calendar.getInstance().apply { clear(); set(year, month, 1) }
        return SimpleDateFormat(pattern, Locale.getDefault()).format(c.time)
    }

    override fun compareTo(other: MonthKey) = compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        fun of(millis: Long): MonthKey {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            return MonthKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }

        fun current() = of(System.currentTimeMillis())

        fun fromIndex(index: Int) = MonthKey(index / 12, index % 12)
    }
}

fun shortMonthName(month: Int): String {
    val c = Calendar.getInstance().apply { clear(); set(2000, month, 1) }
    return SimpleDateFormat("MMM", Locale.getDefault()).format(c.time)
}

/**
 * The Material date picker works in UTC midnight. Convert its value to a local time on that day:
 * now if the day is today, otherwise midday so the date never shifts across time zones.
 */
fun pickerDateToLocalMillis(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val local = Calendar.getInstance().apply {
        clear()
        set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH), 12, 0)
    }
    val now = Calendar.getInstance()
    val isToday = local.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            local.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    return if (isToday) now.timeInMillis else local.timeInMillis
}

/** The reverse of [pickerDateToLocalMillis]: a local time to the picker's UTC midnight. */
fun localMillisToPickerDate(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
