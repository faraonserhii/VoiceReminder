package com.proapps.voiceremind.child

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildCareCommandParserTest {

    @Test
    fun extract_logMealWithTime_parsesLogEvent() {
        val now = LocalDateTime.of(2026, 5, 15, 13, 0)

        val command = ChildCareCommandParser.extract(
            rawText = "Запиши: ребенок поел в 12:00",
            now = now
        )

        assertNotNull(command)
        val log = command as ChildCareCommand.LogEvent
        assertEquals(ChildEventType.MEAL, log.eventType)
        assertEquals(LocalDateTime.of(2026, 5, 15, 12, 0), log.happenedAt)
    }

    @Test
    fun extract_logVitaminsWithoutTime_usesNow() {
        val now = LocalDateTime.of(2026, 5, 15, 19, 10)

        val command = ChildCareCommandParser.extract(
            rawText = "Витамины дала",
            now = now
        )

        assertNotNull(command)
        val log = command as ChildCareCommand.LogEvent
        assertEquals(ChildEventType.VITAMINS, log.eventType)
        assertEquals(now, log.happenedAt)
    }

    @Test
    fun extract_queryMeal_returnsQueryCommand() {
        val command = ChildCareCommandParser.extract("Когда ребенок ел?")

        assertTrue(command is ChildCareCommand.QueryLastEvent)
        assertEquals(ChildEventType.MEAL, (command as ChildCareCommand.QueryLastEvent).eventType)
    }
}

