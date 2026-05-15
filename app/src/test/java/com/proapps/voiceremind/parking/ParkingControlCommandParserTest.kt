package com.proapps.voiceremind.parking

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParkingControlCommandParserTest {

    @Test
    fun extract_ruForOneHour_parsesDuration() {
        val now = LocalDateTime.of(2026, 5, 15, 13, 0)

        val command = ParkingControlCommandParser.extract(
            rawText = "Парковка на час",
            now = now
        )

        assertEquals(LocalDateTime.of(2026, 5, 15, 14, 0), command?.expiresAt)
        assertEquals(10, command?.remindBeforeMinutes)
    }

    @Test
    fun extract_ruUntilTime_parsesSameDay() {
        val now = LocalDateTime.of(2026, 5, 15, 12, 0)

        val command = ParkingControlCommandParser.extract(
            rawText = "Парковка до 14:15",
            now = now
        )

        assertEquals(LocalDateTime.of(2026, 5, 15, 14, 15), command?.expiresAt)
    }

    @Test
    fun extract_ruUntilTime_inPastMovesToNextDay() {
        val now = LocalDateTime.of(2026, 5, 15, 18, 0)

        val command = ParkingControlCommandParser.extract(
            rawText = "Парковка до 14:15",
            now = now
        )

        assertEquals(LocalDateTime.of(2026, 5, 16, 14, 15), command?.expiresAt)
    }

    @Test
    fun extract_enUntilTime_parses() {
        val now = LocalDateTime.of(2026, 5, 15, 9, 0)

        val command = ParkingControlCommandParser.extract(
            rawText = "Parking until 10:30",
            now = now
        )

        assertEquals(LocalDateTime.of(2026, 5, 15, 10, 30), command?.expiresAt)
    }

    @Test
    fun extract_withoutParkingKeyword_returnsNull() {
        val command = ParkingControlCommandParser.extract("До 14:15")

        assertNull(command)
    }
}

