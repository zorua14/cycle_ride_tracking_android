package com.example.cycleridetracker.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.cycleridetracker.MainActivity
import com.example.cycleridetracker.R
import com.example.cycleridetracker.data.ActiveRideState
import com.example.cycleridetracker.data.LatLngPoint
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.RideRepository
import com.example.cycleridetracker.data.AppPrefs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RideForegroundService : Service() {

    @Inject
    lateinit var repository: RideRepository

    @Inject
    lateinit var appPrefs: AppPrefs

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    
    private var lastPersistenceTime = 0L
    
    companion object {
        const val CHANNEL_ID = "ride_tracking_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
            ACTION_PAUSE -> pauseTracking()
            ACTION_RESUME -> resumeTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        val currentState = repository.activeRideState.value
        if (currentState is ActiveRideState.Tracking) {
            // Already tracking or recovered from DB, just ensure service is running
            startForeground(NOTIFICATION_ID, createNotification("Ride tracking active"))
            requestLocationUpdates()
            startTimer()
            return
        }

        val startTime = System.currentTimeMillis()
        
        serviceScope.launch {
            val rideId = repository.insertRide(
                Ride(
                    title = "Cycling Ride",
                    startTimeMillis = startTime,
                    endTimeMillis = startTime,
                    distanceMeters = 0f,
                    averageSpeedKmh = 0f,
                    maxSpeedKmh = 0f,
                    pathPoints = emptyList(),
                    isFinished = false
                )
            ).toInt()
            
            repository.updateActiveRideState(
                ActiveRideState.Tracking(
                    rideId = rideId,
                    startTimeMillis = startTime
                )
            )
        }
        
        startForeground(NOTIFICATION_ID, createNotification("Ride tracking active"))
        requestLocationUpdates()
        startTimer()
    }

    private fun stopTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        timerJob?.cancel()
        
        val currentState = repository.activeRideState.value
        if (currentState is ActiveRideState.Tracking) {
            // Set to Idle immediately to block any pending periodic syncs
            repository.updateActiveRideState(ActiveRideState.Idle)
            
            serviceScope.launch {
                val ride = Ride(
                    id = currentState.rideId,
                    title = "Cycling Ride",
                    startTimeMillis = currentState.startTimeMillis,
                    endTimeMillis = System.currentTimeMillis(),
                    distanceMeters = currentState.distanceMeters,
                    averageSpeedKmh = if (currentState.durationMillis > 0) 
                        (currentState.distanceMeters / 1000f) / (currentState.durationMillis / 3600000f) else 0f,
                    maxSpeedKmh = currentState.maxSpeedKmh,
                    pathPoints = currentState.pathPoints,
                    isFinished = true
                )
                repository.updateRide(ride)
            }
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pauseTracking() {
        val currentState = repository.activeRideState.value
        if (currentState is ActiveRideState.Tracking) {
            repository.updateActiveRideState(currentState.copy(isPaused = true))
            timerJob?.cancel()
            updateNotification("Ride paused")
        }
    }

    private fun resumeTracking() {
        val currentState = repository.activeRideState.value
        if (currentState is ActiveRideState.Tracking) {
            repository.updateActiveRideState(currentState.copy(isPaused = false))
            startTimer()
            updateNotification("Ride tracking active")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        val rate = appPrefs.samplingRate.value
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, rate)
            .setMinUpdateIntervalMillis(rate / 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val currentState = repository.activeRideState.value
                if (currentState is ActiveRideState.Tracking && !currentState.isPaused) {
                    result.lastLocation?.let { location ->
                        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
                        updateMetrics(location, currentState)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    private fun updateMetrics(newLocation: Location, currentState: ActiveRideState.Tracking) {
        val newPoint = LatLngPoint(newLocation.latitude, newLocation.longitude)
        val updatedPoints = currentState.pathPoints + newPoint
        
        var addedDistance = 0f
        if (currentState.pathPoints.isNotEmpty()) {
            val lastPoint = currentState.pathPoints.last()
            val results = FloatArray(1)
            Location.distanceBetween(
                lastPoint.latitude, lastPoint.longitude,
                newPoint.latitude, newPoint.longitude,
                results
            )
            addedDistance = results[0]
        }

        val totalDistance = currentState.distanceMeters + addedDistance
        val currentSpeed = newLocation.speed * 3.6f // convert m/s to km/h
        val maxSpeed = maxOf(currentState.maxSpeedKmh, currentSpeed)

        repository.updateActiveRideState(
            currentState.copy(
                distanceMeters = totalDistance,
                currentSpeedKmh = currentSpeed,
                maxSpeedKmh = maxSpeed,
                pathPoints = updatedPoints
            )
        )

        // Periodic Persistence: Save to DB based on user-defined time interval
        val currentTime = System.currentTimeMillis()
        val interval = appPrefs.persistenceInterval.value
        
        if (lastPersistenceTime == 0L) {
            lastPersistenceTime = currentTime
        }

        if (currentTime - lastPersistenceTime >= interval) {
            lastPersistenceTime = currentTime
            serviceScope.launch {
                // Safety check: Don't persist if the ride was just finished or paused
                if (repository.activeRideState.value !is ActiveRideState.Tracking) return@launch
                
                val ride = Ride(
                    id = currentState.rideId,
                    title = "Cycling Ride",
                    startTimeMillis = currentState.startTimeMillis,
                    endTimeMillis = currentTime,
                    distanceMeters = totalDistance,
                    averageSpeedKmh = if (currentState.durationMillis > 0) 
                        (totalDistance / 1000f) / (currentState.durationMillis / 3600000f) else 0f,
                    maxSpeedKmh = maxSpeed,
                    pathPoints = updatedPoints,
                    isFinished = false
                )
                repository.updateRide(ride)
            }
        }
        
        updateNotification("Distance: %.2f km".format(totalDistance / 1000f))
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (true) {
                val currentState = repository.activeRideState.value
                if (currentState is ActiveRideState.Tracking && !currentState.isPaused) {
                    val duration = System.currentTimeMillis() - currentState.startTimeMillis
                    repository.updateActiveRideState(currentState.copy(durationMillis = duration))
                }
                delay(1000)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ride Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Cycling Tracker")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default for now
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        fusedLocationClient.removeLocationUpdates(locationCallback ?: object : LocationCallback() {})
    }
}
