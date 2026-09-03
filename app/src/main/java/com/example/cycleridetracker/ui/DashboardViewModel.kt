package com.example.cycleridetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.RideRepository
import com.example.cycleridetracker.data.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import kotlinx.coroutines.flow.combine
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(
        val recentRides: List<Ride>,
        val stats: DashboardStats,
        val useMetric: Boolean,
    ) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: RideRepository,
    appPrefs: AppPrefs,
) : ViewModel() {

    val useMetric: StateFlow<Boolean> = appPrefs.useMetric
    val hapticsEnabled: StateFlow<Boolean> = appPrefs.hapticsEnabled

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllRides(),
        appPrefs.weeklyGoal,
        appPrefs.useMetric
    ) { rides, _, useMetric ->
        val startOfWeek = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val weekRides = rides.filter { it.startTimeMillis >= startOfWeek.timeInMillis }
        
        var totalDistanceKm = weekRides.sumOf { it.distanceMeters.toDouble() } / 1000f
        if (!useMetric) {
            totalDistanceKm *= 0.621371f
        }
        
        val totalTimeMinutes = weekRides.sumOf { (it.endTimeMillis - it.startTimeMillis).toDouble() } / 60000f
        
        val streak = calculateStreak(rides)

        DashboardUiState.Success(
            recentRides = rides.take(10),
            stats = DashboardStats(
                distanceValue = "%.1f".format(totalDistanceKm),
                distanceUnit = if (useMetric) "km" else "mi",
                ridesCount = weekRides.size.toString(),
                timeMinutes = totalTimeMinutes.toInt().toString(),
                streakDays = streak
            ),
            useMetric = useMetric
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState.Loading
    )

    private fun calculateStreak(rides: List<Ride>): Int {
        if (rides.isEmpty()) return 0
        val dayMillis = 24 * 60 * 60 * 1000L
        val rideDates = rides.asSequence()
            .map { it.startTimeMillis / dayMillis }
            .distinct()
            .sortedDescending()
            .toList()
        
        var streak = 0
        val today = System.currentTimeMillis() / dayMillis
        
        var currentDay = if (rideDates.first() == today) today else today - 1
        
        for (date in rideDates) {
            if (date == currentDay) {
                streak++
                currentDay--
            } else if (date < currentDay) {
                break
            }
        }
        return streak
    }
}

data class DashboardStats(
    val distanceValue: String = "0.0",
    val distanceUnit: String = "km",
    val ridesCount: String = "0",
    val timeMinutes: String = "0",
    val streakDays: Int = 0
)
