package com.pratul.expensesmanager.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A printable A4 PDF of expenses: header, summary, spending by category, and every expense in a
 * table across as many pages as needed, with page numbers.
 */
object PdfReport {
    private const val W = 595f   // A4 in points
    private const val H = 842f
    private const val M = 40f    // page margin

    private val violet = Color.rgb(0x7C, 0x3A, 0xED)
    private val pink = Color.rgb(0xDB, 0x27, 0x77)
    private val ink = Color.rgb(0x1B, 0x15, 0x30)
    private val muted = Color.rgb(0x6B, 0x63, 0x85)
    private val zebra = Color.rgb(0xF6, 0xF2, 0xFF)
    private val line = Color.rgb(0xE4, 0xDC, 0xF7)

    private fun paint(size: Float, color: Int = ink, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
            textAlign = align
        }

    private fun money(v: Double) = "₹" + String.format(Locale.US, "%,.2f", v)
    private fun ellipsize(text: String, p: Paint, maxWidth: Float): String {
        if (p.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && p.measureText("$t…") > maxWidth) t = t.dropLast(1)
        return "$t…"
    }

    suspend fun write(
        context: Context,
        uri: Uri,
        expenses: List<Expense>,
        periodLabel: String,
        ownerName: String
    ) = withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val sorted = expenses.sortedBy { it.dateMillis }
        val total = sorted.sumOf { it.amount }
        val dateFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        var pageNo = 0
        lateinit var page: PdfDocument.Page
        lateinit var c: Canvas
        var y = 0f

        fun newPage() {
            if (pageNo > 0) {
                c.drawText("Page $pageNo", W - M, H - 20f, paint(9f, muted, align = Paint.Align.RIGHT))
                doc.finishPage(page)
            }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W.toInt(), H.toInt(), pageNo).create())
            c = page.canvas
            y = M
        }

        fun ensure(space: Float) { if (y + space > H - 50f) newPage() }

        // ---------- Header ----------
        newPage()
        val header = RectF(0f, 0f, W, 120f)
        c.drawRect(header, Paint().apply { shader = LinearGradient(0f, 0f, W, 120f, violet, pink, Shader.TileMode.CLAMP) })
        c.drawText("Expense report", M, 58f, paint(26f, Color.WHITE, bold = true))
        c.drawText(
            listOfNotNull(periodLabel, ownerName.ifBlank { null }).joinToString("  ·  "),
            M, 84f, paint(12f, Color.WHITE)
        )
        c.drawText("Expenses Manager · ${dateFmt.format(Date())}", W - M, 104f, paint(9f, Color.WHITE, align = Paint.Align.RIGHT))
        y = 150f

        // ---------- Summary ----------
        val days = if (sorted.isEmpty()) 1 else
            (((sorted.last().dateMillis - sorted.first().dateMillis) / 86_400_000L) + 1).coerceAtLeast(1)
        val boxes = listOf(
            "Total spent" to money(total),
            "Spends" to sorted.size.toString(),
            "Average per day" to money(total / days)
        )
        val boxW = (W - 2 * M - 20f) / 3f
        boxes.forEachIndexed { i, (label, value) ->
            val x = M + i * (boxW + 10f)
            c.drawRoundRect(RectF(x, y, x + boxW, y + 58f), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = zebra })
            c.drawText(label, x + 12f, y + 22f, paint(10f, muted))
            c.drawText(value, x + 12f, y + 44f, paint(15f, ink, bold = true))
        }
        y += 86f

        // ---------- By category ----------
        val byCategory = sorted.groupBy { it.category }
            .map { (cat, list) -> cat to list.sumOf { it.amount } }
            .sortedByDescending { it.second }
        if (byCategory.isNotEmpty()) {
            c.drawText("Spending by category", M, y, paint(14f, ink, bold = true))
            y += 16f
            val barMax = byCategory.first().second
            for ((cat, amount) in byCategory) {
                ensure(22f)
                c.drawText(ellipsize(cat, paint(11f), 120f), M, y + 12f, paint(11f))
                val barX = M + 130f
                val barW = (W - 2 * M - 130f - 170f) * (amount / barMax).toFloat()
                c.drawRoundRect(RectF(barX, y + 3f, barX + barW.coerceAtLeast(2f), y + 13f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = violet })
                val pct = if (total > 0) (amount / total * 100).toInt() else 0
                c.drawText("${money(amount)}  ($pct%)", W - M, y + 12f, paint(11f, ink, align = Paint.Align.RIGHT))
                y += 20f
            }
            y += 18f
        }

        // ---------- All expenses ----------
        val cols = floatArrayOf(M, M + 80f, M + 190f, M + 300f) // date, category, item, note; amount right-aligned
        fun tableHeader() {
            c.drawRect(RectF(M, y, W - M, y + 22f), Paint().apply { color = violet })
            val hp = paint(10f, Color.WHITE, bold = true)
            c.drawText("Date", cols[0] + 6f, y + 15f, hp)
            c.drawText("Category", cols[1], y + 15f, hp)
            c.drawText("Item", cols[2], y + 15f, hp)
            c.drawText("Note", cols[3], y + 15f, hp)
            c.drawText("Amount", W - M - 6f, y + 15f, paint(10f, Color.WHITE, bold = true, align = Paint.Align.RIGHT))
            y += 22f
        }
        ensure(60f)
        c.drawText("All expenses", M, y, paint(14f, ink, bold = true))
        y += 12f
        tableHeader()
        if (sorted.isEmpty()) {
            c.drawText("No expenses in this period.", M + 6f, y + 18f, paint(11f, muted))
            y += 26f
        }
        val cell = paint(10f)
        sorted.forEachIndexed { i, e ->
            if (y + 20f > H - 50f) {
                newPage()
                tableHeader()
            }
            if (i % 2 == 0) c.drawRect(RectF(M, y, W - M, y + 20f), Paint().apply { color = zebra })
            c.drawText(dateFmt.format(Date(e.dateMillis)), cols[0] + 6f, y + 14f, cell)
            c.drawText(ellipsize(e.category, cell, 104f), cols[1], y + 14f, cell)
            c.drawText(ellipsize(e.item.ifBlank { "-" }, cell, 104f), cols[2], y + 14f, cell)
            c.drawText(ellipsize(e.note, cell, W - M - 90f - cols[3]), cols[3], y + 14f, cell)
            c.drawText(money(e.amount), W - M - 6f, y + 14f, paint(10f, ink, align = Paint.Align.RIGHT))
            y += 20f
        }
        // Total row
        ensure(28f)
        c.drawLine(M, y + 2f, W - M, y + 2f, Paint().apply { color = line; strokeWidth = 1f })
        c.drawText("Total", cols[0] + 6f, y + 20f, paint(11f, ink, bold = true))
        c.drawText(money(total), W - M - 6f, y + 20f, paint(11f, ink, bold = true, align = Paint.Align.RIGHT))

        c.drawText("Page $pageNo", W - M, H - 20f, paint(9f, muted, align = Paint.Align.RIGHT))
        doc.finishPage(page)
        val out = context.contentResolver.openOutputStream(uri, "wt") ?: throw IllegalStateException("Couldn't open the file for writing.")
        out.use { doc.writeTo(it) }
        doc.close()
    }
}
