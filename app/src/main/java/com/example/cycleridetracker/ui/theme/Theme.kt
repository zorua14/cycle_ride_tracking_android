package com.example.cycleridetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class AppColors(
    val primary: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val onSurfaceVariant: Color = Color.Unspecified,
    val largeTitle: Color = Color.Unspecified,
    val outline: Color = Color.Unspecified,
    val cardBackground: Color = Color.Unspecified
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

object CycleRideTrackerTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    secondary = White,
    onSecondary = Black,
    tertiary = White,
    onTertiary = Black,
    background = Black,
    surface = Black,
    onBackground = White,
    onSurface = White
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    secondary = Black,
    onSecondary = White,
    tertiary = Black,
    onTertiary = White,
    background = White,
    surface = White,
    onBackground = Black,
    onSurface = Black
)

@Composable
fun CycleRideTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Force Black and White backgrounds/surfaces as per requirements
    val colorScheme = baseColorScheme.copy(
        background = if (darkTheme) Black else White,
        surface = if (darkTheme) Black else White,
        onBackground = if (darkTheme) White else Black,
        onSurface = if (darkTheme) White else Black,
        surfaceVariant = if (darkTheme) Black else White,
        onSurfaceVariant = if (darkTheme) White.copy(alpha = 0.7f) else Black.copy(alpha = 0.7f),
        outlineVariant = if (darkTheme) White.copy(alpha = 0.1f) else Black.copy(alpha = 0.1f)
    )

    val appColors = AppColors(
        primary = colorScheme.primary,
        background = colorScheme.background,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        largeTitle = if (dynamicColor) colorScheme.onSurface else if (darkTheme) LargeTitleDark else LargeTitleLight,
        outline = colorScheme.outlineVariant,
        cardBackground = if (darkTheme) White.copy(alpha = 0.1f) else Black.copy(alpha = 0.05f)
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
