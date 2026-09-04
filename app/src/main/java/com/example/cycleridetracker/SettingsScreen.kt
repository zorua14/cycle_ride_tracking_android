package com.example.cycleridetracker


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.data.ThemePrefs
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.ui.SettingsViewModel

@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    onThemeChange: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(16.dp),
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val selectedTheme by viewModel.theme.collectAsStateWithLifecycle()
    val useMetric by viewModel.useMetric.collectAsStateWithLifecycle()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsStateWithLifecycle()
    val samplingRate by viewModel.samplingRate.collectAsStateWithLifecycle()
    val persistenceInterval by viewModel.persistenceInterval.collectAsStateWithLifecycle()
    val weeklyGoal by viewModel.weeklyGoal.collectAsStateWithLifecycle()
    val monthlyGoal by viewModel.monthlyGoal.collectAsStateWithLifecycle()

    var showSamplingDialog by remember { mutableStateOf(value = false) }
    var showPersistenceDialog by remember { mutableStateOf(value = false) }
    var showWeeklyGoalDialog by remember { mutableStateOf(value = false) }
    var showMonthlyGoalDialog by remember { mutableStateOf(value = false) }

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item(contentType = "Theme") {
                SettingsSectionTitle("THEME & DISPLAY")
                ThemeDisplayCard(
                    selectedTheme = selectedTheme,
                    useMetric = useMetric,
                    onThemeSelect = {
                        viewModel.setTheme(it)
                        onThemeChange(it)
                    },
                    onMetricToggle = { viewModel.setUseMetric(it) }
                )
            }

            item(contentType = "RecordingEngine") {
                SettingsSectionTitle("RECORDING ENGINE")
                RecordingEngineSection(
                    samplingRate = samplingRate,
                    persistenceInterval = persistenceInterval,
                    hapticsEnabled = hapticsEnabled,
                    onSamplingRateClick = { showSamplingDialog = true },
                    onPersistenceIntervalClick = { showPersistenceDialog = true },
                    onHapticsToggle = { viewModel.setHapticsEnabled(it) }
                )
            }

            item(contentType = "RideGoals") {
                SettingsSectionTitle("RIDE GOALS")
                GoalsSection(
                    weeklyGoal = weeklyGoal,
                    monthlyGoal = monthlyGoal,
                    useMetric = useMetric,
                    onWeeklyGoalClick = { showWeeklyGoalDialog = true },
                    onMonthlyGoalClick = { showMonthlyGoalDialog = true }
                )
            }
        }

        if (showSamplingDialog) {
            SamplingRateDialog(
                currentRate = samplingRate,
                onDismiss = { showSamplingDialog = false },
                onSelect = {
                    viewModel.setSamplingRate(it)
                    showSamplingDialog = false
                }
            )
        }

        if (showPersistenceDialog) {
            PersistenceIntervalDialog(
                currentInterval = persistenceInterval,
                onDismiss = { showPersistenceDialog = false },
                onSelect = {
                    viewModel.setPersistenceInterval(it)
                    showPersistenceDialog = false
                }
            )
        }

        if (showWeeklyGoalDialog) {
            GoalEditDialog(
                title = "Weekly Distance Goal",
                currentGoal = weeklyGoal,
                useMetric = useMetric,
                onDismiss = { showWeeklyGoalDialog = false },
                onConfirm = {
                    viewModel.setWeeklyGoal(it)
                    showWeeklyGoalDialog = false
                }
            )
        }

        if (showMonthlyGoalDialog) {
            GoalEditDialog(
                title = "Monthly Distance Goal",
                currentGoal = monthlyGoal,
                useMetric = useMetric,
                onDismiss = { showMonthlyGoalDialog = false },
                onConfirm = {
                    viewModel.setMonthlyGoal(it)
                    showMonthlyGoalDialog = false
                }
            )
        }
    }
}

@Composable
fun SettingsSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = CycleRideTrackerTheme.colors.primary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ThemeDisplayCard(
    selectedTheme: String,
    useMetric: Boolean,
    onThemeSelect: (String) -> Unit,
    onMetricToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "App Theme Mode",
                            style = MaterialTheme.typography.titleMedium,
                            color = CycleRideTrackerTheme.colors.onSurface
                        )
                        Text(
                            "Choose system adaptive theme or override with light or dark.",
                            style = MaterialTheme.typography.bodySmall,
                            color = CycleRideTrackerTheme.colors.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                ConnectedThemeSelector(
                    selected = selectedTheme,
                    onSelect = {
                        onThemeSelect(it)
                        AppHaptics.performSelection(haptic)
                    }
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
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
                    Text(
                        "Metric Units (km, km/h, m)",
                        style = MaterialTheme.typography.titleMedium,
                        color = CycleRideTrackerTheme.colors.onSurface
                    )
                    Text(
                        if (useMetric) "Metric (Kilometers)" else "Imperial (Miles)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useMetric,
                    onCheckedChange = {
                        onMetricToggle(it)
                        AppHaptics.performAction(haptic)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CycleRideTrackerTheme.colors.primary,
                        checkedTrackColor = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = CycleRideTrackerTheme.colors.onSurfaceVariant,
                        uncheckedTrackColor = CycleRideTrackerTheme.colors.cardBackground
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectedThemeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("System", "Light", "Dark")
    val icons = listOf(Icons.Default.SettingsSuggest, Icons.Default.WbSunny, Icons.Default.NightsStay)

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = CycleRideTrackerTheme.colors.primary,
                    activeContentColor = CycleRideTrackerTheme.colors.background,
                    inactiveContainerColor = CycleRideTrackerTheme.colors.cardBackground,
                    inactiveContentColor = CycleRideTrackerTheme.colors.onSurfaceVariant
                ),
                icon = {
                    SegmentedButtonDefaults.Icon(active = selected == option) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(option, style = MaterialTheme.typography.labelLarge)
                }
            )
        }
    }
}

@Composable
fun RecordingEngineSection(
    samplingRate: Long,
    persistenceInterval: Long,
    hapticsEnabled: Boolean,
    onSamplingRateClick: () -> Unit,
    onPersistenceIntervalClick: () -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSamplingRateClick,
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            ListItem(
                supportingContent = {
                    Text(
                        when (samplingRate) {
                            1000L -> "High Accuracy (1s)"
                            2000L -> "Balanced (2s)"
                            5000L -> "Battery Saver (5s)"
                            else -> "${samplingRate / 1000}s"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                Text(
                    "GPS Sampling Rate",
                    style = MaterialTheme.typography.titleMedium,
                    color = CycleRideTrackerTheme.colors.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onPersistenceIntervalClick,
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            ListItem(
                supportingContent = {
                    val minutes = persistenceInterval / 60000
                    Text(
                        if (minutes < 1) "Every ${persistenceInterval / 1000} seconds" else "Every $minutes minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Select",
                        tint = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                Text(
                    "Database Sync Frequency",
                    style = MaterialTheme.typography.titleMedium,
                    color = CycleRideTrackerTheme.colors.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            ListItem(
                supportingContent = {
                    Text(
                        "Vibrations for buttons, hold-to-confirm, and key actions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                },
                trailingContent = {
                    Switch(
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            onHapticsToggle(it)
                            AppHaptics.performAction(haptic)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CycleRideTrackerTheme.colors.primary,
                            checkedTrackColor = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.5f)
                        )
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            ) {
                Text(
                    "Tactile Haptic Feedback",
                    style = MaterialTheme.typography.titleMedium,
                    color = CycleRideTrackerTheme.colors.onSurface
                )
            }
        }
    }
}

@Composable
fun GoalsSection(
    weeklyGoal: Float,
    monthlyGoal: Float,
    useMetric: Boolean,
    onWeeklyGoalClick: () -> Unit,
    onMonthlyGoalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unit = if (useMetric) "km" else "mi"
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onWeeklyGoalClick,
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            GoalListItem(
                icon = Icons.Outlined.Grid3x3,
                title = "Weekly Distance Goal",
                subtitle = "${weeklyGoal.toInt()} $unit per week"
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onMonthlyGoalClick,
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        ) {
            GoalListItem(
                icon = Icons.Outlined.CalendarMonth,
                title = "Monthly Distance Goal",
                subtitle = "${monthlyGoal.toInt()} $unit per month"
            )
        }
    }
}

@Composable
fun GoalListItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    ListItem(
        modifier = modifier,
        supportingContent = {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = CycleRideTrackerTheme.colors.onSurfaceVariant
            )
        },
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CycleRideTrackerTheme.colors.cardBackground,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = CycleRideTrackerTheme.colors.primary
                    )
                }
            }
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Edit",
                modifier = Modifier.size(20.dp),
                tint = CycleRideTrackerTheme.colors.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = CycleRideTrackerTheme.colors.onSurface
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SamplingRateDialog(
    currentRate: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        1000L to "1s",
        2000L to "2s",
        5000L to "5s"
    )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("GPS Sampling Rate") },
        text = {
            Column {
                Text(
                    "Set how often GPS location is updated. Higher rates improve accuracy but consume more battery.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { (rate, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = rate == currentRate,
                            onClick = { onSelect(rate) },
                            label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label) } },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CycleRideTrackerTheme.colors.primary,
                                selectedLabelColor = CycleRideTrackerTheme.colors.background
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersistenceIntervalDialog(
    currentInterval: Long,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        60000L to "1 min",
        300000L to "5 mins",
        600000L to "10 mins",
        900000L to "15 mins",
        1800000L to "30 mins"
    )

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text("Database Sync Frequency") },
        text = {
            Column {
                Text(
                    "Choose how often tracking data is saved. Frequent saves protect data; longer intervals save battery.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // First Row: 3 items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.take(3).forEach { (interval, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = interval == currentInterval,
                            onClick = { onSelect(interval) },
                            label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label) } },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CycleRideTrackerTheme.colors.primary,
                                selectedLabelColor = CycleRideTrackerTheme.colors.background
                            )
                        )
                    }
                }
                
                Spacer(Modifier.height(8.dp))
                
                // Second Row: 2 items
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.drop(3).forEach { (interval, label) ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = interval == currentInterval,
                            onClick = { onSelect(interval) },
                            label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(label) } },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CycleRideTrackerTheme.colors.primary,
                                selectedLabelColor = CycleRideTrackerTheme.colors.background
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun GoalEditDialog(
    title: String,
    currentGoal: Float,
    useMetric: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var textValue by remember { mutableStateOf(currentGoal.toInt().toString()) }
    val unit = if (useMetric) "km" else "mi"

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text("Set your target distance ($unit)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { if (it.all { char -> char.isDigit() }) textValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    suffix = { Text(unit) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    textValue.toFloatOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    CycleRideTrackerTheme {
        SettingsContent(
            modifier = Modifier,
            onThemeChange = {},
            contentPadding = PaddingValues(16.dp),
        )
    }
}
