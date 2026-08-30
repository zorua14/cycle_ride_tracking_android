package com.example.cycleridetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.cycleridetracker.ui.components.RecentRideCard
import com.example.cycleridetracker.ui.components.RideData
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HistoryContent(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onRideClick: (RideData) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val haptic = LocalHapticFeedback.current
    
    val rides = listOf(
        RideData("Morning Downtown Ride", "Thu, Aug 27 • 4:45 PM", "8.4 km", "28:00", "19.8 km/h", true),
        RideData("Sunset Riverbank Return", "Wed, Aug 26 • 5:45 PM", "9.2 km", "30:20", "20.4 km/h", false),
        RideData("Weekend Twin Peaks Loop", "Mon, Aug 24 • 2:45 PM", "18.6 km", "53:20", "22.5 km/h", true),
        RideData("Neighborhood Errand Run", "Sat, Aug 22 • 1:45 PM", "3.2 km", "11:20", "16.8 km/h", false),
        RideData("Morning Downtown Ride", "Thu, Aug 27 • 4:45 PM", "8.4 km", "28:00", "19.8 km/h", true),
        RideData("Sunset Riverbank Return", "Wed, Aug 26 • 5:45 PM", "9.2 km", "30:20", "20.4 km/h", false),
        RideData("Weekend Twin Peaks Loop", "Mon, Aug 24 • 2:45 PM", "18.6 km", "53:20", "22.5 km/h", true),
        RideData("Neighborhood Errand Run", "Sat, Aug 22 • 1:45 PM", "3.2 km", "11:20", "16.8 km/h", false)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search rides, locations, notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = CycleRideTrackerTheme.colors.cardBackground,
                    focusedContainerColor = CycleRideTrackerTheme.colors.cardBackground,
                    unfocusedBorderColor = CycleRideTrackerTheme.colors.outline.copy(alpha = 0.3f)
                )
            )
        }
        
        items(rides) { ride ->
            RecentRideCard(
                ride = ride,
                haptic = haptic,
                keyPrefix = "history",
                onClick = { onRideClick(ride) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    HistoryContent(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
        }
    }
}
