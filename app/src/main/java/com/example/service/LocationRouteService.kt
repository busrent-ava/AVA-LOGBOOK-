package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.RoutePoint
import com.example.data.model.TripRouteState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

object RouteTrackingManager {
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private val _routeState = MutableStateFlow(TripRouteState())
    val routeState: StateFlow<TripRouteState> = _routeState.asStateFlow()

    private var fusedClient: FusedLocationProviderClient? = null
    private var locationManager: LocationManager? = null
    private var locationCallback: LocationCallback? = null
    private var locationListener: LocationListener? = null

    private var timerJob: Job? = null
    private var simulationJob: Job? = null

    private val _isSimulationActive = MutableStateFlow(false)
    val isSimulationActive: StateFlow<Boolean> = _isSimulationActive.asStateFlow()

    fun initialize(context: Context) {
        if (fusedClient == null) {
            fusedClient = LocationServices.getFusedLocationProviderClient(context)
            locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(busNumber: String = "") {
        stopSimulation()
        val current = _routeState.value
        _routeState.value = current.copy(
            isTracking = true,
            isPaused = false,
            busNumber = busNumber.ifEmpty { current.busNumber },
            startTimeMillis = if (current.startTimeMillis == 0L) System.currentTimeMillis() else current.startTimeMillis
        )

        startTimer()

        // Setup Fused Location Provider
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { handleNewLocation(it) }
                }
            }

            fusedClient?.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        } catch (_: Exception) {
            // Fallback to LocationManager
            startLocationManagerFallback()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationManagerFallback() {
        try {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    handleNewLocation(location)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                2000L,
                2f,
                locationListener!!,
                Looper.getMainLooper()
            )
        } catch (_: Exception) {}
    }

    fun pauseTracking() {
        _routeState.value = _routeState.value.copy(isPaused = true)
        timerJob?.cancel()
    }

    fun resumeTracking() {
        _routeState.value = _routeState.value.copy(isPaused = false)
        startTimer()
    }

    fun stopTracking() {
        _routeState.value = _routeState.value.copy(isTracking = false, isPaused = false)
        timerJob?.cancel()
        stopSimulation()

        try {
            locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
            locationListener?.let { locationManager?.removeUpdates(it) }
        } catch (_: Exception) {}
    }

    fun clearRoute() {
        stopTracking()
        _routeState.value = TripRouteState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val state = _routeState.value
                if (state.isTracking && !state.isPaused) {
                    _routeState.value = state.copy(durationSeconds = state.durationSeconds + 1)
                }
            }
        }
    }

    fun handleNewLocation(location: Location) {
        val state = _routeState.value
        if (!state.isTracking || state.isPaused) return

        val point = RoutePoint(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            speedMps = if (location.hasSpeed()) location.speed else 0f,
            bearing = if (location.hasBearing()) location.bearing else 0f,
            accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
            timestamp = location.time
        )

        val previous = state.currentPoint
        var addedDistance = 0.0

        if (previous != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                previous.latitude, previous.longitude,
                point.latitude, point.longitude,
                results
            )
            // Filter jitter: only count if greater than 1.5m and under 200m per interval
            if (results[0] in 1.5f..200f) {
                addedDistance = results[0].toDouble()
            }
        }

        val updatedPoints = state.pathPoints + point
        _routeState.value = state.copy(
            currentPoint = point,
            pathPoints = updatedPoints,
            totalDistanceMeters = state.totalDistanceMeters + addedDistance
        )
    }

    /**
     * Simulation mode for emulators or testing when physical vehicle driving is unavailable.
     * Moves smoothly along realistic transit coordinates in Dubai/City center.
     */
    fun toggleSimulation(busNumber: String = "BUS-01") {
        if (_isSimulationActive.value) {
            stopSimulation()
        } else {
            startSimulation(busNumber)
        }
    }

    fun startSimulation(busNumber: String = "BUS-01") {
        stopSimulation()
        _isSimulationActive.value = true

        val startLat = 25.20485
        val startLng = 55.27078

        clearRoute()
        startTracking(busNumber)

        simulationJob = scope.launch {
            var step = 0
            var currentLat = startLat
            var currentLng = startLng
            var heading = 45f

            while (isActive && _isSimulationActive.value) {
                // Generate next waypoint along a gentle curved route
                val dLat = 0.00035 * cos(Math.toRadians(heading.toDouble()))
                val dLng = 0.00035 * sin(Math.toRadians(heading.toDouble()))
                currentLat += dLat
                currentLng += dLng
                heading = (heading + (if (step % 12 == 0) 25f else -5f)) % 360f

                val simLoc = Location("simulated").apply {
                    latitude = currentLat
                    longitude = currentLng
                    altitude = 12.0 + (step % 5)
                    speed = 11.5f + (step % 4) * 1.5f // ~42 to ~60 km/h
                    bearing = heading
                    accuracy = 4f
                    time = System.currentTimeMillis()
                }

                handleNewLocation(simLoc)
                step++
                delay(1500L)
            }
        }
    }

    fun stopSimulation() {
        _isSimulationActive.value = false
        simulationJob?.cancel()
        simulationJob = null
    }
}

class LocationRouteService : Service() {

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): LocationRouteService = this@LocationRouteService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        RouteTrackingManager.initialize(this)
        createServiceNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START -> {
                val bus = intent.getStringExtra(EXTRA_BUS) ?: ""
                startForeground(NOTIFICATION_ID, buildForegroundNotification("Tracking active route: $bus"))
                RouteTrackingManager.startTracking(bus)
            }
            ACTION_STOP -> {
                RouteTrackingManager.stopTracking()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FleetOps Route Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground service tracking vehicle route path"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FleetOps Live Route Tracking")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "fleetops_live_route_channel"
        const val NOTIFICATION_ID = 2002
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val EXTRA_BUS = "EXTRA_BUS_NUMBER"

        fun start(context: Context, busNumber: String) {
            val intent = Intent(context, LocationRouteService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_BUS, busNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationRouteService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
