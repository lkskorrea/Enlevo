package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FamilyColorScheme = lightColorScheme(
    primary = SkyBluePrimary,
    onPrimary = Color.White,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = OnSkyBlueContainer,
    secondary = RoseSecondary,
    onSecondary = Color.White,
    secondaryContainer = RoseContainer,
    onSecondaryContainer = OnRoseContainer,
    tertiary = MintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = OnMintContainer,
    background = WarmBackground,
    onBackground = TextDark,
    surface = WarmSurface,
    onSurface = TextDark,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = TextMuted
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FamilyColorScheme,
        typography = Typography,
        content = content
    )
}
