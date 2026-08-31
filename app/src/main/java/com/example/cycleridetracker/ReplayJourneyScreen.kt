package com.example.cycleridetracker

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.components.RideData
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

data class Waypoint(
    val frame: Int,
    val title: String,
    val position: Offset,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReplayJourneyScreen(
    ride: RideData,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var isPlaying by rememberSaveable { mutableStateOf(value = false) }
    var currentFrame by rememberSaveable { mutableIntStateOf(13) }
    val totalFrames = 45
    var playbackSpeed by rememberSaveable { mutableIntStateOf(2) }
    val scrollState = rememberScrollState()

    val waypoints = listOf(
        Waypoint(8, "Sunrise over Market St 🌅", Offset(0.2f, 0.75f)),
        Waypoint(25, "Protected cycle track detour 🚴‍♂️", Offset(0.6f, 0.5f)),
        Waypoint(38, "Coffee stop arrival ☕️", Offset(0.9f, 0.3f))
    )

    val activeWaypoint = waypoints.find { currentFrame in (it.frame - 2)..(it.frame + 2) }

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (currentFrame < totalFrames) {
                delay((1000L / playbackSpeed).milliseconds)
                currentFrame++
            }
            isPlaying = false
        }
    }

    val primaryColor = CycleRideTrackerTheme.colors.primary
    val gridColor = CycleRideTrackerTheme.colors.onSurface.copy(alpha = 0.08f)
    val finishColor = MaterialTheme.colorScheme.error
    val inactiveWaypointColor = CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = innerPadding.calculateTopPadding())
                .padding(bottom = innerPadding.calculateBottomPadding() + 40.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Map Area
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CycleRideTrackerTheme.colors.cardBackground)
            ) {
                // Background Grid
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 40.dp.toPx()
                    for (i in 0..(size.width / step).toInt()) {
                        drawLine(gridColor, Offset(i * step, 0f), Offset(i * step, size.height))
                    }
                    for (i in 0..(size.height / step).toInt()) {
                        drawLine(gridColor, Offset(0f, i * step), Offset(size.width, i * step))
                    }
                }

                // Ride Path
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
                        color = primaryColor,
                        style = Stroke(width = 8.dp.toPx())
                    )

                    // Current Position Marker
                    val progress = currentFrame.toFloat() / totalFrames
                    drawCircle(
                        color = primaryColor,
                        radius = 8.dp.toPx(),
                        center = Offset(size.width * progress, size.height * (0.7f - 0.5f * progress))
                    )
                    drawCircle(
                        color = finishColor,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width * progress, size.height * (0.7f - 0.5f * progress))
                    )

                    // Draw Waypoint Icons on Map
                    waypoints.forEach { waypoint ->
                        val isReached = currentFrame >= waypoint.frame
                        drawCircle(
                            color = if (isReached) primaryColor else inactiveWaypointColor,
                            radius = 6.dp.toPx(),
                            center = Offset(size.width * waypoint.position.x, size.height * waypoint.position.y)
                        )
                    }
                }

                // Overlays
                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.TopStart),
                    shape = RoundedCornerShape(12.dp),
                    color = CycleRideTrackerTheme.colors.background.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Speed,
                            contentDescription = null,
                            tint = CycleRideTrackerTheme.colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "22.0 KM/H",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CycleRideTrackerTheme.colors.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
                    shape = RoundedCornerShape(12.dp),
                    color = CycleRideTrackerTheme.colors.background.copy(alpha = 0.8f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.outline)
                ) {
                    Text(
                        "1:30:26 PM",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurface
                    )
                }

                // Photo Waypoint Card
                androidx.compose.animation.AnimatedVisibility(
                    visible = activeWaypoint != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    activeWaypoint?.let { waypoint ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CycleRideTrackerTheme.colors.background.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            tint = CycleRideTrackerTheme.colors.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "PHOTO WAYPOINT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                                    )
                                    Text(
                                        waypoint.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CycleRideTrackerTheme.colors.onSurface
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = CycleRideTrackerTheme.colors.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Telemetry
            val totalDistValue = ride.distance.replace(" km", "").toDoubleOrNull() ?: 9.2
            val currentDist = (totalDistValue * currentFrame / totalFrames)
            val progressPercent = (currentFrame * 100 / totalFrames)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReplayTelemetryCard(Modifier.widthIn(min = 100.dp), Icons.Default.Straighten, "DISTANCE", String.format(Locale.US, "%.2f", currentDist), "km")
                ReplayTelemetryCard(Modifier.widthIn(min = 100.dp), Icons.Default.Landscape, "ALTITUDE", "50", "m")
                ReplayTelemetryCard(Modifier.widthIn(min = 100.dp), Icons.Default.Timelapse, "PROGRESS", progressPercent.toString(), "%")
            }

            // Scrubbing Area
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "START",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.primary
                    )
                    Text(
                        "FRAME $currentFrame OF $totalFrames",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurface
                    )
                    Text(
                        "FINISH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Slider(
                    value = currentFrame.toFloat(),
                    onValueChange = { currentFrame = it.toInt() },
                    valueRange = 0f..totalFrames.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = CycleRideTrackerTheme.colors.primary,
                        activeTrackColor = CycleRideTrackerTheme.colors.primary,
                        inactiveTrackColor = CycleRideTrackerTheme.colors.cardBackground
                    )
                )
            }

            // Controls
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                color = CycleRideTrackerTheme.colors.cardBackground
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { currentFrame = 0 }) {
                            Icon(Icons.Default.Replay, contentDescription = "Reset", tint = CycleRideTrackerTheme.colors.onSurface)
                        }
                        IconButton(onClick = { currentFrame = (currentFrame - 5).coerceAtLeast(0) }) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Back", tint = CycleRideTrackerTheme.colors.onSurface)
                        }
                        FloatingActionButton(
                            onClick = { isPlaying = !isPlaying },
                            containerColor = CycleRideTrackerTheme.colors.primary,
                            contentColor = CycleRideTrackerTheme.colors.background,
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { currentFrame = (currentFrame + 5).coerceAtMost(totalFrames) }) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = CycleRideTrackerTheme.colors.onSurface)
                        }
                        Surface(
                            shape = CircleShape,
                            color = CycleRideTrackerTheme.colors.outline,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CycleRideTrackerTheme.colors.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Speed Selector
                    val speeds = listOf(1, 2, 5, 10)

                    ButtonGroup(
                        overflowIndicator = { menuState ->
                            IconButton(onClick = { menuState.show() }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More speeds", tint = CycleRideTrackerTheme.colors.onSurface)
                            }
                        },
                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        speeds.forEachIndexed { index, speed ->
                            customItem(
                                buttonGroupContent = {
                                    val isSelected = playbackSpeed == speed
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val layoutDirection = LocalLayoutDirection.current
                                    val contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                    val compressionLimit = contentPadding.calculateEndPadding(layoutDirection)

                                    val shapes = when (index) {
                                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                        speeds.size - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                    }

                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                playbackSpeed = speed
                                                AppHaptics.performAction(haptic)
                                            }
                                        },
                                        colors = ToggleButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surface,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shapes = shapes,
                                        contentPadding = contentPadding,
                                        modifier = Modifier
                                            .weight(1f)
                                            .animateWidth(interactionSource, compressionLimit = compressionLimit),
                                        interactionSource = interactionSource,
                                        content = {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                                )
                                                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                                            }
                                            Text(
                                                text = "${speed}x",
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    )
                                },
                                menuContent = { menuState ->
                                    val isSelected = playbackSpeed == speed
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else null,
                                        onClick = {
                                            playbackSpeed = speed
                                            menuState.dismiss()
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReplayTelemetryCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CycleRideTrackerTheme.colors.cardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.outline)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
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

@Preview(showBackground = true, name = "Standard Device")
@Composable
fun ReplayJourneyPreview() {
    val mockRide = RideData(
        "Morning Downtown Ride Return",
        "Wed, Aug 26 • 5:45 PM",
        "9.2 km",
        "30:20",
        "20.4 km/h",
        false
    )
    CycleRideTrackerTheme(darkTheme = true) {
        ReplayJourneyScreen(
            ride = mockRide,
            onBack = {}
        )
    }
}