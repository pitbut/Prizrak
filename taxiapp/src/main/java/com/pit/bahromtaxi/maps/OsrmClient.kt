package com.pit.bahromtaxi.maps

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

private data class OsrmResponse(val routes: List<OsrmRoute> = emptyList(), val code: String = "")
private data class OsrmRoute(val distance: Double, val duration: Double, val geometry: String)

private interface OsrmService {
    @GET("route/v1/driving/{coords}")
    suspend fun route(
        @Path(value = "coords", encoded = true) coords: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline"
    ): OsrmResponse
}

/**
 * Бесплатный роутинг OSRM (публичный демо-сервер — для пилота, не для тяжёлой
 * продакшен-нагрузки; при росте — свой инстанс OSRM, адрес меняется в одном месте).
 */
object OsrmClient {

    data class RouteResult(val distanceKm: Double, val durationMin: Double, val polyline: List<Coordinate>)

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val service: OsrmService = Retrofit.Builder()
        .baseUrl("https://router.project-osrm.org/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(OsrmService::class.java)

    suspend fun fetchRoute(origin: Coordinate, destination: Coordinate): Result<RouteResult> = runCatching {
        val coords = "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
        val response = service.route(coords)
        val route = response.routes.firstOrNull() ?: error("маршрут не найден (${response.code})")
        RouteResult(
            distanceKm = route.distance / 1000.0,
            durationMin = route.duration / 60.0,
            polyline = decodePolyline(route.geometry)
        )
    }

    // Тот же алгоритм encoded polyline, что и у Google Directions — OSRM кодирует так же.
    private fun decodePolyline(encoded: String): List<Coordinate> {
        val points = ArrayList<Coordinate>()
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

            points.add(Coordinate(lat / 1E5, lng / 1E5))
        }
        return points
    }
}
