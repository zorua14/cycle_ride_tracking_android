package com.example.cycleridetracker.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.cycleridetracker.data.ActiveRideState
import com.example.cycleridetracker.ui.theme.CycleRideTrackerTheme
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActiveRideIndicator(
    activeRideState: ActiveRideState,
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTracking = activeRideState is ActiveRideState.Tracking
    val trackingData = activeRideState as? ActiveRideState.Tracking
    val isPaused = trackingData?.isPaused == true

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val coroutineScope = rememberCoroutineScope()
    
    // Initial position: Right edge, middle
    var hasInitializedPosition by remember { mutableStateOf(value = false) }
    val offsetX = remember { Animatable(screenWidthPx) }
    val offsetY = remember { Animatable(screenHeightPx / 2f) }
    
    var componentWidth by remember { mutableFloatStateOf(0f) }
    var componentHeight by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isTracking && !isPaused) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Handle visibility changes and initial positioning
    LaunchedEffect(isVisible, isTracking, screenWidthPx, componentWidth) {
        if ((isVisible && isTracking && !hasInitializedPosition && componentWidth > 0)) {
            offsetX.snapTo(screenWidthPx - componentWidth - with(density) { 16.dp.toPx() })
            hasInitializedPosition = true
        }
    }

    AnimatedVisibility(
        visible = isVisible && isTracking,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
            .offset {
                IntOffset(
                    offsetX.value.roundToInt(),
                    offsetY.value.roundToInt()
                )
            }
            .onGloballyPositioned {
                componentWidth = it.size.width.toFloat()
                componentHeight = it.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        coroutineScope.launch {
                            // Physics: Snap to nearest horizontal edge
                            val targetX = if (offsetX.value + componentWidth / 2 < screenWidthPx / 2) {
                                with(density) { 16.dp.toPx() } // Snap to Left
                            } else {
                                screenWidthPx - componentWidth - with(density) { 16.dp.toPx() } // Snap to Right
                            }
                            
                            // Constrain Y within screen
                            val targetY = offsetY.value.coerceIn(
                                with(density) { 64.dp.toPx() },
                                screenHeightPx - componentHeight - with(density) { 100.dp.toPx() }
                            )

                            launch {
                                offsetX.animateTo(
                                    targetX,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                                )
                            }
                            launch {
                                offsetY.animateTo(
                                    targetY,
                                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                                )
                            }
                        }
                    }
                )
            }
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            color = CycleRideTrackerTheme.colors.cardBackground,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.wrapContentSize()
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isPaused) CycleRideTrackerTheme.colors.onSurfaceVariant.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.DirectionsBike,
                        contentDescription = "Active Ride",
                        tint = if (isPaused) CycleRideTrackerTheme.colors.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(24.dp)
                            .scale(scale)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.padding(end = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isPaused) {
                        Text(
                            text = "PAUSED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = CycleRideTrackerTheme.colors.primary
                        )
                    } else {
                        Text(
                            text = formatDurationShort(trackingData?.durationMillis ?: 0L),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            color = CycleRideTrackerTheme.colors.onSurface
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "%.2f km", (trackingData?.distanceMeters ?: 0f) / 1000f),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = CycleRideTrackerTheme.colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDurationShort(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format(Locale.US, "%dh %dm", hours, minutes)
    } else {
        String.format(Locale.US, "%dm %ds", minutes, seconds)
    }
}
