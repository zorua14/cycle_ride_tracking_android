package com.example.cycleridetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val avgSpeed: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecentRideCard(
    ride: RideData,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    hapticsEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        onClick = {
            onClick()
            AppHaptics.performSelection(haptic, hapticsEnabled)
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
            // Replaced Map Thumbnail with a simple Bike Icon for efficiency
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
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

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = CycleRideTrackerTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
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
