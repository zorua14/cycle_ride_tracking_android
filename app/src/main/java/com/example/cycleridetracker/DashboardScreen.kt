package com.example.cycleridetracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.components.RecentRideCard
import com.example.cycleridetracker.ui.components.RideData
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.ui.theme.StreakPurple

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.ui.DashboardViewModel
import com.example.cycleridetracker.ui.DashboardStats

@Composable
fun DashboardContent(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onRideClick: (Ride) -> Unit = {},
    onReplayLatest: (Ride) -> Unit = {},
    onViewAllClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val recentRides by viewModel.recentRides.collectAsStateWithLifecycle()
    val stats by viewModel.weeklyStats.collectAsStateWithLifecycle()
    val useMetric by viewModel.useMetric.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            WeeklyProgressSection(stats)
        }

        item {
            RecentRidesSection(
                rides = recentRides.take(4),
                useMetric = useMetric,
                hapticsEnabled = hapticsEnabled,
                haptic = haptic,
                onRideClick = onRideClick,
                onReplayLatest = {
                    recentRides.firstOrNull()?.let { onReplayLatest(it) }
                }
            )
        }

        if (recentRides.isNotEmpty()) {
            item {
                Button(
                    onClick = {
                        AppHaptics.performAction(haptic, hapticsEnabled)
                        onViewAllClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.1f),
                        contentColor = CycleRideTrackerTheme.colors.primary
                    )
                ) {
                    Text(
                        "View All Rides",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// DashboardHeader is now handled by MainActivity's TopAppBar

@Composable
fun WeeklyProgressSection(stats: DashboardStats) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "THIS WEEK'S PROGRESS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CycleRideTrackerTheme.colors.primary
            )

            if (stats.streakDays > 0) {
                Surface(
                    color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = null,
                            tint = CycleRideTrackerTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${stats.streakDays} Day Streak",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CycleRideTrackerTheme.colors.primary
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Route,
                label = "DISTANCE",
                value = stats.distanceValue,
                unit = stats.distanceUnit
            )
            ProgressMetricCard(
                modifier = Modifier.weight(0.8f),
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "RIDES",
                value = stats.ridesCount,
                unit = "rides"
            )
            ProgressMetricCard(
                modifier = Modifier.weight(0.9f),
                icon = Icons.Default.Timer,
                label = "TIME",
                value = stats.timeMinutes,
                unit = "min"
            )
        }
    }
}

@Composable
fun ProgressMetricCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = CycleRideTrackerTheme.colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = CycleRideTrackerTheme.colors.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

@Composable
fun RecentRidesSection(
    rides: List<Ride>,
    useMetric: Boolean,
    hapticsEnabled: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onRideClick: (Ride) -> Unit,
    onReplayLatest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "RECENT RIDES",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = CycleRideTrackerTheme.colors.primary
            )

            TextButton(onClick = { 
                onReplayLatest()
                AppHaptics.performAction(haptic) 
            }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Replay Latest",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurface
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rides.forEach { ride ->
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

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        DashboardContent()
    }
}
