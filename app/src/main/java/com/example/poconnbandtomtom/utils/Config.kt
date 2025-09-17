package com.example.poconnbandtomtom.utils

/**
 * Configuration constants for the application
 */
object Config {

    /**
     * TomTom API Key
     * Replace with your actual TomTom API key from https://developer.tomtom.com/
     */
    const val TOMTOM_API_KEY = "3dWb0j1jH79ZLj8xmMbOUoOx17AED32v"

    /**
     * Default map configuration
     */
    object Map {
        const val DEFAULT_ZOOM_LEVEL = 10.0
        const val ROUTE_LINE_WIDTH = 8.0
        const val MARKER_ANCHOR_POINT_X = 0.5f
        const val MARKER_ANCHOR_POINT_Y = 1.0f
        const val MAP_PADDING = 100
    }

    /**
     * Navigation configuration
     */
    object Navigation {
        const val LOCATION_UPDATE_INTERVAL_MS = 2000L
        const val LOCATION_UPDATE_DISTANCE_M = 5f
        const val STEP_PROXIMITY_THRESHOLD_M = 50.0
        const val ROUTE_PROXIMITY_THRESHOLD_M = 100.0
        const val SEARCH_RANGE_POINTS = 50
    }

    /**
     * Route colors for visualization
     */
    object RouteColors {
        val COLORS = listOf(
            0xFF2196F3, // Blue
            0xFFF44336, // Red
            0xFF4CAF50, // Green
            0xFFE91E63, // Pink
            0xFFFF9800, // Orange
            0xFF9C27B0, // Purple
            0xFF00BCD4, // Cyan
            0xFF795548  // Brown
        )

        const val HIGHLIGHTED_ALPHA = 255
        const val DIMMED_ALPHA = 100
    }

    /**
     * UI configuration
     */
    object UI {
        const val NAVIGATION_PANEL_ANIMATION_DURATION_MS = 300L
        const val TOAST_DURATION_MS = 3000L
        const val PROGRESS_UPDATE_INTERVAL_MS = 1000L
    }
}