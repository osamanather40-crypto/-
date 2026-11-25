package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val QaydDarkColorScheme = darkColorScheme(
    primary = BurnishedAmber,
    onPrimary = ObsidianVoid,
    primaryContainer = DeepBronze,
    onPrimaryContainer = AmberGlow,
    secondary = SteelAccent,
    onSecondary = ObsidianVoid,
    secondaryContainer = SteelContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = EmeraldVerified,
    onTertiary = ObsidianVoid,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = ObsidianContainerHigh,
    outline = BorderHairline,
    outlineVariant = BorderHairline,
    error = CrimsonBreach,
    onError = TextPrimary,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonText,
)

private val QaydShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun QaydTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = QaydDarkColorScheme,
        typography = QaydTypography,
        shapes = QaydShapes,
        content = content,
    )
}
