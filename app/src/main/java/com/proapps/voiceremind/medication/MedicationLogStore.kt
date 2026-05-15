package com.proapps.voiceremind.medication

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object MedicationLogStore {

    private const val LOG_FILE = "medication_intake_log.csv"
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE)
    }

    fun hasEntries(context: Context): Boolean {
        val file = getLogFile(context)
        if (!file.exists()) return false
        // Header-only file means user has not logged any intake yet.
        return file.useLines { lines -> lines.drop(1).any { it.isNotBlank() } }
    }

    fun readLatestEntries(context: Context, limit: Int = 10): List<String> {
        if (limit <= 0) return emptyList()
        val file = getLogFile(context)
        if (!file.exists()) return emptyList()

        return file.useLines { lines ->
            lines
                .drop(1)
                .filter { it.isNotBlank() }
                .toList()
                .takeLast(limit)
                .asReversed()
                .mapNotNull { parseCsvLineForDisplay(it) }
        }
    }

    fun append(context: Context, logId: String, title: String) {
        val timestamp = Instant.now().atZone(ZoneId.systemDefault()).format(timestampFormatter)
        val safeTitle = title.replace(',', ' ')
        val safeLogId = logId.replace(',', '_')
        val line = "$timestamp,$safeLogId,$safeTitle\n"

        val file = getLogFile(context)
        if (!file.exists()) {
            file.writeText("timestamp,log_id,title\n")
        }
        file.appendText(line)
    }

    private fun parseCsvLineForDisplay(line: String): String? {
        val parts = line.split(',', limit = 3)
        if (parts.size < 3) return null
        val timestamp = parts[0].trim()
        val title = parts[2].trim()
        if (timestamp.isBlank() || title.isBlank()) return null
        return "$timestamp - $title"
    }
}

