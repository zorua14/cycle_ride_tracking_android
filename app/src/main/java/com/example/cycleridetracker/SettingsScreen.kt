package com.example.cycleridetracker

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val haptic = LocalHapticFeedback.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Settings & Preferences",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ),
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            SettingsNavigationBar()
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = "Start Ride")
            }
        },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSectionTitle("THEME & DISPLAY")
                ThemeDisplayCard()
            }

            item {
                SettingsSectionTitle("RECORDING ENGINE")
                RecordingEngineSection()
            }

            item {
                SettingsSectionTitle("RIDE GOALS")
                GoalsSection()
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ThemeDisplayCard() {
    val haptic = LocalHapticFeedback.current
    var selectedTheme by remember { mutableStateOf("System") }
    var metricUnits by remember { mutableStateOf(value = true) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("App Theme Mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Choose system adaptive theme or override with light or dark.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                ConnectedThemeSelector(
                    selected = selectedTheme
                ) {
                    selectedTheme = it
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Metric Units (km, km/h, m)", style = MaterialTheme.typography.titleMedium)
                    Text("Metric (Kilometers)", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = metricUnits,
                    onCheckedChange = {
                        metricUnits = it
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}

@Composable
fun ConnectedThemeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("System", "Light", "Dark")
    val icons = listOf<ImageVector>(Icons.Default.Check, Icons.Default.WbSunny, Icons.Default.NightsStay)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSelected = selected == option
            val shape = when (index) {
                0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                options.size - 1 -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                else -> RoundedCornerShape(0.dp)
            }

            val containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }

            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Button(
                onClick = { onSelected(option) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if ((isSelected && index == 0)) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                    } else if (index > 0) {
                        Icon(icons[index], contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(option, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun RecordingEngineSection() {
    val haptic = LocalHapticFeedback.current
    var autoPause by remember { mutableStateOf(true) }
    var hapticFeedback by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            ListItem(
                headlineContent = { Text("GPS Sampling Rate", style = MaterialTheme.typography.titleMedium) },
                supportingContent = { Text("High Accuracy (1s)", style = MaterialTheme.typography.bodySmall) },
                trailingContent = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Select") },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            ListItem(
                headlineContent = { Text("Auto-Pause when Stopped", style = MaterialTheme.typography.titleMedium) },
                supportingContent = {
                    Text(
                        "Automatically pauses recording below 2 km/h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = autoPause,
                        onCheckedChange = {
                            autoPause = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            ListItem(
                headlineContent = { Text("Tactile Haptic Feedback", style = MaterialTheme.typography.titleMedium) },
                supportingContent = {
                    Text(
                        "Vibrations for buttons, hold-to-confirm, and key actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = hapticFeedback,
                        onCheckedChange = {
                            hapticFeedback = it
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@Composable
fun GoalsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            GoalListItem(
                icon = Icons.Outlined.Grid3x3,
                title = "Weekly Distance Goal",
                subtitle = "50 km per week"
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            GoalListItem(
                icon = Icons.Outlined.CalendarMonth,
                title = "Monthly Distance Goal",
                subtitle = "180 km per month"
            )
        }
    }
}

@Composable
fun GoalListItem(icon: ImageVector, title: String, subtitle: String) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
        },
        trailingContent = {
            IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
        },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}

@Composable
fun SettingsNavigationBar() {
    var selectedItem by remember { mutableIntStateOf(3) }
    val items = listOf("Dashboard", "History", "Insights", "Settings")
    val icons = listOf(Icons.Outlined.GridView, Icons.Outlined.History, Icons.Outlined.Timeline, Icons.Default.Tune)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(icons[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme {
        SettingsScreen()
    }
}