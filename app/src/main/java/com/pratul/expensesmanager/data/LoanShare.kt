package com.pratul.expensesmanager.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.pratul.expensesmanager.expectExternalReturn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Turns a borrow/lend record into a shareable picture and opens Android's share sheet. */
object LoanShare {
    private const val W = 1080
    private const val H = 1350

    // Paise only when there are some
    private fun money(v: Double) = "₹" + String.format(Locale.getDefault(), if (v % 1.0 != 0.0) "%,.2f" else "%,.0f", v)

    /** Draws the card. Kept separate so it can be previewed or tested. */
    fun render(loan: Loan): Bitmap {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val lent = loan.type == LoanType.Lent
        val dates = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

        // Background: brand gradient, green-ish for lent, orange-ish for borrowed
        val colors = if (lent) intArrayOf(0xFF7C3AED.toInt(), 0xFF0EA5E9.toInt(), 0xFF10B981.toInt())
        else intArrayOf(0xFF7C3AED.toInt(), 0xFFDB2777.toInt(), 0xFFF97316.toInt())
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, W.toFloat(), H.toFloat(), colors, null, Shader.TileMode.CLAMP)
        })
        // Soft glow circles
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(28, 255, 255, 255) }
        c.drawCircle(W * 0.9f, H * 0.08f, 300f, glow)
        c.drawCircle(W * 0.05f, H * 0.95f, 360f, glow)

        // White card
        val card = RectF(70f, 230f, W - 70f, H - 190f)
        c.drawRoundRect(card, 56f, 56f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(40f, 0f, 16f, Color.argb(60, 0, 0, 0))
        })

        // App mark at the top: the ring logo and name
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 14f; color = Color.WHITE }
        val ringBox = RectF(90f, 90f, 160f, 160f)
        c.drawArc(ringBox, -90f, 250f, false, ring)
        c.drawArc(ringBox, 175f, 80f, false, Paint(ring).apply { color = 0xFFFFD54F.toInt() })
        c.drawText("Expenses Manager", 185f, 140f, text(44f, Color.WHITE, bold = true))

        val ink = 0xFF1B1530.toInt()
        val muted = 0xFF6B6385.toInt()
        var y = card.top + 120f
        val cx = W / 2f

        // Emoji + headline
        c.drawText(if (lent) "🤝" else "🙏", cx, y + 30f, text(120f, ink, align = Paint.Align.CENTER))
        y += 120f
        val headline = if (lent) "I lent" else "I borrowed"
        c.drawText(headline, cx, y + 30f, text(46f, muted, align = Paint.Align.CENTER))
        y += 120f
        c.drawText(money(loan.amount), cx, y + 30f, text(120f, ink, bold = true, align = Paint.Align.CENTER))
        y += 90f
        c.drawText((if (lent) "to " else "from ") + loan.person, cx, y + 20f, text(56f, ink, bold = true, align = Paint.Align.CENTER))
        y += 90f

        // Details
        val line = Paint().apply { color = 0xFFE9E1FB.toInt(); strokeWidth = 3f }
        c.drawLine(card.left + 70f, y, card.right - 70f, y, line)
        y += 70f
        fun row(label: String, value: String) {
            c.drawText(label, card.left + 80f, y, text(38f, muted))
            c.drawText(value, card.right - 80f, y, text(38f, ink, bold = true, align = Paint.Align.RIGHT))
            y += 66f
        }
        row("Date", dates.format(Date(loan.dateMillis)))
        loan.dueMillis?.let { row("Pay back by", dates.format(Date(it))) }
        row("Status", if (loan.settled) "Settled ✅" else "Pending ⏳")

        if (loan.note.isNotBlank()) {
            y += 10f
            val tp = TextPaint(text(38f, ink)).apply { textAlign = Paint.Align.LEFT }
            val width = (card.width() - 160f).toInt()
            val note = "📝 ${loan.note}"
            val layout = StaticLayout.Builder.obtain(note, 0, note.length, tp, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setMaxLines(3)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
            c.save()
            c.translate(card.left + 80f, y - 30f)
            layout.draw(c)
            c.restore()
        }

        // Footer
        c.drawText("Shared from Expenses Manager", cx, H - 100f, text(36f, Color.argb(230, 255, 255, 255), align = Paint.Align.CENTER))
        return bmp
    }

    private fun text(size: Float, color: Int, bold: Boolean = false, align: Paint.Align = Paint.Align.LEFT) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            textAlign = align
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    /** Renders the card, saves it privately and opens the share sheet. Returns false if no app can share. */
    suspend fun share(context: Context, loan: Loan): Boolean {
        val file = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "shares").apply { mkdirs() }
            // Only the latest share is kept
            dir.listFiles()?.forEach { it.delete() }
            File(dir, "loan-${loan.id}.png").also { f ->
                f.outputStream().use { render(loan).compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val message = if (loan.type == LoanType.Lent) "I lent ${money(loan.amount)} to ${loan.person}"
        else "I borrowed ${money(loan.amount)} from ${loan.person}"
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, message)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return try {
            context.expectExternalReturn()
            context.startActivity(Intent.createChooser(send, "Share via").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
