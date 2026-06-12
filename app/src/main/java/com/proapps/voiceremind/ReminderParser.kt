package com.proapps.voiceremind

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

private const val DEFAULT_HOUR = 9
private const val DEFAULT_MINUTE = 0
private const val DEFAULT_DURATION_MINUTES = 60

/**
 * Parses reminder phrases in RU/EN, for example:
 * "встретиться 15 апреля в 19:30 в ресторане"
 * "meeting april 15 at 7:30 pm"
 * "завтра позвонить маме"
 */
class ReminderParser(
    private val nowProvider: () -> LocalDateTime = { LocalDateTime.now() },
    private val defaultTimeProvider: () -> LocalTime = { LocalTime.of(DEFAULT_HOUR, DEFAULT_MINUTE) },
    private val defaultEventTitleProvider: () -> String = { "Напоминание" }
) {

    private val ruMonths = mapOf(
        "января" to 1,
        "янв" to 1,
        "февраля" to 2,
        "фев" to 2,
        "марта" to 3,
        "мар" to 3,
        "апреля" to 4,
        "апр" to 4,
        "мая" to 5,
        "июня" to 6,
        "июн" to 6,
        "июля" to 7,
        "июл" to 7,
        "августа" to 8,
        "авг" to 8,
        "сентября" to 9,
        "сент" to 9,
        "сен" to 9,
        "октября" to 10,
        "окт" to 10,
        "ноября" to 11,
        "ноя" to 11,
        "декабря" to 12,
        "дек" to 12
    )

    private val enMonths = mapOf(
        "january" to 1,
        "jan" to 1,
        "february" to 2,
        "feb" to 2,
        "march" to 3,
        "mar" to 3,
        "april" to 4,
        "apr" to 4,
        "may" to 5,
        "june" to 6,
        "jun" to 6,
        "july" to 7,
        "jul" to 7,
        "august" to 8,
        "aug" to 8,
        "september" to 9,
        "sep" to 9,
        "sept" to 9,
        "october" to 10,
        "oct" to 10,
        "november" to 11,
        "nov" to 11,
        "december" to 12,
        "dec" to 12
    )

    // Spanish months
    private val esMonths = mapOf(
        "enero" to 1,
        "ene" to 1,
        "febrero" to 2,
        "feb" to 2,
        "marzo" to 3,
        "mar" to 3,
        "abril" to 4,
        "abr" to 4,
        "mayo" to 5,
        "junio" to 6,
        "jun" to 6,
        "julio" to 7,
        "jul" to 7,
        "agosto" to 8,
        "ago" to 8,
        "septiembre" to 9,
        "sep" to 9,
        "octubre" to 10,
        "oct" to 10,
        "noviembre" to 11,
        "nov" to 11,
        "diciembre" to 12,
        "dic" to 12
    )

    // German months (include common forms without diacritics too)
    private val deMonths = mapOf(
        "januar" to 1,
        "jan" to 1,
        "februar" to 2,
        "feb" to 2,
        "märz" to 3,
        "marz" to 3,
        "mär" to 3,
        "mar" to 3,
        "april" to 4,
        "apr" to 4,
        "mai" to 5,
        "juni" to 6,
        "jun" to 6,
        "juli" to 7,
        "jul" to 7,
        "august" to 8,
        "aug" to 8,
        "september" to 9,
        "sep" to 9,
        "oktober" to 10,
        "okt" to 10,
        "november" to 11,
        "nov" to 11,
        "dezember" to 12,
        "dez" to 12,
        "dec" to 12
    )

    private val ruDateRegex = Regex("""(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря|янв\.?|фев\.?|мар\.?|апр\.?|июн\.?|июл\.?|авг\.?|сент\.?|сен\.?|окт\.?|ноя\.?|дек\.?)(?:\s+(\d{4}))?""", RegexOption.IGNORE_CASE)
    private val enDateRegex = Regex(
        """(january|february|march|april|may|june|july|august|september|october|november|december|jan\.?|feb\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|sept\.?|oct\.?|nov\.?|dec\.?)\s+(\d{1,2})(?:,?\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )
    private val enDateDayFirstRegex = Regex(
        """(\d{1,2})\s+(january|february|march|april|may|june|july|august|september|october|november|december|jan\.?|feb\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|sept\.?|oct\.?|nov\.?|dec\.?)(?:\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )
    private val esDateRegex = Regex(
        """(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|ene\.?|feb\.?|mar\.?|abr\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)\s+(\d{1,2})(?:,?\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )
    private val esDateDayFirstRegex = Regex(
        """(\d{1,2})\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre|ene\.?|feb\.?|mar\.?|abr\.?|jun\.?|jul\.?|ago\.?|sep\.?|oct\.?|nov\.?|dic\.?)(?:\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )

    private val deDateRegex = Regex(
        """(januar|februar|märz|marz|april|mai|juni|juli|august|september|oktober|november|dezember|jan\.?|feb\.?|mär\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|okt\.?|nov\.?|dez\.?)\s+(\d{1,2})(?:,?\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )
    private val deDateDayFirstRegex = Regex(
        """(\d{1,2})\s+(januar|februar|märz|marz|april|mai|juni|juli|august|september|oktober|november|dezember|jan\.?|feb\.?|mär\.?|mar\.?|apr\.?|jun\.?|jul\.?|aug\.?|sep\.?|okt\.?|nov\.?|dez\.?)(?:\s+(\d{4}))?""",
        RegexOption.IGNORE_CASE
    )
    private val numericDayFirstRegex = Regex("""(?:^|\s)(\d{1,2})[./](\d{1,2})(?:[./](\d{4}))?(?=\s|$)""")
    private val numericDashWithYearRegex = Regex("""(?:^|\s)(\d{1,2})-(\d{1,2})-(\d{4})(?=\s|$)""")

    private val ruSpacedTimeRegex = Regex("""(?:^|\s)в\s+(\d{1,2})\s+(\d{2})(?=\s|$)""")
    private val enSpacedTimeRegex = Regex("""(?:^|\s)(?:at\s+)?(\d{1,2})\s+(\d{2})(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val ruTimeRegex = Regex("""(?:^|\s)в\s+(\d{1,2})(?::(\d{2}))?(?!\s+\d{2})(?=\s|$)""")
    private val enTimeRegex = Regex("""(?:^|\s)(?:at\s+)?(\d{1,2}):(\d{2})(?:\s*(am|pm))?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val enHourAmPmRegex = Regex("""(?:^|\s)(?:at\s+)?(\d{1,2})\s*(am|pm)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val compactHourMinuteAmPmRegex = Regex("""(?:^|\s)(\d{3,4})\s*(am|pm)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val compactHourMinuteRegex = Regex("""(?:^|\s)(\d{3,4})(?=\s|$)""")

    private val ruTomorrowRegex = Regex("""(?:^|\s)завтра(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val ruDayAfterTomorrowRegex = Regex("""(?:^|\s)послезавтра(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val enTomorrowRegex = Regex("""(?:^|\s)tomorrow(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val enDayAfterTomorrowRegex = Regex("""(?:^|\s)day\s+after\s+tomorrow(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val esTomorrowRegex = Regex("""(?:^|\s)mañana(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val esDayAfterTomorrowRegex = Regex("""(?:^|\s)pasado\s+mañana(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val deTomorrowRegex = Regex("""(?:^|\s)morgen(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val deDayAfterTomorrowRegex = Regex("""(?:^|\s)(?:übermorgen|uebermorgen)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val ruInWeeksRegex = Regex(
        """(?:^|\s)через\s+(\d{1,2}|один|одну|два|две|три|четыре)\s+недел(?:ю|и|ь)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val enInWeeksRegex = Regex("""(?:^|\s)in\s+(\d{1,2})\s+weeks?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val esInWeeksRegex = Regex("""(?:^|\s)en\s+(\d{1,2})\s+semanas?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val deInWeeksRegex = Regex("""(?:^|\s)in\s+(\d{1,2})\s+wochen?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val ruInHoursRegex = Regex("""(?:^|\s)через\s+(\d{1,3})\s+час(?:а|ов)?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val enInHoursRegex = Regex("""(?:^|\s)in\s+(\d{1,3})\s+hours?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val esInHoursRegex = Regex("""(?:^|\s)en\s+(\d{1,3})\s+horas?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val deInHoursRegex = Regex("""(?:^|\s)in\s+(\d{1,3})\s+stunden?(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val ruDurationRegex = Regex("""(?:^|\s)на\s+(\d{1,3})\s*(мин(?:ут[ауы]?)?|час(?:а|ов)?)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val enDurationRegex = Regex("""(?:^|\s)for\s+(\d{1,3})\s*(minutes?|mins?|hours?|hrs?|hr)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val esDurationRegex = Regex("""(?:^|\s)(?:por|durante)\s+(\d{1,3})\s*(min(?:utos?)?|horas?)(?=\s|$)""", RegexOption.IGNORE_CASE)
    private val deDurationRegex = Regex("""(?:^|\s)für\s+(\d{1,3})\s*(minute|minuten|stunde|stunden)(?=\s|$)""", RegexOption.IGNORE_CASE)

    private val cleanupRegex = Regex(
        """(?:^|\s)(мне\s+нужно|нужно|напомни|напомнить|встреча|встретиться|встречу|создай\s+напоминание|добавь\s+напоминание|meeting|meet|schedule|remind\s+me|set\s+reminder|tavataan|tavata|recuérdame|recordar|recordarme|reunión|reunirse|reunir|erinnere|erinnern|treffen|termin)(?=\s|$)""",
        RegexOption.IGNORE_CASE
    )
    private val ruLocationAtEndRegex = Regex("""(?:^|\s)в\s+([\p{L}][\p{L}\p{N}\s\-\"']{1,80})$""", RegexOption.IGNORE_CASE)
    private val enLocationAtEndRegex = Regex("""(?:^|\s)at\s+([\p{L}][\p{L}\p{N}\s\-\"']{1,80})$""", RegexOption.IGNORE_CASE)
    private val fiLocationAtEndRegex = Regex("""(?:^|\s)([\p{L}][\p{L}\p{N}\s\-\"']{1,80}(?:ssa|ssä|sta|stä|lla|llä|lta|ltä|lle|iin))$""", RegexOption.IGNORE_CASE)
    private val esLocationAtEndRegex = Regex("""(?:^|\s)en\s+([\p{L}][\p{L}\p{N}\s\-\"']{1,80})$""", RegexOption.IGNORE_CASE)
    private val deLocationAtEndRegex = Regex("""(?:^|\s)(?:in|bei)\s+([\p{L}][\p{L}\p{N}\s\-\"']{1,80})$""", RegexOption.IGNORE_CASE)

    fun parse(rawText: String): ParsedReminder? {
        val text = rawText.trim().lowercase(Locale.ROOT)
        if (text.isBlank()) return null

        val relative = parseRelativeDateTime(text)
        val parsedDuration = parseDuration(text)
        if (relative != null) {
            val baseTitle = buildTitle(rawText, listOf(relative.datePart, relative.timePart, parsedDuration?.rawPart))
            val (title, location) = extractLocationFromTitle(baseTitle)
            return ParsedReminder(
                title = title,
                eventDateTime = relative.dateTime,
                usedDefaultTime = relative.usedDefaultTime,
                location = location,
                durationMinutes = parsedDuration?.durationMinutes ?: DEFAULT_DURATION_MINUTES
            )
        }

        val absolute = parseAbsoluteDate(text) ?: return null
        val now = nowProvider().toLocalDate()
        val year = absolute.year ?: resolveYear(absolute.day, absolute.month, now)
        val defaultTime = defaultTimeProvider()

        val timeSearchText = text.replace(absolute.rawPart, " ")
        val parsedTime = parseTime(timeSearchText)
        val hour = parsedTime?.hour ?: defaultTime.hour
        val minute = parsedTime?.minute ?: defaultTime.minute

        if (hour !in 0..23 || minute !in 0..59) return null

        val dateTime = runCatching {
            LocalDateTime.of(LocalDate.of(year, absolute.month, absolute.day), LocalTime.of(hour, minute))
        }.getOrNull() ?: return null

        val baseTitle = buildTitle(rawText, listOf(absolute.rawPart, parsedTime?.rawPart, parsedDuration?.rawPart))
        val (title, location) = extractLocationFromTitle(baseTitle)

        return ParsedReminder(
            title = title,
            eventDateTime = dateTime,
            usedDefaultTime = parsedTime == null,
            location = location,
            durationMinutes = parsedDuration?.durationMinutes ?: DEFAULT_DURATION_MINUTES
        )
    }

    private fun parseDuration(text: String): ParsedDuration? {
        ruDurationRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return null
            if (value <= 0) return null
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val minutes = if (unit.startsWith("час")) value * 60 else value
            return ParsedDuration(rawPart = match.value.trim(), durationMinutes = minutes)
        }

        enDurationRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return null
            if (value <= 0) return null
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val minutes = if (unit.startsWith("hour") || unit.startsWith("hr")) value * 60 else value
            return ParsedDuration(rawPart = match.value.trim(), durationMinutes = minutes)
        }

        esDurationRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return null
            if (value <= 0) return null
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val minutes = if (unit.startsWith("hor")) value * 60 else value
            return ParsedDuration(rawPart = match.value.trim(), durationMinutes = minutes)
        }

        deDurationRegex.find(text)?.let { match ->
            val value = match.groupValues[1].toIntOrNull() ?: return null
            if (value <= 0) return null
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val minutes = if (unit.startsWith("stund") || unit.startsWith("stu")) value * 60 else value
            return ParsedDuration(rawPart = match.value.trim(), durationMinutes = minutes)
        }

        return null
    }

    private fun parseAbsoluteDate(text: String): AbsoluteDateMatch? {
        ruDateRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[2])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[1].toIntOrNull() ?: return null,
                month = ruMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        enDateRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[1])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[2].toIntOrNull() ?: return null,
                month = enMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        enDateDayFirstRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[2])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[1].toIntOrNull() ?: return null,
                month = enMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        esDateRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[1])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[2].toIntOrNull() ?: return null,
                month = esMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        esDateDayFirstRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[2])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[1].toIntOrNull() ?: return null,
                month = esMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        deDateRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[1])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[2].toIntOrNull() ?: return null,
                month = deMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        deDateDayFirstRegex.find(text)?.let { match ->
            val monthKey = normalizeMonthToken(match.groupValues[2])
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = match.groupValues[1].toIntOrNull() ?: return null,
                month = deMonths[monthKey] ?: return null,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        numericDayFirstRegex.find(text)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            if (day !in 1..31 || month !in 1..12) return null
            return AbsoluteDateMatch(
                rawPart = match.value.trim(),
                day = day,
                month = month,
                year = match.groupValues[3].toIntOrNull()
            )
        }

        numericDashWithYearRegex.find(text)?.let { match ->
            val first = match.groupValues[1].toIntOrNull() ?: return null
            val second = match.groupValues[2].toIntOrNull() ?: return null
            val year = match.groupValues[3].toIntOrNull() ?: return null

            val dayFirstCandidate = if (first in 1..31 && second in 1..12) {
                AbsoluteDateMatch(rawPart = match.value.trim(), day = first, month = second, year = year)
            } else {
                null
            }
            val monthFirstCandidate = if (first in 1..12 && second in 1..31) {
                AbsoluteDateMatch(rawPart = match.value.trim(), day = second, month = first, year = year)
            } else {
                null
            }

            // Prefer non-ambiguous interpretation, otherwise default to day-first.
            return when {
                dayFirstCandidate != null && monthFirstCandidate == null -> dayFirstCandidate
                monthFirstCandidate != null && dayFirstCandidate == null -> monthFirstCandidate
                dayFirstCandidate != null -> dayFirstCandidate
                else -> null
            }
        }

        return null
    }

    private fun normalizeMonthToken(token: String): String {
        return token.lowercase(Locale.ROOT).removeSuffix(".")
    }

    private fun parseRelativeDateTime(text: String): RelativeDateMatch? {
        val now = nowProvider()
        val defaultTime = defaultTimeProvider()

        ruInWeeksRegex.find(text)?.let { match ->
            val weeks = parseRuWeekToken(match.groupValues[1]) ?: return null
            if (weeks <= 0L) return null

            val parsedTime = parseTime(text)
            val hour = parsedTime?.hour ?: defaultTime.hour
            val minute = parsedTime?.minute ?: defaultTime.minute
            if (hour !in 0..23 || minute !in 0..59) return null

            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = parsedTime?.rawPart,
                dateTime = LocalDateTime.of(now.toLocalDate().plusWeeks(weeks), LocalTime.of(hour, minute)),
                usedDefaultTime = parsedTime == null
            )
        }

        enInWeeksRegex.find(text)?.let { match ->
            val weeks = match.groupValues[1].toLongOrNull() ?: return null
            if (weeks <= 0L) return null

            val parsedTime = parseTime(text)
            val hour = parsedTime?.hour ?: defaultTime.hour
            val minute = parsedTime?.minute ?: defaultTime.minute
            if (hour !in 0..23 || minute !in 0..59) return null

            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = parsedTime?.rawPart,
                dateTime = LocalDateTime.of(now.toLocalDate().plusWeeks(weeks), LocalTime.of(hour, minute)),
                usedDefaultTime = parsedTime == null
            )
        }

        esInWeeksRegex.find(text)?.let { match ->
            val weeks = match.groupValues[1].toLongOrNull() ?: return null
            if (weeks <= 0L) return null

            val parsedTime = parseTime(text)
            val hour = parsedTime?.hour ?: defaultTime.hour
            val minute = parsedTime?.minute ?: defaultTime.minute
            if (hour !in 0..23 || minute !in 0..59) return null

            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = parsedTime?.rawPart,
                dateTime = LocalDateTime.of(now.toLocalDate().plusWeeks(weeks), LocalTime.of(hour, minute)),
                usedDefaultTime = parsedTime == null
            )
        }

        deInWeeksRegex.find(text)?.let { match ->
            val weeks = match.groupValues[1].toLongOrNull() ?: return null
            if (weeks <= 0L) return null

            val parsedTime = parseTime(text)
            val hour = parsedTime?.hour ?: defaultTime.hour
            val minute = parsedTime?.minute ?: defaultTime.minute
            if (hour !in 0..23 || minute !in 0..59) return null

            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = parsedTime?.rawPart,
                dateTime = LocalDateTime.of(now.toLocalDate().plusWeeks(weeks), LocalTime.of(hour, minute)),
                usedDefaultTime = parsedTime == null
            )
        }

        ruInHoursRegex.find(text)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: return null
            if (hours <= 0L) return null
            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = null,
                dateTime = now.plusHours(hours),
                usedDefaultTime = false
            )
        }

        enInHoursRegex.find(text)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: return null
            if (hours <= 0L) return null
            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = null,
                dateTime = now.plusHours(hours),
                usedDefaultTime = false
            )
        }

        esInHoursRegex.find(text)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: return null
            if (hours <= 0L) return null
            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = null,
                dateTime = now.plusHours(hours),
                usedDefaultTime = false
            )
        }

        deInHoursRegex.find(text)?.let { match ->
            val hours = match.groupValues[1].toLongOrNull() ?: return null
            if (hours <= 0L) return null
            return RelativeDateMatch(
                datePart = match.value.trim(),
                timePart = null,
                dateTime = now.plusHours(hours),
                usedDefaultTime = false
            )
        }

        val dayShift = when {
            ruDayAfterTomorrowRegex.containsMatchIn(text) || enDayAfterTomorrowRegex.containsMatchIn(text) || esDayAfterTomorrowRegex.containsMatchIn(text) || deDayAfterTomorrowRegex.containsMatchIn(text) -> 2L
            ruTomorrowRegex.containsMatchIn(text) || enTomorrowRegex.containsMatchIn(text) || esTomorrowRegex.containsMatchIn(text) || deTomorrowRegex.containsMatchIn(text) -> 1L
            else -> return null
        }

        val datePart = enDayAfterTomorrowRegex.find(text)?.value
            ?: ruDayAfterTomorrowRegex.find(text)?.value
            ?: esDayAfterTomorrowRegex.find(text)?.value
            ?: deDayAfterTomorrowRegex.find(text)?.value
            ?: enTomorrowRegex.find(text)?.value
            ?: ruTomorrowRegex.find(text)?.value
            ?: esTomorrowRegex.find(text)?.value
            ?: deTomorrowRegex.find(text)?.value
            ?: return null

        val parsedTime = parseTime(text)
        val hour = parsedTime?.hour ?: defaultTime.hour
        val minute = parsedTime?.minute ?: defaultTime.minute
        if (hour !in 0..23 || minute !in 0..59) return null

        return RelativeDateMatch(
            datePart = datePart.trim(),
            timePart = parsedTime?.rawPart,
            dateTime = LocalDateTime.of(now.toLocalDate().plusDays(dayShift), LocalTime.of(hour, minute)),
            usedDefaultTime = parsedTime == null
        )
    }

    private fun parseRuWeekToken(token: String): Long? {
        return when (token.lowercase(Locale.ROOT)) {
            "один", "одну" -> 1L
            "два", "две" -> 2L
            "три" -> 3L
            "четыре" -> 4L
            else -> token.toLongOrNull()
        }
    }

    private fun parseTime(text: String): ParsedTime? {
        ruSpacedTimeRegex.find(text)?.let { match ->
            return ParsedTime(
                hour = match.groupValues[1].toIntOrNull() ?: return null,
                minute = match.groupValues[2].toIntOrNull() ?: return null,
                rawPart = match.value.trim()
            )
        }

        enSpacedTimeRegex.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return ParsedTime(hour = hour, minute = minute, rawPart = match.value.trim())
        }

        ruTimeRegex.find(text)?.let { match ->
            return ParsedTime(
                hour = match.groupValues[1].toIntOrNull() ?: return null,
                minute = match.groupValues[2].toIntOrNull() ?: DEFAULT_MINUTE,
                rawPart = match.value.trim()
            )
        }

        enTimeRegex.find(text)?.let { match ->
            val rawHour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            val amPm = match.groupValues[3]
            val hour = to24Hour(rawHour, amPm) ?: return null
            return ParsedTime(hour = hour, minute = minute, rawPart = match.value.trim())
        }

        enHourAmPmRegex.find(text)?.let { match ->
            val rawHour = match.groupValues[1].toIntOrNull() ?: return null
            val hour = to24Hour(rawHour, match.groupValues[2]) ?: return null
            return ParsedTime(hour = hour, minute = 0, rawPart = match.value.trim())
        }

        compactHourMinuteAmPmRegex.find(text)?.let { match ->
            val compact = match.groupValues[1]
            val parsed = parseCompactTime(compact) ?: return null
            val hour = to24Hour(parsed.first, match.groupValues[2]) ?: return null
            return ParsedTime(hour = hour, minute = parsed.second, rawPart = match.value.trim())
        }

        compactHourMinuteRegex.find(text)?.let { match ->
            val parsed = parseCompactTime(match.groupValues[1]) ?: return null
            return ParsedTime(hour = parsed.first, minute = parsed.second, rawPart = match.value.trim())
        }

        return null
    }

    private fun parseCompactTime(compact: String): Pair<Int, Int>? {
        val digits = compact.trim()
        if (digits.length !in 3..4 || digits.any { !it.isDigit() }) return null

        val hour = if (digits.length == 3) {
            digits.substring(0, 1).toIntOrNull()
        } else {
            digits.substring(0, 2).toIntOrNull()
        } ?: return null

        val minute = digits.takeLast(2).toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }

    private fun to24Hour(rawHour: Int, amPm: String?): Int? {
        if (amPm.isNullOrBlank()) return rawHour.takeIf { it in 0..23 }
        if (rawHour !in 1..12) return null

        return when (amPm.lowercase(Locale.ROOT)) {
            "am" -> if (rawHour == 12) 0 else rawHour
            "pm" -> if (rawHour == 12) 12 else rawHour + 12
            else -> null
        }
    }

    private fun resolveYear(day: Int, month: Int, now: LocalDate): Int {
        var year = now.year
        val candidate = runCatching { LocalDate.of(year, month, day) }.getOrNull() ?: return year
        if (candidate.isBefore(now)) {
            year += 1
        }
        return year
    }

    private fun buildTitle(rawText: String, removableParts: List<String?>): String {
        var title = rawText

        removableParts.filterNot { it.isNullOrBlank() }.forEach { part ->
            title = title.replace(part.orEmpty(), " ", ignoreCase = true)
        }

        title = title
            .replace(cleanupRegex, " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-', ':')

        if (title.isBlank()) {
            title = defaultEventTitleProvider().ifBlank { "Напоминание" }
        }

        return title.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    private fun extractLocationFromTitle(baseTitle: String): Pair<String, String?> {
        var match: MatchResult? = null
        var isFinnish = false

        ruLocationAtEndRegex.find(baseTitle)?.let { match = it }
        if (match == null) enLocationAtEndRegex.find(baseTitle)?.let { match = it }
        if (match == null) esLocationAtEndRegex.find(baseTitle)?.let { match = it }
        if (match == null) deLocationAtEndRegex.find(baseTitle)?.let { match = it }
        if (match == null) fiLocationAtEndRegex.find(baseTitle)?.let { match = it; isFinnish = true }

        if (match == null) {
            return baseTitle to null
        }

        val rawLocation = match.groupValues[1].trim().trim(',', '.', ';', ':')
        val location = if (isFinnish) {
            normalizeFinnishLocationCase(rawLocation)
        } else {
            // Keep original casing/format for other languages (tests expect original form)
            rawLocation
        }

        if (!isLikelyLocation(location)) {
            return baseTitle to null
        }

        val titleWithoutLocation = baseTitle
            .removeRange(match.range)
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', ';', ':', '-')
            .ifBlank { defaultEventTitleProvider().ifBlank { "Напоминание" } }

        return titleWithoutLocation to location
    }

    private fun normalizeFinnishLocationCase(rawLocation: String): String {
        val trimmed = rawLocation.trim()
        if (trimmed.isBlank()) return trimmed

        val finnishCityForms = mapOf(
            "helsingissa" to "Helsinki",
            "helsingissä" to "Helsinki",
            "helsingista" to "Helsinki",
            "helsingistä" to "Helsinki",
            "helsingille" to "Helsinki",
            "helsinkiin" to "Helsinki",
            "helsingilla" to "Helsinki",
            "helsingillä" to "Helsinki",
            "helsingilta" to "Helsinki",
            "helsingiltä" to "Helsinki",

            "espoossa" to "Espoo",
            "espoosta" to "Espoo",
            "espoolle" to "Espoo",
            "espooseen" to "Espoo",

            "vantaalla" to "Vantaa",
            "vantaalta" to "Vantaa",
            "vantaalle" to "Vantaa",
            "vantaan" to "Vantaa",

            "tampereella" to "Tampere",
            "tampereelta" to "Tampere",
            "tampereelle" to "Tampere",
            "tampereessa" to "Tampere",

            "turussa" to "Turku",
            "turusta" to "Turku",
            "turulle" to "Turku",
            "turkuun" to "Turku"
        )

        val words = trimmed.split(Regex("""\s+"""))
        val normalized = words.joinToString(" ") { word ->
            val lower = word.lowercase(Locale.ROOT)
            finnishCityForms[lower] ?: word
        }

        return normalized
    }

    private fun isLikelyLocation(value: String): Boolean {
        if (value.length < 2) return false
        val normalized = value.lowercase(Locale.ROOT)

        if (Regex("""\d{1,2}[:\s]\d{2}""").containsMatchIn(normalized)) return false
        if (Regex("""\b(for|на|minutes?|mins?|hours?|hrs?|hr|мин(?:ут[ауы]?)?|час(?:а|ов)?)\b""").containsMatchIn(normalized)) {
            return false
        }

        return true
    }
}

private data class AbsoluteDateMatch(
    val rawPart: String,
    val day: Int,
    val month: Int,
    val year: Int?
)

private data class ParsedTime(
    val hour: Int,
    val minute: Int,
    val rawPart: String
)

private data class RelativeDateMatch(
    val datePart: String,
    val timePart: String?,
    val dateTime: LocalDateTime,
    val usedDefaultTime: Boolean
)

private data class ParsedDuration(
    val rawPart: String,
    val durationMinutes: Int
)

data class ParsedReminder(
    val title: String,
    val eventDateTime: LocalDateTime,
    val usedDefaultTime: Boolean,
    val location: String?,
    val durationMinutes: Int
)
