package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ArcadeDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianBg,
    primaryContainer = DeepCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = ArcadeMagenta,
    onSecondary = Color.White,
    secondaryContainer = DeepMagentaContainer,
    onSecondaryContainer = ArcadeMagenta,
    tertiary = VoltLime,
    onTertiary = ObsidianBg,
    tertiaryContainer = DeepLimeContainer,
    onTertiaryContainer = VoltLime,
    background = ObsidianBg,
    onBackground = SoftIce,
    surface = MidnightSurface,
    onSurface = SoftIce,
    surfaceVariant = ElevatedCard,
    onSurfaceVariant = MutedSlate,
    outline = ElevatedBorder,
    error = CoralAlert,
    onError = Color.White,
)

private val ArcadeLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF5FA),
    onPrimaryContainer = Color(0xFF002025),
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E8),
    onSecondaryContainer = Color(0xFF3E001D),
    tertiary = LightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F8BA),
    onTertiaryContainer = Color(0xFF142200),
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightMuted,
    outline = Color(0xFFCBD5E1),
    error = CoralAlert,
    onError = Color.White,
)

@Composable
fun HotspotArcadeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ArcadeDarkColorScheme else ArcadeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ArcadeTypography,
        content = content,
    )
}
