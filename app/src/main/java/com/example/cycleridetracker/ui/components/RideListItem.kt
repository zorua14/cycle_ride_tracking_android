package com.example.cycleridetracker.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme

data class RideData(
    val title: String,
    val time: String,
    val distance: String,
    val duration: String,
    val avgSpeed: String,
    val isFavorite: Boolean
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun RecentRideCard(
    ride: RideData,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    keyPrefix: String = "ride",
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Card(
            onClick = {
                onClick()
                AppHaptics.performSelection(haptic)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mock Map Thumbnail
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CycleRideTrackerTheme.colors.outline.copy(alpha = 0.2f))
                        .sharedElement(
                            rememberSharedContentState(key = "${keyPrefix}_map_${ride.title}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Simplified mock map visualization using Icons/Shapes
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw a squiggle path or something
                    }
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        ride.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        ride.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RideMetric(Icons.Default.Straighten, ride.distance)
                        RideMetric(Icons.Default.Timer, ride.duration)
                        RideMetric(Icons.Default.Speed, ride.avgSpeed)
                    }
                }

                Spacer(Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    IconButton(
                        onClick = { AppHaptics.performAction(haptic) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (ride.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (ride.isFavorite) Color(0xFFEF9A9A) else CycleRideTrackerTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(40.dp)) // Balances the Heart icon + Spacer to keep Chevron centered
                }
            }
        }
    }
}

@Composable
fun RideMetric(icon: ImageVector, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CycleRideTrackerTheme.colors.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = CycleRideTrackerTheme.colors.onSurface,
            maxLines = 1
        )
    }
}
