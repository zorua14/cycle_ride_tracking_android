package com.example.cycleridetracker

import android.util.Log
import android.view.MotionEvent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.Cyan400
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.ui.theme.Navy700


import androidx.compose.ui.viewinterop.AndroidView

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.ui.RideDetailViewModel
import com.example.cycleridetracker.ui.RideDetailUiState
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.core.content.ContextCompat
import com.example.cycleridetracker.ui.utils.MarkerUtils
import com.example.cycleridetracker.ui.utils.ConnectivityObserver
import com.example.cycleridetracker.ui.utils.NetworkConnectivityObserver
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Polyline
import de.afarber.openmapview.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailScreen(
    rideId: Int,
    onBack: () -> Unit,
    onReplayClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RideDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(rideId) {
        viewModel.loadRide(rideId)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scrollState = rememberLazyListState()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "RideDetailTransition",
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
                    Text("Failed to load ride detail.")
                }
            }
            is RideDetailUiState.Success -> {
                RideDetailSuccessContent(
                    state = state,
                    onBack = onBack,
                    onReplayClick = onReplayClick,
                    onAddPhotos = { viewModel.addPhotos(it) },
                    onUpdateNotes = { viewModel.updateNotes(it) },
                    onDeleteRide = { viewModel.deleteRide() },
                    onUpdateTitle = { viewModel.updateTitle(it) },
                    onDeletePhoto = { viewModel.deletePhoto(it) },
                    haptic = haptic,
                    context = context,
                    scrollState = scrollState,
                    modifier = modifier
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideDetailSuccessContent(
    state: RideDetailUiState.Success,
    onBack: () -> Unit,
    onReplayClick: () -> Unit,
    onAddPhotos: (List<Uri>) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onDeleteRide: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onDeletePhoto: (String) -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context,
    scrollState: LazyListState,
    modifier: Modifier = Modifier
) {
    val currentRide = state.ride
    val useMetric = state.useMetric
    val rideData = currentRide.toRideData(useMetric)
    val photoMarkers = remember { mutableStateMapOf<String, android.graphics.Bitmap>() }
    val pathPoints = remember(currentRide.pathPoints) {
        currentRide.pathPoints.map { LatLng(it.latitude, it.longitude) }
    }
    val avgLat = remember(currentRide.pathPoints) {
        currentRide.pathPoints.asSequence().map { it.latitude }.average()
    }
    val avgLng = remember(currentRide.pathPoints) {
        currentRide.pathPoints.asSequence().map { it.longitude }.average()
    }
    
    
    var fullScreenPhotoUri by rememberSaveable { mutableStateOf<String?>(value = null) }
    var showDeleteRideDialog by rememberSaveable { mutableStateOf(value = false) }
    var showEditTitleDialog by rememberSaveable { mutableStateOf(value = false) }
    var showDeletePhotoDialog by rememberSaveable { mutableStateOf<String?>(value = null) }

    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(
        initialValue = ConnectivityObserver.Status.Available
    )

    BackHandler(enabled = fullScreenPhotoUri != null) {
        fullScreenPhotoUri = null
    }

    LaunchedEffect(currentRide.photos) {
        Log.d("RideDetailScreen", "LaunchedEffect triggered for ${currentRide.photos.size} photos")
        currentRide.photos.forEach { photo ->
            if ((photo.latitude != null) && !photoMarkers.containsKey(photo.uri)) {
                Log.d("RideDetailScreen", "Processing photo marker for ${photo.uri} at ${photo.latitude}, ${photo.longitude}")
                // Offload bitmap loading and marker creation to background
                launch(Dispatchers.Default) {
                    val markerBitmap = MarkerUtils.loadMarkerBitmap(context, photo.uri)
                    if (markerBitmap != null) {
                        Log.d("RideDetailScreen", "Marker bitmap created for ${photo.uri}")
                        photoMarkers[photo.uri] = markerBitmap
                    }
                }
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onAddPhotos(uris)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_MEDIA_LOCATION] ?: false
        val mediaGranted = permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        
        if (locationGranted || mediaGranted) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { 
                                AppHaptics.performAction(haptic)
                                showEditTitleDialog = true 
                            }
                        ) {
                            Text(
                                rideData.title,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Title",
                                tint = CycleRideTrackerTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { 
                                AppHaptics.performAction(haptic)
                                showDeleteRideDialog = true 
                            }
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete Ride",
                                tint = MaterialTheme.colorScheme.error
                            )
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
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item(contentType = "Map") {
                    // OpenStreetMap View
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
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
                                            when (event.action) {
                                                MotionEvent.ACTION_DOWN -> {
                                                    v.parent.requestDisallowInterceptTouchEvent(true)
                                                }
                                                MotionEvent.ACTION_UP -> {
                                                    v.parent.requestDisallowInterceptTouchEvent(false)
                                                    v.performClick()
                                                }
                                                MotionEvent.ACTION_CANCEL -> {
                                                    v.parent.requestDisallowInterceptTouchEvent(false)
                                                }
                                            }
                                            v.onTouchEvent(event)
                                            true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize(),
                                update = { mapView ->
                                    if (pathPoints.isNotEmpty()) {
                                        mapView.clear()
                                        
                                        if (pathPoints.size >= 2) {
                                            val polyline = Polyline(
                                                points = pathPoints,
                                                strokeColor = Color.Cyan,
                                                strokeWidth = 12f
                                            )
                                            mapView.addPolyline(polyline)
                                        }
                                        
                                        // Photo markers
                                        currentRide.photos.forEach { photo ->
                                            if (photo.latitude != null && photo.longitude != null) {
                                                val customBitmap = photoMarkers[photo.uri]
                                                val markerIcon = customBitmap?.let { 
                                                    BitmapDescriptor.BitmapMarker(it) 
                                                }

                                                mapView.addMarker(Marker(
                                                    position = LatLng(photo.latitude, photo.longitude),
                                                    title = "Photo",
                                                    icon = markerIcon,
                                                    anchor = 0.5f to 1.0f
                                                ))
                                            }
                                        }
                                        
                                        if (mapView.tag != "initialized" && pathPoints.isNotEmpty()) {
                                            mapView.setCenter(LatLng(avgLat, avgLng))
                                            mapView.setZoom(14f)
                                            mapView.tag = "initialized"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                item(contentType = "Time") {
                    Text(
                        text = rideData.time,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }

                item(contentType = "ReplayButton") {
                    val isOffline = networkStatus != ConnectivityObserver.Status.Available
                    Button(
                        onClick = {
                            if (!isOffline) {
                                onReplayClick()
                            } else {
                                android.widget.Toast.makeText(context, "Internet connection required for Replay Journey", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = if (isOffline) CycleRideTrackerTheme.colors.onSurfaceVariant else Cyan400
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isOffline) CycleRideTrackerTheme.colors.outline else Navy700),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = (if (isOffline) CycleRideTrackerTheme.colors.onSurfaceVariant else Cyan400).copy(alpha = 0.1f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (isOffline) Icons.Default.CloudOff else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isOffline) CycleRideTrackerTheme.colors.onSurfaceVariant else Cyan400
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (isOffline) "OFFLINE - REPLAY UNAVAILABLE" else "REPLAY INTERACTIVE JOURNEY 🎬",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                                Text(
                                    if (isOffline) "Please connect to the internet to load map tiles." else "Scrub through every turn and view photo waypoints.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CycleRideTrackerTheme.colors.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CycleRideTrackerTheme.colors.onSurfaceVariant)
                        }
                    }
                }

                item(contentType = "Telemetry") {
                    SectionHeader("TELEMETRY SUMMARY")
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TelemetryCard(
                            icon = Icons.Default.Straighten,
                            label = "DISTANCE",
                            value = rideData.distance.split(" ")[0],
                            unit = if (useMetric) "km" else "mi",
                            modifier = Modifier.widthIn(min = 160.dp)
                        )
                        TelemetryCard(
                            icon = Icons.Default.Timer,
                            label = "MOVING TIME",
                            value = rideData.duration,
                            unit = "",
                            modifier = Modifier.widthIn(min = 160.dp)
                        )
                        TelemetryCard(
                            icon = Icons.Default.Speed,
                            label = "AVG SPEED",
                            value = rideData.avgSpeed.split(" ")[0],
                            unit = if (useMetric) "km/h" else "mph",
                            modifier = Modifier.widthIn(min = 160.dp)
                        )
                        TelemetryCard(
                            icon = Icons.Default.ElectricBolt,
                            label = "MAX SPEED",
                            value = "%.1f".format(if (useMetric) currentRide.maxSpeedKmh else currentRide.maxSpeedKmh * 0.621371f),
                            unit = if (useMetric) "km/h" else "mph",
                            modifier = Modifier.widthIn(min = 160.dp)
                        )
                    }
                }

                item(contentType = "Splits") {
                    SectionHeader("KILOMETER SPLITS")
                    Spacer(Modifier.height(16.dp))
                    KilometerSplitsSection()
                }

                item(contentType = "PhotosHeader") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("RIDE PHOTOS (${currentRide.photos.size})")
                        TextButton(onClick = { 
                            AppHaptics.performAction(haptic)
                            val hasLocationPerm = ContextCompat.checkSelfPermission(
                                context, 
                                Manifest.permission.ACCESS_MEDIA_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            val hasMediaPerm = ContextCompat.checkSelfPermission(
                                context, 
                                Manifest.permission.READ_MEDIA_IMAGES
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            if (hasLocationPerm && hasMediaPerm) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_MEDIA_LOCATION,
                                        Manifest.permission.READ_MEDIA_IMAGES
                                    )
                                )
                            }
                        }) {
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
                        items(
                            count = currentRide.photos.size,
                            key = { currentRide.photos[it].uri },
                            contentType = { "Photo" }
                        ) { index ->
                            val photo = currentRide.photos[index]
                            PhotoCard(
                                photo = photo,
                                icon = Icons.Default.CameraAlt,
                                onClick = { fullScreenPhotoUri = photo.uri }
                            )
                        }
                        item(contentType = "AddPhotoPlaceholder") {
                            AddPhotoPlaceholder(onClick = { 
                                val hasLocationPerm = ContextCompat.checkSelfPermission(
                                    context, 
                                    Manifest.permission.ACCESS_MEDIA_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                val hasMediaPerm = ContextCompat.checkSelfPermission(
                                    context, 
                                    Manifest.permission.READ_MEDIA_IMAGES
                                ) == PackageManager.PERMISSION_GRANTED
                                
                                if (hasLocationPerm && hasMediaPerm) {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_MEDIA_LOCATION,
                                            Manifest.permission.READ_MEDIA_IMAGES
                                        )
                                    )
                                }
                            })
                        }
                    }
                }

                item(contentType = "Notes") {
                    SectionHeader("RIDER NOTES")
                    Spacer(Modifier.height(16.dp))
                    
                    var isEditingNotes by rememberSaveable { mutableStateOf(false) }
                    var noteText by rememberSaveable(currentRide.notes) { mutableStateOf(currentRide.notes) }
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = CycleRideTrackerTheme.colors.cardBackground,
                        onClick = { isEditingNotes = true }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Default.Notes,
                                    contentDescription = null,
                                    tint = CycleRideTrackerTheme.colors.onSurfaceVariant
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = currentRide.notes.ifEmpty { "Add a note to this ride..." },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (currentRide.notes.isEmpty()) CycleRideTrackerTheme.colors.onSurfaceVariant else CycleRideTrackerTheme.colors.onSurface
                                )
                            }
                        }
                    }

                    if (isEditingNotes) {
                        val maxChars = 200
                        AlertDialog(
                            onDismissRequest = { isEditingNotes = false },
                            title = { Text("Ride Notes") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = noteText,
                                        onValueChange = { 
                                            if (it.length <= maxChars) noteText = it 
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Chilly breeze. Clear dedicated bike lanes.") },
                                        minLines = 3,
                                        supportingText = {
                                            Text(
                                                text = "${noteText.length} / $maxChars",
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                                            )
                                        }
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    onUpdateNotes(noteText)
                                    isEditingNotes = false
                                }) {
                                    Text("Save")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { isEditingNotes = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Full screen photo viewer
        AnimatedVisibility(
            visible = fullScreenPhotoUri != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            fullScreenPhotoUri?.let { uri ->
                var scale by rememberSaveable { mutableFloatStateOf(1f) }
                var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
                var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
                val swipeOffsetY = remember { Animatable(0f) }
                val coroutineScope = rememberCoroutineScope()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha = (0.95f * (1f - (kotlin.math.abs(swipeOffsetY.value) / 600f)))
                                    .coerceIn(0f, 0.95f)
                            )
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { fullScreenPhotoUri = null },
                                onDoubleTap = {
                                    scale = if (scale > 1f) 1f else 3f
                                    offsetX = 0f
                                    offsetY = 0f
                                    coroutineScope.launch { swipeOffsetY.animateTo(0f) }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.all { !it.pressed }) break
                                    
                                    val zoom = event.calculateZoom()
                                    val pan = event.calculatePan()
                                    
                                    if ((scale > 1f || zoom != 1f)) {
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                        // Reset swipe if we start zooming
                                        coroutineScope.launch { swipeOffsetY.snapTo(0f) }
                                    } else {
                                        // Swipe to dismiss logic
                                        coroutineScope.launch {
                                            swipeOffsetY.snapTo(swipeOffsetY.value + pan.y)
                                        }
                                    }
                                    event.changes.forEach { it.consume() }
                                }
                                
                                // End of gesture
                                if (scale == 1f) {
                                    if (kotlin.math.abs(swipeOffsetY.value) > 300f) {
                                        fullScreenPhotoUri = null
                                    } else {
                                        coroutineScope.launch {
                                            swipeOffsetY.animateTo(0f)
                                        }
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Full Screen Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY + swipeOffsetY.value
                            ),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    // Top Actions
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 48.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                AppHaptics.performAction(haptic)
                                showDeletePhotoDialog = uri 
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { fullScreenPhotoUri = null },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteRideDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteRideDialog = false },
                title = { Text("Delete Ride?") },
                text = { Text("This will permanently remove this ride and all associated photos. This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteRide()
                            showDeleteRideDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteRideDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEditTitleDialog) {
            var titleText by remember { mutableStateOf(currentRide.title) }
            AlertDialog(
                onDismissRequest = { showEditTitleDialog = false },
                title = { Text("Edit Ride Title") },
                text = {
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        onUpdateTitle(titleText)
                        showEditTitleDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditTitleDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeletePhotoDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeletePhotoDialog = null },
                title = { Text("Delete Photo?") },
                text = { Text("Are you sure you want to remove this photo from your ride?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeletePhotoDialog?.let { uri ->
                                onDeletePhoto(uri)
                                fullScreenPhotoUri = null
                            }
                            showDeletePhotoDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePhotoDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun KilometerSplitsSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Split data will be calculated in a future update.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleRideTrackerTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        color = CycleRideTrackerTheme.colors.onSurfaceVariant
    )
}

@Composable
fun TelemetryCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CycleRideTrackerTheme.colors.cardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Cyan400,
                    modifier = Modifier.size(16.dp)
                )


                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = value,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        )
                    ),
                    color = CycleRideTrackerTheme.colors.onSurface
                )

                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = unit,
                        modifier = Modifier.alignByBaseline(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            )
                        ),
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    photo: com.example.cycleridetracker.data.RidePhoto, 
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .size(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = photo.uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            
            // Location Badge
            if (photo.latitude != null) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Has Location",
                            tint = Cyan400,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Icon(
                icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
            )
        }
    }
}

@Composable
fun AddPhotoPlaceholder(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(140.dp),
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

@Preview(showBackground = true, heightDp = 2000)
@Composable
private fun RideDetailPreview() {
    CycleRideTrackerTheme(darkTheme = true) {
        RideDetailScreen(
            rideId = 1,
            onBack = {},
            onReplayClick = {}
        )
    }
}
