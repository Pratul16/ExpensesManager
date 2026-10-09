package com.pratul.expensesmanager.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pratul.expensesmanager.data.BackupData
import com.pratul.expensesmanager.data.BillStore
import com.pratul.expensesmanager.data.BinEntry
import com.pratul.expensesmanager.data.BinKind
import com.pratul.expensesmanager.data.Category
import com.pratul.expensesmanager.data.DEFAULT_EMOJI
import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.data.ExpenseStore
import com.pratul.expensesmanager.data.Loan
import com.pratul.expensesmanager.data.defaultCatalog
import com.pratul.expensesmanager.data.defaultEmojiFor
import java.io.File

/**
 * All of the app's expense data, plus every change that can be made to it.
 * Each change is saved to the phone straight away.
 */
class AppState(private val context: Context) {
    var expenses by mutableStateOf(ExpenseStore.load(context))
        private set
    var catalog by mutableStateOf(ExpenseStore.loadCatalog(context))
        private set
    var bin by mutableStateOf(ExpenseStore.loadBin(context))
        private set
    var loans by mutableStateOf(ExpenseStore.loadLoans(context))
        private set

    private fun updateExpenses(list: List<Expense>) {
        expenses = list
        ExpenseStore.save(context, list)
    }

    private fun updateCatalog(list: List<Category>) {
        catalog = list
        ExpenseStore.saveCatalog(context, list)
    }

    private fun updateLoans(list: List<Loan>) {
        loans = list
        ExpenseStore.saveLoans(context, list)
    }

    private fun updateBin(list: List<BinEntry>) {
        bin = list
        ExpenseStore.saveBin(context, list)
    }

    /** Unique even if two are made in the same millisecond. */
    private fun newExpenseId() = maxOf(System.currentTimeMillis(), (expenses.maxOfOrNull { it.id } ?: 0) + 1)

    // ---------- Expenses ----------

    fun addExpense(expense: Expense) {
        updateExpenses(expenses + expense.copy(id = newExpenseId()))
    }

    fun deleteExpense(expense: Expense) {
        updateExpenses(expenses.filter { it.id != expense.id })
        moveToBin(BinKind.Expense, expense.category, expense.item, emptyList(), listOf(expense))
    }

    // ---------- Categories and items ----------

    fun expensesIn(category: String) = expenses.filter { it.category == category }
    fun expensesIn(category: String, item: String) = expenses.filter { it.category == category && it.item == item }

    fun addCategory(name: String, emoji: String = DEFAULT_EMOJI) {
        if (catalog.none { it.name.equals(name, ignoreCase = true) }) {
            updateCatalog(catalog + Category(name, emptyList(), emoji))
        }
    }

    fun addItem(category: String, item: String) {
        updateCatalog(catalog.map {
            if (it.name == category && it.items.none { i -> i.equals(item, ignoreCase = true) }) {
                it.copy(items = it.items + item)
            } else it
        })
    }

    /** Changes a category's name and emoji. A new name is applied everywhere, including past expenses. */
    fun editCategory(old: String, new: String, emoji: String) {
        updateCatalog(catalog.map { if (it.name == old) it.copy(name = new, emoji = emoji) else it })
        if (new == old) return
        updateExpenses(expenses.map { if (it.category == old) it.copy(category = new) else it })
    }

    fun renameItem(category: String, old: String, new: String) {
        updateCatalog(catalog.map {
            if (it.name == category) it.copy(items = it.items.map { i -> if (i == old) new else i }) else it
        })
        updateExpenses(expenses.map {
            if (it.category == category && it.item == old) it.copy(item = new) else it
        })
    }

    /** Moves the category, its items and all its expenses to the recycle bin. */
    fun deleteCategory(name: String) {
        val category = catalog.firstOrNull { it.name == name } ?: return
        val (removed, kept) = expenses.partition { it.category == name }
        updateCatalog(catalog.filter { it.name != name })
        updateExpenses(kept)
        moveToBin(BinKind.Category, name, "", category.items, removed, category.emoji)
    }

    /** Moves the item and all its expenses to the recycle bin. */
    fun deleteItem(category: String, item: String) {
        val (removed, kept) = expenses.partition { it.category == category && it.item == item }
        updateCatalog(catalog.map {
            if (it.name == category) it.copy(items = it.items.filter { i -> i != item }) else it
        })
        updateExpenses(kept)
        moveToBin(BinKind.Item, category, item, emptyList(), removed)
    }

    // ---------- Borrow & lend ----------

    /** Adds a new record, or replaces the one with the same id (editing). */
    fun saveLoan(loan: Loan) {
        val id = if (loan.id != 0L) loan.id else maxOf(System.currentTimeMillis(), (loans.maxOfOrNull { it.id } ?: 0) + 1)
        val saved = loan.copy(id = id)
        updateLoans(if (loans.any { it.id == id }) loans.map { if (it.id == id) saved else it } else loans + saved)
    }

    fun setLoanSettled(id: Long, settled: Boolean) {
        updateLoans(loans.map {
            if (it.id == id) it.copy(settled = settled, settledAt = if (settled) System.currentTimeMillis() else null) else it
        })
    }

    fun deleteLoan(id: Long) {
        updateLoans(loans.filter { it.id != id })
    }

    // ---------- Recycle bin ----------

    private fun moveToBin(
        kind: BinKind,
        category: String,
        item: String,
        items: List<String>,
        removed: List<Expense>,
        emoji: String = defaultEmojiFor(category)
    ) {
        val now = System.currentTimeMillis()
        val id = maxOf(now, (bin.maxOfOrNull { it.id } ?: 0) + 1)
        updateBin(listOf(BinEntry(id, now, kind, category, item, items, removed, emoji)) + bin)
    }

    /**
     * Puts a deleted entry back. Any category or item it needs is re-created if it no longer
     * exists (for example after a rename), and restored expenses get new ids if theirs are taken.
     */
    fun restore(entry: BinEntry) {
        var newCatalog = catalog
        fun ensure(category: String, items: List<String>) {
            val existing = newCatalog.firstOrNull { it.name == category }
            newCatalog = if (existing == null) {
                val emoji = if (category == entry.category) entry.categoryEmoji else defaultEmojiFor(category)
                newCatalog + Category(category, items.distinct(), emoji)
            } else {
                newCatalog.map {
                    if (it.name == category) it.copy(items = (it.items + items).distinct()) else it
                }
            }
        }
        when (entry.kind) {
            BinKind.Category -> ensure(entry.category, entry.categoryItems)
            BinKind.Item -> ensure(entry.category, listOf(entry.item))
            BinKind.Expense -> Unit
        }
        for (e in entry.expenses) ensure(e.category, listOfNotNull(e.item.ifBlank { null }))
        updateCatalog(newCatalog)

        val usedIds = expenses.mapTo(HashSet()) { it.id }
        var nextId = newExpenseId()
        val restored = entry.expenses.map { if (it.id in usedIds) it.copy(id = nextId++) else it }
        updateExpenses(expenses + restored)
        updateBin(bin.filter { it.id != entry.id })
    }

    fun deleteForever(entry: BinEntry) {
        updateBin(bin.filter { it.id != entry.id })
        deleteUnusedBills(entry.expenses.map { it.bill })
    }

    fun emptyBin() {
        val bills = bin.flatMap { it.expenses }.map { it.bill }
        updateBin(emptyList())
        deleteUnusedBills(bills)
    }

    /** Permanently removes every expense, category change, recycle-bin entry and bill. */
    fun wipeAll() {
        updateExpenses(emptyList())
        updateCatalog(defaultCatalog)
        updateBin(emptyList())
        updateLoans(emptyList())
        BillStore.deleteAll(context)
    }

    // ---------- Bills ----------

    /** Attaches a bill to an expense (or removes it with null). The old bill file is deleted. */
    fun setBill(expenseId: Long, bill: String?) {
        val old = expenses.firstOrNull { it.id == expenseId }?.bill ?: return
        updateExpenses(expenses.map { if (it.id == expenseId) it.copy(bill = bill.orEmpty()) else it })
        if (old != bill) deleteUnusedBills(listOf(old))
    }

    /** Deletes bill files that no expense or recycle-bin entry points to any more. */
    private fun deleteUnusedBills(names: List<String>) {
        val stillUsed = (expenses + bin.flatMap { it.expenses }).map { it.bill }.toHashSet()
        for (name in names) if (name.isNotBlank() && name !in stillUsed) BillStore.delete(context, name)
    }

    // ---------- Import ----------

    /** Swaps all current data (bills included) for the backup's. */
    fun replaceAll(backup: BackupData) {
        BillStore.deleteAll(context)
        copyBills(backup, (backup.expenses + backup.bin.flatMap { it.expenses }).map { it.bill })
        updateCatalog(backup.catalog.ifEmpty { defaultCatalog })
        updateExpenses(backup.expenses)
        updateBin(backup.bin)
        updateLoans(backup.loans)
    }

    /**
     * Adds whatever in the backup isn't here yet. An expense counts as already here when one with
     * the same date, amount, category, item and note exists, so importing the same file twice adds nothing.
     * Returns how many expenses were added.
     */
    fun merge(backup: BackupData): Int {
        // Categories: add new ones, and new items to existing ones (keeping the current emoji)
        var newCatalog = catalog
        for (c in backup.catalog) {
            val existing = newCatalog.firstOrNull { it.name == c.name }
            newCatalog = if (existing == null) newCatalog + c
            else newCatalog.map { if (it.name == c.name) it.copy(items = (it.items + c.items).distinct()) else it }
        }
        // Any category an imported expense uses must exist too
        for (e in backup.expenses) {
            if (newCatalog.none { it.name == e.category }) {
                newCatalog = newCatalog + Category(e.category, emptyList(), defaultEmojiFor(e.category))
            }
            if (e.item.isNotBlank()) {
                newCatalog = newCatalog.map {
                    if (it.name == e.category && e.item !in it.items) it.copy(items = it.items + e.item) else it
                }
            }
        }
        updateCatalog(newCatalog)

        fun sameKey(e: Expense) = listOf(e.dateMillis, e.amount, e.category, e.item, e.note)
        val existingKeys = expenses.mapTo(HashSet(), ::sameKey)
        val usedIds = expenses.mapTo(HashSet()) { it.id }
        var nextId = newExpenseId()
        val added = backup.expenses
            .filter { existingKeys.add(sameKey(it)) }
            .map { if (usedIds.add(it.id)) it else it.copy(id = nextId++).also { e -> usedIds.add(e.id) } }

        val binIds = bin.mapTo(HashSet()) { it.id }
        val addedBin = backup.bin.filter { it.id !in binIds }
        copyBills(backup, (added + addedBin.flatMap { it.expenses }).map { it.bill })

        updateExpenses(expenses + added)
        updateBin(bin + addedBin)
        val loanIds = loans.mapTo(HashSet()) { it.id }
        updateLoans(loans + backup.loans.filter { it.id !in loanIds })
        return added.size
    }

    /** Copies the named bills out of an imported backup into the bills folder. */
    private fun copyBills(backup: BackupData, names: List<String>) {
        val from = backup.billsDir ?: return
        for (name in names.filter { it.isNotBlank() }.distinct()) {
            val source = File(from, name)
            if (source.exists()) source.copyTo(BillStore.file(context, name), overwrite = true)
        }
    }
}
