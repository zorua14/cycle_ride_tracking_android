package com.example.cycleridetracker.data

import android.content.Context
import android.content.SharedPreferences

object ThemePrefs {
    private const val PREFS_NAME = "cycle_ride_tracker_prefs"
    private const val KEY_THEME = "app_theme_mode"

    fun setTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun getTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, "System") ?: "System"
    }
}
