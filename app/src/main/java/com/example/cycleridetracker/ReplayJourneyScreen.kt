package com.example.cycleridetracker

import androidx.compose.animation.*
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.ui.utils.MarkerUtils
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Polyline
import de.afarber.openmapview.Marker
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReplayJourneyScreen(
    ride: Ride,
    useMetric: Boolean,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val rideData = ride.toRideData(useMetric)
    var isPlaying by rememberSaveable { mutableStateOf(value = false) }
    var currentFrame by rememberSaveable { mutableIntStateOf(0) }
    val totalFrames = 100 
    var playbackSpeed by rememberSaveable { mutableIntStateOf(2) }
    val scrollState = rememberScrollState()

    // Map control states
    var mapInitialized by remember { mutableStateOf(false) }
    var endSnapped by remember { mutableStateOf(false) }

    LaunchedEffect(currentFrame) {
        if (currentFrame == 0) {
            mapInitialized = false
            endSnapped = false
        }
    }

    val pathPoints = ride.pathPoints.map { LatLng(it.latitude, it.longitude) }
    val context = LocalContext.current
    val photoMarkers = remember { mutableStateMapOf<String, Bitmap>() }

    LaunchedEffect(ride.photos) {
        ride.photos.forEach { photo ->
            if (photo.latitude != null && !photoMarkers.containsKey(photo.uri)) {
                val markerBitmap = MarkerUtils.loadMarkerBitmap(context, photo.uri)
                if (markerBitmap != null) {
                    photoMarkers[photo.uri] = markerBitmap
                }
            }
        }
    }

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            while (currentFrame < totalFrames) {
                delay((1000L / playbackSpeed / 10).milliseconds)
                currentFrame++
            }
            isPlaying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        rideData.title,
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
            Box(
                modifier = Modifier
                    .height(400.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CycleRideTrackerTheme.colors.cardBackground)
            ) {
                AndroidView(
                    factory = { context ->
                        de.afarber.openmapview.OpenMapView(context)
                    },
                    modifier = Modifier.fillMaxSize(),
                            update = { mapView ->
                                if (pathPoints.isNotEmpty()) {
                                    mapView.clear()
                                    
                                    val pointIndex = (currentFrame.toFloat() / totalFrames * (pathPoints.size - 1)).toInt()
                                    val isComplete = currentFrame >= totalFrames
                                    
                                    // PROGRESSIVE PATH: Draw only up to current frame to "cover" the traveled path
                                    val visiblePoints = pathPoints.take(pointIndex + 1)
                                    if (visiblePoints.size >= 2) {
                                        val polyline = Polyline(
                                            points = visiblePoints,
                                            strokeColor = androidx.compose.ui.graphics.Color.Cyan,
                                            strokeWidth = 10f
                                        )
                                        mapView.addPolyline(polyline)
                                    }

                                    // PHOTO MARKERS: Only show if reached in playback, or playback is complete
                                    ride.photos.forEachIndexed { index, photo ->
                                        val photoFrame = (index + 1) * totalFrames / (ride.photos.size + 1)
                                        if (photo.latitude != null && photo.longitude != null && (photoFrame <= currentFrame || isComplete)) {
                                            val customBitmap = photoMarkers[photo.uri]
                                            val markerIcon = if (customBitmap != null) {
                                                BitmapDescriptor.BitmapMarker(customBitmap)
                                            } else null

                                            mapView.addMarker(Marker(
                                                position = LatLng(photo.latitude, photo.longitude),
                                                title = "Photo",
                                                icon = markerIcon,
                                                anchor = 0.5f to 1.0f
                                            ))
                                        }
                                    }

                                    // CAMERA HANDLING:
                                    // 1. Zoom out only when complete (once)
                                    // 2. Set initial zoom/position once
                                    // 3. Follow rider during playback WITHOUT forcing zoom level
                                    if (isComplete) {
                                        if (!endSnapped) {
                                            val avgLat = pathPoints.map { it.latitude }.average()
                                            val avgLng = pathPoints.map { it.longitude }.average()
                                            mapView.setCenter(LatLng(avgLat, avgLng))
                                            mapView.setZoom(14f)
                                            endSnapped = true
                                        }
                                    } else {
                                        if (!mapInitialized) {
                                            // Initial Position & Zoom
                                            mapView.setCenter(pathPoints.first())
                                            mapView.setZoom(16f)
                                            mapInitialized = true
                                        } else if (isPlaying) {
                                            // Follow the current progress point closely, but don't force zoom
                                            val currentPoint = pathPoints.getOrElse(pointIndex) { pathPoints.last() }
                                            mapView.setCenter(currentPoint)
                                        }
                                        endSnapped = false
                                    }
                                }
                            }
                )
                
                // Photo Waypoint Overlay
                val photoWaypoints = ride.photos.mapIndexed { index, photo ->
                    val frame = (index + 1) * totalFrames / (ride.photos.size + 1)
                    frame to photo.uri
                }
                
                val activePhoto = photoWaypoints.find { currentFrame in (it.first - 5)..(it.first + 5) }

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
                            "%.1f KM/H".format(ride.averageSpeedKmh),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = CycleRideTrackerTheme.colors.onSurface
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = activePhoto != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    activePhoto?.let { (_, uri) ->
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = CycleRideTrackerTheme.colors.background.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "PHOTO WAYPOINT",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                                    )
                                    Text(
                                        "Snapshot from ride",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CycleRideTrackerTheme.colors.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val totalDistValue = rideData.distance.replace(" km", "").toDoubleOrNull() ?: 9.2
            val currentDist = (totalDistValue * currentFrame / totalFrames)
            val progressPercent = (currentFrame * 100 / totalFrames)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReplayTelemetryCard(Modifier.widthIn(min = 100.dp), Icons.Default.Straighten, "DISTANCE", String.format(Locale.US, "%.2f", currentDist), "km")
                ReplayTelemetryCard(Modifier.widthIn(min = 100.dp), Icons.Default.Timelapse, "PROGRESS", progressPercent.toString(), "%")
            }

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
                                                maxLines = 1, softWrap = false,
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
    val mockRide = Ride(
        title = "Morning Downtown Ride Return",
        startTimeMillis = System.currentTimeMillis() - 3600000,
        endTimeMillis = System.currentTimeMillis(),
        distanceMeters = 9200f,
        averageSpeedKmh = 20.4f,
        maxSpeedKmh = 25f,
        pathPoints = emptyList(),
        photos = emptyList()
    )
    CycleRideTrackerTheme(darkTheme = true) {
        ReplayJourneyScreen(
            ride = mockRide,
            useMetric = true,
            onBack = {}
        )
    }
}
