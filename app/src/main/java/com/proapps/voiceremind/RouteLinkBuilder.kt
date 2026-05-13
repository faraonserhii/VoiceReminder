package com.proapps.voiceremind

import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class RouteLinks(
    val hslAppUrl: String,
    val hslWebUrl: String,
    val googleMapsUrl: String
)

object RouteLinkBuilder {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun build(destination: String, arrivalDateTime: LocalDateTime, zoneId: ZoneId): RouteLinks {
        val normalizedDestination = destination.trim()
        val date = arrivalDateTime.format(dateFormatter)
        val time = arrivalDateTime.format(timeFormatter)
        val arrivalEpochSeconds = arrivalDateTime.atZone(zoneId).toEpochSecond()

        val destinationEncoded = encode(normalizedDestination)
        val dateEncoded = encode(date)
        val timeEncoded = encode(time)

        val hslQuery = "toPlace=$destinationEncoded&arriveBy=true&date=$dateEncoded&time=$timeEncoded"
        val hslAppUrl = "hsl://journey-planner?$hslQuery"
        val hslWebUrl = "https://reittiopas.hsl.fi/reitti?$hslQuery"
        val googleMapsUrl =
            "https://www.google.com/maps/dir/?api=1&destination=$destinationEncoded&travelmode=transit&arrival_time=$arrivalEpochSeconds"

        return RouteLinks(
            hslAppUrl = hslAppUrl,
            hslWebUrl = hslWebUrl,
            googleMapsUrl = googleMapsUrl
        )
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

