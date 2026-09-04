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
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import androidx.compose.ui.tooling.preview.Preview





import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.ui.HistoryViewModel
import com.example.cycleridetracker.ui.HistoryUiState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun HistoryContent(
    uiState: HistoryUiState,
    hapticsEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onRideClick: (Ride) -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "HistoryTransition",
    ) { state ->
        when (state) {
            is HistoryUiState.Loading -> {
                HistoryLoadingPlaceholder(contentPadding)
            }
            is HistoryUiState.Success -> {
                HistorySuccessContent(
                    state = state,
                    contentPadding = contentPadding,
                    onRideClick = onRideClick,
                    haptic = haptic,
                    hapticsEnabled = hapticsEnabled,
                    onSearchQueryChange = onSearchQueryChange,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun HistorySuccessContent(
    state: HistoryUiState.Success,
    contentPadding: PaddingValues,
    onRideClick: (Ride) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    hapticsEnabled: Boolean,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEmpty = state.rides.isEmpty() && state.query.isEmpty()

    if (isEmpty) {
        HistoryEmptyState(contentPadding, modifier = modifier)
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(contentType = "Search") {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onSearchQueryChange,
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
            
            if (state.rides.isEmpty()) {
                item(contentType = "EmptyResults") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results for \"${state.query}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(
                    items = state.rides,
                    key = { it.id },
                    contentType = { "Ride" }
                ) { ride ->
                    RecentRideCard(
                        ride = ride.toRideData(state.useMetric),
                        haptic = haptic,
                        hapticsEnabled = hapticsEnabled,
                    ) { onRideClick(ride) }
                }
            }
        }
    }
}

@Composable
fun HistoryEmptyState(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
}

@Composable
fun HistoryLoadingPlaceholder(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(CycleRideTrackerTheme.colors.cardBackground, MaterialTheme.shapes.extraLarge)
        )
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(CycleRideTrackerTheme.colors.cardBackground, RoundedCornerShape(16.dp))
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.background) {
            HistoryContent(
                uiState = HistoryUiState.Loading,
                hapticsEnabled = true,
                onSearchQueryChange = {}
            )
        }
    }
}
