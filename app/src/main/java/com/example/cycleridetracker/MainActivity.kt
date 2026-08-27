package com.example.cycleridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CycleRideTrackerTheme {
                var currentScreen by remember { mutableStateOf("Insights") }

                if (currentScreen == "Insights") {
                    InsightsScreen(onNavigateToSettings = { currentScreen = "Settings" })
                } else {
                    SettingsScreen(onNavigateToInsights = { currentScreen = "Insights" })
                }
            }
        }
    }
}

