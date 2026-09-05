package com.example.cycleridetracker

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cycleridetracker.ui.haptics.AppHaptics
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerVisibilityListener
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import kotlin.math.roundToInt

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.cycleridetracker.ui.InsightsViewModel
import com.example.cycleridetracker.ui.InsightsStats
import com.example.cycleridetracker.ui.InsightsUiState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background

@Immutable
data class ChartData(
    val data: List<Float>,
    val labels: List<String>
)

@Composable
fun InsightsContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "InsightsTransition",
    ) { state ->
        when (state) {
            is InsightsUiState.Loading -> {
                InsightsLoadingPlaceholder(contentPadding)
            }
            is InsightsUiState.Success -> {
                InsightsSuccessContent(
                    state = state,
                    contentPadding = contentPadding,
                    onRangeSelect = { viewModel.selectRange(it) },
                    onNextMonth = { viewModel.nextMonth() },
                    onPreviousMonth = { viewModel.previousMonth() },
                    haptic = haptic,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
fun InsightsSuccessContent(
    state: InsightsUiState.Success,
    contentPadding: PaddingValues,
    onRangeSelect: (String) -> Unit,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item(contentType = "RangeSelector") {
            TimeRangeSelector(
                selected = state.selectedRange,
                onRangeSelect = {
                    onRangeSelect(it)
                    AppHaptics.performSelection(haptic)
                }
            )
        }

        if (state.stats.showGoal) {
            item(contentType = "GoalProgress") {
                GoalProgressCard(selectedRange = state.selectedRange, stats = state.stats)
            }
        }

        item(contentType = "DistanceVisualization") {
            InsightsSectionTitle(title = "DISTANCE VISUALIZATION")
            DistanceVisualizationCard(
                selectedRange = state.selectedRange,
                stats = state.stats,
                onNextMonth = onNextMonth,
                onPreviousMonth = onPreviousMonth
            )
        }

        item(contentType = "PerformanceMetrics") {
            InsightsSectionTitle(title = "RIDE PERFORMANCE METRICS")
            PerformanceMetricsGrid(stats = state.stats)
        }

        item(contentType = "EnvironmentalImpact") {
            InsightsSectionTitle(title = "ENVIRONMENTAL & CO2 FOOTPRINT")
            EnvironmentalPlaceholder()
        }
    }
}

@Composable
fun InsightsLoadingPlaceholder(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(CycleRideTrackerTheme.colors.cardBackground, RoundedCornerShape(24.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(CycleRideTrackerTheme.colors.cardBackground, RoundedCornerShape(24.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(CycleRideTrackerTheme.colors.cardBackground, RoundedCornerShape(24.dp))
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeRangeSelector(
    selected: String,
    onRangeSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("Week", "Month")
    val icons = listOf(Icons.Default.ViewWeek, Icons.Default.CalendarMonth)

    SingleChoiceSegmentedButtonRow(
        modifier = modifier.fillMaxWidth()
    ) {
        for (index in options.indices) {
            val option = options[index]
            SegmentedButton(
                selected = selected == option,
                onClick = { onRangeSelect(option) },
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
                    Text(text = option, style = MaterialTheme.typography.labelLarge)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GoalProgressCard(
    selectedRange: String,
    stats: InsightsStats,
    modifier: Modifier = Modifier
) {
    val goal = stats.currentGoal
    val current = stats.totalDistanceKmValue.toFloatOrNull() ?: 0f
    val targetProgress = (current / goal).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "GoalProgressAnimation"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                CircularWavyProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    trackColor = CycleRideTrackerTheme.colors.outline,
                    color = CycleRideTrackerTheme.colors.primary
                )
                Text(
                    text = "${(animatedProgress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = null,
                        tint = CycleRideTrackerTheme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedRange == "Week") "WEEKLY MILEAGE GOAL" else "TARGET GOAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CycleRideTrackerTheme.colors.primary)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${stats.totalDistanceKmValue} / ${goal.toInt()} ${stats.distanceUnit}",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                val remaining = (goal - current).coerceAtLeast(0f)
                Text(
                    text = "%.1f ${stats.distanceUnit} remaining to target.".format(remaining),
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleRideTrackerTheme.colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DistanceVisualizationCard(
    selectedRange: String,
    stats: InsightsStats,
    onNextMonth: () -> Unit,
    onPreviousMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${stats.totalDistanceKmValue} ${stats.distanceUnit.uppercase()} TOTAL",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface
                )
                Surface(
                    color = CycleRideTrackerTheme.colors.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${stats.rideCount} rides",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CycleRideTrackerTheme.colors.primary
                    )
                }
            }

            if (selectedRange == "Month") {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousMonth,
                        enabled = stats.canNavigatePrevious
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = if (stats.canNavigatePrevious) CycleRideTrackerTheme.colors.primary else CycleRideTrackerTheme.colors.outline
                        )
                    }
                    Text(
                        text = stats.displayMonth,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CycleRideTrackerTheme.colors.onSurface
                    )
                    IconButton(
                        onClick = onNextMonth,
                        enabled = stats.canNavigateNext
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Month",
                            tint = if (stats.canNavigateNext) CycleRideTrackerTheme.colors.primary else CycleRideTrackerTheme.colors.outline
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Crossfade(targetState = selectedRange, label = "ChartTransition") { range ->
                val chartData = ChartData(stats.chartData, stats.chartLabels)
                when (range) {
                    "Week", "Month" -> BarChart(chartData)
                }
            }
        }
    }
}

@Composable
fun rememberMarkerHapticListener(
    lastX: Double?,
    onLastXChange: (Double?) -> Unit,
): CartesianMarkerVisibilityListener {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, lastX) {
        object : CartesianMarkerVisibilityListener {
            override fun onShown(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val newX = targets.firstOrNull()?.x
                if (newX != lastX) {
                    AppHaptics.performAction(haptic)
                    onLastXChange(newX)
                }
            }

            override fun onUpdated(marker: CartesianMarker, targets: List<CartesianMarker.Target>) {
                val newX = targets.firstOrNull()?.x
                if (newX != lastX) {
                    AppHaptics.performAction(haptic)
                    onLastXChange(newX)
                }
            }

            override fun onHidden(marker: CartesianMarker) {
                onLastXChange(null)
            }
        }
    }
}

@Composable
fun BarChart(
    chartData: ChartData,
    modifier: Modifier = Modifier,
) {
    val data = chartData.data
    val labels = chartData.labels
    var lastX by remember { mutableStateOf<Double?>(null) }
    if (data.isEmpty() || data.all { it == 0f }) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No data available for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleRideTrackerTheme.colors.onSurfaceVariant
            )
        }
    } else {
        val model = remember(data) {
            CartesianChartModel(
                ColumnCartesianLayerModel.build { series(data) }
            )
        }
        
        val bottomAxisValueFormatter = CartesianValueFormatter { _, value, _ ->
            labels.getOrNull(value.toInt()) ?: ""
        }

        val colors = CycleRideTrackerTheme.colors

        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                        rememberLineComponent(
                            fill = Fill(colors.primary),
                            thickness = 12.dp,
                            shape = RoundedCornerShape(4.dp)
                        )
                    )
                ),
                startAxis = VerticalAxis.rememberStart(
                    label = rememberAxisLabelComponent(style = TextStyle(color = colors.onSurfaceVariant)),
                    line = null,
                    tick = null,
                    guideline = rememberLineComponent(fill = Fill(colors.outline.copy(alpha = 0.2f)))
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    label = rememberAxisLabelComponent(style = TextStyle(color = colors.onSurfaceVariant)),
                    line = null,
                    tick = null,
                    valueFormatter = bottomAxisValueFormatter
                ),
                marker = rememberDefaultCartesianMarker(
                    label = rememberTextComponent(
                        style = TextStyle(color = MaterialTheme.colorScheme.onPrimary),
                        background = rememberShapeComponent(fill = Fill(colors.primary), shape = RoundedCornerShape(4.dp))
                    )
                ),
                markerVisibilityListener = rememberMarkerHapticListener(
                lastX = lastX,
                onLastXChange = { lastX = it }
            )
            ),
            model = model,
            scrollState = rememberVicoScrollState(),
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp)
        )
    }
}

@Composable
fun PerformanceMetricsGrid(stats: InsightsStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                label = "AVERAGE SPEED",
                value = stats.avgSpeedValue,
                unit = stats.speedUnit
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.FlashOn,
                label = "PEAK MAX SPEED",
                value = stats.maxSpeedValue,
                unit = stats.speedUnit
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                label = "RIDING TIME",
                value = stats.totalDuration,
                unit = ""
            )
            Box(modifier = Modifier.weight(1f)) // Empty box to maintain layout grid
        }
    }
}

@Composable
fun MetricCard(
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CycleRideTrackerTheme.colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = CycleRideTrackerTheme.colors.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = CycleRideTrackerTheme.colors.onSurfaceVariant)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = CycleRideTrackerTheme.colors.onSurface,
                    modifier = Modifier.alignByBaseline()
                )
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        unit,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = CycleRideTrackerTheme.colors.primary,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }
        }
    }
}

@Composable
fun EnvironmentalPlaceholder(modifier: Modifier = Modifier) {
    Text(
        "CO2 savings and environmental impact data will appear here as you track more rides.",
        style = MaterialTheme.typography.bodySmall,
        color = CycleRideTrackerTheme.colors.onSurfaceVariant,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun InsightsSectionTitle(title: String, modifier: Modifier = Modifier) {
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

@Preview(showBackground = true)
@Composable
private fun InsightsScreenPreview() {
    CycleRideTrackerTheme {
        InsightsContent()
    }
}
