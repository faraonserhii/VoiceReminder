package com.proapps.voiceremind.medication

import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MedicationReminderParserTest {

    @Test
    fun extract_dailyMorningWithTime_buildsDailyPlan() {
        val plan = MedicationReminderParser.extract(
            rawText = "Напомни пить витамины каждое утро в 8:00",
            now = LocalDateTime.of(2026, 5, 13, 7, 0),
            defaultTime = LocalTime.of(9, 0)
        )

        assertNotNull(plan)
        assertEquals("Пить витамины", plan?.title)
        assertEquals(LocalDateTime.of(2026, 5, 13, 8, 0), plan?.firstIntakeDateTime)
        assertEquals(null, plan?.daysCount)
        assertEquals(false, plan?.usedDefaultTime)
    }

    @Test
    fun extract_antibioticCourse_setsCountRule() {
        val plan = MedicationReminderParser.extract(
            rawText = "Курс антибиотиков на 7 дней",
            now = LocalDateTime.of(2026, 5, 13, 10, 0),
            defaultTime = LocalTime.of(8, 0)
        )

        assertNotNull(plan)
        assertEquals("Курс антибиотиков", plan?.title)
        assertEquals(7, plan?.daysCount)
        assertEquals(LocalDateTime.of(2026, 5, 14, 8, 0), plan?.firstIntakeDateTime)
        assertEquals(true, plan?.usedDefaultTime)
        assertEquals("FREQ=DAILY;COUNT=7", MedicationReminderParser.toDailyRRule(plan?.daysCount))
    }

    @Test
    fun extract_nonMedication_returnsNull() {
        val plan = MedicationReminderParser.extract(
            rawText = "Встреча в офисе завтра",
            now = LocalDateTime.of(2026, 5, 13, 10, 0),
            defaultTime = LocalTime.of(8, 0)
        )

        assertEquals(null, plan)
    }
}

