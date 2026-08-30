package com.example.cycleridetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
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
import androidx.compose.foundation.isSystemInDarkTheme

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
                dynamicColor = useDynamicColor
            ) {
                MainApp(onThemeChanged = { themeMode.value = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainApp(onThemeChanged: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var currentScreen by remember { mutableStateOf("Insights") }
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val floatingToolbarState = rememberFloatingToolbarState()
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
        state = floatingToolbarState
    )

    var toolbarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
            .nestedScroll(floatingToolbarScrollBehavior),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        if (currentScreen == "Insights") "Cycling Insights" else "Settings & Preferences",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CycleRideTrackerTheme.colors.largeTitle
                        )
                    )
                },
                actions = {
                    if (currentScreen == "Insights") {
                        IconButton(onClick = { AppHaptics.performAction(haptic) }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "Share",
                                tint = CycleRideTrackerTheme.colors.onSurface
                            )
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
        },
        floatingActionButton = {
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
                scrollBehavior = floatingToolbarScrollBehavior
            ) {
                val items = listOf("Dashboard", "History", "Insights", "Settings")
                val icons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Outlined.Timeline, Icons.Outlined.Tune)
                val selectedIcons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Default.Timeline, Icons.Default.Tune)

                items.forEachIndexed { index, item ->
                    val isSelected = currentScreen == item
                    val scale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                        label = "IconScale"
                    )

                    IconButton(
                        onClick = {
                            if (currentScreen != item && (item == "Insights" || item == "Settings")) {
                                currentScreen = item
                                AppHaptics.performSelection(haptic)
                            } else {
                                AppHaptics.performAction(haptic)
                            }
                        },
                        modifier = Modifier.scale(scale)
                    ) {
                        Icon(
                            if (isSelected) selectedIcons[index] else icons[index],
                            contentDescription = item,
                            tint = if (isSelected) CycleRideTrackerTheme.colors.primary else CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = CycleRideTrackerTheme.colors.background
    ) { innerPadding ->
        val measuredBottomPadding = with(density) { toolbarHeightPx.toDp() }

        Box(modifier = Modifier.fillMaxSize()) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                val contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding() + measuredBottomPadding + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                )
                when (screen) {
                    "Insights" -> InsightsContent(contentPadding)
                    "Settings" -> SettingsContent(onThemeChanged, contentPadding)
                    else -> Box(
                        Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Coming Soon: $screen")
                    }
                }
            }
        }
    }
}
