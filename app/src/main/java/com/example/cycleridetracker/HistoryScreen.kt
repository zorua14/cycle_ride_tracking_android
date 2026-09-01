package com.example.cycleridetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.cycleridetracker.ui.components.RecentRideCard
import com.example.cycleridetracker.ui.components.RideData
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.animation.*

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.ui.HistoryViewModel

@Composable
fun HistoryContent(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onRideClick: (Ride) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val haptic = LocalHapticFeedback.current
    val allRides by viewModel.allRides.collectAsStateWithLifecycle()
    val useMetric by viewModel.useMetric.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isEmpty = allRides.isEmpty() && searchQuery.isEmpty()

    if (isEmpty) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    color = CycleRideTrackerTheme.colors.cardBackground,
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.AutoMirrored.Outlined.DirectionsBike,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    "No Rides Yet",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your completed cycling journeys will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search rides, notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = CycleRideTrackerTheme.colors.cardBackground,
                        focusedContainerColor = CycleRideTrackerTheme.colors.cardBackground,
                        unfocusedBorderColor = CycleRideTrackerTheme.colors.outline.copy(alpha = 0.3f)
                    )
                )
            }
            
            if (allRides.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(allRides) { ride ->
                    RecentRideCard(
                        ride = ride.toRideData(useMetric),
                        haptic = haptic,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onRideClick(ride) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistoryContent()
        }
    }
}
