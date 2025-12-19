package com.tyson.fishinglogbook.export

import android.content.ContentResolver
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.tyson.fishinglogbook.data.CatchEntity
import com.tyson.fishinglogbook.data.TripEntity
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporters {

    private fun csvEscape(s: String?): String {
        val v = s ?: ""
        val needsQuotes = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")
        val escaped = v.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    fun writeCsv(
        resolver: ContentResolver,
        uri: Uri,
        trips: List<TripEntity>,
        catches: List<CatchEntity>
    ) {
        resolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os).use { w ->
                w.appendLine(
                    "type,id,tripId,timestamp,species,lengthCm,weightKg,lure,notes," +
                        "latitude,longitude,accuracyM,photoUri," +
                        "weatherTempC,weatherPressureHpa," +
                        "tripName,waterway,tripNotes,tripStart,tripEnd"
                )

                val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val tripById = trips.associateBy { it.id }

                for (c in catches) {
                    val t = c.tripId?.let { tripById[it] }

                    val row = listOf(
                        "catch",
                        c.id.toString(),
                        c.tripId?.toString() ?: "",
                        fmt.format(Date(c.timestampMillis)),
                        csvEscape(c.species),
                        c.lengthCm?.toString() ?: "",
                        c.weightKg?.toString() ?: "",
                        csvEscape(c.lure),
                        csvEscape(c.notes),
                        c.latitude?.toString() ?: "",
                        c.longitude?.toString() ?: "",
                        c.accuracyM?.toString() ?: "",
                        csvEscape(c.photoUri),
                        c.weatherTempC?.toString() ?: "",
                        c.weatherPressureHpa?.toString() ?: "",
                        csvEscape(t?.name),
                        csvEscape(t?.waterway),
                        csvEscape(t?.notes),
                        t?.startMillis?.let { fmt.format(Date(it)) } ?: "",
                        t?.endMillis?.let { fmt.format(Date(it)) } ?: ""
                    )

                    w.appendLine(row.joinToString(","))
                }
            }
        }
    }

    fun writePdf(
        context: Context,
        resolver: ContentResolver,
        uri: Uri,
        trips: List<TripEntity>,
        catches: List<CatchEntity>
    ) {
        val doc = PdfDocument()
        val paint = Paint().apply { textSize = 12f }
        val titlePaint = Paint().apply { textSize = 16f }

        val fmt = SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault())
        val tripById = trips.associateBy { it.id }

        var pageNum = 1
        var y = 0f
        fun newPage(): PdfDocument.Page {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum++).create()
            y = 40f
            return doc.startPage(pageInfo)
        }

        var page = newPage()
        var canvas = page.canvas
        canvas.drawText("Fishing Logbook Export", 40f, y, titlePaint)
        y += 26f

        for (c in catches.sortedByDescending { it.timestampMillis }) {
            if (y > 800f) {
                doc.finishPage(page)
                page = newPage()
                canvas = page.canvas
            }

            val trip = c.tripId?.let { tripById[it] }
            val line1 = "${fmt.format(Date(c.timestampMillis))} — ${c.species}" +
                (c.lengthCm?.let { "  ${it}cm" } ?: "") +
                (c.weightKg?.let { "  ${it}kg" } ?: "")
            canvas.drawText(line1, 40f, y, paint); y += 16f

            val gps = if (c.latitude != null && c.longitude != null)
                "GPS: %.5f, %.5f (±%sm)".format(c.latitude, c.longitude, c.accuracyM?.toInt() ?: 0)
            else "GPS: —"
            canvas.drawText(gps, 40f, y, paint); y += 16f

            val weatherLine =
                if (c.weatherTempC != null || c.weatherPressureHpa != null)
                    "Weather: " +
                        (c.weatherTempC?.let { "${"%.1f".format(it)}°C" } ?: "—") +
                        "  " +
                        (c.weatherPressureHpa?.let { "${"%.0f".format(it)} hPa" } ?: "—")
                else null

            if (weatherLine != null) {
                canvas.drawText(weatherLine, 40f, y, paint); y += 16f
            }

            if (!trip?.waterway.isNullOrBlank() || !trip?.name.isNullOrBlank()) {
                val tLine = "Trip: " + listOfNotNull(trip?.name, trip?.waterway).joinToString(" — ")
                canvas.drawText(tLine.take(90), 40f, y, paint); y += 16f
            }

            if (!c.lure.isNullOrBlank()) { canvas.drawText("Lure/Bait: ${c.lure}".take(100), 40f, y, paint); y += 16f }
            if (!c.notes.isNullOrBlank()) { canvas.drawText("Notes: ${c.notes}".take(100), 40f, y, paint); y += 16f }

            y += 8f
        }

        doc.finishPage(page)
        resolver.openOutputStream(uri)?.use { os -> doc.writeTo(os) }
        doc.close()
    }
}
