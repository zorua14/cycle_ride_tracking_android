package com.example.cycleridetracker

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun InsightsContent(contentPadding: PaddingValues = PaddingValues(16.dp)) {
    val haptic = LocalHapticFeedback.current
    var selectedRange by remember { mutableStateOf("Week") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            TimeRangeSelector(
                selected = selectedRange,
                onSelected = {
                    selectedRange = it
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            )
        }

        item {
            GoalProgressCard(selectedRange)
        }

        item {
            InsightsSectionTitle("DISTANCE VISUALIZATION")
            DistanceVisualizationCard(selectedRange)
        }

        item {
            InsightsSectionTitle("RIDE PERFORMANCE METRICS")
            PerformanceMetricsGrid()
        }

        item {
            InsightsSectionTitle("ENVIRONMENTAL & CO2 FOOTPRINT")
            EnvironmentalPlaceholder()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(selected: String, onSelected: (String) -> Unit) {
    val options = listOf("Week", "Month", "All Time")
    val icons = listOf(Icons.Default.ViewWeek, Icons.Default.CalendarMonth, Icons.Default.AllInclusive)

    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelected(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoalProgressCard(selectedRange: String) {
    val targetProgress = when (selectedRange) {
        "Week" -> 0.78f
        "Month" -> 0.45f
        else -> 0.07f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "GoalProgressAnimation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    trackColor = MaterialTheme.colorScheme.outlineVariant
                )
                Text(
                    text = "${(animatedProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedRange == "Week") "WEEKLY MILEAGE GOAL" else "TARGET GOAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (selectedRange == "Week") "39.5 / 50 km" else "39.5 / 500 km",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (selectedRange == "Week") "10.5 km remaining to target." else "460.6 km remaining to target.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DistanceVisualizationCard(selectedRange: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "39.5 KM TOTAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "4 rides",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Crossfade(targetState = selectedRange, label = "ChartTransition") { range ->
                if (range == "Week") {
                    BarChartMock()
                } else {
                    LineChartMock()
                }
            }
        }
    }
}

@Composable
fun BarChartMock() {
    val haptic = LocalHapticFeedback.current
    val data = listOf(0.4f, 0.45f, 0.1f, 0.42f, 0.46f, 0.8f, 0.15f)
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var activeIndex by remember { mutableIntStateOf(-1) }

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(data.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = (offset.x / size.width * data.size).toInt().coerceIn(0, data.size - 1)
                            if (index != activeIndex) {
                                activeIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { change, _ ->
                            val index = (change.position.x / size.width * data.size).toInt().coerceIn(0, data.size - 1)
                            if (index != activeIndex) {
                                activeIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = { activeIndex = -1 },
                        onDragCancel = { activeIndex = -1 }
                    )
                }
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val outlineVariant = MaterialTheme.colorScheme.outlineVariant

            Canvas(modifier = Modifier.fillMaxSize()) {
                val barWidth = size.width / (data.size * 2)
                val spacing = size.width / data.size

                data.forEachIndexed { index, value ->
                    val x = index * spacing + spacing / 2
                    val height = value * size.height

                    // Background track
                    drawRoundRect(
                        color = outlineVariant.copy(alpha = 0.3f),
                        topLeft = Offset(x - barWidth / 2, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )

                    // Active bar
                    drawRoundRect(
                        color = if (index == activeIndex) primaryColor else primaryColor.copy(alpha = 0.7f),
                        topLeft = Offset(x - barWidth / 2, size.height - height),
                        size = Size(barWidth, height),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LineChartMock() {
    val haptic = LocalHapticFeedback.current
    val data = listOf(0.1f, 0.2f, 0.35f, 0.42f, 0.58f, 0.7f, 0.9f)
    val labels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
    var activeIndex by remember { mutableIntStateOf(-1) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(data.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = (offset.x / size.width * (data.size - 1)).roundToInt().coerceIn(0, data.size - 1)
                            if (index != activeIndex) {
                                activeIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDrag = { change, _ ->
                            val index = (change.position.x / size.width * (data.size - 1)).roundToInt().coerceIn(0, data.size - 1)
                            if (index != activeIndex) {
                                activeIndex = index
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        },
                        onDragEnd = { activeIndex = -1 },
                        onDragCancel = { activeIndex = -1 }
                    )
                }
        ) {
            val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val spacing = size.width / (data.size - 1)
                val path = Path()
                val fillPath = Path()

                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = size.height - (value * size.height)
                    if (index == 0) {
                        path.moveTo(x, y)
                        fillPath.moveTo(x, size.height)
                        fillPath.lineTo(x, y)
                    } else {
                        path.lineTo(x, y)
                        fillPath.lineTo(x, y)
                    }
                    if (index == data.size - 1) {
                        fillPath.lineTo(x, size.height)
                        fillPath.close()
                    }
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                data.forEachIndexed { index, value ->
                    val x = index * spacing
                    val y = size.height - (value * size.height)
                    val radius = if (index == activeIndex) 6.dp.toPx() else 4.dp.toPx()
                    val color = if (index == activeIndex) onPrimaryColor else primaryColor

                    if (index == activeIndex) {
                        drawCircle(color = primaryColor, radius = radius + 2.dp.toPx(), center = Offset(x, y))
                    }
                    drawCircle(color = color, radius = radius, center = Offset(x, y))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PerformanceMetricsGrid() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                label = "AVERAGE SPEED",
                value = "19.2",
                unit = "km/h"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.FlashOn,
                label = "PEAK MAX SPEED",
                value = "46.8",
                unit = "km/h"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Landscape,
                label = "TOTAL ELEVATION",
                value = "+382",
                unit = "m"
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                label = "RIDING TIME",
                value = "2h 3m",
                unit = ""
            )
        }
    }
}

@Composable
fun MetricCard(modifier: Modifier, icon: ImageVector, label: String, value: String, unit: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
}

@Composable
fun EnvironmentalPlaceholder() {
    Text(
        "CO2 savings and environmental impact data will appear here as you track more rides.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun InsightsSectionTitle(title: String) {
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

@Preview(showBackground = true)
@Composable
fun InsightsScreenPreview() {
    MaterialTheme {
        InsightsContent()
    }
}
