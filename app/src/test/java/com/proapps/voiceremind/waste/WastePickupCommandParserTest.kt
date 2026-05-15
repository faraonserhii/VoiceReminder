package com.proapps.voiceremind.waste

import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WastePickupCommandParserTest {

    @Test
    fun extract_biWeeklyRuPhrase_parsesCommand() {
        val command = WastePickupCommandParser.extract("Бумага по вторникам раз в две недели")

        assertNotNull(command)
        assertEquals("Бумага", command?.materialLabel)
        assertEquals(DayOfWeek.TUESDAY, command?.dayOfWeek)
        assertEquals(2, command?.intervalWeeks)
    }

    @Test
    fun extract_weeklyRuPhrase_defaultsToWeekly() {
        val command = WastePickupCommandParser.extract("Пластик по пятницам")

        assertNotNull(command)
        assertEquals("Пластик", command?.materialLabel)
        assertEquals(DayOfWeek.FRIDAY, command?.dayOfWeek)
        assertEquals(1, command?.intervalWeeks)
    }

    @Test
    fun extract_withoutWeekday_returnsNull() {
        val command = WastePickupCommandParser.extract("Бумага раз в две недели")

        assertNull(command)
    }
}

