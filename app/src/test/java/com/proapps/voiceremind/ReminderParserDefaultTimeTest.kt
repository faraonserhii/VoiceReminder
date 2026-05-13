package com.proapps.voiceremind

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReminderParserDefaultTimeTest {

    @Test
    fun parse_absoluteDateWithoutTime_usesConfiguredDefaultTime() {
        val parser = ReminderParser(
            nowProvider = { LocalDateTime.of(2026, 5, 7, 10, 0) },
            defaultTimeProvider = { LocalTime.of(14, 30) }
        )

        val parsed = parser.parse("meeting april 15")

        assertNotNull(parsed)
        assertEquals(LocalTime.of(14, 30), parsed?.eventDateTime?.toLocalTime())
        assertEquals(true, parsed?.usedDefaultTime)
    }

    @Test
    fun parse_tomorrowWithoutTime_usesConfiguredDefaultTime() {
        val parser = ReminderParser(
            nowProvider = { LocalDateTime.of(2026, 5, 7, 10, 0) },
            defaultTimeProvider = { LocalTime.of(8, 45) }
        )

        val parsed = parser.parse("tomorrow call mom")

        assertNotNull(parsed)
        assertEquals(LocalTime.of(8, 45), parsed?.eventDateTime?.toLocalTime())
        assertEquals(true, parsed?.usedDefaultTime)
    }
}

