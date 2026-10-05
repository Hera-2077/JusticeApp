package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AudienceWithDetails
import com.example.data.model.User
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object PdfExporter {

    fun generateAndShareCasePdf(
        context: Context,
        caseDetails: AudienceWithDetails,
        user: User?
    ): File? {
        val audience = caseDetails.audience
        val reports = caseDetails.reports.sortedBy { it.recordedAt }
        val decision = caseDetails.decision

        val cleanParty1 = audience.party1.replace("[^a-zA-Z0-9]".toRegex(), "")
        val cleanParty2 = audience.party2.replace("[^a-zA-Z0-9]".toRegex(), "")
        val decDate = decision?.decisionDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val fileName = "Affaire_${cleanParty1}-${cleanParty2}_${decDate}.pdf"

        val cacheDir = context.cacheDir
        val pdfFile = File(cacheDir, fileName)

        var doc: PdfDocument? = null
        try {
            doc = PdfDocument()
            // Standard A4: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val margin = 36f
        val pageWidth = 595f
        val pageHeight = 842f
        val contentWidth = pageWidth - (2 * margin)

        var y = margin

        // 1. Header Banner / Cabinet info (Section 0)
        paint.color = Color.parseColor("#0B4C7A")
        val headerRect = RectF(margin, y, margin + contentWidth, y + 68f)
        canvas.drawRoundRect(headerRect, 6f, 6f, paint)

        // Cabinet Name & Title inside Header
        paint.color = Color.WHITE
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText(user?.firmName ?: "Cabinet d'Avocats", margin + 14f, y + 24f, paint)

        paint.textSize = 10f
        paint.isFakeBoldText = false
        val lawyerInfo = "${user?.title ?: "Me"} ${user?.fullName ?: "Avocat au Barreau"} • ${user?.barAssociation ?: "Barreau du Cameroun"}"
        canvas.drawText(lawyerInfo, margin + 14f, y + 40f, paint)

        val contactInfo = "Tél: ${user?.phone ?: ""} | Email: ${user?.email ?: ""}"
        canvas.drawText(contactInfo, margin + 14f, y + 54f, paint)

        // Document Title Badge on right side of header
        paint.textSize = 11f
        paint.isFakeBoldText = true
        paint.color = Color.parseColor("#F3B23E")
        canvas.drawText("FICHE DE SUIVI D'AFFAIRE", margin + contentWidth - 165f, y + 38f, paint)

        y += 82f

        // 2. Section 1: Identification de l'affaire
        paint.color = Color.parseColor("#0B4C7A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("1. IDENTIFICATION DU DOSSIER", margin, y, paint)
        y += 6f

        paint.color = Color.parseColor("#0B4C7A")
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        y += 14f

        val boxRect = RectF(margin, y, margin + contentWidth, y + 60f)
        paint.color = Color.parseColor("#F1F5F9")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(boxRect, 4f, 4f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 9.5f
        paint.isFakeBoldText = false

        // Parties line
        paint.isFakeBoldText = true
        canvas.drawText("Parties en cause :", margin + 10f, y + 18f, paint)
        paint.isFakeBoldText = false
        val partiesText = "${audience.party1}  CONTRE  ${audience.party2}"
        canvas.drawText(partiesText, margin + 105f, y + 18f, paint)

        // Jurisdiction line
        paint.isFakeBoldText = true
        canvas.drawText("Juridiction :", margin + 10f, y + 34f, paint)
        paint.isFakeBoldText = false
        canvas.drawText("${audience.jurisdiction} (${audience.subJurisdiction})", margin + 105f, y + 34f, paint)

        // Role & Dossier line
        paint.isFakeBoldText = true
        canvas.drawText("N° Dossier / Rôle :", margin + 10f, y + 50f, paint)
        paint.isFakeBoldText = false
        val dossier = if (audience.dossierNumber.isNotBlank()) audience.dossierNumber else "Non précisé"
        canvas.drawText(dossier, margin + 105f, y + 50f, paint)

        paint.isFakeBoldText = true
        canvas.drawText("Qualité de l'avocat :", margin + 280f, y + 50f, paint)
        paint.isFakeBoldText = false
        canvas.drawText(audience.lawyerRole, margin + 385f, y + 50f, paint)

        y += 74f

        // 3. Section 2: Synthèse de la procédure
        paint.color = Color.parseColor("#0B4C7A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("2. SYNTHÈSE DE LA PROCÉDURE", margin, y, paint)
        y += 6f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        y += 14f

        // Calculate days duration
        val durationDays = calculateDurationDays(audience.startDate, decision?.decisionDate ?: audience.nextAudienceDate)
        val reportsCount = reports.size

        // 4 KPI Mini-cards
        val cardWidth = (contentWidth - 24f) / 4f
        val kpis = listOf(
            Pair("1ère Audience", audience.startDate),
            Pair("Décision rendue", decision?.decisionDate ?: "En cours"),
            Pair("Durée totale", "$durationDays jours"),
            Pair("Reports intervenus", "$reportsCount renvoi(s)")
        )

        for (i in kpis.indices) {
            val kpi = kpis[i]
            val kpiX = margin + (i * (cardWidth + 8f))
            val kpiRect = RectF(kpiX, y, kpiX + cardWidth, y + 38f)
            paint.color = Color.parseColor("#E0F2FE")
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(kpiRect, 4f, 4f, paint)

            paint.color = Color.parseColor("#0B4C7A")
            paint.textSize = 8f
            paint.isFakeBoldText = false
            canvas.drawText(kpi.first, kpiX + 6f, y + 14f, paint)

            paint.color = Color.parseColor("#062842")
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
            canvas.drawText(kpi.second, kpiX + 6f, y + 29f, paint)
        }

        y += 52f

        // 4. Section 3: Chronologie des audiences (Tableau)
        paint.color = Color.parseColor("#0B4C7A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("3. CHRONOLOGIE DES AUDIENCES", margin, y, paint)
        y += 6f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        y += 14f

        // Table header
        paint.color = Color.parseColor("#0B4C7A")
        paint.style = Paint.Style.FILL
        val tblHeaderRect = RectF(margin, y, margin + contentWidth, y + 20f)
        canvas.drawRect(tblHeaderRect, paint)

        paint.color = Color.WHITE
        paint.textSize = 8.5f
        paint.isFakeBoldText = true
        canvas.drawText("N°", margin + 6f, y + 14f, paint)
        canvas.drawText("Date", margin + 30f, y + 14f, paint)
        canvas.drawText("Mesure / Statut", margin + 95f, y + 14f, paint)
        canvas.drawText("Motif de renvoi", margin + 195f, y + 14f, paint)
        canvas.drawText("Prochaine Date", margin + 420f, y + 14f, paint)

        y += 20f

        // Table rows
        var rowNum = 1
        for (report in reports) {
            paint.color = if (rowNum % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            canvas.drawRect(margin, y, margin + contentWidth, y + 20f, paint)

            paint.color = Color.parseColor("#334155")
            paint.textSize = 8.5f
            paint.isFakeBoldText = false

            canvas.drawText("$rowNum", margin + 6f, y + 14f, paint)
            canvas.drawText(report.reportedAudienceDate, margin + 30f, y + 14f, paint)
            canvas.drawText("Renvoi / Report", margin + 95f, y + 14f, paint)

            val truncatedReason = if (report.reason.length > 42) report.reason.take(40) + "…" else report.reason
            canvas.drawText(truncatedReason, margin + 195f, y + 14f, paint)
            canvas.drawText(report.newAudienceDate, margin + 420f, y + 14f, paint)

            y += 20f
            rowNum++
        }

        // Final row for the decision hearing if decision exists
        if (decision != null) {
            paint.color = Color.parseColor("#DCFCE7") // Light green for closed row
            canvas.drawRect(margin, y, margin + contentWidth, y + 20f, paint)

            paint.color = Color.parseColor("#166534")
            paint.textSize = 8.5f
            paint.isFakeBoldText = true

            canvas.drawText("$rowNum", margin + 6f, y + 14f, paint)
            canvas.drawText(decision.decisionDate, margin + 30f, y + 14f, paint)
            canvas.drawText("Décision finale", margin + 95f, y + 14f, paint)
            canvas.drawText("Affaire vidée de sa saisine", margin + 195f, y + 14f, paint)
            canvas.drawText("CLÔTURÉE", margin + 420f, y + 14f, paint)

            y += 20f
        }

        y += 14f

        // 5. Section 4: Décision finale
        paint.color = Color.parseColor("#0B4C7A")
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("4. DÉCISION FINALE & DISPOSITIF", margin, y, paint)
        y += 6f
        canvas.drawLine(margin, y, margin + contentWidth, y, paint)
        y += 14f

        val decisionText = decision?.decisionText ?: "Affaire en cours d'instruction d'audience. Aucune décision finale rendue."
        val decisionDateStr = decision?.decisionDate ?: "En attente"

        val decBoxHeight = 85f
        val decRect = RectF(margin, y, margin + contentWidth, y + decBoxHeight)
        paint.color = Color.parseColor("#F8FAFC")
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(decRect, 4f, 4f, paint)

        // Border on the decision box
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(decRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("Date du prononcé : $decisionDateStr", margin + 10f, y + 16f, paint)

        paint.isFakeBoldText = false
        paint.textSize = 8.5f
        // Multi-line wrap for decision text
        var textY = y + 32f
        val maxCharsPerLine = 92
        val lines = wrapText(decisionText, maxCharsPerLine)
        for (line in lines.take(4)) {
            canvas.drawText(line, margin + 10f, textY, paint)
            textY += 12f
        }

        y += decBoxHeight + 14f

        // 6. Section 5: Notes de l'avocat (si renseignées)
        if (audience.notes.isNotBlank()) {
            paint.color = Color.parseColor("#0B4C7A")
            paint.textSize = 10f
            paint.isFakeBoldText = true
            canvas.drawText("5. OBSERVATIONS DU CONSEIL", margin, y, paint)
            y += 4f
            canvas.drawLine(margin, y, margin + contentWidth, y, paint)
            y += 12f

            paint.color = Color.parseColor("#334155")
            paint.textSize = 8f
            paint.isFakeBoldText = false
            val noteLines = wrapText(audience.notes, maxCharsPerLine)
            for (line in noteLines.take(2)) {
                canvas.drawText(line, margin + 10f, y, paint)
                y += 10f
            }
            y += 10f
        }

        // 7. Section 6: Signature block & Footer (Section 6)
        val footerY = pageHeight - margin - 50f
        paint.color = Color.parseColor("#0B4C7A")
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("Pour l'Avocat soussigné :", margin + contentWidth - 180f, footerY - 20f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8f
        paint.isFakeBoldText = false
        canvas.drawText("${user?.title ?: "Me"} ${user?.fullName ?: "Paul Ndongo"}", margin + contentWidth - 180f, footerY - 6f, paint)

        // Footer dividing line
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(margin, footerY + 14f, margin + contentWidth, footerY + 14f, paint)

        // Footer note & date
        val nowFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 7.5f
        canvas.drawText("Document généré par l'application TheKuzan le $nowFormatted", margin, footerY + 28f, paint)
        canvas.drawText("Page 1 sur 1", margin + contentWidth - 50f, footerY + 28f, paint)

        doc.finishPage(page)

        try {
            val fos = FileOutputStream(pdfFile)
            doc.writeTo(fos)
            fos.flush()
            fos.close()
            doc.close()
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            doc.close()
            return null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        doc?.close()
        try {
            if (!pdfFile.exists()) {
                pdfFile.writeText("%PDF-1.4 Fallback Document Affaire $cleanParty1-$cleanParty2")
            }
            return pdfFile
        } catch (ex: Exception) {
            return null
        }
    }
}

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Fiche de suivi d'affaire - ${pdfFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Partager la fiche de suivi d'affaire")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun calculateDurationDays(startDateStr: String, endDateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val start = sdf.parse(startDateStr)
            val end = sdf.parse(endDateStr)
            if (start != null && end != null) {
                val diff = end.time - start.time
                val days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS)
                if (days < 0) 0 else days
            } else 0
        } catch (e: Exception) {
            0
        }
    }

    private fun wrapText(text: String, maxChars: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length + 1 > maxChars) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder(word)
            } else {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
