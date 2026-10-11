package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
  primary = DarkTealPrimary,
  onPrimary = OnDarkTealPrimary,
  primaryContainer = DarkTealContainer,
  onPrimaryContainer = OnDarkTealContainer,
  secondary = DarkSageSecondary,
  onSecondary = OnDarkSageSecondary,
  secondaryContainer = DarkSageContainer,
  onSecondaryContainer = OnDarkSageContainer,
  tertiary = DarkAmberTertiary,
  onTertiary = OnDarkAmberTertiary,
  tertiaryContainer = DarkAmberContainer,
  onTertiaryContainer = OnDarkAmberContainer,
  background = DeepForestBackground,
  onBackground = OnDeepForestBackground,
  surface = DeepForestSurface,
  onSurface = OnDeepForestSurface,
  surfaceVariant = DeepSurfaceVariant,
  onSurfaceVariant = OnDeepSurfaceVariant,
  outline = DeepOutline
)

private val LightColorScheme = lightColorScheme(
  primary = ForestTealPrimary,
  onPrimary = OnForestTealPrimary,
  primaryContainer = ForestTealContainer,
  onPrimaryContainer = OnForestTealContainer,
  secondary = WarmSageSecondary,
  onSecondary = OnWarmSageSecondary,
  secondaryContainer = WarmSageContainer,
  onSecondaryContainer = OnWarmSageContainer,
  tertiary = TerracottaTertiary,
  onTertiary = OnTerracottaTertiary,
  tertiaryContainer = TerracottaContainer,
  onTertiaryContainer = OnTerracottaContainer,
  background = ParchmentBackground,
  onBackground = OnParchmentBackground,
  surface = WarmCardSurface,
  onSurface = OnWarmCardSurface,
  surfaceVariant = WarmSurfaceVariant,
  onSurfaceVariant = OnWarmSurfaceVariant,
  outline = WarmOutline
)

val SteadyMindShapes = Shapes(
  extraSmall = RoundedCornerShape(10.dp),
  small = RoundedCornerShape(14.dp),
  medium = RoundedCornerShape(20.dp),
  large = RoundedCornerShape(26.dp),
  extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = SteadyMindShapes,
    content = content
  )
}
