package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemeMode

enum class BrandColorOption(
  val key: String,
  val label: String,
  val lightPrimary: Color,
  val darkPrimary: Color,
  val lightContainer: Color,
  val darkContainer: Color
) {
  BLUE("BLUE", "Biru Utama", CobaltPrimary, CobaltPrimaryDark, CobaltContainerLight, CobaltContainerDark),
  SKY("SKY", "Biru Cerah", Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF0C4A6E)),
  NAVY("NAVY", "Biru Dongker", Color(0xFF1D4ED8), Color(0xFF93C5FD), Color(0xFFDBEAFE), Color(0xFF1E3A8A)),
  TEAL("TEAL", "Biru Tosca", Color(0xFF0D9488), Color(0xFF2DD4BF), Color(0xFFCCFBF1), Color(0xFF134E4A)),
  INDIGO("INDIGO", "Biru Indigo", Color(0xFF4F46E5), Color(0xFFA5B4FC), Color(0xFFE0E7FF), Color(0xFF312E81)),
  EMERALD("EMERALD", "Hijau Segar", Color(0xFF059669), Color(0xFF34D399), Color(0xFFD1FAE5), Color(0xFF064E3B));

  fun localizedLabel(isEn: Boolean): String = when (this) {
    BLUE -> if (isEn) "Primary Blue" else label
    SKY -> if (isEn) "Sky Blue" else label
    NAVY -> if (isEn) "Navy Blue" else label
    TEAL -> if (isEn) "Teal" else label
    INDIGO -> if (isEn) "Indigo Blue" else label
    EMERALD -> if (isEn) "Fresh Green" else label
  }

  companion object {
    fun fromKey(key: String?): BrandColorOption =
      entries.firstOrNull { it.key == key } ?: BLUE
  }
}

val AdaShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AdaLaundryTheme(
  themeMode: ThemeMode = ThemeMode.LIGHT,
  brandColorKey: String = "BLUE",
  content: @Composable () -> Unit
) {
  val brand = BrandColorOption.fromKey(brandColorKey)
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = if (isDark) {
    darkColorScheme(
      primary = brand.darkPrimary,
      onPrimary = Color(0xFF0A2458),
      primaryContainer = brand.darkContainer,
      onPrimaryContainer = Color(0xFFDBEAFE),
      secondary = CyanSecondaryDark,
      onSecondary = Color(0xFF083344),
      secondaryContainer = CyanContainerDark,
      onSecondaryContainer = Color(0xFFCFFAFE),
      tertiary = EmeraldAccentDark,
      onTertiary = Color(0xFF022C22),
      tertiaryContainer = EmeraldContainerDark,
      onTertiaryContainer = Color(0xFFD1FAE5),
      background = DarkBackground,
      onBackground = DarkOnSurface,
      surface = DarkSurface,
      onSurface = DarkOnSurface,
      surfaceVariant = DarkSurfaceVariant,
      onSurfaceVariant = DarkOnSurfaceVariant,
      outline = DarkOutline,
      error = Color(0xFFF87171),
      onError = Color(0xFF450A0A),
      errorContainer = ErrorContainerDark,
      onErrorContainer = Color(0xFFFEE2E2)
    )
  } else {
    lightColorScheme(
      primary = brand.lightPrimary,
      onPrimary = Color.White,
      primaryContainer = brand.lightContainer,
      onPrimaryContainer = Color(0xFF0A2E75),
      secondary = CyanSecondary,
      onSecondary = Color.White,
      secondaryContainer = CyanContainerLight,
      onSecondaryContainer = Color(0xFF083344),
      tertiary = EmeraldAccent,
      onTertiary = Color.White,
      tertiaryContainer = EmeraldContainerLight,
      onTertiaryContainer = Color(0xFF064E3B),
      background = LightBackground,
      onBackground = LightOnSurface,
      surface = LightSurface,
      onSurface = LightOnSurface,
      surfaceVariant = LightSurfaceVariant,
      onSurfaceVariant = LightOnSurfaceVariant,
      outline = LightOutline,
      error = ErrorCrimson,
      onError = Color.White,
      errorContainer = ErrorContainerLight,
      onErrorContainer = Color(0xFF7F1D1D)
    )
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AdaShapes,
    content = content
  )
}
