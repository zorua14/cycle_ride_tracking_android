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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.ui.RideDetailViewModel
import com.example.cycleridetracker.ui.RideDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplayJourneyScreen(
    rideId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(rideId) {
        viewModel.loadRide(rideId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ReplayJourneyTransition",
        contentKey = { it::class },
    ) { state ->
        when (state) {
            is RideDetailUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is RideDetailUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Failed to load journey data.")
                }
            }
            is RideDetailUiState.Success -> {
                ReplayJourneyContent(
                    ride = state.ride,
                    useMetric = state.useMetric,
                    onBack = onBack,
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReplayJourneyContent(
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
    
    val sortedPhotos = remember(ride.photos) { ride.photos.sortedBy { it.timestamp } }
    var displayedPhotoUris by rememberSaveable { mutableStateOf(listOf<String>()) }
    var lastActivePhotoUris by remember { mutableStateOf(listOf<String>()) }
    var alreadyPausedUris by remember { mutableStateOf(setOf<String>()) }
    
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
            displayedPhotoUris = emptyList()
            lastActivePhotoUris = emptyList()
            alreadyPausedUris = emptySet()
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

                // Check for new photos nearby to pause playback
                val pointIndex = currentFrame.coerceAtMost(pathPoints.size - 1)
                val currentPoint = pathPoints.getOrNull(pointIndex)
                val photosAtWaypoint = currentPoint?.let { cp ->
                    sortedPhotos.filter { photo ->
                        if (photo.latitude != null && photo.longitude != null && !alreadyPausedUris.contains(photo.uri)) {
                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                cp.latitude, cp.longitude,
                                photo.latitude, photo.longitude,
                                results
                            )
                            results[0] <= 30f // trigger pause within 30m
                        } else false
                    }
                } ?: emptyList()

                if (photosAtWaypoint.isNotEmpty()) {
                    // Mark as paused immediately to avoid re-triggering
                    alreadyPausedUris = alreadyPausedUris + photosAtWaypoint.map { it.uri }
                    
                    // Pause for 3 seconds as requested
                    delay(3000.milliseconds)
                    
                    // Reset lastTime so we don't have a huge jump in deltaTimeSec after the pause
                    lastTime = android.os.SystemClock.elapsedRealtime()
                }

                subFrame += (baseFps * playbackSpeed) * deltaTimeSec
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
                } else if (pathPoints.size < 2) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CycleRideTrackerTheme.colors.cardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = CycleRideTrackerTheme.colors.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Not enough data points",
                                color = CycleRideTrackerTheme.colors.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "This journey has only one recorded point and cannot be replayed.",
                                color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
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
                
                // Photo Waypoint Overlay (Proximity check: 30 meters)
                val currentPoint = if (pathPoints.isNotEmpty()) {
                    val pointIndex = currentFrame.coerceAtMost(pathPoints.size - 1)
                    pathPoints.getOrElse(pointIndex) { pathPoints.last() }
                } else null

                val activePhotos = remember(currentPoint, sortedPhotos, displayedPhotoUris) {
                    currentPoint?.let { cp ->
                        sortedPhotos.filter { photo ->
                            if (photo.latitude != null && photo.longitude != null && !displayedPhotoUris.contains(photo.uri)) {
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
                } ?: emptyList()
                
                LaunchedEffect(activePhotos) {
                    if (activePhotos.isEmpty() && lastActivePhotoUris.isNotEmpty()) {
                        // Rider moved away from a photo location
                        displayedPhotoUris = (displayedPhotoUris + lastActivePhotoUris).distinct()
                        lastActivePhotoUris = emptyList()
                    } else if (activePhotos.isNotEmpty()) {
                        lastActivePhotoUris = (lastActivePhotoUris + activePhotos.map { it.uri }).distinct()
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = activePhotos.isNotEmpty(),
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    if (activePhotos.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = CycleRideTrackerTheme.colors.background.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "PHOTO WAYPOINT${if (activePhotos.size > 1) "S" else ""}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                                            color = CycleRideTrackerTheme.colors.onSurfaceVariant
                                        )
                                        Text(
                                            if (activePhotos.size > 1) "${activePhotos.size} snapshots nearby" else "Snapshot from ride",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = CycleRideTrackerTheme.colors.onSurface
                                        )
                                    }
                                    if (activePhotos.size > 1) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = "Swipe to see more",
                                            tint = CycleRideTrackerTheme.colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(12.dp))
                                
                                if (activePhotos.size == 1) {
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                    ) {
                                        AsyncImage(
                                            model = activePhotos[0].uri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(activePhotos.size) { index ->
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.size(width = 240.dp, height = 180.dp)
                                            ) {
                                                AsyncImage(
                                                    model = activePhotos[index].uri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                        // Reset seen photos so waypoints can trigger again if we scrub back/over them
                        displayedPhotoUris = emptyList()
                        lastActivePhotoUris = emptyList()
                        alreadyPausedUris = emptySet()
                    },
                    valueRange = 0f..(totalFrames - 1).toFloat().coerceAtLeast(0f),
                    enabled = pathPoints.size >= 2,
                    colors = SliderDefaults.colors(
                        thumbColor = CycleRideTrackerTheme.colors.primary,
                        activeTrackColor = CycleRideTrackerTheme.colors.primary,
                        inactiveTrackColor = CycleRideTrackerTheme.colors.cardBackground,
                        disabledThumbColor = CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f),
                        disabledActiveTrackColor = CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f)
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
                        IconButton(onClick = { currentFrame = 0 }, enabled = pathPoints.size >= 2) {
                            Icon(Icons.Default.Replay, contentDescription = "Reset", tint = if (pathPoints.size >= 2) CycleRideTrackerTheme.colors.onSurface else CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f))
                        }
                        IconButton(onClick = { currentFrame = (currentFrame - 5).coerceAtLeast(0) }, enabled = pathPoints.size >= 2) {
                            Icon(Icons.Default.FastRewind, contentDescription = "Back", tint = if (pathPoints.size >= 2) CycleRideTrackerTheme.colors.onSurface else CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f))
                        }
                        FloatingActionButton(
                            onClick = { 
                                if (pathPoints.size >= 2) {
                                    isPlaying = !isPlaying
                                }
                            },
                            containerColor = if (pathPoints.size >= 2) CycleRideTrackerTheme.colors.primary else CycleRideTrackerTheme.colors.outline,
                            contentColor = if (pathPoints.size >= 2) CycleRideTrackerTheme.colors.background else CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f),
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        IconButton(onClick = { currentFrame = (currentFrame + 5).coerceAtMost(totalFrames - 1) }, enabled = pathPoints.size >= 2) {
                            Icon(Icons.Default.FastForward, contentDescription = "Forward", tint = if (pathPoints.size >= 2) CycleRideTrackerTheme.colors.onSurface else CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.38f))
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
                                        enabled = pathPoints.size >= 2,
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
        ReplayJourneyContent(
            ride = mockRide,
            useMetric = true,
            onBack = {}
        )
    }
}
