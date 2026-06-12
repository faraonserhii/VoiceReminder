package com.proapps.voiceremind.transport.gtfs

data class GTFSStop(
    val stopId: String,
    val name: String
)

data class GTFSTrip(
    val tripId: String,
    val routeId: String,
    val headsign: String?
)

data class GTFSRoute(
    val routeId: String,
    val shortName: String?
)

data class GTFSFeed(
    val url: String,
    val stops: List<GTFSStop>,
    val routes: Map<String, GTFSRoute>,
    val trips: Map<String, GTFSTrip>,
    // stop_id -> list of pair(departureSeconds, tripId)
    val stopTimes: Map<String, List<Pair<Int, String>>>
)

