package com.example.poconnbandtomtom.navigation

import android.content.Context
import android.util.Log
import com.tomtom.sdk.location.GeoPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Navigation Manager class to handle turn-by-turn navigation simulation
 * This implementation provides realistic navigation simulation using NextBillion route data
 */
class NavigationManager(
    private val context: Context,
    private val apiKey: String
) {
    companion object {
        private const val TAG = "NavigationManager"
    }

    private var isNavigating = false

    // Navigation state flows
    private val _navigationInstruction = MutableStateFlow("No navigation active")
    val navigationInstruction: StateFlow<String> = _navigationInstruction.asStateFlow()

    private val _progressPercentage = MutableStateFlow(0.0)
    val progressPercentage: StateFlow<Double> = _progressPercentage.asStateFlow()

    private val _remainingDistance = MutableStateFlow("0 km")
    val remainingDistance: StateFlow<String> = _remainingDistance.asStateFlow()

    private val _isNavigationActive = MutableStateFlow(false)
    val isNavigationActive: StateFlow<Boolean> = _isNavigationActive.asStateFlow()

    interface NavigationCallback {
        fun onNavigationStarted()
        fun onNavigationStopped()
        fun onNavigationError(error: String)
        fun onInstructionUpdated(instruction: String)
        fun onProgressUpdated(percentage: Double, remainingDistance: String)
    }

    private var callback: NavigationCallback? = null

    fun setNavigationCallback(callback: NavigationCallback) {
        this.callback = callback
    }

    /**
     * Initialize the Navigation Manager
     */
    fun initialize(): Boolean {
        return try {
            Log.d(TAG, "Initializing Navigation Manager")
            Log.d(TAG, "Navigation Manager initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Navigation Manager", e)
            callback?.onNavigationError("Failed to initialize navigation: ${e.message}")
            false
        }
    }

    /**
     * Start navigation with route geometry from NextBillion
     */
    fun startNavigationWithGeometry(routeGeometry: List<GeoPoint>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (routeGeometry.size < 2) {
                    callback?.onNavigationError("Route must have at least 2 waypoints")
                    return@launch
                }

                val startPoint = routeGeometry.first()
                val endPoint = routeGeometry.last()

                Log.d(TAG, "Starting navigation from $startPoint to $endPoint with ${routeGeometry.size} waypoints")

                // Start navigation simulation
                startNavigationSimulation(routeGeometry)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start navigation", e)
                callback?.onNavigationError("Failed to start navigation: ${e.message}")
            }
        }
    }

    /**
     * Start navigation simulation
     */
    private fun startNavigationSimulation(routeGeometry: List<GeoPoint>) {
        try {
            isNavigating = true
            _isNavigationActive.value = true
            callback?.onNavigationStarted()

            // Calculate total distance for simulation
            val totalDistance = calculateDistanceAlongRoute(routeGeometry)

            Log.d(TAG, "Starting navigation simulation with total distance: ${formatDistance(totalDistance)}")

            // Start navigation simulation
            CoroutineScope(Dispatchers.IO).launch {
                simulateNavigation(routeGeometry, totalDistance)
            }

            Log.d(TAG, "Navigation started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start navigation", e)
            callback?.onNavigationError("Failed to start navigation: ${e.message}")
        }
    }

    /**
     * Simulate navigation progress and instructions
     */
    private suspend fun simulateNavigation(routeGeometry: List<GeoPoint>, totalDistance: Double) {
        val instructions = generateNavigationInstructions(routeGeometry)
        val totalSteps = instructions.size
        var currentStep = 0

        while (isNavigating && currentStep < totalSteps) {
            val instruction = instructions[currentStep]
            val progress = (currentStep.toDouble() / totalSteps.toDouble()) * 100.0
            val remainingDistance = totalDistance * ((totalSteps - currentStep).toDouble() / totalSteps.toDouble())

            // Update state
            _navigationInstruction.value = instruction
            _progressPercentage.value = progress
            _remainingDistance.value = formatDistance(remainingDistance)

            // Notify callback
            callback?.onInstructionUpdated(instruction)
            callback?.onProgressUpdated(progress, formatDistance(remainingDistance))

            Log.d(TAG, "Navigation step $currentStep: $instruction (${String.format("%.1f", progress)}%)")

            // Wait before next instruction (simulate real navigation timing)
            delay(3000) // 3 seconds between instructions for demo purposes

            currentStep++
        }

        // Navigation completed
        if (isNavigating) {
            _navigationInstruction.value = "You have arrived at your destination"
            _progressPercentage.value = 100.0
            _remainingDistance.value = "0 m"
            callback?.onInstructionUpdated("You have arrived at your destination")
            callback?.onProgressUpdated(100.0, "0 m")

            Log.d(TAG, "Navigation completed - arrived at destination")

            // Auto-stop after arrival
            delay(3000)
            stopNavigation()
        }
    }

    /**
     * Generate realistic navigation instructions based on route geometry
     */
    private fun generateNavigationInstructions(routeGeometry: List<GeoPoint>): List<String> {
        val instructions = mutableListOf<String>()

        if (routeGeometry.size < 2) return instructions

        instructions.add("Starting navigation")
        instructions.add("Head towards your destination")

        // Generate instructions based on route segments
        val segmentCount = routeGeometry.size - 1
        val instructionTemplates = listOf(
            "Continue straight",
            "Turn slight right",
            "Turn right",
            "Turn left",
            "Turn slight left",
            "Continue on current road",
            "Keep right",
            "Keep left",
            "Follow the road"
        )

        for (i in 1 until segmentCount) {
            val progressRatio = i.toDouble() / segmentCount.toDouble()

            val instruction = when {
                progressRatio < 0.2 -> "Continue straight for ${randomDistance(500, 1500)}"
                progressRatio < 0.4 -> instructionTemplates[i % instructionTemplates.size]
                progressRatio < 0.6 -> "Continue on current road for ${randomDistance(800, 2000)}"
                progressRatio < 0.8 -> instructionTemplates[(i + 2) % instructionTemplates.size]
                else -> "Approaching destination area"
            }

            instructions.add(instruction)
        }

        instructions.add("In 200m, you will reach your destination")
        instructions.add("In 50m, you will reach your destination")
        instructions.add("You have arrived at your destination")

        return instructions
    }

    /**
     * Generate random distance for instructions
     */
    private fun randomDistance(min: Int, max: Int): String {
        val distance = (min..max).random()
        return if (distance >= 1000) {
            "${distance / 1000}.${(distance % 1000) / 100} km"
        } else {
            "$distance m"
        }
    }

    /**
     * Calculate approximate distance along route using Haversine formula
     */
    private fun calculateDistanceAlongRoute(routeGeometry: List<GeoPoint>): Double {
        var totalDistance = 0.0

        for (i in 0 until routeGeometry.size - 1) {
            val point1 = routeGeometry[i]
            val point2 = routeGeometry[i + 1]
            totalDistance += calculateDistance(point1, point2)
        }

        return totalDistance
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371000.0 // Earth radius in meters

        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLonRad = Math.toRadians(point2.longitude - point1.longitude)

        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Format distance for display
     */
    private fun formatDistance(distanceInMeters: Double): String {
        return if (distanceInMeters >= 1000) {
            String.format("%.1f km", distanceInMeters / 1000.0)
        } else {
            String.format("%.0f m", distanceInMeters)
        }
    }

    /**
     * Stop active navigation
     */
    fun stopNavigation() {
        try {
            isNavigating = false
            _isNavigationActive.value = false
            _navigationInstruction.value = "Navigation stopped"
            _progressPercentage.value = 0.0
            _remainingDistance.value = "0 km"
            callback?.onNavigationStopped()
            Log.d(TAG, "Navigation stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop navigation", e)
            callback?.onNavigationError("Failed to stop navigation: ${e.message}")
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            isNavigating = false
            _isNavigationActive.value = false
            Log.d(TAG, "Navigation resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up navigation resources", e)
        }
    }

    /**
     * Get current navigation status
     */
    fun isNavigating(): Boolean = _isNavigationActive.value

    /**
     * Get current instruction
     */
    fun getCurrentInstruction(): String = _navigationInstruction.value

    /**
     * Get current progress percentage
     */
    fun getCurrentProgress(): Double = _progressPercentage.value

    /**
     * Get remaining distance
     */
    fun getRemainingDistance(): String = _remainingDistance.value
}