package com.example.cycleridetracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val rideDao: RideDao
) {
    private val repositoryScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    init {
        repositoryScope.launch {
            val activeRide = rideDao.getActiveRide()
            activeRide?.let {
                val points = rideDao.getPathPointsForRide(it.id).map { p -> LatLngPoint(p.latitude, p.longitude) }
                _activeRideState.value = ActiveRideState.Tracking(
                    rideId = it.id,
                    startTimeMillis = it.startTimeMillis,
                    durationMillis = System.currentTimeMillis() - it.startTimeMillis,
                    distanceMeters = it.distanceMeters,
                    maxSpeedKmh = it.maxSpeedKmh,
                    pathPoints = points
                )
            }
        }
    }

    fun getAllRides(): Flow<List<Ride>> = rideDao.getAllRides()

    fun getRidesByTimeRange(start: Long, end: Long): Flow<List<Ride>> = rideDao.getRidesByTimeRange(start, end)

    fun getOldestRideTimestamp(): Flow<Long?> = rideDao.getOldestRideTimestamp()

    suspend fun getRideById(id: Int): Ride? {
        val ride = rideDao.getRideById(id) ?: return null
        val points = rideDao.getPathPointsForRide(id).map { LatLngPoint(it.latitude, it.longitude) }
        return ride.copy(pathPoints = points)
    }

    suspend fun insertRide(ride: Ride): Long = rideDao.insertRide(ride)

    suspend fun updateRide(ride: Ride) = rideDao.updateRide(ride)

    suspend fun deleteRide(id: Int) = rideDao.deleteRide(id)

    suspend fun getActiveRideFromDb(): Ride? {
        val ride = rideDao.getActiveRide() ?: return null
        val points = rideDao.getPathPointsForRide(ride.id).map { LatLngPoint(it.latitude, it.longitude) }
        return ride.copy(pathPoints = points)
    }

    suspend fun insertPathPoints(rideId: Int, points: List<LatLngPoint>) {
        val dbPoints = points.map { point ->
            RidePathPoint(
                rideId = rideId,
                latitude = point.latitude,
                longitude = point.longitude,
                timestamp = System.currentTimeMillis()
            )
        }
        rideDao.insertPathPoints(dbPoints)
    }

    suspend fun deleteUnfinishedRides() = rideDao.deleteUnfinishedRides()

    // Active Ride State Management
    private val _activeRideState = MutableStateFlow<ActiveRideState>(ActiveRideState.Idle)
    val activeRideState: StateFlow<ActiveRideState> = _activeRideState

    fun updateActiveRideState(state: ActiveRideState) {
        _activeRideState.value = state
    }
}

sealed class ActiveRideState {
    object Idle : ActiveRideState()
    data class Tracking(
        val rideId: Int = 0,
        val startTimeMillis: Long,
        val durationMillis: Long = 0,
        val distanceMeters: Float = 0f,
        val currentSpeedKmh: Float = 0f,
        val maxSpeedKmh: Float = 0f,
        val pathPoints: List<LatLngPoint> = emptyList(),
        val isPaused: Boolean = false
    ) : ActiveRideState()
}
