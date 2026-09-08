package com.pit.bahromtaxi

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.pit.bahromtaxi.maps.MapsConfig
import com.pit.bahromtaxi.network.AuthStore

class BahromTaxiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthStore.init(this)

        val mapsKey = MapsConfig.apiKey(this)
        if (mapsKey.isNotBlank() && !Places.isInitialized()) {
            runCatching { Places.initialize(applicationContext, mapsKey) }
        }
    }
}
