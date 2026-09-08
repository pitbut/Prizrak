package com.pit.bahromtaxi

import android.app.Application
import com.pit.bahromtaxi.network.AuthStore

class BahromTaxiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthStore.init(this)
    }
}
