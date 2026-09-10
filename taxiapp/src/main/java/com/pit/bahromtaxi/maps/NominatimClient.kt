package com.pit.bahromtaxi.maps

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private data class NominatimResult(
    val lat: String,
    val lon: String,
    @Json(name = "display_name") val displayName: String
)

private interface NominatimService {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int? = null,
        @Query("countrycodes") countryCodes: String? = null,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 6,
        @Query("accept-language") lang: String = "ru"
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): NominatimResult
}

/**
 * Бесплатный геокодинг OpenStreetMap. Политика использования публичного сервера
 * требует различимый User-Agent и не больше ~1 запроса/сек — для пилота достаточно,
 * при росте нагрузки поднять свой инстанс Nominatim (адрес меняется в одном месте).
 */
object NominatimClient {
    private const val USER_AGENT = "BahromTaxi/1.0 (Android; taxi app pilot)"
    private const val UZBEKISTAN = "uz"

    // Радиусы в градусах — грубая прикидка (не геодезически точная), достаточная
    // для биасинга результатов, а не для точных границ.
    private const val CITY_RADIUS_DEG = 0.15   // ~15-17 км
    private const val REGION_RADIUS_DEG = 1.2  // ~130 км

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
        }
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val service: NominatimService = Retrofit.Builder()
        .baseUrl("https://nominatim.openstreetmap.org/")
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NominatimService::class.java)

    /**
     * Ищет адрес с приоритетом на местоположение пользователя: сначала рядом (город),
     * потом шире (область), потом по всей стране, и только если совсем ничего не нашлось —
     * без ограничений вообще. Раньше поиск шёл без привязки к региону и "Шота Руставели"
     * в Ташкенте вполне мог найтись в Москве или Грузии просто по текстовой релевантности.
     */
    suspend fun search(query: String, near: Coordinate? = null): Result<List<PlacePoint>> {
        val tiers: List<suspend () -> List<NominatimResult>> = if (near != null) {
            listOf(
                { service.search(query, viewbox = boxAround(near, CITY_RADIUS_DEG), bounded = 1) },
                { service.search(query, viewbox = boxAround(near, REGION_RADIUS_DEG), bounded = 1) },
                { service.search(query, countryCodes = UZBEKISTAN) },
                { service.search(query) }
            )
        } else {
            listOf(
                { service.search(query, countryCodes = UZBEKISTAN) },
                { service.search(query) }
            )
        }

        var lastError: Throwable? = null
        for (tier in tiers) {
            val attempt = runCatching { tier() }
            attempt.onSuccess { list ->
                if (list.isNotEmpty()) {
                    return Result.success(list.map { PlacePoint(it.displayName, Coordinate(it.lat.toDouble(), it.lon.toDouble())) })
                }
            }
            attempt.onFailure { lastError = it }
        }
        return lastError?.let { Result.failure(it) } ?: Result.success(emptyList())
    }

    suspend fun reverse(coordinate: Coordinate): Result<String> = runCatching {
        service.reverse(coordinate.lat, coordinate.lng).displayName
    }.recover {
        "Точка на карте (%.5f, %.5f)".format(java.util.Locale.US, coordinate.lat, coordinate.lng)
    }

    private fun boxAround(center: Coordinate, deltaDeg: Double): String {
        val left = center.lng - deltaDeg
        val right = center.lng + deltaDeg
        val top = center.lat + deltaDeg
        val bottom = center.lat - deltaDeg
        return "$left,$top,$right,$bottom"
    }
}
