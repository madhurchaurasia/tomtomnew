package com.example.poconnbandtomtom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.poconnbandtomtom.models.NextBillionResponse
import com.example.poconnbandtomtom.models.NextBillionRoute
import com.example.poconnbandtomtom.navigation.NavigationManager
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.marker.Marker
import com.tomtom.sdk.map.display.polyline.PolylineOptions
import com.tomtom.sdk.map.display.polyline.Polyline
import com.tomtom.sdk.map.display.image.ImageFactory
import java.io.InputStreamReader

class MainActivitySimple : AppCompatActivity() {

    private var tomTomMap: TomTomMap? = null
    private lateinit var mapContainer: FrameLayout
    private var mapFragment: MapFragment? = null
    private var currentRoutePolyline: Polyline? = null
    private val routeMarkers = mutableListOf<Marker>()

    private lateinit var btnLoadRoutes: Button
    private lateinit var btnClearRoutes: Button
    private lateinit var spinnerRoutes: Spinner
    private lateinit var btnStartNavigation: Button
    private lateinit var navigationPanel: LinearLayout
    private lateinit var tvNavigationInstruction: TextView
    private lateinit var tvProgress: TextView
    private lateinit var tvRemainingDistance: TextView
    private lateinit var btnStopNavigation: Button
    private lateinit var tvRouteInfo: TextView

    private var routes: List<NextBillionRoute> = emptyList()
    private var selectedRouteIndex = -1

    // Navigation Manager for turn-by-turn navigation
    private lateinit var navigationManager: NavigationManager
    private var isNavigationActive = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivitySimple"
        private const val TOMTOM_API_KEY = BuildConfig.TOMTOM_API_KEY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_simple)

        try {
            initializeViews()
            setupUI()

            // Initialize map after a short delay
            window.decorView.postDelayed({
                initializeMap()
            }, 100)

            if (checkLocationPermission()) {
                Log.d(TAG, "Location permission granted")
            } else {
                requestLocationPermission()
            }

            // Initialize NavigationManager
            initializeNavigationManager()

            // Load routes
            loadRoutesFromAssets()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error initializing: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        mapContainer = findViewById(R.id.mapContainer)
        btnLoadRoutes = findViewById(R.id.btnLoadRoutes)
        btnClearRoutes = findViewById(R.id.btnClearRoutes)
        spinnerRoutes = findViewById(R.id.spinnerRoutes)
        btnStartNavigation = findViewById(R.id.btnStartNavigation)
        navigationPanel = findViewById(R.id.navigationPanel)
        tvNavigationInstruction = findViewById(R.id.tvNavigationInstruction)
        tvProgress = findViewById(R.id.tvProgress)
        tvRemainingDistance = findViewById(R.id.tvRemainingDistance)
        btnStopNavigation = findViewById(R.id.btnStopNavigation)
        tvRouteInfo = findViewById(R.id.tvRouteInfo)
    }

    private fun initializeNavigationManager() {
        try {
            navigationManager = NavigationManager(this, TOMTOM_API_KEY)

            navigationManager.setNavigationCallback(object : NavigationManager.NavigationCallback {
                override fun onNavigationStarted() {
                    runOnUiThread {
                        isNavigationActive = true
                        navigationPanel.visibility = View.VISIBLE
                        btnStartNavigation.isEnabled = false
                        btnStopNavigation.isEnabled = true
                        Log.d(TAG, "Navigation started via NavigationManager")
                    }
                }

                override fun onNavigationStopped() {
                    runOnUiThread {
                        isNavigationActive = false
                        navigationPanel.visibility = View.GONE
                        btnStartNavigation.isEnabled = true
                        btnStopNavigation.isEnabled = false
                        Log.d(TAG, "Navigation stopped via NavigationManager")
                    }
                }

                override fun onNavigationError(error: String) {
                    runOnUiThread {
                        Log.e(TAG, "Navigation error: $error")
                        Toast.makeText(this@MainActivitySimple, "Navigation error: $error", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onInstructionUpdated(instruction: String) {
                    runOnUiThread {
                        tvNavigationInstruction.text = instruction
                        Log.d(TAG, "Navigation instruction: $instruction")
                    }
                }

                override fun onProgressUpdated(percentage: Double, remainingDistance: String) {
                    runOnUiThread {
                        tvProgress.text = "Progress: ${String.format("%.1f", percentage)}%"
                        tvRemainingDistance.text = "Remaining: $remainingDistance"
                        Log.d(TAG, "Navigation progress: ${String.format("%.1f", percentage)}%, remaining: $remainingDistance")
                    }
                }
            })

            if (navigationManager.initialize()) {
                Log.d(TAG, "NavigationManager initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize NavigationManager")
                Toast.makeText(this, "Failed to initialize navigation", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing NavigationManager", e)
            Toast.makeText(this, "Navigation initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        btnLoadRoutes.setOnClickListener {
            loadRoutesFromAssets()
        }

        btnClearRoutes.setOnClickListener {
            clearRoutes()
        }

        spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < routes.size) {
                    selectedRouteIndex = position
                    showRouteInfo(routes[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRouteIndex = -1
                tvRouteInfo.text = "No route selected"
            }
        }

        btnStartNavigation.setOnClickListener {
            startNavigation()
        }

        btnStopNavigation.setOnClickListener {
            stopNavigation()
        }
    }

    private fun initializeMap() {
        try {
            Log.d(TAG, "Initializing map with API key: ${TOMTOM_API_KEY.take(10)}...")

            if (TOMTOM_API_KEY.isBlank()) {
                Log.e(TAG, "TomTom API key is empty!")
                Toast.makeText(this, "TomTom API key not configured", Toast.LENGTH_LONG).show()
                return
            }

            val mapOptions = MapOptions(mapKey = TOMTOM_API_KEY)
            mapFragment = MapFragment.newInstance(mapOptions)

            supportFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, mapFragment!!)
                .commitNow()

            mapFragment?.getMapAsync { map ->
                tomTomMap = map
                Log.d(TAG, "Map initialized successfully")

                // Set initial position
                val johannesburg = GeoPoint(-26.2041, 28.0473)
                map.moveCamera(
                    CameraOptions(
                        position = johannesburg,
                        zoom = 10.0
                    )
                )

                Toast.makeText(this, "TomTom map ready", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize map", e)
            Toast.makeText(this, "Map initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadRoutesFromAssets() {
        try {
            val inputStream = assets.open("Next_Billion_response_4.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val response = gson.fromJson(reader, NextBillionResponse::class.java)

            routes = response.result.routes

            val routeNames = routes.mapIndexed { index, route ->
                "Route ${index + 1}: ${route.vehicle}"
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRoutes.adapter = adapter

            Toast.makeText(this, "Loaded ${routes.size} routes", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error loading routes", e)
            Toast.makeText(this, "Error loading routes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearRoutes() {
        routes = emptyList()
        selectedRouteIndex = -1
        spinnerRoutes.adapter = null
        tvRouteInfo.text = "Routes cleared"
        navigationPanel.visibility = View.GONE
        clearMapRoute()
    }

    private fun showRouteInfo(route: NextBillionRoute) {
        val info = buildString {
            appendLine("Vehicle: ${route.vehicle}")
            appendLine("Steps: ${route.steps.size}")
            appendLine("Geometry length: ${route.geometry?.length ?: 0}")
        }
        tvRouteInfo.text = info
    }

    private fun startNavigation() {
        if (selectedRouteIndex >= 0 && selectedRouteIndex < routes.size) {
            val selectedRoute = routes[selectedRouteIndex]

            try {
                selectedRoute.geometry?.let { encodedPolyline ->
                    // Decode NextBillion polyline to GeoPoints
                    val geoPoints = decodePolyline(encodedPolyline)

                    if (geoPoints.size >= 2) {
                        // Display route on map first
                        parseAndDisplayNextBillionRoute(encodedPolyline, selectedRoute.vehicle)

                        // Start actual turn-by-turn navigation using NavigationManager
                        navigationManager.startNavigationWithGeometry(geoPoints)

                        Log.d(TAG, "Starting turn-by-turn navigation with ${geoPoints.size} waypoints")
                        Toast.makeText(this, "Turn-by-turn navigation started", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Route must have at least 2 waypoints", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this, "Route geometry not available", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start navigation with NextBillion route", e)
                Toast.makeText(this, "Failed to start navigation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopNavigation() {
        try {
            // Stop navigation via NavigationManager
            navigationManager.stopNavigation()

            // Clear route from map
            clearMapRoute()

            Log.d(TAG, "Navigation stopped by user")
            Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping navigation", e)
            Toast.makeText(this, "Error stopping navigation: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Parse NextBillion route geometry and display on TomTom map
     * This is the key integration point between NextBillion and TomTom
     */
    private fun parseAndDisplayNextBillionRoute(encodedPolyline: String, routeName: String) {
        try {
            Log.d(TAG, "Parsing NextBillion route: $routeName")
            Log.d(TAG, "Encoded polyline length: ${encodedPolyline.length}")

            // Decode polyline from NextBillion geometry (encoded polyline format)
            val geoPoints = decodePolyline(encodedPolyline)

            if (geoPoints.size >= 2) {
                // Create TomTom route from NextBillion geometry
                createTomTomRouteFromGeoPoints(geoPoints)

                Log.d(TAG, "Successfully converted NextBillion route to TomTom route: ${geoPoints.size} points")
                Toast.makeText(this, "Route loaded: $routeName (${geoPoints.size} waypoints)", Toast.LENGTH_SHORT).show()
            } else {
                throw IllegalArgumentException("Route must have at least 2 points")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse NextBillion route", e)
            Toast.makeText(this, "Failed to parse route: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * Decode polyline string to list of GeoPoints
     * NextBillion uses encoded polyline format (similar to Google's format)
     */
    private fun decodePolyline(encoded: String): List<GeoPoint> {
        val poly = mutableListOf<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val geoPoint = GeoPoint(lat / 1E5, lng / 1E5)
            poly.add(geoPoint)
        }

        return poly
    }

    /**
     * Create TomTom route from GeoPoints and display on map
     */
    private fun createTomTomRouteFromGeoPoints(geoPoints: List<GeoPoint>) {
        try {
            val map = tomTomMap ?: run {
                Log.e(TAG, "TomTom map not initialized")
                Toast.makeText(this, "Map not ready yet", Toast.LENGTH_SHORT).show()
                return
            }

            // Clear existing route display
            clearMapRoute()

            if (geoPoints.size < 2) {
                Log.e(TAG, "Route must have at least 2 points")
                return
            }

            // Create polyline for the route
            val polylineOptions = PolylineOptions(
                coordinates = geoPoints,
                tag = "route"
            )
            currentRoutePolyline = map.addPolyline(polylineOptions)

            // Add markers for start and end points
            val startPoint = geoPoints.first()
            val endPoint = geoPoints.last()

            // Start marker (green)
            val startMarker = MarkerOptions(
                coordinate = startPoint,
                pinImage = ImageFactory.fromResource(R.drawable.ic_marker_start),
                tag = "route_markers"
            )
            routeMarkers.add(map.addMarker(startMarker))

            // End marker (red)
            val endMarker = MarkerOptions(
                coordinate = endPoint,
                pinImage = ImageFactory.fromResource(R.drawable.ic_marker_end),
                tag = "route_markers"
            )
            routeMarkers.add(map.addMarker(endMarker))

            // Zoom to fit the route
            val bounds = geoPoints.fold(
                Pair(
                    GeoPoint(90.0, 180.0),  // min
                    GeoPoint(-90.0, -180.0)  // max
                )
            ) { acc, point ->
                Pair(
                    GeoPoint(
                        minOf(acc.first.latitude, point.latitude),
                        minOf(acc.first.longitude, point.longitude)
                    ),
                    GeoPoint(
                        maxOf(acc.second.latitude, point.latitude),
                        maxOf(acc.second.longitude, point.longitude)
                    )
                )
            }

            // Move camera to show the entire route
            val center = GeoPoint(
                (bounds.first.latitude + bounds.second.latitude) / 2,
                (bounds.first.longitude + bounds.second.longitude) / 2
            )

            map.moveCamera(
                CameraOptions(
                    position = center,
                    zoom = calculateZoomLevel(bounds)
                )
            )

            Log.d(TAG, "Route displayed with ${geoPoints.size} points")
            Toast.makeText(this, "Route displayed on map", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to display route on map", e)
            Toast.makeText(this, "Failed to display route: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearMapRoute() {
        // Clear all polylines and markers by tag
        tomTomMap?.removePolylines("route")
        tomTomMap?.removeMarkers("route_markers")
        currentRoutePolyline = null
        routeMarkers.clear()
    }

    private fun calculateZoomLevel(bounds: Pair<GeoPoint, GeoPoint>): Double {
        val latDiff = bounds.second.latitude - bounds.first.latitude
        val lonDiff = bounds.second.longitude - bounds.first.longitude
        val maxDiff = maxOf(latDiff, lonDiff)

        return when {
            maxDiff > 10 -> 5.0
            maxDiff > 5 -> 7.0
            maxDiff > 2 -> 9.0
            maxDiff > 1 -> 11.0
            maxDiff > 0.5 -> 13.0
            else -> 15.0
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted")
            } else {
                Toast.makeText(this, "Location permission required for navigation", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            // Clean up NavigationManager resources
            if (::navigationManager.isInitialized) {
                navigationManager.cleanup()
                Log.d(TAG, "NavigationManager resources cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up NavigationManager", e)
        }
    }
}