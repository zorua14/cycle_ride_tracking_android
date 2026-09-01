package com.example.cycleridetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.RideRepository
import com.example.cycleridetracker.data.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: RideRepository,
    private val appPrefs: AppPrefs
) : ViewModel() {

    private val _selectedRange = MutableStateFlow("Week")
    val selectedRange: StateFlow<String> = _selectedRange

    fun selectRange(range: String) {
        _selectedRange.value = range
    }

    val insightsStats = combine(
        repository.getAllRides(),
        _selectedRange,
        appPrefs.weeklyGoal,
        appPrefs.monthlyGoal,
        appPrefs.useMetric
    ) { rides, range, weeklyGoal, monthlyGoal, useMetric ->
        val filteredRides = filterRidesByRange(rides, range)
        
        var totalDistanceKm = filteredRides.sumOf { it.distanceMeters.toDouble() } / 1000f
        if (!useMetric) {
            totalDistanceKm *= 0.621371f
        }

        val avgSpeed = if (filteredRides.isNotEmpty()) filteredRides.map { it.averageSpeedKmh }.average() else 0.0
        val maxSpeed = if (filteredRides.isNotEmpty()) filteredRides.maxOf { it.maxSpeedKmh } else 0.0f
        
        val displayAvgSpeed = if (useMetric) avgSpeed else avgSpeed * 0.621371f
        val displayMaxSpeed = if (useMetric) maxSpeed else maxSpeed * 0.621371f

        val totalDurationMillis = filteredRides.sumOf { (it.endTimeMillis - it.startTimeMillis).toDouble() }

        val goal = if (range == "Week") weeklyGoal else monthlyGoal
        val displayGoal = if (useMetric) goal else goal * 0.621371f

        InsightsStats(
            totalDistanceKmValue = "%.1f".format(totalDistanceKm),
            distanceUnit = if (useMetric) "km" else "mi",
            rideCount = filteredRides.size,
            avgSpeedValue = "%.1f".format(displayAvgSpeed),
            speedUnit = if (useMetric) "km/h" else "mph",
            maxSpeedValue = "%.1f".format(displayMaxSpeed),
            totalDuration = formatDuration(totalDurationMillis.toLong()),
            chartData = prepareChartData(filteredRides, range, useMetric),
            currentGoal = displayGoal
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsStats()
    )

    private fun filterRidesByRange(rides: List<Ride>, range: String): List<Ride> {
        val startTime = when (range) {
            "Week" -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            "Month" -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis
            }
            else -> 0L
        }
        return rides.filter { it.startTimeMillis >= startTime }
    }

    private fun prepareChartData(rides: List<Ride>, range: String, useMetric: Boolean): List<Float> {
        return if (range == "Week") {
            val data = MutableList(7) { 0f }
            val cal = Calendar.getInstance()
            rides.forEach { ride ->
                cal.timeInMillis = ride.startTimeMillis
                val day = (cal.get(Calendar.DAY_OF_WEEK) - cal.firstDayOfWeek + 7) % 7
                var distance = ride.distanceMeters / 1000f
                if (!useMetric) {
                    distance *= 0.621371f
                }
                data[day] += distance
            }
            data
        } else {
            List(7) { 0f }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

data class InsightsStats(
    val totalDistanceKmValue: String = "0.0",
    val distanceUnit: String = "km",
    val rideCount: Int = 0,
    val avgSpeedValue: String = "0.0",
    val speedUnit: String = "km/h",
    val maxSpeedValue: String = "0.0",
    val totalDuration: String = "0m",
    val chartData: List<Float> = emptyList(),
    val currentGoal: Float = 50f
)
