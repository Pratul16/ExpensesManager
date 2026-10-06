package com.pratul.expensesmanager.data

import org.json.JSONArray
import org.json.JSONObject

/** Money the user gave to someone (Lent) or got from someone (Borrowed). */
enum class LoanType(val label: String) { Lent("Lent"), Borrowed("Borrowed") }

/**
 * One borrow/lend record. [settled] once it has been paid back; [dueMillis] is an optional date
 * it should be repaid by.
 */
data class Loan(
    val id: Long,
    val type: LoanType,
    val person: String,
    val amount: Double,
    val dateMillis: Long,
    val note: String = "",
    val dueMillis: Long? = null,
    val settled: Boolean = false,
    val settledAt: Long? = null
)

/** Outstanding (not yet settled) totals. Surplus when others owe you more than you owe them. */
data class LoanTotals(val lentOpen: Double, val borrowedOpen: Double) {
    val net get() = lentOpen - borrowedOpen
}

fun List<Loan>.totals() = LoanTotals(
    lentOpen = filter { !it.settled && it.type == LoanType.Lent }.sumOf { it.amount },
    borrowedOpen = filter { !it.settled && it.type == LoanType.Borrowed }.sumOf { it.amount }
)

object LoanJson {
    fun toJson(loans: List<Loan>): JSONArray {
        val array = JSONArray()
        for (l in loans) {
            array.put(
                JSONObject()
                    .put("id", l.id)
                    .put("type", l.type.name)
                    .put("person", l.person)
                    .put("amount", l.amount)
                    .put("date", l.dateMillis)
                    .put("note", l.note)
                    .put("settled", l.settled)
                    .apply {
                        l.dueMillis?.let { put("due", it) }
                        l.settledAt?.let { put("settledAt", it) }
                    }
            )
        }
        return array
    }

    fun fromJson(array: JSONArray): List<Loan> = List(array.length()) { i ->
        val o = array.getJSONObject(i)
        Loan(
            id = o.getLong("id"),
            type = LoanType.valueOf(o.getString("type")),
            person = o.getString("person"),
            amount = o.getDouble("amount"),
            dateMillis = o.getLong("date"),
            note = o.optString("note", ""),
            dueMillis = if (o.has("due")) o.getLong("due") else null,
            settled = o.optBoolean("settled", false),
            settledAt = if (o.has("settledAt")) o.getLong("settledAt") else null
        )
    }
}
