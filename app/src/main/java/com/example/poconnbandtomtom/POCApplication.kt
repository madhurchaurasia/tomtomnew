package com.example.poconnbandtomtom

import android.app.Application

/**
 * Application class to initialize TomTom SDK
 */
class POCApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // TomTom Maps SDK 1.9.0 initialization is handled automatically
        // API key is configured via metadata in AndroidManifest.xml
    }
}