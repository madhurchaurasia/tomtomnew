package com.example.poconnbandtomtom

import android.app.Application
import android.content.IntentFilter
import android.widget.Toast
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.MarkerOptions

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