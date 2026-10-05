package com.pratul.expensesmanager.data

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    /** Sub-category such as "Milk" or "Daal". Blank when none was chosen. */
    val item: String,
    val note: String,
    val dateMillis: Long,
    /** File name of an attached bill (photo or PDF) in the app's bills folder. Blank when none. */
    val bill: String = ""
)

/** A category, its emoji and the items (sub-categories) that belong to it. */
data class Category(val name: String, val items: List<String>, val emoji: String = DEFAULT_EMOJI)

const val DEFAULT_EMOJI = "🏷️"

/** Emoji offered when creating or editing a category. */
val categoryEmojiChoices = listOf(
    "🍔", "🛒", "🚇", "💡", "🛍️", "💊", "🎬", "✨", "🏠", "📚", "🎮", "✈️",
    "🐶", "👶", "💪", "☕", "🍺", "🎁", "💇", "🧾", "📱", "🚗", "⛽", "💰"
)

enum class BinKind { Expense, Category, Item }

/**
 * Something deleted, kept so it can be restored. A deleted category or item takes its expenses
 * with it; [categoryItems] and [categoryEmoji] keep a deleted category's items and emoji.
 */
data class BinEntry(
    val id: Long,
    val deletedAt: Long,
    val kind: BinKind,
    val category: String,
    val item: String,
    val categoryItems: List<String>,
    val expenses: List<Expense>,
    val categoryEmoji: String = DEFAULT_EMOJI
)

val defaultCatalog = listOf(
    Category("Food", listOf("Lunch", "Dinner", "Snacks", "Tea/Coffee", "Restaurant"), "🍔"),
    Category("Groceries", listOf("Milk", "Daal", "Rice", "Atta", "Vegetables", "Fruits", "Oil", "Eggs"), "🛒"),
    Category("Transport", listOf("Fuel", "Metro", "Bus", "Cab", "Auto"), "🚇"),
    Category("Bills", listOf("Electricity", "Water", "Gas", "Mobile", "Internet", "Rent"), "💡"),
    Category("Shopping", listOf("Clothes", "Shoes", "Electronics", "Home"), "🛍️"),
    Category("Health", listOf("Medicines", "Doctor", "Tests"), "💊"),
    Category("Entertainment", listOf("Movies", "Subscriptions", "Outings"), "🎬"),
    Category("Other", emptyList(), "✨")
)

/** Emoji for a built-in category name, used when older saved data has none. */
fun defaultEmojiFor(category: String): String =
    defaultCatalog.firstOrNull { it.name == category }?.emoji ?: DEFAULT_EMOJI

enum class GroupBy(val label: String) { Category("Category wise"), Item("Item wise") }

/** One row of a breakdown: a category or an item, with the category it belongs to. */
data class GroupTotal(val label: String, val category: String, val amount: Double)

fun Expense.groupLabel(by: GroupBy): String = when (by) {
    GroupBy.Category -> category
    GroupBy.Item -> item.ifBlank { "$category (general)" }
}

/** Sum of amounts per category or per item, largest first. */
fun List<Expense>.totalsBy(by: GroupBy): List<GroupTotal> =
    groupBy { it.groupLabel(by) }
        .map { (label, items) -> GroupTotal(label, items.first().category, items.sumOf { it.amount }) }
        .sortedByDescending { it.amount }
