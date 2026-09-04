package com.example.cycleridetracker.ui

import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

sealed interface InsightsUiState {
    object Loading : InsightsUiState
    data class Success(
        val stats: InsightsStats,
        val selectedRange: String,
    ) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    repository: RideRepository,
    appPrefs: AppPrefs,
) : ViewModel() {

    private val _selectedRange = MutableStateFlow("Week")

    fun selectRange(range: String) {
        _selectedRange.value = range
    }

    val uiState: StateFlow<InsightsUiState> = combine(
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

        val avgSpeed = if (filteredRides.isNotEmpty()) {
            filteredRides.asSequence().map { it.averageSpeedKmh }.average()
        } else 0.0
        val maxSpeed = if (filteredRides.isNotEmpty()) filteredRides.maxOf { it.maxSpeedKmh } else 0.0f
        
        val displayAvgSpeed = if (useMetric) avgSpeed else avgSpeed * 0.621371f
        val displayMaxSpeed = if (useMetric) maxSpeed else maxSpeed * 0.621371f

        val totalDurationMillis = filteredRides.sumOf { (it.endTimeMillis - it.startTimeMillis).toDouble() }

        val goal = if (range == "Week") weeklyGoal else monthlyGoal
        val displayGoal = if (useMetric) goal else goal * 0.621371f

        val chartResult = prepareChartData(filteredRides, range, useMetric)

        InsightsUiState.Success(
            stats = InsightsStats(
                totalDistanceKmValue = "%.1f".format(totalDistanceKm),
                distanceUnit = if (useMetric) "km" else "mi",
                rideCount = filteredRides.size,
                avgSpeedValue = "%.1f".format(displayAvgSpeed),
                speedUnit = if (useMetric) "km/h" else "mph",
                maxSpeedValue = "%.1f".format(displayMaxSpeed),
                totalDuration = formatDuration(totalDurationMillis.toLong()),
                chartData = chartResult.data,
                chartLabels = chartResult.labels,
                currentGoal = displayGoal,
                showGoal = range != "All Time"
            ),
            selectedRange = range
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState.Loading
    )

    private fun filterRidesByRange(rides: List<Ride>, range: String): List<Ride> {
        val startTime = when (range) {
            "Week" -> {
                Calendar.getInstance().apply {
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = ((dayOfWeek - Calendar.MONDAY) + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            "Month" -> {
                Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
            else -> 0L
        }
        return if (range == "All Time") rides else rides.filter { it.startTimeMillis >= startTime }
    }

    private data class ChartDataResult(val data: List<Float>, val labels: List<String>)

    private fun prepareChartData(rides: List<Ride>, range: String, useMetric: Boolean): ChartDataResult {
        val cal = Calendar.getInstance()
        val conversion = if (useMetric) 1f else 0.621371f

        return when (range) {
            "Week" -> {
                val data = MutableList(7) { 0f }
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                rides.forEach { ride ->
                    cal.timeInMillis = ride.startTimeMillis
                    val day = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7
                    data[day] += (ride.distanceMeters / 1000f) * conversion
                }
                ChartDataResult(data, labels)
            }
            "Month" -> {
                val data = MutableList(5) { 0f }
                val labels = listOf("W1", "W2", "W3", "W4", "W5")
                rides.forEach { ride ->
                    cal.timeInMillis = ride.startTimeMillis
                    val week = (cal.get(Calendar.DAY_OF_MONTH) - 1) / 7
                    if (week in 0..4) {
                        data[week] += (ride.distanceMeters / 1000f) * conversion
                    }
                }
                ChartDataResult(data, labels)
            }
            "All Time" -> {
                if (rides.isEmpty()) return ChartDataResult(emptyList(), emptyList())
                
                val sortedRides = rides.sortedBy { it.startTimeMillis }
                val firstRideTime = sortedRides.first().startTimeMillis
                val lastRideTime = Calendar.getInstance().timeInMillis
                
                val startCal = Calendar.getInstance().apply { 
                    timeInMillis = firstRideTime
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val endCal = Calendar.getInstance().apply { 
                    timeInMillis = lastRideTime
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                
                val results = mutableListOf<Float>()
                val labels = mutableListOf<String>()
                val monthFormat = java.text.SimpleDateFormat("MMM ''yy", java.util.Locale.getDefault())
                
                val tempCal = Calendar.getInstance().apply { timeInMillis = startCal.timeInMillis }
                while (tempCal.timeInMillis <= endCal.timeInMillis) {
                    val currentMonth = tempCal.get(Calendar.MONTH)
                    val currentYear = tempCal.get(Calendar.YEAR)
                    
                    val monthDistance = rides.asSequence().filter {
                        cal.timeInMillis = it.startTimeMillis
                        cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                    }.sumOf { it.distanceMeters.toDouble() }.toFloat() / 1000f * conversion
                    
                    results.add(monthDistance)
                    labels.add(monthFormat.format(tempCal.time))
                    
                    tempCal.add(Calendar.MONTH, 1)
                }
                ChartDataResult(results, labels)
            }
            else -> ChartDataResult(emptyList(), emptyList())
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = millis / 3600000
        val minutes = (millis % 3600000) / 60000
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

@Immutable
data class InsightsStats(
    val totalDistanceKmValue: String = "0.0",
    val distanceUnit: String = "km",
    val rideCount: Int = 0,
    val avgSpeedValue: String = "0.0",
    val speedUnit: String = "km/h",
    val maxSpeedValue: String = "0.0",
    val totalDuration: String = "0m",
    val chartData: List<Float> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val currentGoal: Float = 50f,
    val showGoal: Boolean = true
)
