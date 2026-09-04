package com.example.cycleridetracker

import androidx.compose.animation.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.theme.Cyan400
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.data.ActiveRideState
import com.example.cycleridetracker.ui.ActiveRideViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveRideScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveRideViewModel = hiltViewModel(),
) {
    val uiState by viewModel.activeRideState.collectAsStateWithLifecycle()
    val trackingState = remember(uiState) { uiState as? ActiveRideState.Tracking }

    LaunchedEffect(viewModel) {
        if (uiState is ActiveRideState.Idle) {
            viewModel.startRide()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = CycleRideTrackerTheme.colors.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.DirectionsBike,
                                    contentDescription = null,
                                    tint = CycleRideTrackerTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Active Ride",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = CycleRideTrackerTheme.colors.onSurface
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(contentType = "Timer") {
                    // Elapsed Time
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(32.dp),
                        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "ELAPSED TIME",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                ),
                                color = CycleRideTrackerTheme.colors.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            RollingText(
                                text = formatDuration(trackingState?.durationMillis ?: 0L),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 72.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = CycleRideTrackerTheme.colors.onSurface
                            )
                        }
                    }
                }

                item(contentType = "Telemetry") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ActiveTelemetryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Straighten,
                            label = "DISTANCE",
                            value = String.format(Locale.US, "%.2f", (trackingState?.distanceMeters ?: 0f) / 1000f),
                            unit = "km"
                        )
                        ActiveTelemetryCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Speed,
                            label = "SPEED",
                            value = String.format(Locale.US, "%.1f", trackingState?.currentSpeedKmh ?: 0f),
                            unit = "km/h"
                        )
                    }
                }

                item(contentType = "Controls") {
                    Spacer(Modifier.height(16.dp))
                    // Controls
                    Box(
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                    ) {
                        val isPaused = trackingState?.isPaused ?: false
                        
                        if (!isPaused) {
                            Button(
                                onClick = { viewModel.pauseRide() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp),
                                shape = RoundedCornerShape(36.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF3F456F),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(28.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "PAUSE RIDE",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.resumeRide() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp),
                                    shape = RoundedCornerShape(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CycleRideTrackerTheme.colors.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "RESUME",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { 
                                        viewModel.finishRide()
                                        onFinish()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(72.dp),
                                    shape = RoundedCornerShape(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEF9A9A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Stop, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "FINISH",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveTelemetryCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = CycleRideTrackerTheme.colors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        fontFeatureSettings = "tnum"
                    ),
                    color = CycleRideTrackerTheme.colors.onSurface
                )

                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = unit,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified
) {
    Row(modifier = modifier) {
        text.forEach { char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (
                            slideInVertically { it } + fadeIn()
                            ) togetherWith (
                            slideOutVertically { -it } + fadeOut()
                            ) using SizeTransform(clip = false)
                },
                label = "RollingChar"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

@Preview(showBackground = true)
@Composable
private fun ActiveRidePreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        ActiveRideScreen(onFinish = {})
    }
}
