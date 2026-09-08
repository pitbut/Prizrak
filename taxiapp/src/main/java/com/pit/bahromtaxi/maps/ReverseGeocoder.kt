package com.pit.bahromtaxi.maps

import android.content.Context
import android.location.Geocoder
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Адрес по точке, тапнутой на карте — используется как альтернатива Places Autocomplete. */
object ReverseGeocoder {
    suspend fun addressFor(context: Context, latLng: LatLng): String = withContext(Dispatchers.IO) {
        val fallback = "Точка на карте (%.5f, %.5f)".format(Locale.US, latLng.latitude, latLng.longitude)
        runCatching {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(latLng.latitude, latLng.longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
        }.getOrNull() ?: fallback
    }
}
