package com.pit.bahromtaxi.maps

import com.google.android.gms.maps.model.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private data class DirectionsResponse(
    val routes: List<DirectionsRoute> = emptyList(),
    val status: String = ""
)

private data class DirectionsRoute(
    val legs: List<DirectionsLeg> = emptyList(),
    @Json(name = "overview_polyline") val overviewPolyline: OverviewPolyline? = null
)

private data class DirectionsLeg(val distance: ValueText, val duration: ValueText)
private data class ValueText(val value: Long, val text: String)
private data class OverviewPolyline(val points: String)

private interface DirectionsService {
    @GET("maps/api/directions/json")
    suspend fun route(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("key") key: String
    ): DirectionsResponse
}

/** Отдельный клиент на maps.googleapis.com — намеренно без интерцептора авторизации ApiClient. */
object DirectionsClient {

    data class RouteResult(val distanceKm: Double, val durationMin: Double, val polyline: List<LatLng>)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val service: DirectionsService = Retrofit.Builder()
        .baseUrl("https://maps.googleapis.com/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(DirectionsService::class.java)

    suspend fun fetchRoute(origin: LatLng, destination: LatLng, apiKey: String): Result<RouteResult> = runCatching {
        require(apiKey.isNotBlank()) { "не настроен ключ Google Maps API" }
        val response = service.route(
            origin = "${origin.latitude},${origin.longitude}",
            destination = "${destination.latitude},${destination.longitude}",
            key = apiKey
        )
        val route = response.routes.firstOrNull() ?: error("маршрут не найден (${response.status})")
        val leg = route.legs.firstOrNull() ?: error("в ответе нет участков маршрута")
        RouteResult(
            distanceKm = leg.distance.value / 1000.0,
            durationMin = leg.duration.value / 60.0,
            polyline = route.overviewPolyline?.points?.let(::decodePolyline).orEmpty()
        )
    }

    // Стандартный алгоритм декодирования Google encoded polyline.
    private fun decodePolyline(encoded: String): List<LatLng> {
        val points = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1

            points.add(LatLng(lat / 1E5, lng / 1E5))
        }
        return points
    }
}
