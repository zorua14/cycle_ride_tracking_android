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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun DashboardContent(
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onRideClick: (RideData) -> Unit = {},
    onReplayLatest: () -> Unit = {},
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            WeeklyProgressSection()
        }

        item {
            RecentRidesSection(
                haptic = LocalHapticFeedback.current,
                onRideClick = onRideClick,
                onReplayLatest = onReplayLatest,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

// DashboardHeader is now handled by MainActivity's TopAppBar

@Composable
fun WeeklyProgressSection() {
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

            Surface(
                color = StreakPurple,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Whatshot,
                        contentDescription = null,
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "4 Day Streak",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFD0BCFF)
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProgressMetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Route,
                label = "DISTANCE",
                value = "42.8",
                unit = "km"
            )
            ProgressMetricCard(
                modifier = Modifier.weight(0.8f),
                icon = Icons.AutoMirrored.Filled.DirectionsBike,
                label = "RIDES",
                value = "6",
                unit = "rides"
            )
            ProgressMetricCard(
                modifier = Modifier.weight(0.9f),
                icon = Icons.Default.Timer,
                label = "TIME",
                value = "128",
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RecentRidesSection(
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onRideClick: (RideData) -> Unit,
    onReplayLatest: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
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

        val rides = listOf(
            RideData("Morning Downtown Ride", "Thu, Aug 27 • 4:45 PM", "8.4 km", "28:00", "19.8 km/h", true),
            RideData("Sunset Riverbank Return", "Wed, Aug 26 • 5:45 PM", "9.2 km", "30:20", "20.4 km/h", false),
            RideData("Weekend Twin Peaks Loop", "Mon, Aug 24 • 2:45 PM", "18.6 km", "53:20", "22.5 km/h", true),
            RideData("Neighborhood Errand Run", "Sat, Aug 22 • 1:45 PM", "3.2 km", "11:20", "16.8 km/h", false)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rides.forEach { ride ->
                RecentRideCard(
                    ride = ride,
                    haptic = haptic,
                    keyPrefix = "dashboard",
                    onClick = { onRideClick(ride) },
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                DashboardContent(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
