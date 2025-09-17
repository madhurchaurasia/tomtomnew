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
import com.google.gson.Gson
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import java.io.InputStreamReader

class MainActivitySimple : AppCompatActivity() {

    private var tomTomMap: TomTomMap? = null
    private lateinit var mapContainer: FrameLayout
    private var mapFragment: MapFragment? = null

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
            navigationPanel.visibility = View.VISIBLE
            tvNavigationInstruction.text = "Navigation started for ${routes[selectedRouteIndex].vehicle}"
            tvProgress.text = "Progress: 0%"
            tvRemainingDistance.text = "Starting navigation..."
            Toast.makeText(this, "Navigation started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopNavigation() {
        navigationPanel.visibility = View.GONE
        Toast.makeText(this, "Navigation stopped", Toast.LENGTH_SHORT).show()
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
}