package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccentDark,
    onPrimary = TextPrimaryDark,
    primaryContainer = SlateSurfaceVariantDark,
    onPrimaryContainer = TextPrimaryDark,
    secondary = BlueAccentDark,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SlateSurfaceVariantDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = BlueAccentHoverDark,
    onTertiary = TextPrimaryDark,
    background = SlateBgDark,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark,
    outlineVariant = SlateBorderDark.copy(alpha = 0.5f),
    error = ErrorDark,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = BlueAccentLight,
    onPrimary = Color.White,
    primaryContainer = SlateSurfaceVariantLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = BlueAccentLight,
    onSecondary = Color.White,
    secondaryContainer = SlateSurfaceVariantLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = BlueAccentHoverLight,
    onTertiary = Color.White,
    background = SlateBgLight,
    onBackground = TextPrimaryLight,
    surface = SlateSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SlateSurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = SlateBorderLight,
    outlineVariant = SlateBorderLight.copy(alpha = 0.6f),
    error = ErrorLight,
    onError = Color.White
)

@Composable
fun GameLingoTheme(
    darkTheme: Boolean = true, // Dark theme is default per design specification
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GameLingoTypography,
        content = content
    )
}
