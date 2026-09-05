package com.example.cycleridetracker.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPrefs @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("cycle_ride_tracker_prefs", Context.MODE_PRIVATE)

    private val _theme = MutableStateFlow(prefs.getString("app_theme_mode", "System") ?: "System")
    val theme: StateFlow<String> = _theme

    private val _useMetric = MutableStateFlow(prefs.getBoolean("use_metric", true))
    val useMetric: StateFlow<Boolean> = _useMetric

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled

    private val _samplingRate = MutableStateFlow(prefs.getLong("sampling_rate", 1000L))
    val samplingRate: StateFlow<Long> = _samplingRate

    private val _weeklyGoal = MutableStateFlow(prefs.getFloat("weekly_goal", 50f))
    val weeklyGoal: StateFlow<Float> = _weeklyGoal

    private val _monthlyGoal = MutableStateFlow(prefs.getFloat("monthly_goal", 180f))
    val monthlyGoal: StateFlow<Float> = _monthlyGoal

    fun setTheme(theme: String) {
        prefs.edit().putString("app_theme_mode", theme).apply()
        _theme.value = theme
    }

    fun setUseMetric(enabled: Boolean) {
        prefs.edit().putBoolean("use_metric", enabled).apply()
        _useMetric.value = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setSamplingRate(rateMillis: Long) {
        prefs.edit().putLong("sampling_rate", rateMillis).apply()
        _samplingRate.value = rateMillis
    }

    fun setWeeklyGoal(goal: Float) {
        prefs.edit().putFloat("weekly_goal", goal).apply()
        _weeklyGoal.value = goal
    }

    fun setMonthlyGoal(goal: Float) {
        prefs.edit().putFloat("monthly_goal", goal).apply()
        _monthlyGoal.value = goal
    }
}
