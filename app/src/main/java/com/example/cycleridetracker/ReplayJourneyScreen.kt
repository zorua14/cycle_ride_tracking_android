package com.example.cycleridetracker

import android.util.Log
import android.view.MotionEvent
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
import androidx.compose.ui.graphics.Color
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
import com.example.cycleridetracker.ui.utils.ConnectivityObserver
import com.example.cycleridetracker.ui.utils.NetworkConnectivityObserver
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Polyline
import de.afarber.openmapview.Marker
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReplayJourneyScreen(
    ride: Ride,
    useMetric: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val rideData = ride.toRideData(useMetric)
    var isPlaying by rememberSaveable { mutableStateOf(value = false) }

    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(
        initialValue = ConnectivityObserver.Status.Available
    )

    var showOfflineAlert by remember { mutableStateOf(false) }

    LaunchedEffect(networkStatus) {
        Log.d("ReplayJourneyScreen", "Network status changed: $networkStatus")
        if (networkStatus != ConnectivityObserver.Status.Available) {
            showOfflineAlert = true
            isPlaying = false
        }
    }
    
    val pathPoints = remember(ride.pathPoints) { 
        ride.pathPoints.map { LatLng(it.latitude, it.longitude) } 
    }
    val avgLat = remember(pathPoints) { pathPoints.asSequence().map { it.latitude }.average() }
    val avgLng = remember(pathPoints) { pathPoints.asSequence().map { it.longitude }.average() }
    
    var currentFrame by rememberSaveable { mutableIntStateOf(0) }
    val totalFrames = remember(pathPoints) { pathPoints.size.coerceAtLeast(1) }
    var playbackSpeed by rememberSaveable { mutableFloatStateOf(1f) }
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

    val bikeBitmap = remember { MarkerUtils.getBikeMarkerBitmap() }

    LaunchedEffect(isPlaying, playbackSpeed) {
        if (isPlaying) {
            if (currentFrame >= (totalFrames - 1)) {
                isPlaying = false
                return@LaunchedEffect
            }

            // 1x = 250 frames per second.
            val baseFps = 250.0 
            var lastTime = android.os.SystemClock.elapsedRealtime()
            var subFrame = currentFrame.toDouble()
            
            while ((isPlaying && currentFrame < totalFrames - 1)) {
                val now = android.os.SystemClock.elapsedRealtime()
                val deltaTimeSec = (now - lastTime) / 1000.0
                lastTime = now

                // Check for photos nearby to slow down
                val pointIndex = currentFrame.coerceAtMost(pathPoints.size - 1)
                val currentPoint = pathPoints.getOrNull(pointIndex)
                val isNearPhoto = currentPoint?.let { cp ->
                    ride.photos.any { photo ->
                        if (photo.latitude != null && photo.longitude != null) {
                            val distanceResults = FloatArray(1)
                            android.location.Location.distanceBetween(
                                cp.latitude, cp.longitude,
                                photo.latitude, photo.longitude,
                                distanceResults
                            )
                            distanceResults[0] <= 40f // detect photo 40m ahead/around
                        } else false
                    }
                } ?: false

                // If near a photo, drop to 50 FPS (slow motion) regardless of selected speed
                // to give the user time to see the waypoint.
                val effectiveFps = if (isNearPhoto) 50.0 else (baseFps * playbackSpeed)
                
                subFrame += effectiveFps * deltaTimeSec
                currentFrame = subFrame.toInt().coerceAtMost(totalFrames - 1)
                delay(16.milliseconds)
            }
            
            // If we reached the end naturally, pause for a moment so the user sees the rider at the finish
            // before the map snaps out to the full view.
            if (currentFrame >= totalFrames - 1) {
                delay(500.milliseconds)
            }
            
            isPlaying = false
        }
    }

    Scaffold(
        modifier = modifier,
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
                if (networkStatus != ConnectivityObserver.Status.Available) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No Internet Connection",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                "Map tiles cannot be loaded",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                
                if (networkStatus == ConnectivityObserver.Status.Available) {
                    AndroidView(
                        factory = { context ->
                            de.afarber.openmapview.OpenMapView(context).apply {
                                setOnTouchListener { v, event ->
                                    // Panning might already work, but we want to ensure it's not intercepted if this was inside a scrollable
                                    // In ReplayJourneyScreen, it IS inside a Column with verticalScroll.
                                    when (event.action) {
                                        MotionEvent.ACTION_DOWN -> {
                                            v.parent.requestDisallowInterceptTouchEvent(true)
                                        }
                                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                            v.parent.requestDisallowInterceptTouchEvent(false)
                                            if (event.action == MotionEvent.ACTION_UP) v.performClick()
                                        }
                                    }
                                    v.onTouchEvent(event)
                                    true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { mapView ->
                        if (pathPoints.isNotEmpty()) {
                            mapView.clear()
                            
                            val pointIndex = currentFrame.coerceAtMost(pathPoints.size - 1)
                            val isComplete = currentFrame >= totalFrames - 1
                            
                            // PROGRESSIVE PATH: Draw only up to current frame to "cover" the traveled path
                            val visiblePoints = pathPoints.take(pointIndex + 1)
                            if (visiblePoints.size >= 2) {
                                val polyline = Polyline(
                                    points = visiblePoints,
                                    strokeColor = Color.Cyan,
                                    strokeWidth = 10f
                                )
                                mapView.addPolyline(polyline)
                            }

                            // RIDER MARKER
                            val currentPoint = pathPoints.getOrElse(pointIndex) { pathPoints.last() }
                            mapView.addMarker(Marker(
                                position = currentPoint,
                                title = "Rider",
                                icon = BitmapDescriptor.BitmapMarker(bikeBitmap),
                                anchor = 0.5f to 0.5f
                            ))

                            // CAMERA HANDLING:
                            // 1. Zoom out only when complete (once)
                            // 2. Set initial zoom/position once
                            // 3. Follow rider during playback WITHOUT forcing zoom level
                            if (isComplete && !isPlaying) {
                                if (!endSnapped) {
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
                                } else {
                                    // Follow the current progress point closely, but don't force zoom
                                    // This handles both automatic playback and manual slider scrubbing
                                    val cp = pathPoints.getOrElse(pointIndex) { pathPoints.last() }
                                    mapView.setCenter(cp)
                                }
                                endSnapped = false
                            }
                        }
                    }
                }
                
                // Photo Waypoint Overlay (Proximity check: 20 meters)
                val currentPoint = if (pathPoints.isNotEmpty()) {
                    val pointIndex = currentFrame.coerceAtMost(pathPoints.size - 1)
                    pathPoints.getOrElse(pointIndex) { pathPoints.last() }
                } else null

                val activePhoto = remember(currentPoint, ride.photos) {
                    currentPoint?.let { cp ->
                        ride.photos.find { photo ->
                            if (photo.latitude != null && photo.longitude != null) {
                                val results = FloatArray(1)
                                android.location.Location.distanceBetween(
                                    cp.latitude, cp.longitude,
                                    photo.latitude, photo.longitude,
                                    results
                                )
                                results[0] <= 30f // 30 meters
                            } else false
                        }
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
                    activePhoto?.let { photo ->
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
                                        model = photo.uri,
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
            val currentDist = (totalDistValue * currentFrame / (totalFrames - 1).coerceAtLeast(1))
            val progressPercent = (currentFrame * 100 / (totalFrames - 1).coerceAtLeast(1))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ReplayTelemetryCard(
                    icon = Icons.Default.Straighten,
                    label = "DISTANCE",
                    value = String.format(Locale.US, "%.2f", currentDist),
                    unit = "km",
                    modifier = Modifier.widthIn(min = 100.dp)
                )
                ReplayTelemetryCard(
                    icon = Icons.Default.Timelapse,
                    label = "PROGRESS",
                    value = progressPercent.toString(),
                    unit = "%",
                    modifier = Modifier.widthIn(min = 100.dp)
                )
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
                        "FRAME ${currentFrame + 1} OF $totalFrames",
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
                    onValueChange = {
                        if (isPlaying) isPlaying = false
                        currentFrame = it.toInt()
                    },
                    valueRange = 0f..(totalFrames - 1).toFloat().coerceAtLeast(0f),
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
                        IconButton(onClick = { currentFrame = (currentFrame + 5).coerceAtMost(totalFrames - 1) }) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = CycleRideTrackerTheme.colors.onSurface)
                        }
                        Surface(
                            shape = CircleShape,
                            color = CycleRideTrackerTheme.colors.outline,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    if (playbackSpeed >= 1f) "${playbackSpeed.toInt()}x" else "${playbackSpeed}x",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = CycleRideTrackerTheme.colors.onSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    val speeds = listOf(1f, 5f, 10f, 20f)

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
                                                text = if (speed >= 1f) "${speed.toInt()}x" else "${speed}x",
                                                maxLines = 1, softWrap = false,
                                                overflow = TextOverflow.Visible
                                            )
                                        }
                                    )
                                },
                                menuContent = { menuState ->
                                    val isSelected = playbackSpeed == speed
                                    DropdownMenuItem(
                                        text = { Text(if (speed >= 1f) "${speed.toInt()}x" else "${speed}x") },
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
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
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
private fun ReplayJourneyPreview() {
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
