
package com.example.cycleridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.cycleridetracker.data.ThemePrefs
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.data.Ride
import com.example.cycleridetracker.data.ActiveRideState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.ui.ActiveRideViewModel
import com.example.cycleridetracker.ui.DashboardViewModel
import com.example.cycleridetracker.ui.HistoryViewModel
import com.example.cycleridetracker.ui.SortOption
import com.example.cycleridetracker.ui.SortOrder
import com.example.cycleridetracker.ui.FilterOption
import com.example.cycleridetracker.ui.components.ActiveRideIndicator
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.example.cycleridetracker.ui.SettingsViewModel

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.location.LocationManager
import android.provider.Settings
import android.content.Context
import android.widget.Toast
import com.example.cycleridetracker.ui.utils.ConnectivityObserver
import com.example.cycleridetracker.ui.utils.NetworkConnectivityObserver
import kotlinx.coroutines.flow.map

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val themeMode by settingsViewModel.theme.collectAsStateWithLifecycle()

            val isDarkTheme = when (themeMode) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            val useDynamicColor = themeMode == "System"

            CycleRideTrackerTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
            ) {
                MainApp(settingsViewModel = settingsViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainApp(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    activeRideViewModel: ActiveRideViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val hapticsEnabled by settingsViewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val useMetric by settingsViewModel.useMetric.collectAsStateWithLifecycle()
    
    val activeState by activeRideViewModel.activeRideState.collectAsStateWithLifecycle()
    val isRideActive = activeState is ActiveRideState.Tracking

    val connectivityObserver = remember { NetworkConnectivityObserver(context) }
    val networkStatus by connectivityObserver.observe().collectAsStateWithLifecycle(
        initialValue = ConnectivityObserver.Status.Available
    )

    // Auto-resume service if ride is active in repository (recovered from DB)
    LaunchedEffect(isRideActive) {
        if (isRideActive) {
            val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                  ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (locationGranted) {
                activeRideViewModel.startRide()
            }
        }
    }

    var currentScreen by rememberSaveable { mutableStateOf("Dashboard") }
    var previousScreen by rememberSaveable { mutableStateOf("Dashboard") }
    var showActiveRideSheet by rememberSaveable { mutableStateOf(false) }
    var selectedRide by rememberSaveable { mutableStateOf<Ride?>(null) }
    
    var showLocationOffDialog by remember { mutableStateOf(false) }

    fun checkLocationAndNavigate() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        if (isGpsEnabled || isNetworkEnabled) {
            showActiveRideSheet = true
        } else {
            showLocationOffDialog = true
        }
    }

    // Permission handling
    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.POST_NOTIFICATIONS
    )
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val fineLocationGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseLocationGranted = result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (fineLocationGranted || coarseLocationGranted) {
            checkLocationAndNavigate()
        }
    }

    val floatingToolbarState = rememberFloatingToolbarState()
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
        state = floatingToolbarState,
    )

    val toolbarScreens = listOf("Dashboard", "History", "Insights", "Settings")

    BackHandler(enabled = showActiveRideSheet || currentScreen != "Dashboard") {
        if (showActiveRideSheet) {
            showActiveRideSheet = false
        } else {
            val nextScreen = when (currentScreen) {
                "RideDetail" -> previousScreen
                "ReplayJourney" -> "RideDetail"
                else -> "Dashboard"
            }

            // Immediate toolbar state adjustment for flicker-free transitions
            if (nextScreen !in toolbarScreens) {
                floatingToolbarState.offset = -2000f // Force hide
            } else {
                floatingToolbarState.offset = 0f
            }

            currentScreen = nextScreen
        }
    }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    var showSortFilterSheet by remember { mutableStateOf(value = false) }

    val toolbarContent: @Composable RowScope.() -> Unit = {
        val items = listOf("Dashboard", "History", "Insights", "Settings")
        val icons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Outlined.Timeline, Icons.Outlined.Tune)
        val selectedIcons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Default.Timeline, Icons.Default.Tune)

        items.forEachIndexed { index, item ->
            val isSelected = currentScreen == item

            IconButton(
                onClick = {
                    if (currentScreen != item) {
                        currentScreen = item
                        AppHaptics.performSelection(haptic, hapticsEnabled)
                        // Reset the toolbar scroll state when switching main tabs.
                        floatingToolbarState.offset = 0f
                        floatingToolbarState.contentOffset = 0f
                    } else {
                        AppHaptics.performAction(haptic, hapticsEnabled)
                    }
                }
            ) {
                Box(
                    modifier = if (isSelected) {
                        Modifier
                            .size(50.dp)
                            .background(
                                color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                    } else Modifier,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isSelected) selectedIcons[index] else icons[index],
                        contentDescription = item,
                        tint = if (isSelected) CycleRideTrackerTheme.colors.primary else CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }

    Box(modifier = modifier) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(floatingToolbarScrollBehavior)
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            // Header is handled per-screen for seamless transitions
        },
        floatingActionButton = {
            if (currentScreen in toolbarScreens) {
                if (isRideActive) {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                toolbarHeightPx = coordinates.size.height
                            }
                            .then(floatingToolbarScrollBehavior.floatingScrollBehaviorModifier),
                        scrollBehavior = floatingToolbarScrollBehavior,
                        content = toolbarContent
                    )
                } else {
                    HorizontalFloatingToolbar(
                        expanded = true,
                        floatingActionButton = {
                            FloatingToolbarDefaults.VibrantFloatingActionButton(
                                onClick = {
                                    AppHaptics.performAction(haptic, hapticsEnabled)
                                    previousScreen = currentScreen
                                    val locationGranted = (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                                          ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                                    
                                    if (locationGranted) {
                                        checkLocationAndNavigate()
                                    } else {
                                        launcher.launch(permissions)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Start Ride")
                            }
                        },
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                toolbarHeightPx = coordinates.size.height
                            }
                            .then(floatingToolbarScrollBehavior.floatingScrollBehaviorModifier),
                        scrollBehavior = floatingToolbarScrollBehavior,
                        content = toolbarContent
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = CycleRideTrackerTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Handle insets manually for stable transitions
    ) { innerPadding ->
        val measuredBottomPadding = with(density) { toolbarHeightPx.toDp() }

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val items = listOf("Dashboard", "History", "Insights", "Settings", "RideDetail", "ReplayJourney")
            
            AnimatedContent(
                targetState = currentScreen,
                label = "ScreenTransition",
                transitionSpec = {
                    val initialIndex = items.indexOf(initialState)
                    val targetIndex = items.indexOf(targetState)
                    val direction = if (targetIndex > initialIndex) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }

                    slideIntoContainer(towards = direction) togetherWith
                            slideOutOfContainer(towards = direction)
                },
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                Column(modifier = Modifier.fillMaxSize()) {
                    if (screen !in listOf("RideDetail", "ReplayJourney")) {
                        LargeTopAppBar(
                            title = {
                                Text(
                                    when (screen) {
                                        "Dashboard" -> "Dashboard"
                                        "Insights" -> "Cycling Insights"
                                        "Settings" -> "Settings & Preferences"
                                        "History" -> "Ride History"
                                        else -> screen
                                    },
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = CycleRideTrackerTheme.colors.largeTitle
                                    )
                                )
                            },
                            actions = {
                                when (screen) {
                                    "History" -> {
                                        IconButton(onClick = { 
                                            showSortFilterSheet = true
                                            AppHaptics.performAction(haptic, hapticsEnabled) 
                                        }) {
                                            Icon(Icons.Default.Tune, contentDescription = "Sort & Filter", tint = CycleRideTrackerTheme.colors.onSurface)
                                        }
                                    }
                                }
                            },
                            scrollBehavior = topAppBarScrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = CycleRideTrackerTheme.colors.background,
                                scrolledContainerColor = CycleRideTrackerTheme.colors.background,
                                titleContentColor = CycleRideTrackerTheme.colors.largeTitle
                            )
                        )
                    }

                    val contentPadding = PaddingValues(
                        top = 0.dp,
                        bottom = if (screen in toolbarScreens) {
                            measuredBottomPadding + 48.dp
                        } else {
                            16.dp
                        },
                        start = 16.dp,
                        end = 16.dp
                    )

                    when (screen) {
                        "Dashboard" -> DashboardContent(
                            contentPadding = contentPadding,
                            onRideClick = { ride ->
                                selectedRide = ride
                                previousScreen = "Dashboard"
                                currentScreen = "RideDetail"
                            },
                            onReplayLatest = { ride ->
                                if (networkStatus == ConnectivityObserver.Status.Available) {
                                    selectedRide = ride
                                    currentScreen = "ReplayJourney"
                                } else {
                                    Toast.makeText(context, "Internet connection required for Replay Journey", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onViewAllClick = {
                                currentScreen = "History"
                            },
                            networkStatus = networkStatus,
                        )
                        "RideDetail" -> selectedRide?.let { ride ->
                            RideDetailScreen(
                                rideId = ride.id,
                                onBack = {
                                    val prev = previousScreen
                                    currentScreen = prev
                                },
                                onReplayClick = {
                                    if (networkStatus == ConnectivityObserver.Status.Available) {
                                        currentScreen = "ReplayJourney"
                                    } else {
                                        Toast.makeText(context, "Internet connection required for Replay Journey", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        "ReplayJourney" -> selectedRide?.let { ride ->
                            ReplayJourneyScreen(
                                rideId = ride.id,
                                onBack = {
                                    currentScreen = "RideDetail"
                                }
                            )
                        }
                        "History" -> {
                            val historyUiState by historyViewModel.uiState.collectAsStateWithLifecycle()
                            val hapticsEnabledForHistory by historyViewModel.hapticsEnabled.collectAsStateWithLifecycle()
                            HistoryContent(
                                uiState = historyUiState,
                                hapticsEnabled = hapticsEnabledForHistory,
                                onSearchQueryChange = { historyViewModel.onSearchQueryChange(it) },
                                contentPadding = contentPadding,
                                onRideClick = { ride ->
                                    selectedRide = ride
                                    previousScreen = "History"
                                    currentScreen = "RideDetail"
                                },
                            )
                        }
                        "Insights" -> InsightsContent(contentPadding = contentPadding)
                        "Settings" -> SettingsContent(contentPadding = contentPadding)
                        else -> Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                            Text("Coming Soon: $screen", color = CycleRideTrackerTheme.colors.onSurface)
                        }
                    }
                }
            }

            ActiveRideIndicator(
                activeRideState = activeState,
                isVisible = currentScreen !in listOf("Settings"),
                onClick = {
                    showActiveRideSheet = true
                }
            )
        }
    }

    if (showActiveRideSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActiveRideSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CycleRideTrackerTheme.colors.background,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            contentWindowInsets = { WindowInsets.statusBars }
        ) {
            ActiveRideScreen(
                onFinish = { showActiveRideSheet = false }
            )
        }
    }

    if (showLocationOffDialog) {
        AlertDialog(
            onDismissRequest = { showLocationOffDialog = false },
            title = { Text("Location Disabled") },
            text = { Text("To track your ride, please enable device location in your system settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationOffDialog = false
                        context.startActivity(android.content.Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    }
                ) {
                    Text("Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationOffDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSortFilterSheet) {
        val currentSort by historyViewModel.sortOption.collectAsStateWithLifecycle()
        val currentOrder by historyViewModel.sortOrder.collectAsStateWithLifecycle()
        val currentFilter by historyViewModel.filterOption.collectAsStateWithLifecycle()

        ModalBottomSheet(
            onDismissRequest = { showSortFilterSheet = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = CycleRideTrackerTheme.colors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Sort & Filter",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "SORT BY",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOption.entries.forEach { option ->
                        FilterChip(
                            selected = currentSort == option,
                            onClick = { 
                                historyViewModel.onSortOptionChange(option)
                                AppHaptics.performSelection(haptic, hapticsEnabled)
                            },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "ORDER",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOrder.entries.forEach { order ->
                        FilterChip(
                            selected = currentOrder == order,
                            onClick = { 
                                historyViewModel.onSortOrderChange(order)
                                AppHaptics.performSelection(haptic, hapticsEnabled)
                            },
                            label = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Text(
                    "FILTER BY DATE",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.primary
                )
                
                Spacer(Modifier.height(12.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterOption.entries.forEach { option ->
                        FilterChip(
                            selected = currentFilter == option,
                            onClick = { 
                                historyViewModel.onFilterOptionChange(option)
                                AppHaptics.performSelection(haptic, hapticsEnabled)
                            },
                            label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                Spacer(Modifier.height(48.dp))
            }
        }
    }
}
}
