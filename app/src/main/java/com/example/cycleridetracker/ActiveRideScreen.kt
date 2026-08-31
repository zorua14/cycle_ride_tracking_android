package com.example.cycleridetracker

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.theme.Cyan400
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActiveRideScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    var isPaused by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
                            "Cycling Ride",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CycleRideTrackerTheme.colors.background,
                    titleContentColor = CycleRideTrackerTheme.colors.onSurface
                )
            )
        },
        containerColor = CycleRideTrackerTheme.colors.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Map Placeholder with Grid and Path
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CycleRideTrackerTheme.colors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridColor = Color.White.copy(alpha = 0.05f)
                        val step = 40.dp.toPx()
                        
                        // Draw Grid
                        for (x in 0..(size.width / step).toInt()) {
                            drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height), 1.dp.toPx())
                        }
                        for (y in 0..(size.height / step).toInt()) {
                            drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step), 1.dp.toPx())
                        }

                        // Draw Path
                        val path = Path().apply {
                            moveTo(size.width * 0.1f, size.height * 0.8f)
                            lineTo(size.width * 0.1f, size.height * 0.6f)
                            lineTo(size.width * 0.5f, size.height * 0.5f)
                            lineTo(size.width * 0.8f, size.height * 0.1f)
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFF90CAF9), // Light Blue path
                            style = Stroke(width = 8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        )

                        // Start point
                        drawCircle(
                            color = Color.White,
                            radius = 6.dp.toPx(),
                            center = Offset(size.width * 0.1f, size.height * 0.8f)
                        )
                        
                        // Current position point
                        drawCircle(
                            color = Color.White,
                            radius = 12.dp.toPx(),
                            center = Offset(size.width * 0.8f, size.height * 0.1f)
                        )
                        drawCircle(
                            color = Color(0xFF90CAF9),
                            radius = 8.dp.toPx(),
                            center = Offset(size.width * 0.8f, size.height * 0.1f)
                        )
                    }

                    // Speed indicator on map
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart),
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "24.5",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "KM/H",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }
                }
            }

            item {
                // Elapsed Time
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "ELAPSED TIME",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                        Text(
                            "01:05",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 56.sp
                            ),
                            color = CycleRideTrackerTheme.colors.onSurface
                        )
                    }
                }
            }

            item {
                // Metrics Row 1: Distance and Avg Speed (50/50 split)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActiveTelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Straighten,
                        label = "DISTANCE",
                        value = "0.33",
                        unit = "km"
                    )
                    ActiveTelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Speed,
                        label = "AVG SPEED",
                        value = "18.3",
                        unit = "km/h"
                    )
                }
            }

            item {
                // Metrics Row 2: Elevation, Max Speed, and Photos (33/33/33 split)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActiveTelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Landscape,
                        label = "ELEVATION",
                        value = "+12",
                        unit = "m"
                    )
                    ActiveTelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ElectricBolt,
                        label = "MAX SPEED",
                        value = "24.5",
                        unit = "km/h"
                    )
                    ActiveTelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CameraAlt,
                        label = "PHOTOS",
                        value = "0",
                        unit = "pins"
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                // Controls
                Box(
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    if (!isPaused) {
                        Button(
                            onClick = { isPaused = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3F456F), // Muted dark blue
                                contentColor = Color.White
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "PAUSE",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { isPaused = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(32.dp),
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
                                onClick = onFinish,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                shape = RoundedCornerShape(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF9A9A), // Muted Red
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

@Composable
fun ActiveTelemetryCard(
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
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(8.dp))
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

@Preview(showBackground = true)
@Composable
fun ActiveRidePreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        ActiveRideScreen(onBack = {}, onFinish = {})
    }
}
