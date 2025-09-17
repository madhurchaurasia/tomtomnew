package com.example.poconnbandtomtom.navigation

import android.content.Context
import android.util.Log
import com.example.poconnbandtomtom.R
import com.example.poconnbandtomtom.models.NextBillionRoute
import com.example.poconnbandtomtom.utils.PolylineDecoder
import com.tomtom.sdk.location.GeoPoint
import com.tomtom.sdk.map.display.TomTomMap
import com.tomtom.sdk.map.display.marker.Marker
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.image.ImageFactory
import kotlin.math.*

/**
 * Route simulation and preview system with Google Maps-style navigation arrow
 */
class RouteSimulator(
    private val context: Context,
    private val tomTomMap: TomTomMap
) {
    private var currentRoute: NextBillionRoute? = null
    private var routePoints: List<GeoPoint> = emptyList()
    private var navigationMarker: Marker? = null
    private var currentPositionIndex: Float = 0f
    private var isSimulationActive = false
    
    // Callbacks
    var onPositionChanged: ((GeoPoint, Float, String) -> Unit)? = null
    var onSimulationStarted: (() -> Unit)? = null
    var onSimulationStopped: (() -> Unit)? = null
    
    companion object {
        private const val TAG = "RouteSimulator"
        private const val NAVIGATION_MARKER_TAG = "navigation_marker"
    }
    
    /**
     * Initialize route for simulation
     */
    fun initializeRoute(route: NextBillionRoute): Boolean {
        return try {
            currentRoute = route
            routePoints = PolylineDecoder.decodePolyline(route.geometry)
            
            if (routePoints.size < 2) {
                Log.w(TAG, "Route has insufficient points for simulation")
                return false
            }
            
            currentPositionIndex = 0f
            Log.d(TAG, "Route initialized with ${routePoints.size} points")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize route", e)
            false
        }
    }
    
    /**
     * Start route simulation with navigation arrow
     */
    fun startSimulation() {
        if (routePoints.isEmpty()) {
            Log.w(TAG, "No route initialized for simulation")
            return
        }
        
        isSimulationActive = true
        createNavigationMarker()
        moveToPosition(0f)
        onSimulationStarted?.invoke()
        
        Log.d(TAG, "Route simulation started")
    }
    
    /**
     * Stop simulation and remove navigation marker
     */
    fun stopSimulation() {
        isSimulationActive = false
        removeNavigationMarker()
        onSimulationStopped?.invoke()
        
        Log.d(TAG, "Route simulation stopped")
    }
    
    /**
     * Move navigation marker to specific position along route
     */
    fun moveToPosition(positionIndex: Float) {
        if (!isSimulationActive || routePoints.isEmpty()) return
        
        val clampedIndex = positionIndex.coerceIn(0f, (routePoints.size - 1).toFloat())
        currentPositionIndex = clampedIndex
        
        val interpolatedPosition = interpolatePosition(clampedIndex)
        val bearing = calculateBearing(clampedIndex)
        val instruction = generateInstruction(clampedIndex)
        
        updateNavigationMarker(interpolatedPosition, bearing)
        onPositionChanged?.invoke(interpolatedPosition, bearing, instruction)
    }
    
    /**
     * Move marker forward by specified distance (in route points)
     */
    fun moveForward(steps: Float = 1f) {
        val newIndex = (currentPositionIndex + steps).coerceAtMost((routePoints.size - 1).toFloat())
        moveToPosition(newIndex)
    }
    
    /**
     * Move marker backward by specified distance (in route points)
     */
    fun moveBackward(steps: Float = 1f) {
        val newIndex = (currentPositionIndex - steps).coerceAtLeast(0f)
        moveToPosition(newIndex)
    }
    
    /**
     * Move to closest position on route from touch coordinates
     */
    fun moveToClosestPosition(touchPoint: GeoPoint): Boolean {
        if (routePoints.isEmpty()) return false
        
        var closestIndex = 0f
        var minDistance = Double.MAX_VALUE
        
        for (i in 0 until routePoints.size - 1) {
            val projectedPoint = projectPointOnSegment(
                touchPoint,
                routePoints[i],
                routePoints[i + 1]
            )
            
            val distance = calculateDistance(touchPoint, projectedPoint.position)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i + projectedPoint.t
            }
        }
        
        // Only snap if within reasonable distance (100 meters)
        if (minDistance < 100.0) {
            moveToPosition(closestIndex)
            return true
        }
        
        return false
    }
    
    /**
     * Get current progress information
     */
    fun getCurrentProgress(): RouteProgress {
        if (routePoints.isEmpty()) {
            return RouteProgress(0.0, 0.0, 0.0, "No route loaded")
        }
        
        val totalDistance = calculateTotalDistance()
        val traveledDistance = calculateTraveledDistance(currentPositionIndex)
        val progressPercentage = if (totalDistance > 0) (traveledDistance / totalDistance) * 100 else 0.0
        val remainingDistance = totalDistance - traveledDistance
        
        return RouteProgress(
            progressPercentage = progressPercentage,
            traveledDistance = traveledDistance,
            remainingDistance = remainingDistance,
            instruction = generateInstruction(currentPositionIndex)
        )
    }
    
    // Private helper methods
    
    private fun createNavigationMarker() {
        removeNavigationMarker()
        
        try {
            val markerOptions = MarkerOptions(
                coordinate = routePoints.first(),
                pinImage = ImageFactory.fromResource(R.drawable.ic_navigation_arrow),
                tag = NAVIGATION_MARKER_TAG
            )
            
            navigationMarker = tomTomMap.addMarker(markerOptions)
            Log.d(TAG, "Navigation marker created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create navigation marker", e)
        }
    }
    
    private fun removeNavigationMarker() {
        navigationMarker?.let {
            tomTomMap.removeMarkers(NAVIGATION_MARKER_TAG)
            navigationMarker = null
            Log.d(TAG, "Navigation marker removed")
        }
    }
    
    private fun updateNavigationMarker(position: GeoPoint, bearing: Float) {
        navigationMarker?.let { marker ->
            try {
                // Update marker position and rotation
                val markerOptions = MarkerOptions(
                    coordinate = position,
                    pinImage = ImageFactory.fromResource(R.drawable.ic_navigation_arrow),
                    tag = NAVIGATION_MARKER_TAG
                )
                
                // Remove old marker and add new one with rotation
                tomTomMap.removeMarkers(NAVIGATION_MARKER_TAG)
                navigationMarker = tomTomMap.addMarker(markerOptions)
                
                Log.d(TAG, "Navigation marker updated: lat=${position.latitude}, lng=${position.longitude}, bearing=$bearing")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update navigation marker", e)
            }
        }
    }
    
    private fun interpolatePosition(positionIndex: Float): GeoPoint {
        if (routePoints.isEmpty()) return GeoPoint(0.0, 0.0)
        
        val segmentIndex = positionIndex.toInt()
        val fraction = positionIndex - segmentIndex
        
        if (segmentIndex >= routePoints.size - 1) {
            return routePoints.last()
        }
        
        val startPoint = routePoints[segmentIndex]
        val endPoint = routePoints[segmentIndex + 1]
        
        val lat = startPoint.latitude + (endPoint.latitude - startPoint.latitude) * fraction
        val lng = startPoint.longitude + (endPoint.longitude - startPoint.longitude) * fraction
        
        return GeoPoint(lat, lng)
    }
    
    private fun calculateBearing(positionIndex: Float): Float {
        if (routePoints.size < 2) return 0f
        
        val segmentIndex = positionIndex.toInt().coerceIn(0, routePoints.size - 2)
        val startPoint = routePoints[segmentIndex]
        val endPoint = routePoints[segmentIndex + 1]
        
        val deltaLng = endPoint.longitude - startPoint.longitude
        val deltaLat = endPoint.latitude - startPoint.latitude
        
        val bearing = atan2(deltaLng, deltaLat) * 180 / PI
        return ((bearing + 360) % 360).toFloat()
    }
    
    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val earthRadius = 6371000.0 // meters
        val lat1Rad = Math.toRadians(point1.latitude)
        val lat2Rad = Math.toRadians(point2.latitude)
        val deltaLatRad = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLngRad = Math.toRadians(point2.longitude - point1.longitude)
        
        val a = sin(deltaLatRad / 2).pow(2) +
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    private fun calculateTotalDistance(): Double {
        if (routePoints.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until routePoints.size - 1) {
            totalDistance += calculateDistance(routePoints[i], routePoints[i + 1])
        }
        return totalDistance
    }
    
    private fun calculateTraveledDistance(positionIndex: Float): Double {
        if (routePoints.size < 2 || positionIndex <= 0f) return 0.0
        
        val segmentIndex = positionIndex.toInt()
        val fraction = positionIndex - segmentIndex
        
        var traveledDistance = 0.0
        
        // Add distances of completed segments
        for (i in 0 until segmentIndex.coerceAtMost(routePoints.size - 1)) {
            if (i < routePoints.size - 1) {
                traveledDistance += calculateDistance(routePoints[i], routePoints[i + 1])
            }
        }
        
        // Add partial distance of current segment
        if (segmentIndex < routePoints.size - 1 && fraction > 0) {
            val segmentDistance = calculateDistance(routePoints[segmentIndex], routePoints[segmentIndex + 1])
            traveledDistance += segmentDistance * fraction
        }
        
        return traveledDistance
    }
    
    private fun projectPointOnSegment(
        point: GeoPoint,
        segmentStart: GeoPoint,
        segmentEnd: GeoPoint
    ): ProjectionResult {
        val A = segmentStart
        val B = segmentEnd
        val P = point
        
        val AB_lat = B.latitude - A.latitude
        val AB_lng = B.longitude - A.longitude
        val AP_lat = P.latitude - A.latitude
        val AP_lng = P.longitude - A.longitude
        
        val AB_AB = AB_lat * AB_lat + AB_lng * AB_lng
        val AP_AB = AP_lat * AB_lat + AP_lng * AB_lng
        
        val t = if (AB_AB == 0.0) 0.0 else (AP_AB / AB_AB).coerceIn(0.0, 1.0)
        
        val projectedLat = A.latitude + t * AB_lat
        val projectedLng = A.longitude + t * AB_lng
        
        return ProjectionResult(
            position = GeoPoint(projectedLat, projectedLng),
            t = t.toFloat()
        )
    }
    
    private fun generateInstruction(positionIndex: Float): String {
        val progress = getCurrentProgress()
        val progressPercent = progress.progressPercentage
        
        return when {
            progressPercent < 1.0 -> "Ready to start navigation"
            progressPercent >= 99.0 -> "You have arrived at your destination"
            else -> {
                val remainingKm = progress.remainingDistance / 1000
                "Continue along route • ${String.format("%.1f", remainingKm)} km remaining"
            }
        }
    }
    
    // Data classes
    data class RouteProgress(
        val progressPercentage: Double,
        val traveledDistance: Double,
        val remainingDistance: Double,
        val instruction: String
    )
    
    private data class ProjectionResult(
        val position: GeoPoint,
        val t: Float
    )
}