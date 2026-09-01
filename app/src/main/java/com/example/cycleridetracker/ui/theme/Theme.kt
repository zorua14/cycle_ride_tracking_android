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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
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
    primary = Cyan400,
    onPrimary = Navy900,
    secondary = Cyan200,
    onSecondary = Navy900,
    tertiary = Gray400,
    onTertiary = White,
    background = Navy900,
    surface = Navy900,
    onBackground = White,
    onSurface = White,
    surfaceVariant = Navy800,
    onSurfaceVariant = Gray400,
    outline = Navy700
)

private val LightColorScheme = lightColorScheme(
    primary = Navy900,
    onPrimary = White,
    secondary = Navy700,
    onSecondary = White,
    tertiary = Gray400,
    onTertiary = Navy900,
    background = White,
    surface = White,
    onBackground = Navy900,
    onSurface = Navy900
)

@Composable
fun CycleRideTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
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

    val colorScheme = baseColorScheme

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
        primary = colorScheme.primary,
        background = colorScheme.background,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
        largeTitle = if (darkTheme) Cyan400 else Navy900,
        outline = colorScheme.outline,
        cardBackground = if (darkTheme) Navy800 else Color.LightGray.copy(alpha = 0.1f)
    )

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
