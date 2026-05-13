package com.proapps.voiceremind

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ExampleUnitTest {

    private val fixedNow = LocalDateTime.of(2026, 5, 4, 10, 0)
    private val parser = ReminderParser(nowProvider = { fixedNow })

    @Test
    fun parse_ruDateAndTime() {
        val parsed = parser.parse("встретиться 15 апреля в 19:30")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_enDateAndTime() {
        val parsed = parser.parse("meeting april 15 at 7:30 pm")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(2027, parsed?.eventDateTime?.year)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_ruShortMonth() {
        val parsed = parser.parse("встреча 15 апр в 18:20")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(18, parsed?.eventDateTime?.hour)
        assertEquals(20, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_enShortMonth() {
        val parsed = parser.parse("meet sep 8 at 9:05 am")

        assertNotNull(parsed)
        assertEquals(8, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(9, parsed?.eventDateTime?.monthValue)
        assertEquals(9, parsed?.eventDateTime?.hour)
        assertEquals(5, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_numericDayFirstSlash() {
        val parsed = parser.parse("dentist 15/04 19:30")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_numericMonthFirstDashWithYear() {
        val parsed = parser.parse("meeting 04-15-2026 7:30 pm")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(2026, parsed?.eventDateTime?.year)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_numericDayFirstDashWithYear() {
        val parsed = parser.parse("meeting 15-04-2026 1930")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(2026, parsed?.eventDateTime?.year)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_compactAmPmTime() {
        val parsed = parser.parse("meeting apr 15 730pm")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(4, parsed?.eventDateTime?.monthValue)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_hourWithAmPmNoColon() {
        val parsed = parser.parse("tomorrow 7 pm call Alex")

        assertNotNull(parsed)
        assertEquals(5, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_hourWithAmPmNoSpace() {
        val parsed = parser.parse("meeting april 15 at 7pm")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_spacedHourMinute() {
        val parsed = parser.parse("встретиться 15 апреля в 19 30")

        assertNotNull(parsed)
        assertEquals(15, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(19, parsed?.eventDateTime?.hour)
        assertEquals(30, parsed?.eventDateTime?.minute)
    }

    @Test
    fun parse_ruLocation() {
        val parsed = parser.parse("встретиться 15 апреля в 19:30 в ресторане у дома")

        assertNotNull(parsed)
        assertEquals("ресторане у дома", parsed?.location)
    }

    @Test
    fun parse_enLocation() {
        val parsed = parser.parse("meeting april 15 at 7:30 pm at central cafe")

        assertNotNull(parsed)
        assertEquals("central cafe", parsed?.location)
    }

    @Test
    fun parse_ruDurationMinutes() {
        val parsed = parser.parse("встреча 15 апреля в 19:30 на 45 минут")

        assertNotNull(parsed)
        assertEquals(45, parsed?.durationMinutes)
    }

    @Test
    fun parse_enDurationHours() {
        val parsed = parser.parse("meeting april 15 at 7:30 pm for 2 hours")

        assertNotNull(parsed)
        assertEquals(120, parsed?.durationMinutes)
    }

    @Test
    fun parse_locationAndDurationTogether() {
        val parsed = parser.parse("meeting april 15 at 7pm at central cafe for 45 minutes")

        assertNotNull(parsed)
        assertEquals("central cafe", parsed?.location)
        assertEquals(45, parsed?.durationMinutes)
        assertEquals(19, parsed?.eventDateTime?.hour)
    }

    @Test
    fun parse_tomorrow_ru_defaultTime() {
        val parsed = parser.parse("завтра позвонить маме")

        assertNotNull(parsed)
        assertEquals(5, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(9, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
        assertEquals(true, parsed?.usedDefaultTime)
    }

    @Test
    fun parse_dayAfterTomorrow_en_withTime() {
        val parsed = parser.parse("day after tomorrow at 8:15 call doctor")

        assertNotNull(parsed)
        assertEquals(6, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(8, parsed?.eventDateTime?.hour)
        assertEquals(15, parsed?.eventDateTime?.minute)
        assertEquals(false, parsed?.usedDefaultTime)
    }

    @Test
    fun parse_inHours_ru() {
        val parsed = parser.parse("через 2 часа проверить почту")

        assertNotNull(parsed)
        assertEquals(4, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(12, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
        assertEquals(false, parsed?.usedDefaultTime)
    }

    @Test
    fun parse_inTwoWeeks_ruWords_usesDefaultTime() {
        val parsed = parser.parse("напомни вернуть книги в библиотеку через две недели")

        assertNotNull(parsed)
        assertEquals(18, parsed?.eventDateTime?.dayOfMonth)
        assertEquals(5, parsed?.eventDateTime?.monthValue)
        assertEquals(9, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
        assertEquals(true, parsed?.usedDefaultTime)
    }

    @Test
    fun parse_dateWithoutTime_usesDefaultTime() {
        val parsed = parser.parse("мне нужно встретиться 15 апреля в ресторане")

        assertNotNull(parsed)
        assertEquals(true, parsed?.usedDefaultTime)
        assertEquals(9, parsed?.eventDateTime?.hour)
        assertEquals(0, parsed?.eventDateTime?.minute)
        assertEquals("ресторане", parsed?.location)
        assertEquals(60, parsed?.durationMinutes)
    }

    @Test
    fun parse_withoutDate_returnsNull() {
        val parsed = parser.parse("встретиться в ресторане")
        assertNull(parsed)
    }

    @Test
    fun parse_withoutLocation_returnsNullLocation() {
        val parsed = parser.parse("meeting april 15 at 7:30 pm call mom")

        assertNotNull(parsed)
        assertNull(parsed?.location)
    }
}