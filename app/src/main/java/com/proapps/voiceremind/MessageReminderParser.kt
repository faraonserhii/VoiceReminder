package com.proapps.voiceremind

import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

enum class MessageChannel {
    SMS,
    WHATSAPP
}

data class MessageReminderCommand(
    val recipient: String?,
    val messageText: String,
    val preferredChannel: MessageChannel,
    val explicitTime: LocalTime?,
    val dayShift: Long
) {
    fun resolveTriggerDateTime(now: LocalDateTime, defaultTime: LocalTime): LocalDateTime {
        val targetTime = explicitTime ?: defaultTime
        var target = LocalDateTime.of(now.toLocalDate().plusDays(dayShift), targetTime)
        if (dayShift == 0L && target.isBefore(now.plusMinutes(1))) {
            target = target.plusDays(1)
        }
        return target
    }
}

object MessageReminderParser {

    private val ruTimeRegex = Regex("""(?:^|\s)в\s+(\d{1,2})(?::(\d{2}))?(?=\s|,|$)""", RegexOption.IGNORE_CASE)
    private val enTimeRegex = Regex("""(?:^|\s)at\s+(\d{1,2})(?::(\d{2}))?(?:\s*(am|pm))?(?=\s|,|$)""", RegexOption.IGNORE_CASE)
    private val ruMessageRegex = Regex("""(?:^|\s|,)что\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val enMessageRegex = Regex("""(?:^|\s|,)that\s+(.+)$""", RegexOption.IGNORE_CASE)

    fun extract(rawText: String): MessageReminderCommand? {
        val text = rawText.trim()
        if (text.isBlank()) return null

        val normalized = text.lowercase(Locale.ROOT).replace(Regex("""\s+"""), " ")
        val isRu = normalized.contains("напомни отправить сообщение") || normalized.contains("напомни отправить msg")
        val isEn = normalized.contains("remind me to send message") || normalized.contains("remind me to send a message")
        if (!isRu && !isEn) return null

        val timeMatch = ruTimeRegex.find(text) ?: enTimeRegex.find(text)
        val explicitTime = timeMatch?.let { parseTime(it) }

        val recipient = extractRecipient(text, isRu, isEn, timeMatch?.range?.first)
        val messageText = extractMessageText(text, recipient).ifBlank {
            if (isRu) "Я задержусь" else "I will be late"
        }

        val preferredChannel = detectPreferredChannel(text)
        val dayShift = detectDayShift(text)

        return MessageReminderCommand(
            recipient = recipient,
            messageText = messageText,
            preferredChannel = preferredChannel,
            explicitTime = explicitTime,
            dayShift = dayShift
        )
    }

    private fun parseTime(match: MatchResult): LocalTime? {
        val hourRaw = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
        val minute = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        if (minute !in 0..59) return null

        val amPm = match.groupValues.getOrNull(3).orEmpty().lowercase(Locale.ROOT)
        val hour = when (amPm) {
            "am" -> if (hourRaw == 12) 0 else hourRaw
            "pm" -> if (hourRaw == 12) 12 else hourRaw + 12
            else -> hourRaw
        }
        if (hour !in 0..23) return null
        return LocalTime.of(hour, minute)
    }

    private fun extractRecipient(
        text: String,
        isRu: Boolean,
        isEn: Boolean,
        timeStart: Int?
    ): String? {
        val lower = text.lowercase(Locale.ROOT)
        val marker = when {
            isRu -> "сообщ"
            isEn -> "message"
            else -> return null
        }
        val markerIndex = lower.indexOf(marker)
        if (markerIndex < 0) return null

        val start = text.indexOf(' ', markerIndex).takeIf { it >= 0 }?.plus(1) ?: return null
        val end = timeStart ?: text.length
        if (start >= end) return null

        return text.substring(start, end)
            .replace("завтра", " ", ignoreCase = true)
            .replace("tomorrow", " ", ignoreCase = true)
            .replace(Regex("""\b(в\s+)?(whatsapp|ватсап|вацап|sms|смс)\b""", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', ':', ';', '-')
            .ifBlank { null }
    }

    private fun extractMessageText(text: String, recipient: String?): String {
        ruMessageRegex.find(text)?.let { return it.groupValues[1].trim().trimEnd('.', '!', '?') }
        enMessageRegex.find(text)?.let { return it.groupValues[1].trim().trimEnd('.', '!', '?') }

        // Fallback: if there is no explicit message body, use a recipient-focused phrase.
        return recipient?.let { "Сообщение для $it" } ?: "Сообщение"
    }

    private fun detectPreferredChannel(text: String): MessageChannel {
        val normalized = text.lowercase(Locale.ROOT)
        return when {
            normalized.contains("whatsapp") || normalized.contains("ватсап") || normalized.contains("вацап") -> MessageChannel.WHATSAPP
            else -> MessageChannel.SMS
        }
    }

    private fun detectDayShift(text: String): Long {
        val normalized = text.lowercase(Locale.ROOT)
        return if (normalized.contains("завтра") || normalized.contains("tomorrow")) 1L else 0L
    }
}

