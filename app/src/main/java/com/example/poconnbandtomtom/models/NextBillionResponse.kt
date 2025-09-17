package com.example.poconnbandtomtom.models

import com.google.gson.annotations.SerializedName

/**
 * Data models for NextBillion API response
 */
data class NextBillionResponse(
    @SerializedName("description")
    val description: String,

    @SerializedName("result")
    val result: NextBillionResult
)

data class NextBillionResult(
    @SerializedName("code")
    val code: Int,

    @SerializedName("summary")
    val summary: RouteSummary,

    @SerializedName("routes")
    val routes: List<NextBillionRoute>
)

data class RouteSummary(
    @SerializedName("cost")
    val cost: Int,

    @SerializedName("routes")
    val routeCount: Int,

    @SerializedName("unassigned")
    val unassigned: Int,

    @SerializedName("setup")
    val setup: Int,

    @SerializedName("service")
    val service: Int,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("waiting_time")
    val waitingTime: Int,

    @SerializedName("priority")
    val priority: Int,

    @SerializedName("delivery")
    val delivery: List<Int>,

    @SerializedName("pickup")
    val pickup: List<Int>,

    @SerializedName("distance")
    val distance: Int
)

data class NextBillionRoute(
    @SerializedName("vehicle")
    val vehicle: String,

    @SerializedName("cost")
    val cost: Int,

    @SerializedName("steps")
    val steps: List<RouteStep>,

    @SerializedName("distance")
    val distance: Int,

    @SerializedName("geometry")
    val geometry: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("internal_id")
    val internalId: Long
)

data class RouteStep(
    @SerializedName("type")
    val type: String, // "start", "job", "end"

    @SerializedName("arrival")
    val arrival: Long,

    @SerializedName("duration")
    val duration: Int,

    @SerializedName("service")
    val service: Int,

    @SerializedName("waiting_time")
    val waitingTime: Int,

    @SerializedName("location")
    val location: List<Double>, // [latitude, longitude]

    @SerializedName("location_index")
    val locationIndex: Int,

    @SerializedName("load")
    val load: List<Int>,

    @SerializedName("distance")
    val distance: Int,

    @SerializedName("snapped_location")
    val snappedLocation: List<Double>? = null,

    @SerializedName("depot")
    val depot: String? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("job")
    val job: String? = null
)