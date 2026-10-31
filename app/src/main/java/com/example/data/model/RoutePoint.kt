package com.example.data.model

data class RoutePoint(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val speedMps: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val speedKmh: Float get() = speedMps * 3.6f
}

data class TripRouteState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val currentPoint: RoutePoint? = null,
    val pathPoints: List<RoutePoint> = emptyList(),
    val totalDistanceMeters: Double = 0.0,
    val startTimeMillis: Long = 0L,
    val durationSeconds: Long = 0L,
    val busNumber: String = ""
) {
    val totalDistanceKm: Double get() = totalDistanceMeters / 1000.0

    val currentSpeedKmh: Float get() = currentPoint?.speedKmh ?: 0f

    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
}
