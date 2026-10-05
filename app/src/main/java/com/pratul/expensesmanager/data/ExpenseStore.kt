package com.pratul.expensesmanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Saves expenses, the category list and the recycle bin on the phone as JSON in SharedPreferences.
 * The same JSON shapes are used inside backup files (see [Backup]).
 */
object ExpenseStore {
    private const val PREFS = "expenses"
    private const val KEY = "list"
    private const val CATALOG_KEY = "catalog"
    private const val BIN_KEY = "bin"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(context: Context): List<Expense> {
        val text = prefs(context).getString(KEY, "[]") ?: "[]"
        return try {
            expensesFromJson(JSONArray(text))
        } catch (e: Exception) {
            // Keep a copy of the unreadable data so the next save doesn't destroy it
            prefs(context).edit().putString("${KEY}_unreadable_${System.currentTimeMillis()}", text).apply()
            emptyList()
        }
    }

    fun save(context: Context, expenses: List<Expense>) {
        prefs(context).edit().putString(KEY, expensesToJson(expenses).toString()).apply()
    }

    /** The categories and their items, starting from [defaultCatalog] on first use. */
    fun loadCatalog(context: Context): List<Category> {
        val text = prefs(context).getString(CATALOG_KEY, null) ?: return defaultCatalog
        return try {
            catalogFromJson(JSONArray(text))
        } catch (e: Exception) {
            defaultCatalog
        }
    }

    fun saveCatalog(context: Context, catalog: List<Category>) {
        prefs(context).edit().putString(CATALOG_KEY, catalogToJson(catalog).toString()).apply()
    }

    fun loadBin(context: Context): List<BinEntry> {
        val text = prefs(context).getString(BIN_KEY, "[]") ?: "[]"
        return try {
            binFromJson(JSONArray(text))
        } catch (e: Exception) {
            prefs(context).edit().putString("${BIN_KEY}_unreadable_${System.currentTimeMillis()}", text).apply()
            emptyList()
        }
    }

    fun saveBin(context: Context, bin: List<BinEntry>) {
        prefs(context).edit().putString(BIN_KEY, binToJson(bin).toString()).apply()
    }

    // ---------- JSON conversion (shared with backups) ----------

    fun expensesFromJson(array: JSONArray): List<Expense> =
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Expense(
                id = o.getLong("id"),
                amount = o.getDouble("amount"),
                category = o.getString("category"),
                item = o.optString("item", ""),
                note = o.optString("note", ""),
                dateMillis = o.getLong("date"),
                bill = o.optString("bill", "")
            )
        }

    fun expensesToJson(expenses: List<Expense>): JSONArray {
        val array = JSONArray()
        for (e in expenses) {
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("amount", e.amount)
                    .put("category", e.category)
                    .put("item", e.item)
                    .put("note", e.note)
                    .put("date", e.dateMillis)
                    .put("bill", e.bill)
            )
        }
        return array
    }

    fun catalogFromJson(array: JSONArray): List<Category> =
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val name = o.getString("name")
            Category(name, stringsFromJson(o.getJSONArray("items")), o.optString("emoji", defaultEmojiFor(name)))
        }

    fun catalogToJson(catalog: List<Category>): JSONArray {
        val array = JSONArray()
        for (c in catalog) {
            array.put(JSONObject().put("name", c.name).put("items", JSONArray(c.items)).put("emoji", c.emoji))
        }
        return array
    }

    fun binFromJson(array: JSONArray): List<BinEntry> =
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            BinEntry(
                id = o.getLong("id"),
                deletedAt = o.getLong("deletedAt"),
                kind = BinKind.valueOf(o.getString("kind")),
                category = o.getString("category"),
                item = o.optString("item", ""),
                categoryItems = stringsFromJson(o.getJSONArray("categoryItems")),
                expenses = expensesFromJson(o.getJSONArray("expenses")),
                categoryEmoji = o.optString("categoryEmoji", defaultEmojiFor(o.getString("category")))
            )
        }

    fun binToJson(bin: List<BinEntry>): JSONArray {
        val array = JSONArray()
        for (b in bin) {
            array.put(
                JSONObject()
                    .put("id", b.id)
                    .put("deletedAt", b.deletedAt)
                    .put("kind", b.kind.name)
                    .put("category", b.category)
                    .put("item", b.item)
                    .put("categoryItems", JSONArray(b.categoryItems))
                    .put("expenses", expensesToJson(b.expenses))
                    .put("categoryEmoji", b.categoryEmoji)
            )
        }
        return array
    }

    private fun stringsFromJson(array: JSONArray): List<String> = List(array.length()) { array.getString(it) }
}
