package com.proapps.voiceremind.transport

import com.proapps.voiceremind.BusDeparture

interface TransportProvider {
    // whether provider reasonably supports the given country code (ISO Alpha-2) or location
    fun supportsRegion(countryCode: String?): Boolean

    // find next departure for route near coordinates; return null if not found
    fun findNextDeparture(routeNumber: String, lat: Double, lon: Double, radiusMeters: Int = 400): BusDeparture?
}

