package com.example.invoicegen.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.invoicegen.data.model.BusinessInfo
import com.example.invoicegen.data.model.DocumentType
import com.example.invoicegen.data.model.Invoice
import com.example.invoicegen.data.model.LineItem
import com.example.invoicegen.util.CurrencyUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoicePdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // US Letter at 72 DPI
        private const val PAGE_WIDTH = 612f
        private const val PAGE_HEIGHT = 792f
        private const val M = 36f                    // margin
        private const val CW = PAGE_WIDTH - 2 * M   // content width = 540
    }

    // ---- Paint factory helpers ----
    private fun tp(size: Float, bold: Boolean = false, color: Int = Color.BLACK) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.color = color
        }

    private fun lp(w: Float = 0.75f, color: Int = Color.BLACK) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = w
            this.color = color
        }

    private fun fp(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    /**
     * Generate a PDF matching the invoice.jpg contractor proposal form layout.
     * Returns the generated File in the app's private pdfs directory.
     */
    fun generate(invoice: Invoice, businessInfo: BusinessInfo): File {
        val pdfDir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val pdfFile = File(pdfDir, buildFileName(invoice))

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH.toInt(), PAGE_HEIGHT.toInt(), 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas

        var y = M

        // 1. Header block
        y = drawHeader(c, businessInfo, y)

        // 2. Date + Checkboxes row
        y = drawDateAndCheckboxes(c, invoice.proposalDate, invoice.documentType, y)

        // 3. "PROPOSAL SUBMITTED TO" / "WORK TO BE PERFORMED AT" two-column
        y = drawClientJobSection(c, invoice, y)

        // 4. Work description area
        y = drawWorkArea(c, invoice, y)

        // 5. Amount / terms section
        y = drawAmountTerms(c, invoice, y)

        // 6. Signature/payment section
        y = drawSignatureSection(c, y)

        // 7. Footer disclaimers + acceptance block
        drawFooterSection(c, y)

        doc.finishPage(page)
        FileOutputStream(pdfFile).use { doc.writeTo(it) }
        doc.close()
        return pdfFile
    }

    // ─── Section drawers ──────────────────────────────────────────────────────

    /** Business logo (left) + business contact (center) */
    private fun drawHeader(c: Canvas, info: BusinessInfo, y0: Float): Float {
        var y = y0

        // Logo on the left
        val logoFile = if (info.logoPath.isNotEmpty()) File(info.logoPath) else null
        if (logoFile != null && logoFile.exists()) {
            val bmp = BitmapFactory.decodeFile(logoFile.absolutePath)
            if (bmp != null) {
                val size = 56f
                val scaled = Bitmap.createScaledBitmap(bmp, size.toInt(), size.toInt(), true)
                c.drawBitmap(scaled, M, y, null)
            }
        }

        // Business name centered
        val namePaint = tp(14f, bold = true)
        val name = info.businessName.ifEmpty { "Your Business Name" }
        c.drawText(name, PAGE_WIDTH / 2f - namePaint.measureText(name) / 2f, y + 16f, namePaint)

        // Contact lines centered
        val subP = tp(8.5f)
        var subY = y + 28f
        for (line in listOf(info.email, info.phone, info.address).filter { it.isNotEmpty() }) {
            c.drawText(line, PAGE_WIDTH / 2f - subP.measureText(line) / 2f, subY, subP)
            subY += 11f
        }

        val bottom = maxOf(subY + 4f, y + 65f)
        c.drawLine(M, bottom, PAGE_WIDTH - M, bottom, lp(1f))
        return bottom + 6f
    }

    /** Date field (center) + Contract/Proposal checkboxes (right) */
    private fun drawDateAndCheckboxes(
        c: Canvas, date: String, docType: DocumentType, y0: Float
    ): Float {
        val y = y0
        val p = tp(9f)
        val bp = tp(8.5f, bold = true)

        // Date
        val dateLabel = "DATE:"
        val dateX = M + 60f
        c.drawText(dateLabel, dateX, y + 10f, bp)
        c.drawText(date, dateX + 36f, y + 10f, p)
        c.drawLine(dateX + 32f, y + 11f, dateX + 32f + 100f, y + 11f, lp(0.5f))

        // Checkboxes top-right — CONTRACT above PROPOSAL
        val cbX = PAGE_WIDTH - M - 100f
        val cbP = tp(8f)
        drawCheckbox(c, cbX, y, docType == DocumentType.CONTRACT, "CONTRACT")
        drawCheckbox(c, cbX, y + 14f, docType == DocumentType.PROPOSAL, "PROPOSAL")

        return y + 30f
    }

    private fun drawCheckbox(c: Canvas, x: Float, y: Float, checked: Boolean, label: String) {
        val boxSize = 8f
        c.drawRect(x, y + 1f, x + boxSize, y + 1f + boxSize, lp(0.75f))
        if (checked) {
            val cp = tp(9f, bold = true)
            c.drawText("✓", x + 1f, y + boxSize - 0.5f, cp)
        }
        c.drawText(label, x + boxSize + 3f, y + boxSize - 1f, tp(8f))
    }

    /** Two-column: Proposal Submitted To | Work To Be Performed At */
    private fun drawClientJobSection(c: Canvas, invoice: Invoice, y0: Float): Float {
        var y = y0
        val colW = CW / 2f - 2f
        val x1 = M
        val x2 = M + colW + 4f
        val hH = 13f        // header bar height
        val rowH = 16f
        val rows = 3
        val boxH = hH + rowH * rows + 4f

        // Header bars
        c.drawRect(x1, y, x1 + colW, y + hH, fp(Color.rgb(50, 50, 50)))
        c.drawRect(x2, y, x2 + colW, y + hH, fp(Color.rgb(50, 50, 50)))

        val hP = tp(7f, bold = true, color = Color.WHITE)
        c.drawText("PROPOSAL SUBMITTED TO:", x1 + 2f, y + 9f, hP)
        c.drawText("WORK TO BE PERFORMED AT:", x2 + 2f, y + 9f, hP)

        // Outlines
        c.drawRect(x1, y, x1 + colW, y + boxH, lp())
        c.drawRect(x2, y, x2 + colW, y + boxH, lp())

        // Field labels + values
        val labelP = tp(7.5f, bold = true)
        val valP = tp(8.5f)

        data class FieldRow(val label: String, val value: String)

        val col1 = listOf(
            FieldRow("NAME", invoice.clientName),
            FieldRow("ADDRESS", invoice.clientAddress),
            FieldRow("PHONE NO.", invoice.clientPhone)
        )
        val col2 = listOf(
            FieldRow("ADDRESS", invoice.jobAddress),
            FieldRow("DATE OF PLANS", invoice.datePlans),
            FieldRow("ARCHITECT", invoice.architect)
        )

        var ry = y + hH + 1f
        for (i in 0..2) {
            val lineY = ry + rowH
            // Col 1
            c.drawText(col1[i].label, x1 + 2f, ry + 8f, labelP)
            c.drawLine(x1, lineY, x1 + colW, lineY, lp(0.4f))
            if (col1[i].value.isNotEmpty())
                c.drawText(col1[i].value.take(35), x1 + 2f, ry + 8f + 7f, valP)
            // Col 2
            c.drawText(col2[i].label, x2 + 2f, ry + 8f, labelP)
            c.drawLine(x2, lineY, x2 + colW, lineY, lp(0.4f))
            if (col2[i].value.isNotEmpty())
                c.drawText(col2[i].value.take(35), x2 + 2f, ry + 8f + 7f, valP)
            ry += rowH
        }

        return y + boxH + 8f
    }

    /** Work description area (big box) + line items if any */
    private fun drawWorkArea(c: Canvas, invoice: Invoice, y0: Float): Float {
        var y = y0
        val intro = tp(8.5f)
        val introText =
            "We hereby propose to furnish the materials and perform the labor necessary for the completion of:"
        c.drawText(introText, M, y + 10f, intro)
        y += 16f

        // Work description box
        val descLines = wrapText(invoice.workDescription, tp(8.5f), CW - 4f)
        val descBoxH = maxOf(40f, descLines.size * 12f + 8f)
        c.drawRect(M, y, M + CW, y + descBoxH, lp())
        var ly = y + 12f
        for (line in descLines) {
            c.drawText(line, M + 3f, ly, tp(8.5f))
            ly += 12f
        }

        // "CONTINUE ON THE BACK" note if items overflow (kept simple for single-page)
        val noteP = tp(7f, color = Color.GRAY)
        c.drawText("(CONTINUE ON THE BACK)", PAGE_WIDTH - M - noteP.measureText("(CONTINUE ON THE BACK)"), y + descBoxH - 4f, noteP)

        y += descBoxH + 6f

        // Line items table (compact)
        if (invoice.lineItems.isNotEmpty()) {
            y = drawLineItemsCompact(c, invoice.lineItems, y)
        }

        return y
    }

    /** Compact line items table */
    private fun drawLineItemsCompact(c: Canvas, items: List<LineItem>, y0: Float): Float {
        var y = y0
        val colDesc = M
        val colQty = M + 300f
        val colPrice = M + 360f
        val colTotal = M + 440f
        val rowH = 14f
        val headerH = 13f

        // Header
        c.drawRect(M, y, M + CW, y + headerH, fp(Color.rgb(80, 80, 80)))
        val hP = tp(7.5f, bold = true, color = Color.WHITE)
        c.drawText("DESCRIPTION", colDesc + 2f, y + 9f, hP)
        c.drawText("QTY", colQty + 2f, y + 9f, hP)
        c.drawText("UNIT PRICE", colPrice + 2f, y + 9f, hP)
        c.drawText("TOTAL", colTotal + 2f, y + 9f, hP)
        y += headerH

        val vP = tp(8f)
        items.forEachIndexed { i, item ->
            if (i % 2 == 1) c.drawRect(M, y, M + CW, y + rowH, fp(Color.rgb(245, 245, 245)))
            c.drawText(item.description.take(46), colDesc + 2f, y + 9f, vP)
            c.drawText(String.format("%.2f", item.quantity), colQty + 2f, y + 9f, vP)
            c.drawText(CurrencyUtil.formatCents(item.unitPrice), colPrice + 2f, y + 9f, vP)
            c.drawText(CurrencyUtil.formatCents(item.lineTotal), colTotal + 2f, y + 9f, vP)
            y += rowH
        }
        // Box around table
        c.drawRect(M, y0, M + CW, y, lp())
        // Column dividers
        for (x in listOf(colQty, colPrice, colTotal)) {
            c.drawLine(x, y0, x, y, lp(0.4f))
        }
        return y + 6f
    }

    /** "All material is guaranteed…the sum of $__ Dollars" block */
    private fun drawAmountTerms(c: Canvas, invoice: Invoice, y0: Float): Float {
        var y = y0 + 8f
        val p  = tp(8f)
        val bp = tp(8.5f, bold = true)
        val totalStr = CurrencyUtil.formatCents(invoice.total)
        val taxAmt   = CurrencyUtil.calcTax(invoice.subtotal, invoice.taxPercent)
        val totX     = PAGE_WIDTH - M - 160f   // right-side column x

        // ── Guarantee paragraph (left side, full width, wrapped) ──────────────
        val guaranteeText = "All material is guaranteed to be as specified, and the above to be performed " +
                "in accordance with the drawings and specifications submitted for the above work, " +
                "and completed in a substantial workmanlike manner of the sum of"
        for (line in wrapText(guaranteeText, p, CW)) {
            y += 11f
            c.drawText(line, M, y, p)
        }
        y += 8f

        // ── Dollar amount written out (like a check) ──────────────────────────
        val wordsAmount = amountInWords(invoice.total)
        val dollarLine = "$wordsAmount Dollars ($totalStr)"
        for (line in wrapText(dollarLine, p, CW)) {
            y += 11f
            c.drawText(line, M, y, p)
        }
        y += 6f

        // ── Subtotal / Tax / TOTAL (right side, drawn BELOW guarantee) ────────
        c.drawText("Subtotal:", totX, y, p)
        c.drawText(CurrencyUtil.formatCents(invoice.subtotal), PAGE_WIDTH - M, y, tp(8f).also { it.textAlign = Paint.Align.RIGHT })
        y += 12f

        c.drawText("Tax (${String.format("%.1f", invoice.taxPercent)}%):", totX, y, p)
        c.drawText(CurrencyUtil.formatCents(taxAmt), PAGE_WIDTH - M, y, tp(8f).also { it.textAlign = Paint.Align.RIGHT })
        y += 12f

        c.drawText("TOTAL:", totX, y, bp)
        c.drawText(totalStr, PAGE_WIDTH - M, y, tp(8.5f, bold = true).also { it.textAlign = Paint.Align.RIGHT })
        y += 14f

        // ── Payment terms ─────────────────────────────────────────────────────
        c.drawText("With payments to be made as follows:", M, y, p)
        y += 12f
        c.drawLine(M, y, PAGE_WIDTH - M, y, lp(0.5f))
        y += 12f
        c.drawText("Respectively submitted", M, y, p)
        y += 16f

        return y
    }

    /** Signature, date, cash/check/deposit/balance row */
    private fun drawSignatureSection(c: Canvas, y0: Float): Float {
        var y = y0
        val p = tp(8f)
        val lLen = 90f
        val gap = 12f

        fun underline(label: String, x: Float, yy: Float) {
            c.drawText(label, x, yy, p)
            val lx = x + p.measureText(label) + 2f
            c.drawLine(lx, yy, lx + lLen, yy, lp(0.5f))
        }

        // Row 1: Signature + Date (left side)
        underline("Signature", M, y)
        underline("Date", M + lLen + p.measureText("Signature") + gap + 10f, y)
        y += 18f

        // Row 2: second set for client acceptance (drawn in footer section)

        // Row 3: Cash / Check # / Deposit / Balance
        var cx = M
        for ((label, spacing) in listOf("Cash" to 60f, "Check #" to 80f, "Deposit" to 80f, "Balance" to 80f)) {
            underline(label, cx, y)
            cx += p.measureText(label) + spacing + 4f
        }
        y += 20f

        return y
    }

    /**
     * Footer: disclaimers, acceptance block, 15% late fee notice, VALID 30 DAYS.
     */
    private fun drawFooterSection(c: Canvas, y0: Float) {
        var y = y0

        val sp = tp(7f)
        val bp = tp(7.5f, bold = true)

        fun drawWrapped(text: String, paint: Paint, maxW: Float) {
            for (line in wrapText(text, paint, maxW)) {
                if (y + 9f > PAGE_HEIGHT - 10f) return
                c.drawText(line, M, y, paint)
                y += 9f
            }
        }

        c.drawLine(M, y, PAGE_WIDTH - M, y, lp(0.5f))
        y += 8f

        drawWrapped(
            "Not responsible for location of fence when survey is not provided by customer. " +
                    "Not responsible for flat repair or underground cables, pipes.",
            sp, CW
        )
        y += 2f
        drawWrapped(
            "Any alteration or deviation from the above specifications involving extra costs will be executed only " +
                    "upon written order, and will become an extra charge and above the estimate. " +
                    "All agreements contingent upon strikes, accidents, or delays beyond our control.",
            sp, CW
        )

        y += 6f
        c.drawLine(M, y, PAGE_WIDTH - M, y, lp(0.5f))
        y += 8f

        // Acceptance section
        drawWrapped(
            "The above prices, specifications and conditions are satisfactory and are hereby accepted. " +
                    "You are authorized to do the work as specified. " +
                    "Payments will be made as outlined above. Prices and product availability are subject to change.",
            sp, CW
        )
        y += 6f

        // Acceptance signature row
        val p8 = tp(8f)
        c.drawText("Signature", M, y, p8)
        c.drawLine(M + 58f, y, M + 150f, y, lp(0.5f))
        c.drawText("Date", M + 160f, y, p8)
        c.drawLine(M + 178f, y, M + 250f, y, lp(0.5f))
        c.drawText("Signature", M + 270f, y, p8)
        c.drawLine(M + 328f, y, M + 420f, y, lp(0.5f))
        c.drawText("Date", M + 430f, y, p8)
        c.drawLine(M + 448f, y, M + 504f, y, lp(0.5f))
        y += 16f

        // 15% late fee — bold notice
        val lateP = tp(8.5f, bold = true)
        val late = "Please note there is a 15% late fee added to the balance if not paid within 5 days from the completion of work"
        val lateX = PAGE_WIDTH / 2f - lateP.measureText(late) / 2f
        if (lateX > M) {
            c.drawText(late, lateX, y, lateP)
        } else {
            // Wrap it
            for (line in wrapText(late, lateP, CW)) {
                c.drawText(line, M, y, lateP)
                y += 11f
            }
            y -= 11f
        }
        y += 14f

        // VALID FOR 30 DAYS
        val validP = tp(11f, bold = true)
        val valid = "VALID FOR 30 DAYS"
        c.drawText(valid, PAGE_WIDTH / 2f - validP.measureText(valid) / 2f, y, validP)
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    // ─── Number-to-words (for the dollar amount line) ─────────────────────────

    /** Converts a cents value to written English, e.g. 188075 → "One Thousand Eight Hundred Eighty and 75/100" */
    private fun amountInWords(cents: Long): String {
        val dollars = cents / 100
        val centsRemainder = cents % 100
        val words = if (dollars == 0L) "Zero" else dollarsToWords(dollars)
        return "$words and ${centsRemainder.toString().padStart(2, '0')}/100"
    }

    private fun dollarsToWords(n: Long): String {
        if (n == 0L) return ""
        val billions  = n / 1_000_000_000L
        val millions  = (n % 1_000_000_000L) / 1_000_000L
        val thousands = (n % 1_000_000L) / 1_000L
        val remainder = n % 1_000L
        val parts = mutableListOf<String>()
        if (billions  > 0) parts.add("${hundredsToWords(billions)} Billion")
        if (millions  > 0) parts.add("${hundredsToWords(millions)} Million")
        if (thousands > 0) parts.add("${hundredsToWords(thousands)} Thousand")
        if (remainder > 0) parts.add(hundredsToWords(remainder))
        return parts.joinToString(" ")
    }

    private fun hundredsToWords(n: Long): String {
        val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight",
            "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        val parts = mutableListOf<String>()
        val h = n / 100
        if (h > 0) parts.add("${ones[h.toInt()]} Hundred")
        val remainder = (n % 100).toInt()
        when {
            remainder in 1..19 -> parts.add(ones[remainder])
            remainder >= 20    -> {
                val t = remainder / 10
                val o = remainder % 10
                parts.add(if (o > 0) "${tens[t]} ${ones[o]}" else tens[t])
            }
        }
        return parts.joinToString(" ")
    }

    /**
     * Builds a human-readable filename like "Pat_L_invoice_1.pdf".
     * Uses client first name + last initial when available.
     */
    private fun buildFileName(invoice: Invoice): String {
        val parts = invoice.clientName.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
        val prefix = when {
            parts.isEmpty() -> "invoice"
            parts.size == 1 -> sanitizeName(parts[0])
            else -> "${sanitizeName(parts[0])}_${parts.last().first().uppercaseChar()}"
        }
        return "${prefix}_invoice_${invoice.id}.pdf"
    }

    private fun sanitizeName(s: String) = s.replace(Regex("[^A-Za-z0-9]"), "")

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}
