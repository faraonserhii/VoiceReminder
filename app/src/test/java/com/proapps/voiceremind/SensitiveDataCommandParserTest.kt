package com.proapps.voiceremind

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveDataCommandParserTest {

    @Test
    fun parse_saveTaxNumber_returnsSaveCommand() {
        val command = SensitiveDataCommandParser.parse("Запомни мой налоговый номер: 123-ABC")

        assertTrue(command is SensitiveDataCommand.Save)
        val save = command as SensitiveDataCommand.Save
        assertEquals(SensitiveDataType.TAX_NUMBER, save.type)
        assertEquals("123-ABC", save.value)
    }

    @Test
    fun parse_queryTaxNumber_returnsQueryCommand() {
        val command = SensitiveDataCommandParser.parse("Какой мой налоговый номер?")

        assertTrue(command is SensitiveDataCommand.Query)
        assertEquals(SensitiveDataType.TAX_NUMBER, (command as SensitiveDataCommand.Query).type)
    }

    @Test
    fun parse_saveKela_returnsInsuranceType() {
        val command = SensitiveDataCommandParser.parse("Save my Kela number: FI-777")

        assertTrue(command is SensitiveDataCommand.Save)
        assertEquals(SensitiveDataType.INSURANCE_NUMBER, (command as SensitiveDataCommand.Save).type)
    }

    @Test
    fun parse_unknownText_returnsNull() {
        val command = SensitiveDataCommandParser.parse("Напомни про встречу завтра")

        assertEquals(null, command)
    }
}

