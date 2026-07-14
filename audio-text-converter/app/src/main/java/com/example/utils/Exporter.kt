package com.example.utils

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.TranscriptNote
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    fun exportToTxt(context: Context, note: TranscriptNote): Uri {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date(note.timestamp))
        val durationText = formatDuration(note.durationSeconds)

        val builder = StringBuilder()
        builder.append("=== ${note.title} ===\n")
        builder.append("Date: $formattedDate\n")
        builder.append("Duration: $durationText\n\n")

        if (note.summary.isNotEmpty()) {
            builder.append("--- AI SUMMARY ---\n")
            builder.append(note.summary).append("\n\n")
        }

        val takeaways = note.getKeyTakeawaysList()
        if (takeaways.isNotEmpty()) {
            builder.append("--- KEY TAKEAWAYS ---\n")
            takeaways.forEach { builder.append("• ").append(it).append("\n") }
            builder.append("\n")
        }

        builder.append("--- FULL TRANSCRIPT ---\n")
        builder.append(note.rawText)

        val file = File(context.cacheDir, "${sanitizeFileName(note.title)}.txt")
        FileOutputStream(file).use { out ->
            out.write(builder.toString().toByteArray())
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    fun exportToMarkdown(context: Context, note: TranscriptNote): Uri {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val formattedDate = sdf.format(Date(note.timestamp))
        val durationText = formatDuration(note.durationSeconds)

        val builder = StringBuilder()
        builder.append("# ${note.title}\n\n")
        builder.append("**Date:** $formattedDate  \n")
        builder.append("**Duration:** $durationText  \n\n")

        if (note.summary.isNotEmpty()) {
            builder.append("## AI Summary\n\n")
            builder.append(note.summary).append("\n\n")
        }

        val takeaways = note.getKeyTakeawaysList()
        if (takeaways.isNotEmpty()) {
            builder.append("## Key Takeaways\n\n")
            takeaways.forEach { builder.append("- ").append(it).append("\n") }
            builder.append("\n")
        }

        builder.append("## Full Transcript\n\n")
        builder.append(note.rawText)

        val file = File(context.cacheDir, "${sanitizeFileName(note.title)}.md")
        FileOutputStream(file).use { out ->
            out.write(builder.toString().toByteArray())
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    fun exportToPdf(context: Context, note: TranscriptNote): Uri {
        val document = PdfDocument()
        val pageWidth = 595 // A4 page width in points
        val pageHeight = 842 // A4 page height in points
        val margin = 50

        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val h2Paint = TextPaint().apply {
            color = Color.rgb(63, 81, 181) // Beautiful Indigo color for headings
            textSize = 14f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        val metaPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateText = "Recorded: ${sdf.format(Date(note.timestamp))}  |  Duration: ${formatDuration(note.durationSeconds)}"

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = margin

        // Draw Title
        val titleLayout = StaticLayout.Builder.obtain(note.title, 0, note.title.length, titlePaint, pageWidth - 2 * margin)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), yPosition.toFloat())
        titleLayout.draw(canvas)
        canvas.restore()
        yPosition += titleLayout.height + 12

        // Draw Metadata
        val metaLayout = StaticLayout.Builder.obtain(dateText, 0, dateText.length, metaPaint, pageWidth - 2 * margin)
            .build()
        canvas.save()
        canvas.translate(margin.toFloat(), yPosition.toFloat())
        metaLayout.draw(canvas)
        canvas.restore()
        yPosition += metaLayout.height + 25

        fun checkAndCreateNewPageIfNeeded(contentHeight: Int) {
            if (yPosition + contentHeight > pageHeight - margin) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = margin
            }
        }

        fun drawSection(sectionTitle: String, textContent: String) {
            val sectionTitleLayout = StaticLayout.Builder.obtain(sectionTitle, 0, sectionTitle.length, h2Paint, pageWidth - 2 * margin).build()
            checkAndCreateNewPageIfNeeded(sectionTitleLayout.height + 20)
            canvas.save()
            canvas.translate(margin.toFloat(), yPosition.toFloat())
            sectionTitleLayout.draw(canvas)
            canvas.restore()
            yPosition += sectionTitleLayout.height + 10

            val paragraphs = textContent.split("\n")
            for (p in paragraphs) {
                if (p.isBlank()) continue
                val pLayout = StaticLayout.Builder.obtain(p, 0, p.length, bodyPaint, pageWidth - 2 * margin).build()
                checkAndCreateNewPageIfNeeded(pLayout.height + 15)
                canvas.save()
                canvas.translate(margin.toFloat(), yPosition.toFloat())
                pLayout.draw(canvas)
                canvas.restore()
                yPosition += pLayout.height + 10
            }
            yPosition += 10
        }

        // Draw Summary Section
        if (note.summary.isNotEmpty()) {
            drawSection("AI Summary", note.summary)
        }

        // Draw Key Takeaways Section
        val takeaways = note.getKeyTakeawaysList()
        if (takeaways.isNotEmpty()) {
            val sectionTitleLayout = StaticLayout.Builder.obtain("Key Takeaways", 0, 13, h2Paint, pageWidth - 2 * margin).build()
            checkAndCreateNewPageIfNeeded(sectionTitleLayout.height + 20)
            canvas.save()
            canvas.translate(margin.toFloat(), yPosition.toFloat())
            sectionTitleLayout.draw(canvas)
            canvas.restore()
            yPosition += sectionTitleLayout.height + 10

            for (item in takeaways) {
                val bulletPointText = "• $item"
                val bulletLayout = StaticLayout.Builder.obtain(bulletPointText, 0, bulletPointText.length, bodyPaint, pageWidth - 2 * margin - 15).build()
                checkAndCreateNewPageIfNeeded(bulletLayout.height + 10)
                canvas.save()
                canvas.translate((margin + 15).toFloat(), yPosition.toFloat())
                bulletLayout.draw(canvas)
                canvas.restore()
                yPosition += bulletLayout.height + 6
            }
            yPosition += 15
        }

        // Draw Full Transcript Section
        if (note.rawText.isNotEmpty()) {
            drawSection("Full Transcript", note.rawText)
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "${sanitizeFileName(note.title)}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    private fun formatDuration(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun sanitizeFileName(title: String): String {
        val sanitized = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return if (sanitized.isBlank()) "note" else sanitized
    }
}
