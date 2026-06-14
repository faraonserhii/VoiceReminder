package com.proapps.voiceremind

import java.time.Instant

/**
 * Structured departure information returned by transport providers.
 */
data class BusDeparture(
    val route: String,
    val stopName: String,
    val headsign: String?,
    val departureEpochMillis: Long,
    val minutesUntil: Int,
    val realtime: Boolean
    ,
    // simple source tag for UI/debug (e.g. "GTFS-disk", "GTFS-network", "stop_schedules", "synthetic")
    val source: String? = null
)

