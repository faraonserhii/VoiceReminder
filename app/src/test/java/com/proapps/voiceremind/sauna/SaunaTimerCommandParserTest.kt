package com.proapps.voiceremind.sauna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaunaTimerCommandParserTest {

    @Test
    fun extract_ruCommand_parsesMinutes() {
        val command = SaunaTimerCommandParser.extract("Сауна через 30 минут")

        assertEquals(30, command?.minutes)
    }

    @Test
    fun extract_enCommand_parsesMinutes() {
        val command = SaunaTimerCommandParser.extract("Sauna in 45 minutes")

        assertEquals(45, command?.minutes)
    }

    @Test
    fun extract_enAfterCommand_parsesMinutes() {
        val command = SaunaTimerCommandParser.extract("Sauna after 30 min")

        assertEquals(30, command?.minutes)
    }

    @Test
    fun extract_ruHalfHour_parsesThirtyMinutes() {
        val command = SaunaTimerCommandParser.extract("Сауна через полчаса")

        assertEquals(30, command?.minutes)
    }

    @Test
    fun extract_ruHourCommand_convertsToMinutes() {
        val command = SaunaTimerCommandParser.extract("Сауна через 1 час")

        assertEquals(60, command?.minutes)
    }

    @Test
    fun extract_enHoursCommand_convertsToMinutes() {
        val command = SaunaTimerCommandParser.extract("Sauna in 2 hours")

        assertEquals(120, command?.minutes)
    }

    @Test
    fun extract_withoutSaunaKeyword_returnsNull() {
        val command = SaunaTimerCommandParser.extract("Через 30 минут напомни")

        assertNull(command)
    }

    @Test
    fun extract_withTooLargeDelay_returnsNull() {
        val command = SaunaTimerCommandParser.extract("Сауна через 721 минуту")

        assertNull(command)
    }
}

