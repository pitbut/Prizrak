package com.pit.prizrak

import android.app.Application
import org.opencv.android.OpenCVLoader

class PrizrakApp : Application() {
    override fun onCreate() {
        super.onCreate()
        OpenCVLoader.initLocal()
    }
}
