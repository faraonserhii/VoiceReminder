package com.proapps.voiceremind.sahko

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SahkoVahtiCommandParserTest {

    @Test
    fun extract_ruPhrase_returnsCommand() {
        val command = SahkoVahtiCommandParser.extract(
            "Напомни включить стиралку, когда электричество будет дешевым"
        )

        assertNotNull(command)
        assertEquals("стиралку", command?.applianceLabel)
    }

    @Test
    fun extract_enPhrase_returnsCommand() {
        val command = SahkoVahtiCommandParser.extract(
            "Remind me to turn on dishwasher when electricity is cheap"
        )

        assertNotNull(command)
        assertEquals("dishwasher", command?.applianceLabel)
    }

    @Test
    fun extract_ruSpotTariffPhrase_returnsCommand() {
        val command = SahkoVahtiCommandParser.extract(
            "Запусти посудомойку, когда спот тариф будет дешёвым"
        )

        assertNotNull(command)
        assertTrue(command?.applianceLabel?.isNotBlank() == true)
    }

    @Test
    fun extract_fiPhrase_returnsCommand() {
        val command = SahkoVahtiCommandParser.extract(
            "Kaynnista pesukone kun pörssisähkö on halpa"
        )

        assertNotNull(command)
        assertEquals("pesukone", command?.applianceLabel)
    }

    @Test
    fun extract_enStartVerb_returnsCommand() {
        val command = SahkoVahtiCommandParser.extract(
            "Start dryer when electricity is low"
        )

        assertNotNull(command)
        assertEquals("dryer", command?.applianceLabel)
    }

    @Test
    fun extract_nonEnergyPhrase_returnsNull() {
        val command = SahkoVahtiCommandParser.extract("Напомни про встречу завтра")

        assertNull(command)
    }
}

