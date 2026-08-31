package com.example.cycleridetracker

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.data.ThemePrefs
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme

@Composable
fun SettingsContent(
    onThemeChanged: (String) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            SettingsSectionTitle("THEME & DISPLAY")
            ThemeDisplayCard(onThemeChanged)
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

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            color = CycleRideTrackerTheme.colors.primary,
            letterSpacing = 1.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun ThemeDisplayCard(onThemeChanged: (String) -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var selectedTheme by remember { mutableStateOf(ThemePrefs.getTheme(context)) }
    var metricUnits by remember { mutableStateOf(value = true) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    selected = selectedTheme
                ) {
                    selectedTheme = it
                    ThemePrefs.setTheme(context, it)
                    onThemeChanged(it)
                    AppHaptics.performSelection(haptic)
                }
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
                        "Metric (Kilometers)",
                        style = MaterialTheme.typography.bodySmall,
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
                Switch(
                    checked = metricUnits,
                    onCheckedChange = {
                        metricUnits = it
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
fun ConnectedThemeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("System", "Light", "Dark")
    val icons = listOf(Icons.Default.SettingsSuggest, Icons.Default.WbSunny, Icons.Default.NightsStay)

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelected(option) },
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
                }
            ) {
                Text(option, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun RecordingEngineSection() {
    val haptic = LocalHapticFeedback.current
    var hapticFeedback by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
            ),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        ) {
            ListItem(
                supportingContent = {
                    Text(
                        "High Accuracy (1s)",
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
                        checked = hapticFeedback,
                        onCheckedChange = {
                            hapticFeedback = it
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
fun GoalsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CycleRideTrackerTheme.colors.cardBackground
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
                containerColor = CycleRideTrackerTheme.colors.cardBackground
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
            IconButton(onClick = { AppHaptics.performSelection(haptic) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(20.dp),
                    tint = CycleRideTrackerTheme.colors.onSurfaceVariant
                )
            }
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

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    CycleRideTrackerTheme {
        SettingsContent()
    }
}
