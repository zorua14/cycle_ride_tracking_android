
package com.example.cycleridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.FavoriteBorder
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
import com.example.cycleridetracker.data.ThemePrefs
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.example.cycleridetracker.ui.components.RideData
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

val RideDataSaver = listSaver<RideData?, Any>(
    save = { 
        if (it == null) emptyList() 
        else listOf(it.title, it.time, it.distance, it.duration, it.avgSpeed, it.isFavorite) 
    },
    restore = { 
        if (it.isEmpty()) null 
        else RideData(
            title = it[0] as String,
            time = it[1] as String,
            distance = it[2] as String,
            duration = it[3] as String,
            avgSpeed = it[4] as String,
            isFavorite = it[5] as Boolean
        ) 
    }
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val themeMode = remember { mutableStateOf(ThemePrefs.getTheme(context)) }

            val isDarkTheme = when (themeMode.value) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            val useDynamicColor = themeMode.value == "System"

            CycleRideTrackerTheme(
                darkTheme = isDarkTheme,
                dynamicColor = useDynamicColor,
            ) {
                MainApp { themeMode.value = it }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainApp(onThemeChanged: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var currentScreen by rememberSaveable { mutableStateOf("Dashboard") }
    var previousScreen by rememberSaveable { mutableStateOf("Dashboard") }
    var selectedRide by rememberSaveable(stateSaver = RideDataSaver) { mutableStateOf<RideData?>(null) }

    val floatingToolbarState = rememberFloatingToolbarState()
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
        state = floatingToolbarState,
    )

    val toolbarScreens = listOf("Dashboard", "History", "Insights", "Settings")

    BackHandler(enabled = currentScreen != "Dashboard") {
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
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

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
                HorizontalFloatingToolbar(
                    expanded = true,
                    floatingActionButton = {
                        FloatingToolbarDefaults.VibrantFloatingActionButton(
                            onClick = {
                                AppHaptics.performAction(haptic)
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
                ) {
                    val items = listOf("Dashboard", "History", "Insights", "Settings")
                    val icons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Outlined.Timeline, Icons.Outlined.Tune)
                    val selectedIcons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Default.Timeline, Icons.Default.Tune)

                    items.forEachIndexed { index, item ->
                        val isSelected = currentScreen == item

                        IconButton(
                            onClick = {
                                if (currentScreen != item) {
                                    currentScreen = item
                                    AppHaptics.performSelection(haptic)
                                    // Reset the toolbar scroll state when switching main tabs.
                                    floatingToolbarState.offset = 0f
                                    floatingToolbarState.contentOffset = 0f
                                } else {
                                    AppHaptics.performAction(haptic)
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
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = CycleRideTrackerTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // Handle insets manually for stable transitions
    ) { innerPadding ->
        val measuredBottomPadding = with(density) { toolbarHeightPx.toDp() }

        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            SharedTransitionLayout {
                val items = listOf("Dashboard", "History", "Insights", "Settings", "RideDetail", "ReplayJourney")
                AnimatedContent(
                    targetState = when (currentScreen) {
                        "RideDetail" -> "RideDetail"
                        "ReplayJourney" -> "ReplayJourney"
                        else -> currentScreen
                    },
                    label = "ScreenTransition",
                    transitionSpec = {
                        if ((initialState in listOf("RideDetail", "ReplayJourney")) ||
                            (targetState in listOf("RideDetail", "ReplayJourney"))) {
                            fadeIn(tween(600)) togetherWith fadeOut(tween(600))
                        } else {
                            val initialIndex = items.indexOf(initialState)
                            val targetIndex = items.indexOf(targetState)
                            val direction = if (targetIndex > initialIndex) {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            }

                            (slideIntoContainer(
                                towards = direction,
                                animationSpec = tween(280)
                            ) + fadeIn(animationSpec = tween(280))) togetherWith
                                    (slideOutOfContainer(
                                        towards = direction,
                                        animationSpec = tween(280)
                                    ) + fadeOut(animationSpec = tween(280)))
                        }
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
                                        "Dashboard" -> {
                                            IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                                                Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = "Help", tint = CycleRideTrackerTheme.colors.onSurface, modifier = Modifier.size(28.dp))
                                            }
                                        }
                                        "Insights" -> {
                                            IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                                                Icon(Icons.Default.Share, contentDescription = "Share", tint = CycleRideTrackerTheme.colors.onSurface)
                                            }
                                        }
                                        "History" -> {
                                            IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                                                Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites", tint = CycleRideTrackerTheme.colors.onSurface)
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
                                onReplayLatest = {
                                    // Mock selecting the first ride for replay
                                    selectedRide = RideData(
                                        title = "Morning Downtown Ride",
                                        time = "Thu, Aug 27 • 4:45 PM",
                                        distance = "8.4 km",
                                        duration = "28:00",
                                        avgSpeed = "19.8 km/h",
                                        isFavorite = true
                                    )
                                    currentScreen = "ReplayJourney"
                                },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            )
                            "RideDetail" -> selectedRide?.let { ride ->
                                RideDetailScreen(
                                    ride = ride,
                                    onBack = {
                                        val prev = previousScreen
                                        currentScreen = prev
                                    },
                                    onReplayClick = {
                                        currentScreen = "ReplayJourney"
                                    },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedContent,
                                    keyPrefix = previousScreen.lowercase()
                                )
                            }
                            "ReplayJourney" -> selectedRide?.let { ride ->
                                ReplayJourneyScreen(
                                    ride = ride,
                                    onBack = {
                                        currentScreen = "RideDetail"
                                    }
                                )
                            }
                            "History" -> HistoryContent(
                                contentPadding = contentPadding,
                                onRideClick = { ride ->
                                    selectedRide = ride
                                    previousScreen = "History"
                                    currentScreen = "RideDetail"
                                },
                                sharedTransitionScope = this@SharedTransitionLayout,
                                animatedVisibilityScope = this@AnimatedContent
                            )
                            "Insights" -> InsightsContent(contentPadding)
                            "Settings" -> SettingsContent(onThemeChanged, contentPadding)
                            else -> Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                                Text("Coming Soon: $screen", color = CycleRideTrackerTheme.colors.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

