package com.example.cycleridetracker

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.components.RideData
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.Cyan400
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.ui.theme.Navy700

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    ride: RideData,
    onBack: () -> Unit,
    onReplayClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    keyPrefix: String = "ride"
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        ride.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                        Icon(
                            if (ride.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (ride.isFavorite) Color(0xFFEF9A9A) else CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Download")
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
        with(sharedTransitionScope) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    // Map Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(CycleRideTrackerTheme.colors.cardBackground)
                            .sharedElement(
                                rememberSharedContentState(key = "${keyPrefix}_map_${ride.title}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Drawing a simple path to represent the ride
                        Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                            val path = Path().apply {
                                moveTo(0f, size.height * 0.7f)
                                quadraticTo(
                                    size.width * 0.2f, size.height * 0.8f,
                                    size.width * 0.3f, size.height * 0.5f
                                )
                                cubicTo(
                                    size.width * 0.5f, size.height * 0.2f,
                                    size.width * 0.7f, size.height * 0.8f,
                                    size.width, size.height * 0.2f
                                )
                            }
                            drawPath(
                                path = path,
                                color = Cyan400,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            
                            // Start point
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(0f, size.height * 0.7f)
                            )
                            
                            // End point
                            drawCircle(
                                color = Color(0xFFFF8A80), // Light red/pink
                                radius = 6.dp.toPx(),
                                center = Offset(size.width, size.height * 0.2f)
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Saturday, Aug 29, 2026 • 11:27 AM",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }

                item {
                    Button(
                        onClick = onReplayClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Cyan400
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Cyan400.copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Cyan400
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "REPLAY INTERACTIVE JOURNEY 🎬",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    "Scrub through every turn and view photo waypo...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CycleRideTrackerTheme.colors.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CycleRideTrackerTheme.colors.onSurfaceVariant)
                        }
                    }
                }

                item {
                    SectionHeader("TELEMETRY SUMMARY")
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryCard(
                            modifier = Modifier.widthIn(min = 160.dp),
                            icon = Icons.Default.Straighten,
                            label = "DISTANCE",
                            value = ride.distance.split(" ")[0],
                            unit = "km"
                        )
                        TelemetryCard(
                            modifier = Modifier.widthIn(min = 160.dp),
                            icon = Icons.Default.Timer,
                            label = "MOVING TIME",
                            value = ride.duration,
                            unit = ""
                        )
                        TelemetryCard(
                            modifier = Modifier.widthIn(min = 160.dp),
                            icon = Icons.Default.Speed,
                            label = "AVG SPEED",
                            value = ride.avgSpeed.split(" ")[0],
                            unit = "km/h"
                        )
                        TelemetryCard(
                            modifier = Modifier.widthIn(min = 160.dp),
                            icon = Icons.Default.ElectricBolt,
                            label = "MAX SPEED",
                            value = "34.0",
                            unit = "km/h"
                        )
                        TelemetryCard(
                            modifier = Modifier.widthIn(min = 160.dp),
                            icon = Icons.Default.Landscape,
                            label = "ELEVATION",
                            value = "+36",
                            unit = "m"
                        )
                    }
                }

                item {
                    SectionHeader("ELEVATION PROFILE")
                    Spacer(Modifier.height(16.dp))
                    ElevationChart()
                }

                item {
                    SectionHeader("KILOMETER SPLITS")
                    Spacer(Modifier.height(16.dp))
                    KilometerSplitsSection()
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("RIDE PHOTOS (1)")
                        TextButton(onClick = { AppHaptics.performAction(haptic) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Add Photo", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            PhotoCard(
                                title = "Golden hour bridge reflection 🌉",
                                icon = Icons.Default.CameraAlt
                            )
                        }
                        item {
                            AddPhotoPlaceholder()
                        }
                    }
                }

                item {
                    SectionHeader("RIDER NOTES")
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = CycleRideTrackerTheme.colors.cardBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = CycleRideTrackerTheme.colors.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Chilly breeze. Clear dedicated bike lanes.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CycleRideTrackerTheme.colors.onSurface
                            )
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun KilometerSplitsSection() {
    val splits = listOf(
        SplitData(1, "1.0 km", "3:12/km", "18.7 km/h", false),
        SplitData(2, "1.0 km", "2:58/km", "20.2 km/h", true),
        SplitData(3, "1.0 km", "2:45/km", "21.8 km/h", true),
        SplitData(4, "1.0 km", "3:30/km", "17.1 km/h", false),
        SplitData(5, "1.0 km", "3:00/km", "20.0 km/h", true),
        SplitData(6, "1.0 km", "2:50/km", "21.1 km/h", true),
        SplitData(7, "1.0 km", "3:15/km", "18.5 km/h", false),
        SplitData(8, "1.0 km", "3:20/km", "18.0 km/h", false),
        SplitData(9, "0.45 km", "1:30/km", "18.0 km/h", false)
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = CycleRideTrackerTheme.colors.cardBackground
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            splits.forEach { split ->
                SplitItem(split)
            }
        }
    }
}

data class SplitData(
    val index: Int,
    val distance: String,
    val pace: String,
    val speed: String,
    val isUp: Boolean
)

@Composable
fun SplitItem(split: SplitData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = CircleShape,
            color = CycleRideTrackerTheme.colors.outline.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    split.index.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            split.distance,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = CycleRideTrackerTheme.colors.onSurface
        )
        Spacer(Modifier.weight(1f))
        Text(
            split.pace,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = CycleRideTrackerTheme.colors.onSurface
        )
        Spacer(Modifier.width(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (split.isUp) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (split.isUp) Color(0xFF81D4FA) else Color(0xFFFF8A80),
                modifier = Modifier.size(20.dp)
            )
            Text(
                split.speed,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = CycleRideTrackerTheme.colors.onSurface
            )
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = CycleRideTrackerTheme.colors.onSurfaceVariant
    )
}

@Composable
fun TelemetryCard(
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
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ElevationChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.8f)
                cubicTo(
                    size.width * 0.2f, size.height * 0.9f,
                    size.width * 0.4f, size.height * 0.4f,
                    size.width * 0.6f, size.height * 0.7f
                )
                cubicTo(
                    size.width * 0.8f, size.height * 0.9f,
                    size.width * 0.9f, size.height * 0.2f,
                    size.width, size.height * 0.5f
                )
            }
            
            // Fill area
            val fillPath = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Cyan400.copy(alpha = 0.3f), Color.Transparent),
                    startY = 0f,
                    endY = size.height
                )
            )
            
            drawPath(
                path = path,
                color = Cyan400,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun PhotoCard(title: String, icon: ImageVector) {
    Card(
        modifier = Modifier.size(140.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Icon(
                icon,
                contentDescription = null,
                tint = CycleRideTrackerTheme.colors.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = CycleRideTrackerTheme.colors.onSurface,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@Composable
fun AddPhotoPlaceholder() {
    Surface(
        modifier = Modifier.size(120.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, Navy700)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = Cyan400)
            Spacer(Modifier.height(8.dp))
            Text(
                "Add Photo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Cyan400
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true)
@Composable
fun RideDetailPreview() {
    val mockRide = RideData(
        "Sunset Riverbank Return",
        "Wed, Aug 26 • 5:45 PM",
        "9.2 km",
        "30:20",
        "20.4 km/h",
        false
    )
    CycleRideTrackerTheme(darkTheme = true) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                RideDetailScreen(
                    ride = mockRide,
                    onBack = {},
                    onReplayClick = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility,
                    keyPrefix = "dashboard"
                )
            }
        }
    }
}
