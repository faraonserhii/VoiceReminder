package com.proapps.voiceremind.child

import android.content.Context
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class ChildLogEntry(
    val timestampEpochMillis: Long,
    val eventType: ChildEventType,
    val note: String
)

object ChildCareLogStore {

    private const val LOG_FILE = "child_care_log.csv"
    private val displayFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE)
    }

    fun append(context: Context, eventType: ChildEventType, happenedAtEpochMillis: Long, note: String) {
        val safeNote = note.replace(',', ' ')
        val line = "$happenedAtEpochMillis,${eventType.name},$safeNote\n"

        val file = getLogFile(context)
        if (!file.exists()) {
            file.writeText("timestamp_epoch,event_type,note\n")
        }
        file.appendText(line)
    }

    fun findLatestByType(context: Context, eventType: ChildEventType): ChildLogEntry? {
        val file = getLogFile(context)
        if (!file.exists()) return null

        return file.useLines { lines ->
            lines.drop(1)
                .filter { it.isNotBlank() }
                .mapNotNull { parseLine(it) }
                .filter { it.eventType == eventType }
                .maxByOrNull { it.timestampEpochMillis }
        }
    }

    fun formatTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .toLocalTime()
            .format(displayFormatter)
    }

    private fun parseLine(line: String): ChildLogEntry? {
        val parts = line.split(',', limit = 3)
        if (parts.size < 3) return null

        val epoch = parts[0].trim().toLongOrNull() ?: return null
        val type = runCatching { ChildEventType.valueOf(parts[1].trim()) }.getOrNull() ?: return null
        val note = parts[2].trim().ifBlank { "child event" }

        return ChildLogEntry(
            timestampEpochMillis = epoch,
            eventType = type,
            note = note
        )
    }
}

