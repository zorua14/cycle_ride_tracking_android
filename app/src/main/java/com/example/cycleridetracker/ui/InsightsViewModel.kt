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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Locale

sealed interface InsightsUiState {
    object Loading : InsightsUiState
    data class Success(
        val stats: InsightsStats,
        val selectedRange: String,
    ) : InsightsUiState
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val repository: RideRepository,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    private val _selectedRange = MutableStateFlow("Week")
    private val _currentMonthCalendar = MutableStateFlow(Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        set(Calendar.DAY_OF_MONTH, 1)
    })

    fun selectRange(range: String) {
        _selectedRange.value = range
    }

    fun nextMonth() {
        val next = (_currentMonthCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        _currentMonthCalendar.value = next
    }

    fun previousMonth() {
        val prev = (_currentMonthCalendar.value.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
        }
        _currentMonthCalendar.value = prev
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<InsightsUiState> = combine(
        combine(_selectedRange, _currentMonthCalendar) { range, monthCal -> range to monthCal },
        appPrefs.useMetric,
        appPrefs.weeklyGoal,
        appPrefs.monthlyGoal,
        repository.getOldestRideTimestamp()
    ) { (range, monthCal), useMetric, weeklyGoal, monthlyGoal, oldestTimestamp ->
        val rangeInfo = getRangeInfo(range, monthCal)
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        
        val canNext = monthCal.before(today)
        val canPrev = oldestTimestamp != null && oldestTimestamp < monthCal.timeInMillis

        NavigationState(
            rangeInfo = rangeInfo,
            useMetric = useMetric,
            goal = if (range == "Week") weeklyGoal else monthlyGoal,
            canNext = canNext,
            canPrev = canPrev
        )
    }.flatMapLatest { nav ->
        val ridesFlow = repository.getRidesByTimeRange(nav.rangeInfo.start, nav.rangeInfo.end)
        
        ridesFlow.map { filteredRides ->
            val useMetric = nav.useMetric
            
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
            val displayGoal = if (useMetric) nav.goal else nav.goal * 0.621371f

            val chartResult = prepareChartData(filteredRides, nav.rangeInfo.range, useMetric, nav.rangeInfo.startCalendar)

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
                    showGoal = true,
                    displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(nav.rangeInfo.startCalendar.time),
                    canNavigateNext = nav.canNext,
                    canNavigatePrevious = nav.canPrev
                ),
                selectedRange = nav.rangeInfo.range
            )
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsUiState.Loading
    )

    private data class NavigationState(
        val rangeInfo: RangeInfo,
        val useMetric: Boolean,
        val goal: Float,
        val canNext: Boolean,
        val canPrev: Boolean
    )

    private data class RangeInfo(
        val range: String,
        val start: Long,
        val end: Long,
        val startCalendar: Calendar
    )

    private fun getRangeInfo(range: String, monthCal: Calendar): RangeInfo {
        val cal = Calendar.getInstance()
        return when (range) {
            "Week" -> {
                val start = cal.apply {
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val daysToSubtract = ((dayOfWeek - Calendar.MONDAY) + 7) % 7
                    add(Calendar.DAY_OF_YEAR, -daysToSubtract)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (cal.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, 7)
                }.timeInMillis
                RangeInfo(range, start, end, cal)
            }
            else -> {
                val start = (monthCal.clone() as Calendar).timeInMillis
                val end = (monthCal.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1)
                }.timeInMillis
                RangeInfo("Month", start, end, monthCal)
            }
        }
    }

    private data class ChartDataResult(val data: List<Float>, val labels: List<String>)

    private fun prepareChartData(rides: List<Ride>, range: String, useMetric: Boolean, startCal: Calendar): ChartDataResult {
        val cal = Calendar.getInstance()
        val conversion = if (useMetric) 1f else 0.621371f

        return when (range) {
            "Week" -> {
                val data = MutableList(7) { 0f }
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                rides.forEach { ride ->
                    cal.timeInMillis = ride.startTimeMillis
                    val day = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY) + 7) % 7
                    if (day in 0..6) {
                        data[day] += (ride.distanceMeters / 1000f) * conversion
                    }
                }
                ChartDataResult(data, labels)
            }
            else -> {
                val daysInMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val data = MutableList(daysInMonth) { 0f }
                val labels = (1..daysInMonth).map { it.toString() }
                rides.forEach { ride ->
                    cal.timeInMillis = ride.startTimeMillis
                    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                    if (dayOfMonth in 1..daysInMonth) {
                        data[dayOfMonth - 1] += (ride.distanceMeters / 1000f) * conversion
                    }
                }
                ChartDataResult(data, labels)
            }
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
    val showGoal: Boolean = true,
    val displayMonth: String = "",
    val canNavigateNext: Boolean = false,
    val canNavigatePrevious: Boolean = false
)
