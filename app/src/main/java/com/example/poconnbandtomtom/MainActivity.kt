package com.example.poconnbandtomtom

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.poconnbandtomtom.databinding.ActivityMainBinding
import com.example.poconnbandtomtom.models.NextBillionResponse
import com.example.poconnbandtomtom.models.NextBillionRoute
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
// TomTom SDK imports
import com.tomtom.sdk.location.GeoLocation
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.marker.Marker
import com.tomtom.sdk.map.display.polyline.PolylineOptions
import com.tomtom.sdk.map.display.polyline.Polyline
import com.tomtom.sdk.map.display.ui.MapFragment
import com.tomtom.sdk.map.display.style.StyleMode
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.location.DefaultLocationProviderFactory
import com.example.poconnbandtomtom.navigation.NavigationManager
import kotlinx.coroutines.launch
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapFragment: MapFragment
    private var tomTomMap: TomTomMap? = null
    private var currentRoutePolyline: Polyline? = null
    private val routeMarkers = mutableListOf<Marker>()

    private var routes: List<NextBillionRoute> = emptyList()
    private var selectedRouteIndex = -1

    // Navigation Manager
    private lateinit var navigationManager: NavigationManager

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MainActivity"

        // API Key - should be configured in gradle.properties
        private const val TOMTOM_API_KEY = BuildConfig.TOMTOM_API_KEY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupUI()

            // Defer map initialization slightly to ensure view is ready
            binding.root.post {
                initializeTomTomServices()
            }

            if (checkLocationPermission()) {
                initializeLocationServices()
            } else {
                requestLocationPermission()
            }

            // Initialize Navigation Manager
            initializeNavigationManager()

            // Load routes immediately
            loadRoutesFromAssets()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeTomTomServices() {
        try {
            Log.d(TAG, "Initializing TomTom services with API key: ${TOMTOM_API_KEY.take(10)}...")

            // Create MapOptions
            val mapOptions = MapOptions(mapKey = TOMTOM_API_KEY)

            // Get or create the MapFragment
            val existingFragment = supportFragmentManager.findFragmentById(R.id.mapFragment)

            if (existingFragment is MapFragment) {
                mapFragment = existingFragment
                Log.d(TAG, "Found existing MapFragment")
            } else {
                mapFragment = MapFragment.newInstance(mapOptions)
                supportFragmentManager.beginTransaction()
                    .replace(R.id.mapFragment, mapFragment)
                    .commitNow() // Use commitNow for immediate execution
                Log.d(TAG, "Created new MapFragment")
            }

            // Get the TomTom map instance
            mapFragment.getMapAsync { map ->
                tomTomMap = map
                Log.d(TAG, "Got TomTom map instance")

                // Set initial camera position (Johannesburg, South Africa as default)
                val johannesburg = GeoPoint(-26.2041, 28.0473)
                map.moveCamera(
                    CameraOptions(
                        position = johannesburg,
                        zoom = 10.0
                    )
                )

                // Enable user location if permission granted
                if (checkLocationPermission()) {
                    try {
                        val locationProvider = DefaultLocationProviderFactory.create(this)
                        map.setLocationProvider(locationProvider)
                        locationProvider.enable()
                        Log.d(TAG, "Location provider enabled")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to enable location provider", e)
                    }
                }

                Log.d(TAG, "TomTom map initialized successfully")
                Toast.makeText(this, "TomTom map ready", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TomTom services", e)
            e.printStackTrace()
            Toast.makeText(this, "TomTom SDK initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeNavigationManager() {
        try {
            Log.d(TAG, "Initializing Navigation Manager")

            navigationManager = NavigationManager(this, TOMTOM_API_KEY)

            // Set navigation callback to handle events
            navigationManager.setNavigationCallback(object : NavigationManager.NavigationCallback {
                override fun onNavigationStarted() {
                    runOnUiThread {
                        binding.navigationPanel.visibility = View.VISIBLE
                        binding.tvNavigationInstruction.text = "Navigation started - calculating route..."
                        Toast.makeText(this@MainActivity, "Turn-by-turn navigation started", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onNavigationStopped() {
                    runOnUiThread {
                        binding.navigationPanel.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Navigation stopped", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onNavigationError(error: String) {
                    runOnUiThread {
                        Log.e(TAG, "Navigation error: $error")
                        Toast.makeText(this@MainActivity, "Navigation error: $error", Toast.LENGTH_LONG).show()
                        binding.navigationPanel.visibility = View.GONE
                    }
                }

                override fun onInstructionUpdated(instruction: String) {
                    runOnUiThread {
                        binding.tvNavigationInstruction.text = instruction
                    }
                }

                override fun onProgressUpdated(percentage: Double, remainingDistance: String) {
                    runOnUiThread {
                        binding.tvProgress.text = "Progress: ${String.format("%.1f", percentage)}%"
                        binding.tvRemainingDistance.text = "Remaining: $remainingDistance"
                    }
                }
            })

            // Initialize the navigation manager
            if (navigationManager.initialize()) {
                Log.d(TAG, "Navigation Manager initialized successfully")
            } else {
                Log.e(TAG, "Failed to initialize Navigation Manager")
                Toast.makeText(this, "Failed to initialize navigation system", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Navigation Manager", e)
            Toast.makeText(this, "Navigation initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUI() {
        binding.btnLoadRoutes.setOnClickListener {
            loadRoutesFromAssets()
        }

        binding.btnClearRoutes.setOnClickListener {
            clearRoutes()
        }

        binding.spinnerRoutes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < routes.size) {
                    selectedRouteIndex = position
                    showRouteInfo(routes[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRouteIndex = -1
                binding.tvRouteInfo.text = "No route selected"
            }
        }

        binding.btnStartNavigation.setOnClickListener {
            startNavigation()
        }

        binding.btnStopNavigation.setOnClickListener {
            stopNavigation()
        }
    }

    private fun loadRoutesFromAssets() {
        try {
            val inputStream = assets.open("Next_Billion_response_4.json")
            val reader = InputStreamReader(inputStream)
            val gson = Gson()
            val response = gson.fromJson(reader, NextBillionResponse::class.java)

            routes = response.result.routes

            // Update spinner
            val routeNames = routes.mapIndexed { index, route ->
                "Route ${index + 1}: ${route.vehicle}"
            }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRoutes.adapter = adapter

            Toast.makeText(this, "Loaded ${routes.size} routes", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error loading routes: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun clearRoutes() {
        routes = emptyList()
        selectedRouteIndex = -1
        binding.spinnerRoutes.adapter = null
        binding.tvRouteInfo.text = "Routes cleared"
        binding.navigationPanel.visibility = View.GONE
    }

    private fun showRouteInfo(route: NextBillionRoute) {
        val info = buildString {
            appendLine("Vehicle: ${route.vehicle}")
            appendLine("Steps: ${route.steps.size}")
            appendLine("Geometry length: ${route.geometry?.length ?: 0}")
            appendLine()
            route.steps.forEachIndexed { index, step ->
                appendLine("Step ${index + 1}: ${step.type} at ${step.location}")
            }
        }
        binding.tvRouteInfo.text = info
    }

    private fun startNavigation() {
        if (selectedRouteIndex >= 0 && selectedRouteIndex < routes.size) {
            val selectedRoute = routes[selectedRouteIndex]

            try {
                selectedRoute.geometry?.let { geoJsonString ->
                    // Parse NextBillion route geometry to GeoPoints
                    val routePoints = parseGeoJsonString(geoJsonString)
                    val geoPoints = parseGeoJsonRoute(routePoints)

                    if (geoPoints.size >= 2) {
                        // Display route on map
                        createTomTomRouteFromGeoPoints(geoPoints)

                        // Start actual turn-by-turn navigation using NavigationManager
                        Log.d(TAG, "Starting navigation with ${geoPoints.size} waypoints")
                        navigationManager.startNavigationWithGeometry(geoPoints)

                        Toast.makeText(this, "Starting turn-by-turn navigation for ${selectedRoute.vehicle}", Toast.LENGTH_SHORT).show()
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

    /**
     * Parse NextBillion route geometry and display on TomTom map
     * This is the key integration point between NextBillion and TomTom
     */
    private fun parseAndDisplayNextBillionRoute(geoJsonString: String, routeName: String) {
        try {
            Log.d(TAG, "Parsing NextBillion route: $routeName")

            // Parse GeoJSON coordinates from NextBillion response
            val routePoints = parseGeoJsonString(geoJsonString)
            val geoPoints = parseGeoJsonRoute(routePoints)

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
     * Parse GeoJSON coordinate array to GeoPoints
     * GeoJSON format: [longitude, latitude] -> GeoPoint(latitude, longitude)
     */
    private fun parseGeoJsonRoute(geoJsonCoords: List<List<Double>>): List<GeoPoint> {
        return geoJsonCoords.map { coord ->
            if (coord.size >= 2) {
                GeoPoint(coord[1], coord[0]) // GeoJSON is [lng, lat], GeoPoint is (lat, lng)
            } else {
                throw IllegalArgumentException("Invalid coordinate format: $coord")
            }
        }
    }

    /**
     * Parse GeoJSON string to coordinate list
     */
    private fun parseGeoJsonString(geoJsonString: String): List<List<Double>> {
        try {
            val jsonParser = JsonParser()
            val jsonElement = jsonParser.parse(geoJsonString)

            val coordArray = if (jsonElement.isJsonArray) {
                jsonElement.asJsonArray
            } else {
                throw IllegalArgumentException("Expected JSON array format")
            }

            val coordList = mutableListOf<List<Double>>()
            for (element in coordArray) {
                val coord = element.asJsonArray
                coordList.add(listOf(coord[0].asDouble, coord[1].asDouble))
            }

            return coordList
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing GeoJSON string", e)
            throw IllegalArgumentException("Invalid GeoJSON format: ${e.message}")
        }
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

    private fun stopNavigation() {
        try {
            navigationManager.stopNavigation()
            binding.navigationPanel.visibility = View.GONE
            Log.d(TAG, "Navigation stopped via UI")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping navigation", e)
            binding.navigationPanel.visibility = View.GONE
            Toast.makeText(this, "Error stopping navigation: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun initializeLocationServices() {
        if (!checkLocationPermission()) return

        try {
            // Location services initialization placeholder
            // Will use TomTom LocationProvider when map is integrated
            Log.d(TAG, "Location services initialized")
            Toast.makeText(this, "Location services initialized", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission denied", e)
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize location services", e)
            Toast.makeText(this, "Failed to initialize location services", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeLocationServices()
            } else {
                Toast.makeText(this, "Location permission required for navigation", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Location update method placeholder - will be implemented with TomTom LocationProvider
    private fun onLocationUpdate(latitude: Double, longitude: Double) {
        // Update navigation with new location
        if (binding.navigationPanel.visibility == View.VISIBLE) {
            binding.tvRemainingDistance.text = "Location: $latitude, $longitude"
        }

        Log.d(TAG, "Location updated: $latitude, $longitude")
    }

    // MapFragment handles its own lifecycle, no need for manual lifecycle management

    override fun onDestroy() {
        super.onDestroy()

        try {
            // Clean up navigation resources
            if (::navigationManager.isInitialized) {
                navigationManager.cleanup()
            }

            // Clean up map resources
            clearMapRoute()
            tomTomMap?.setLocationProvider(null)
            Log.d(TAG, "Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
    }
}