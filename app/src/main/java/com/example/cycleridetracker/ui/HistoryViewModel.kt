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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class SortOption {
    DATE, DISTANCE, DURATION, SPEED
}

enum class SortOrder {
    ASCENDING, DESCENDING
}

enum class FilterOption {
    ALL, WEEK, MONTH, YEAR
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: RideRepository,
    private val appPrefs: AppPrefs
) : ViewModel() {

    val useMetric: StateFlow<Boolean> = appPrefs.useMetric
    val hapticsEnabled: StateFlow<Boolean> = appPrefs.hapticsEnabled

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _filterOption = MutableStateFlow(FilterOption.ALL)
    val filterOption: StateFlow<FilterOption> = _filterOption

    val allRides: StateFlow<List<Ride>> = combine(
        repository.getAllRides(),
        _searchQuery,
        _sortOption,
        _sortOrder,
        _filterOption
    ) { rides, query, sort, order, filter ->
        rides.asSequence()
            .filter { ride ->
                ride.title.contains(query, ignoreCase = true) ||
                ride.notes.contains(query, ignoreCase = true)
            }
            .filter { ride ->
                when (filter) {
                    FilterOption.ALL -> true
                    FilterOption.WEEK -> {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        ride.startTimeMillis >= cal.timeInMillis
                    }
                    FilterOption.MONTH -> {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        ride.startTimeMillis >= cal.timeInMillis
                    }
                    FilterOption.YEAR -> {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        ride.startTimeMillis >= cal.timeInMillis
                    }
                }
            }
            .sortedWith { r1, r2 ->
                val comparison = when (sort) {
                    SortOption.DATE -> r1.startTimeMillis.compareTo(r2.startTimeMillis)
                    SortOption.DISTANCE -> r1.distanceMeters.compareTo(r2.distanceMeters)
                    SortOption.DURATION -> (r1.endTimeMillis - r1.startTimeMillis).compareTo(r2.endTimeMillis - r2.startTimeMillis)
                    SortOption.SPEED -> r1.averageSpeedKmh.compareTo(r2.averageSpeedKmh)
                }
                if (order == SortOrder.ASCENDING) comparison else -comparison
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOptionChange(option: SortOption) {
        _sortOption.value = option
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    fun onFilterOptionChange(option: FilterOption) {
        _filterOption.value = option
    }
}
