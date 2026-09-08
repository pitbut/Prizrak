package com.pit.bahromtaxi.maps

import android.content.Context
import android.content.pm.PackageManager

/** Тот же ключ, что и в манифесте (`com.google.android.geo.API_KEY`) — единый источник. */
object MapsConfig {
    private var cached: String? = null

    fun apiKey(context: Context): String {
        cached?.let { return it }
        val key = runCatching {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        }.getOrDefault("")
        cached = key
        return key
    }
}
