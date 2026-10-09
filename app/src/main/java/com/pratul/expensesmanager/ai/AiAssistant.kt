package com.pratul.expensesmanager.ai

import com.pratul.expensesmanager.data.Expense
import com.pratul.expensesmanager.util.MonthKey
import com.pratul.expensesmanager.util.formatMoney
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ExtractedExpenseCandidate(
    val amount: Double,
    val category: String,
    val item: String,
    val rawText: String
)

sealed class AiResponse {
    data class Text(val content: String) : AiResponse()
    data class ParsedExpenses(val text: String, val candidates: List<ExtractedExpenseCandidate>) : AiResponse()
}

object AiAssistant {

    fun processQuery(prompt: String, expenses: List<Expense>): AiResponse {
        val clean = prompt.trim()
        if (clean.isBlank()) return AiResponse.Text("Please type or speak your query! / Kripya apna sawal puchein!")

        // 1. Try extracting expense entries from text
        val candidates = extractExpenseCandidates(clean)
        if (candidates.isNotEmpty()) {
            val total = candidates.sumOf { it.amount }
            val count = candidates.size
            val msg = if (count == 1) {
                "I extracted 1 spend of ${formatMoney(total)} (${candidates[0].category}: ${candidates[0].item}). Would you like to add it?"
            } else {
                "Found $count spends totaling ${formatMoney(total)}! Check below and tap 'Add to App' to save them."
            }
            return AiResponse.ParsedExpenses(msg, candidates)
        }

        // 2. Try answering expense spend queries
        val lower = clean.lowercase(Locale.ROOT)
        val queryResponse = answerSpendQuery(lower, expenses)
        if (queryResponse != null) {
            return AiResponse.Text(queryResponse)
        }

        // 3. Try answering App Feature / How-To questions
        val featureResponse = answerAppHowTo(lower)
        if (featureResponse != null) {
            return AiResponse.Text(featureResponse)
        }

        // 4. Default helpful response
        return AiResponse.Text(
            "I'm your Expenses Manager AI Assistant! 🤖✨\n\n" +
            "You can ask me:\n" +
            "• Log spends: \"Bought 150 coffee and 500 petrol\"\n" +
            "• Ask queries: \"How much spent on food this month?\" / \"Aaj kitna kharcha hua?\"\n" +
            "• App help: \"How to lock app with PIN?\" / \"How to backup data?\""
        )
    }

    private fun extractExpenseCandidates(text: String): List<ExtractedExpenseCandidate> {
        val candidates = mutableListOf<ExtractedExpenseCandidate>()

        // Match patterns like "khane ke 500", "coffee 150", or "500 petrol"
        val pattern = Regex("([a-zA-Z\\u0900-\\u097F\\s/\\-&]+?)\\s*(?:₹|rs\\.?|inr)?\\s*(\\d+(?:\\.\\d{1,2})?)\\s*(?:rs|rupees|rupee|ki|ka)?", RegexOption.IGNORE_CASE)
        val matches = pattern.findAll(text).toList()

        if (matches.isNotEmpty()) {
            for (m in matches) {
                val rawWord = m.groupValues[1].trim()
                val amount = m.groupValues[2].toDoubleOrNull() ?: continue
                if (amount <= 0) continue

                val word = rawWord.removePrefix("ke ").removePrefix("ka ").removeSuffix(" ke").removeSuffix(" ka").trim()
                val (category, item) = categorizeText(word.ifBlank { "Expense" }, m.value)
                candidates.add(ExtractedExpenseCandidate(amount, category, item, m.value))
            }
        }

        return candidates
    }

    private fun categorizeText(desc: String, fullText: String): Pair<String, String> {
        val combined = "$desc $fullText".lowercase(Locale.ROOT)

        val category = when {
            combined.containsAny("coffee", "tea", "chai", "lunch", "dinner", "food", "swiggy", "zomato", "restaurant", "burger", "pizza", "khana", "breakfast", "snacks") -> "Food"
            combined.containsAny("milk", "doodh", "grocery", "groceries", "rashan", "atta", "rice", "veggies", "sabzi", "fruits", "oil", "egg") -> "Groceries"
            combined.containsAny("petrol", "fuel", "diesel", "cab", "uber", "ola", "auto", "metro", "bus", "kiraya", "fare", "parking") -> "Transport"
            combined.containsAny("electricity", "bijli", "water", "pani", "gas", "recharge", "mobile", "wifi", "rent", "bill") -> "Bills"
            combined.containsAny("clothes", "kapde", "shoes", "amazon", "flipkart", "shopping", "mall") -> "Shopping"
            combined.containsAny("medicine", "dawa", "doctor", "hospital", "pharma", "tests") -> "Health"
            combined.containsAny("movie", "cinema", "netflix", "game", "outing") -> "Entertainment"
            else -> "Other"
        }

        val item = desc.ifBlank { category }.capitalizeWords()
        return Pair(category, item)
    }

    private fun answerSpendQuery(lower: String, expenses: List<Expense>): String? {
        val isQuery = lower.containsAny(
            "how much", "spent", "total", "kharcha", "kharch", "kitna", "kitne", "spend", "cost", "history"
        )
        if (!isQuery) return null

        val now = Calendar.getInstance()
        val todayStart = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val currentMonth = MonthKey.current()

        // Filter by time range
        val filteredByTime = when {
            lower.containsAny("today", "aaj") -> expenses.filter { it.dateMillis >= todayStart }
            lower.containsAny("this month", "iss mahine", "is mahine", "current month") -> expenses.filter { MonthKey.of(it.dateMillis) == currentMonth }
            lower.containsAny("last month", "pichle mahine") -> expenses.filter { MonthKey.of(it.dateMillis) == currentMonth.previous() }
            lower.containsAny("this year", "iss saal", "is saal") -> expenses.filter { yearOf(it.dateMillis) == now.get(Calendar.YEAR) }
            else -> expenses // All time by default if no period specified
        }

        // Filter by category if mentioned
        val categoryKeywords = mapOf(
            "Food" to listOf("food", "khana", "restaurant", "lunch", "dinner", "coffee", "tea", "swiggy", "zomato"),
            "Groceries" to listOf("grocery", "groceries", "milk", "doodh", "rashan", "sabzi"),
            "Transport" to listOf("transport", "petrol", "fuel", "cab", "uber", "ola", "auto", "metro", "bus"),
            "Bills" to listOf("bill", "bills", "electricity", "bijli", "water", "rent", "recharge"),
            "Shopping" to listOf("shopping", "clothes", "kapde", "shoes", "amazon"),
            "Health" to listOf("health", "medicine", "dawa", "doctor"),
            "Entertainment" to listOf("entertainment", "movie", "cinema", "netflix")
        )

        var matchedCategory: String? = null
        for ((cat, keywords) in categoryKeywords) {
            if (keywords.any { lower.contains(it) }) {
                matchedCategory = cat
                break
            }
        }

        val targetExpenses = if (matchedCategory != null) {
            filteredByTime.filter { it.category.equals(matchedCategory, ignoreCase = true) }
        } else {
            filteredByTime
        }

        val totalAmount = targetExpenses.sumOf { it.amount }
        val periodText = when {
            lower.containsAny("today", "aaj") -> "today"
            lower.containsAny("this month", "iss mahine", "is mahine") -> "this month (${currentMonth.monthName()})"
            lower.containsAny("last month", "pichle mahine") -> "last month (${currentMonth.previous().monthName()})"
            lower.containsAny("this year", "iss saal") -> "this year (${now.get(Calendar.YEAR)})"
            else -> "total overall"
        }

        val catText = if (matchedCategory != null) " on $matchedCategory" else ""

        return if (targetExpenses.isEmpty()) {
            "No spends recorded for $periodText$catText! 0 expenses found."
        } else {
            val formatted = formatMoney(totalAmount)
            "💰 You spent **$formatted**$catText $periodText across ${targetExpenses.size} transaction(s)."
        }
    }

    private fun answerAppHowTo(lower: String): String? {
        return when {
            lower.containsAny("lock", "pin", "biometric", "security", "protect") ->
                "🔒 **App Security & Lock**:\n" +
                "Go to **Profile** tab $\\rightarrow$ **Security**. You can enable PIN or Biometric lock to protect your expenses!"

            lower.containsAny("backup", "export", "save data", "zip", "restore") ->
                "💾 **Backup & Restore**:\n" +
                "Go to **Profile** tab $\\rightarrow$ **Data & Backup**. You can create a complete ZIP backup or export a spreadsheet CSV file anytime!"

            lower.containsAny("compare", "versus", "vs") ->
                "📊 **Compare Months**:\n" +
                "Tap the **Compare** tab in the bottom bar to compare your spending breakdown between any two months!"

            lower.containsAny("wrapped", "summary", "story") ->
                "✨ **Monthly Wrapped**:\n" +
                "In the **Month** tab, tap the **✨ Wrapped** button on the top card to see your monthly spending highlights!"

            lower.containsAny("delete", "recycle", "bin", "trash") ->
                "🗑️ **Recycle Bin**:\n" +
                "Deleted expenses are kept safely in **Profile** $\\rightarrow$ **Recycle Bin** where you can restore them anytime."

            else -> null
        }
    }

    private fun String.containsAny(vararg keywords: String): Boolean = keywords.any { this.contains(it) }

    private fun yearOf(millis: Long): Int = Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.YEAR)

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
