package com.example.cycleridetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cycleridetracker.data.AppPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appPrefs: AppPrefs
) : ViewModel() {

    val theme: StateFlow<String> = appPrefs.theme
    val useMetric: StateFlow<Boolean> = appPrefs.useMetric
    val hapticsEnabled: StateFlow<Boolean> = appPrefs.hapticsEnabled
    val samplingRate: StateFlow<Long> = appPrefs.samplingRate
    
    private val kmToMi = 0.621371f

    val weeklyGoal: StateFlow<Float> = combine(appPrefs.weeklyGoal, appPrefs.useMetric) { goal, metric ->
        if (metric) goal else goal * kmToMi
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = appPrefs.weeklyGoal.value
    )

    val monthlyGoal: StateFlow<Float> = combine(appPrefs.monthlyGoal, appPrefs.useMetric) { goal, metric ->
        if (metric) goal else goal * kmToMi
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = appPrefs.monthlyGoal.value
    )

    fun setTheme(theme: String) {
        appPrefs.setTheme(theme)
    }

    fun setUseMetric(enabled: Boolean) {
        appPrefs.setUseMetric(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        appPrefs.setHapticsEnabled(enabled)
    }

    fun setSamplingRate(rateMillis: Long) {
        appPrefs.setSamplingRate(rateMillis)
    }

    fun setWeeklyGoal(goal: Float) {
        val baseGoal = if (appPrefs.useMetric.value) goal else goal / kmToMi
        appPrefs.setWeeklyGoal(baseGoal)
    }

    fun setMonthlyGoal(goal: Float) {
        val baseGoal = if (appPrefs.useMetric.value) goal else goal / kmToMi
        appPrefs.setMonthlyGoal(baseGoal)
    }
}
