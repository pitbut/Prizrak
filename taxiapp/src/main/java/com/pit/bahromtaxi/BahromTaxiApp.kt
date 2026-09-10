package com.pit.bahromtaxi

import android.app.Application
import android.preference.PreferenceManager
import com.pit.bahromtaxi.network.AuthStore
import com.pit.bahromtaxi.notify.TripReminderScheduler
import org.osmdroid.config.Configuration
import java.io.File

class BahromTaxiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthStore.init(this)
        TripReminderScheduler.init(this)

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath = File(cacheDir, "osmdroid")
        Configuration.getInstance().osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }
}
