package com.pit.bahromtaxi.maps

import android.net.Uri

/**
 * Разбор ссылок с координатами, которые Android передаёт приложению через ACTION_VIEW —
 * когда пользователь делится локацией из другого приложения (например, из Telegram — "Поделиться"
 * на сообщении с геопозицией) и выбирает "Бахром Такси" в системном списке "Открыть с помощью".
 * Поддерживает стандартный geo: URI и ссылки на Google Maps.
 */
object GeoUri {

    fun parse(uri: Uri): Coordinate? = when (uri.scheme?.lowercase()) {
        "geo" -> parseGeo(uri)
        "http", "https" -> parseGoogleMaps(uri)
        else -> null
    }

    private fun parseGeo(uri: Uri): Coordinate? {
        val ssp = uri.schemeSpecificPart ?: return null
        val coordsPart = ssp.substringBefore('?')
        val direct = parsePair(coordsPart)
        if (direct != null) return direct

        // geo:0,0?q=lat,lng(label) — координаты только в q, если сами коорды нулевые/пустые.
        val q = uri.getQueryParameter("q") ?: return null
        return parsePair(q.substringBefore('('))
    }

    private fun parseGoogleMaps(uri: Uri): Coordinate? {
        val host = uri.host?.lowercase() ?: return null
        if ("google" !in host && "goo.gl" !in host) return null

        uri.getQueryParameter("q")?.let { q -> parsePair(q)?.let { return it } }
        uri.getQueryParameter("ll")?.let { ll -> parsePair(ll)?.let { return it } }

        // .../maps/@41.311,69.240,15z
        val at = uri.pathSegments.firstOrNull { it.startsWith("@") }
        if (at != null) {
            val parts = at.removePrefix("@").split(',')
            if (parts.size >= 2) {
                val lat = parts[0].toDoubleOrNull()
                val lng = parts[1].toDoubleOrNull()
                if (lat != null && lng != null) return Coordinate(lat, lng)
            }
        }
        return null
    }

    private fun parsePair(raw: String): Coordinate? {
        val parts = raw.trim().split(',')
        if (parts.size < 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        if (lat == 0.0 && lng == 0.0) return null
        return Coordinate(lat, lng)
    }
}
