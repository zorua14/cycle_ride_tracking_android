package com.example.cycleridetracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class AppColors(
    val primary: Color = Color.Unspecified,
    val background: Color = Color.Unspecified,
    val surface: Color = Color.Unspecified,
    val onSurface: Color = Color.Unspecified,
    val onSurfaceVariant: Color = Color.Unspecified,
    val largeTitle: Color = Color.Unspecified,
    val outline: Color = Color.Unspecified,
    val cardBackground: Color = Color.Unspecified,
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

    secondary = Color(0xFFD1D1D1),
    onSecondary = Black,

    tertiary = Color(0xFF9E9E9E),
    onTertiary = Black,

    // True black background
    background = Black,

    // Slightly raised surface
    surface = Color(0xFF121212),

    onBackground = White,
    onSurface = White,

    // Card / secondary surface
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = Color(0xFFB8B8B8),

    outline = Color(0xFF3D3D3D)
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,

    secondary = Color(0xFF2C2C2C),
    onSecondary = White,

    tertiary = Color(0xFF2C2C2C),
    onTertiary = White,

    // Noticeably gray background
    background = Color(0xFFE5E5E5),

    // Cards remain white
    surface = White,

    onBackground = Color(0xFF111111),
    onSurface = Color(0xFF111111),

    // Slightly darker gray for secondary surfaces
    surfaceVariant = Color(0xFFDADADA),
    onSurfaceVariant = Color(0xFF666666),

    outline = Color(0xFFC2C2C2)
)

@Composable
fun CycleRideTrackerTheme(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val appColors = AppColors(
        primary = baseColorScheme.primary,
        background = baseColorScheme.background,
        surface = baseColorScheme.surface,
        onSurface = baseColorScheme.onSurface,
        onSurfaceVariant = baseColorScheme.onSurfaceVariant,

        // No more Cyan400 / Navy900
        largeTitle = baseColorScheme.primary,

        outline = baseColorScheme.outline,

        // Uses the theme's surface hierarchy
        cardBackground = baseColorScheme.surfaceVariant
    )

    CompositionLocalProvider(
        LocalAppColors provides appColors
    ) {
        Box(modifier = modifier) {
            MaterialTheme(
                colorScheme = baseColorScheme,
                typography = Typography,
                content = content
            )
        }
    }
}