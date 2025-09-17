package com.example.poconnbandtomtom.utils


import com.tomtom.sdk.location.GeoPoint
import android.util.Log

object PolylineDecoder {
    private const val TAG = "PolylineDecoder"
    
    /**
     * Decodes a polyline string into a list of GeoPoints.
     * Handles both 5 and 6 decimal precision levels.
     */
    fun decodePolyline(encoded: String?): List<GeoPoint> {
        if (encoded.isNullOrEmpty()) {
            Log.w(TAG, "Empty or null polyline string")
            return emptyList()
        }
        
        return try {
            decodePolyline5(encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode polyline", e)
            emptyList()
        }
    }
    
    /**
     * Decodes a polyline string with 5 decimal precision.
     * Standard polyline algorithm used by NextBillion and Google.
     */
    private fun decodePolyline5(encoded: String): List<GeoPoint> {
        val poly = mutableListOf<GeoPoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            
            // Decode latitude
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            
            shift = 0
            result = 0
            
            // Decode longitude
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            
            poly.add(GeoPoint(lat / 1E5, lng / 1E5))
        }
        
        Log.d(TAG, "Decoded ${poly.size} points from polyline")
        return poly
    }
}