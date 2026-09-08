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

    suspend fun search(query: String): Result<List<PlacePoint>> = runCatching {
        service.search(query).map {
            PlacePoint(it.displayName, Coordinate(it.lat.toDouble(), it.lon.toDouble()))
        }
    }

    suspend fun reverse(coordinate: Coordinate): Result<String> = runCatching {
        service.reverse(coordinate.lat, coordinate.lng).displayName
    }.recover {
        "Точка на карте (%.5f, %.5f)".format(java.util.Locale.US, coordinate.lat, coordinate.lng)
    }
}
