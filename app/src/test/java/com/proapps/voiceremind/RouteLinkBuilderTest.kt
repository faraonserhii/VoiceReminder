package com.proapps.voiceremind

import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteLinkBuilderTest {

    @Test
    fun build_containsDestinationAndArrivalInAllLinks() {
        val links = RouteLinkBuilder.build(
            destination = "Kamppi Helsinki",
            arrivalDateTime = LocalDateTime.of(2026, 5, 7, 18, 0),
            zoneId = ZoneId.of("Europe/Helsinki")
        )

        val hslApp = links.hslAppUrl
        val hslWeb = links.hslWebUrl
        val maps = links.googleMapsUrl

        assertTrue(hslApp.startsWith("hsl://journey-planner?"))
        assertTrue(hslApp.contains("toPlace=Kamppi+Helsinki"))
        assertTrue(hslApp.contains("arriveBy=true"))
        assertTrue(hslApp.contains("date=2026-05-07"))
        assertTrue(hslApp.contains("time=18%3A00"))

        assertTrue(hslWeb.startsWith("https://reittiopas.hsl.fi/reitti?"))
        assertTrue(hslWeb.contains("toPlace=Kamppi+Helsinki"))

        assertTrue(maps.contains("destination=Kamppi+Helsinki"))
        assertTrue(maps.contains("travelmode=transit"))
        assertTrue(maps.contains("arrival_time="))
    }
}

