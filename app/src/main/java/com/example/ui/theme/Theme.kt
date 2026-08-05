package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

// --- Golden Summer (summer) ---
// Light: Golden Summer Fields
// Dark: Chocolate Sunset (but styled darker for Golden Summer vibe)
private val SummerLightColorScheme = lightColorScheme(
    primary = Color(0xFFD4A373),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFAEDCD),
    onPrimaryContainer = Color(0xFF512E00),
    secondary = Color(0xFFCCD5AE),
    onSecondary = Color(0xFF3F4E23),
    secondaryContainer = Color(0xFFE9EDC9),
    onSecondaryContainer = Color(0xFF1E280A),
    tertiary = Color(0xFFE9EDC9),
    onTertiary = Color(0xFF1E280A),
    background = Color(0xFFFEFAE0),
    surface = Color(0xFFFEFAE0),
    onBackground = Color(0xFF231C0C),
    onSurface = Color(0xFF231C0C),
    surfaceVariant = Color(0xFFE9EDC9),
    onSurfaceVariant = Color(0xFF444B39)
)

private val SummerDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE0B48A),
    onPrimary = Color(0xFF3D1F00),
    primaryContainer = Color(0xFF5A391A),
    onPrimaryContainer = Color(0xFFFFDDBA),
    secondary = Color(0xFFCCD5AE),
    onSecondary = Color(0xFF242F0B),
    secondaryContainer = Color(0xFF3B4422),
    onSecondaryContainer = Color(0xFFE9EDC9),
    tertiary = Color(0xFFCCD5AE),
    onTertiary = Color(0xFF242F0B),
    background = Color(0xFF1A1711),
    surface = Color(0xFF1E1B14),
    onBackground = Color(0xFFE9EDC9),
    onSurface = Color(0xFFE9EDC9),
    surfaceVariant = Color(0xFF444B39),
    onSurfaceVariant = Color(0xFFCCD5AE)
)

// --- Cozy Pastel (cozy) ---
// Light: Cozy Pastel Home
// Dark: Deep Peach/Cozy Dark
private val CozyLightColorScheme = lightColorScheme(
    primary = Color(0xFFD19E82),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF7D3AD),
    onPrimaryContainer = Color(0xFF52280A),
    secondary = Color(0xFFEDB791),
    onSecondary = Color(0xFF5E2E10),
    secondaryContainer = Color(0xFFF7E6D0),
    onSecondaryContainer = Color(0xFF3B1E05),
    tertiary = Color(0xFFD19E82),
    onTertiary = Color.White,
    background = Color(0xFFF7F3DC),
    surface = Color(0xFFF7F3DC),
    onBackground = Color(0xFF352C16),
    onSurface = Color(0xFF352C16),
    surfaceVariant = Color(0xFFF7E6D0),
    onSurfaceVariant = Color(0xFF5E2E10)
)

private val CozyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFEBAF94),
    onPrimary = Color(0xFF3E1705),
    primaryContainer = Color(0xFF5E2E19),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFFF2CBB2),
    onSecondary = Color(0xFF412210),
    secondaryContainer = Color(0xFF5E3A25),
    onSecondaryContainer = Color(0xFFFFE3D3),
    tertiary = Color(0xFFEBAF94),
    onTertiary = Color(0xFF3E1705),
    background = Color(0xFF1C1914),
    surface = Color(0xFF211D17),
    onBackground = Color(0xFFF7F3DC),
    onSurface = Color(0xFFF7F3DC),
    surfaceVariant = Color(0xFF5E2E10),
    onSurfaceVariant = Color(0xFFF2CBB2)
)

// --- Winter Wonderland (winter) ---
// Light: Whimsical Winter Wonderland
// Dark: Charcoal & Frost
private val WinterLightColorScheme = lightColorScheme(
    primary = Color(0xFFC4B69E),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFAF7F0),
    onPrimaryContainer = Color(0xFF332918),
    secondary = Color(0xFFE7DECD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFEFE8DB),
    onSecondaryContainer = Color(0xFF332918),
    tertiary = Color(0xFFF4EFE6),
    onTertiary = Color.Black,
    background = Color(0xFFFBFAF8),
    surface = Color(0xFFFAF7F0),
    onBackground = Color(0xFF24221E),
    onSurface = Color(0xFF24221E),
    surfaceVariant = Color(0xFFEFE8DB),
    onSurfaceVariant = Color(0xFF4A453A)
)

private val WinterDarkColorScheme = darkColorScheme(
    primary = Color(0xFFDDD3C1),
    onPrimary = Color(0xFF241C10),
    primaryContainer = Color(0xFF4A4132),
    onPrimaryContainer = Color(0xFFFAF7F0),
    secondary = Color(0xFFC4B69E),
    onSecondary = Color(0xFF241C10),
    secondaryContainer = Color(0xFF3A3427),
    onSecondaryContainer = Color(0xFFEFE8DB),
    tertiary = Color(0xFFE7DECD),
    onTertiary = Color(0xFF241C10),
    background = Color(0xFF1A1918),
    surface = Color(0xFF201F1D),
    onBackground = Color(0xFFFBFAF8),
    onSurface = Color(0xFFFBFAF8),
    surfaceVariant = Color(0xFF4A453A),
    onSurfaceVariant = Color(0xFFC4B69E)
)

// --- Soft Pastel Serenity (serenity) ---
// Light: Soft Pastel Serenity
// Dark: Deep Serene Muted Gold
private val SerenityLightColorScheme = lightColorScheme(
    primary = Color(0xFFD4C294),
    onPrimary = Color(0xFF3E3106),
    primaryContainer = Color(0xFFF3ECD3),
    onPrimaryContainer = Color(0xFF3E3106),
    secondary = Color(0xFFEEE4C3),
    onSecondary = Color(0xFF3E3106),
    secondaryContainer = Color(0xFFF8F4E3),
    onSecondaryContainer = Color(0xFF3E3106),
    tertiary = Color(0xFFFCFCF2),
    onTertiary = Color(0xFF3E3106),
    background = Color(0xFFFCFCF2),
    surface = Color(0xFFF8F4E3),
    onBackground = Color(0xFF3E3106),
    onSurface = Color(0xFF3E3106),
    surfaceVariant = Color(0xFFF3ECD3),
    onSurfaceVariant = Color(0xFF5A4D21)
)

private val SerenityDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE6D7AB),
    onPrimary = Color(0xFF332A02),
    primaryContainer = Color(0xFF4F431B),
    onPrimaryContainer = Color(0xFFFCFCF2),
    secondary = Color(0xFFD4C294),
    onSecondary = Color(0xFF332A02),
    secondaryContainer = Color(0xFF423B1E),
    onSecondaryContainer = Color(0xFFF8F4E3),
    tertiary = Color(0xFFEEE4C3),
    onTertiary = Color(0xFF332A02),
    background = Color(0xFF1D1B16),
    surface = Color(0xFF222019),
    onBackground = Color(0xFFFCFCF2),
    onSurface = Color(0xFFFCFCF2),
    surfaceVariant = Color(0xFF5A4D21),
    onSurfaceVariant = Color(0xFFD4C294)
)

// --- Green Serenity (green_serenity) ---
private val GreenSerenityLightColorScheme = lightColorScheme(
    primary = Color(0xFF588157),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA1CCA5),
    onPrimaryContainer = Color(0xFF112E15),
    secondary = Color(0xFF709775),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9F5EB),
    onSecondaryContainer = Color(0xFF111D13),
    tertiary = Color(0xFF415D43),
    onTertiary = Color.White,
    background = Color(0xFFF4FBF5),
    surface = Color(0xFFE9F5EB),
    onBackground = Color(0xFF111D13),
    onSurface = Color(0xFF111D13),
    surfaceVariant = Color(0xFFD6E9D9),
    onSurfaceVariant = Color(0xFF415D43)
)

private val GreenSerenityDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA1CCA5),
    onPrimary = Color(0xFF111D13),
    primaryContainer = Color(0xFF415D43),
    onPrimaryContainer = Color(0xFFE9F5EB),
    secondary = Color(0xFF8FB996),
    onSecondary = Color(0xFF111D13),
    secondaryContainer = Color(0xFF1C3A23),
    onSecondaryContainer = Color(0xFFE9F5EB),
    tertiary = Color(0xFF709775),
    onTertiary = Color(0xFF111D13),
    background = Color(0xFF111D13),
    surface = Color(0xFF18281B),
    onBackground = Color(0xFFE9F5EB),
    onSurface = Color(0xFFE9F5EB),
    surfaceVariant = Color(0xFF415D43),
    onSurfaceVariant = Color(0xFFA1CCA5)
)

// --- Blood Moon Serenity (blood_moon) ---
private val BloodMoonLightColorScheme = lightColorScheme(
    primary = Color(0xFF6A3937),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF0EF),
    onPrimaryContainer = Color(0xFF3B0D11),
    secondary = Color(0xFF706563),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0EF),
    onSecondaryContainer = Color(0xFF3B0D11),
    tertiary = Color(0xFF748386),
    onTertiary = Color.White,
    background = Color(0xFFFFF5F5),
    surface = Color(0xFFFBE8E6),
    onBackground = Color(0xFF2D070A),
    onSurface = Color(0xFF2D070A),
    surfaceVariant = Color(0xFFF3D2D0),
    onSurfaceVariant = Color(0xFF6A3937)
)

private val BloodMoonDarkColorScheme = darkColorScheme(
    primary = Color(0xFF9DC7C8),
    onPrimary = Color(0xFF3B0D11),
    primaryContainer = Color(0xFF6A3937),
    onPrimaryContainer = Color(0xFFFFF0EF),
    secondary = Color(0xFF748386),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B0D11),
    onSecondaryContainer = Color(0xFFFFF0EF),
    tertiary = Color(0xFF706563),
    onTertiary = Color.White,
    background = Color(0xFF3B0D11),
    surface = Color(0xFF2D070A),
    onBackground = Color(0xFFFFF0EF),
    onSurface = Color(0xFFFFF0EF),
    surfaceVariant = Color(0xFF6A3937),
    onSurfaceVariant = Color(0xFF9DC7C8)
)

// --- Earthy Harmony (earthy_harmony) ---
private val EarthyHarmonyLightColorScheme = lightColorScheme(
    primary = Color(0xFF896A67),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDC8C4),
    onPrimaryContainer = Color(0xFF24151C),
    secondary = Color(0xFF6B4D57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFF9F0),
    onSecondaryContainer = Color(0xFF13070C),
    tertiary = Color(0xFFDDC8C4),
    onTertiary = Color(0xFF24151C),
    background = Color(0xFFFAF7F7),
    surface = Color(0xFFEFF9F0),
    onBackground = Color(0xFF13070C),
    onSurface = Color(0xFF13070C),
    surfaceVariant = Color(0xFFE5D5D3),
    onSurfaceVariant = Color(0xFF896A67)
)

private val EarthyHarmonyDarkColorScheme = darkColorScheme(
    primary = Color(0xFFDDC8C4),
    onPrimary = Color(0xFF13070C),
    primaryContainer = Color(0xFF896A67),
    onPrimaryContainer = Color(0xFFEFF9F0),
    secondary = Color(0xFF6B4D57),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF24151C),
    onSecondaryContainer = Color(0xFFEFF9F0),
    tertiary = Color(0xFF896A67),
    onTertiary = Color(0xFFEFF9F0),
    background = Color(0xFF13070C),
    surface = Color(0xFF24151C),
    onBackground = Color(0xFFEFF9F0),
    onSurface = Color(0xFFEFF9F0),
    surfaceVariant = Color(0xFF3C2C30),
    onSurfaceVariant = Color(0xFFDDC8C4)
)

// --- Chocolate Sunset (chocolate_sunset) ---
private val ChocolateSunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFF8E443D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFF5F0),
    onPrimaryContainer = Color(0xFF320A28),
    secondary = Color(0xFFCB9173),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0D68A),
    onSecondaryContainer = Color(0xFF320A28),
    tertiary = Color(0xFFE0D68A),
    onTertiary = Color(0xFF320A28),
    background = Color(0xFFFFF5F0),
    surface = Color(0xFFFAEDE3),
    onBackground = Color(0xFF320A28),
    onSurface = Color(0xFF320A28),
    surfaceVariant = Color(0xFFF2DED2),
    onSurfaceVariant = Color(0xFF8E443D)
)

private val ChocolateSunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCB9173),
    onPrimary = Color(0xFF320A28),
    primaryContainer = Color(0xFF8E443D),
    onPrimaryContainer = Color(0xFFFFF1EB),
    secondary = Color(0xFFE0D68A),
    onSecondary = Color(0xFF320A28),
    secondaryContainer = Color(0xFF511730),
    onSecondaryContainer = Color(0xFFFFECEF),
    tertiary = Color(0xFF511730),
    onTertiary = Color(0xFFFFECEF),
    background = Color(0xFF320A28),
    surface = Color(0xFF26041D),
    onBackground = Color(0xFFFFECEF),
    onSurface = Color(0xFFFFECEF),
    surfaceVariant = Color(0xFF511730),
    onSurfaceVariant = Color(0xFFCB9173)
)

// --- Red Sunburst (red_sunburst) ---
private val RedSunburstLightColorScheme = lightColorScheme(
    primary = Color(0xFFCE4257),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEC),
    onPrimaryContainer = Color(0xFF4F000B),
    secondary = Color(0xFFFF7F51),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEAEA),
    onSecondaryContainer = Color(0xFF4F000B),
    tertiary = Color(0xFFFF9B54),
    onTertiary = Color.White,
    background = Color(0xFFFFF5F5),
    surface = Color(0xFFFFEAEA),
    onBackground = Color(0xFF4F000B),
    onSurface = Color(0xFF4F000B),
    surfaceVariant = Color(0xFFFDCFD2),
    onSurfaceVariant = Color(0xFFCE4257)
)

private val RedSunburstDarkColorScheme = darkColorScheme(
    primary = Color(0xFFCE4257),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF720026),
    onPrimaryContainer = Color(0xFFFFEBEC),
    secondary = Color(0xFFFF7F51),
    onSecondary = Color(0xFF4F000B),
    secondaryContainer = Color(0xFF5E121E),
    onSecondaryContainer = Color(0xFFFFEAEA),
    tertiary = Color(0xFFFF9B54),
    onTertiary = Color(0xFF4F000B),
    background = Color(0xFF4F000B),
    surface = Color(0xFF720026),
    onBackground = Color(0xFFFFEAEA),
    onSurface = Color(0xFFFFEAEA),
    surfaceVariant = Color(0xFF5E121E),
    onSurfaceVariant = Color(0xFFCE4257)
)

// --- Earthy Forest Hues (earthy_forest) ---
private val EarthyForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF344E41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDAD7CD),
    onPrimaryContainer = Color(0xFF1E2F26),
    secondary = Color(0xFF588157),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFA3B18A),
    onSecondaryContainer = Color(0xFF1E2F26),
    tertiary = Color(0xFF3A5A40),
    onTertiary = Color.White,
    background = Color(0xFFF3F2EE),
    surface = Color(0xFFE5E2DA),
    onBackground = Color(0xFF1B2620),
    onSurface = Color(0xFF1B2620),
    surfaceVariant = Color(0xFFDAD7CD),
    onSurfaceVariant = Color(0xFF344E41)
)

private val EarthyForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFFA3B18A),
    onPrimary = Color(0xFF1E2F26),
    primaryContainer = Color(0xFF344E41),
    onPrimaryContainer = Color(0xFFDAD7CD),
    secondary = Color(0xFF588157),
    onSecondary = Color(0xFF1E2F26),
    secondaryContainer = Color(0xFF263D31),
    onSecondaryContainer = Color(0xFFDAD7CD),
    tertiary = Color(0xFF3A5A40),
    onTertiary = Color(0xFFDAD7CD),
    background = Color(0xFF1B2620),
    surface = Color(0xFF223129),
    onBackground = Color(0xFFDAD7CD),
    onSurface = Color(0xFFDAD7CD),
    surfaceVariant = Color(0xFF344E41),
    onSurfaceVariant = Color(0xFFA3B18A)
)

// --- Deep Sea (deep_sea) ---
private val DeepSeaLightColorScheme = lightColorScheme(
    primary = Color(0xFF415A77),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF0F6),
    onPrimaryContainer = Color(0xFF011B2A),
    secondary = Color(0xFF778DA9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E1DD),
    onSecondaryContainer = Color(0xFF011B2A),
    tertiary = Color(0xFF1B263B),
    onTertiary = Color.White,
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFE0E1DD),
    onBackground = Color(0xFF011B2A),
    onSurface = Color(0xFF011B2A),
    surfaceVariant = Color(0xFFCDD5DF),
    onSurfaceVariant = Color(0xFF415A77)
)

private val DeepSeaDarkColorScheme = darkColorScheme(
    primary = Color(0xFF778DA9),
    onPrimary = Color(0xFF011B2A),
    primaryContainer = Color(0xFF1B263B),
    onPrimaryContainer = Color(0xFFE0E1DD),
    secondary = Color(0xFF415A77),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF011B2A),
    onSecondaryContainer = Color(0xFFE0E1DD),
    tertiary = Color(0xFFE0E1DD),
    onTertiary = Color(0xFF011B2A),
    background = Color(0xFF011B2A),
    surface = Color(0xFF0B1F2D),
    onBackground = Color(0xFFE0E1DD),
    onSurface = Color(0xFFE0E1DD),
    surfaceVariant = Color(0xFF1B263B),
    onSurfaceVariant = Color(0xFF778DA9)
)

// --- Soft Lavender (soft_lavender) ---
private val SoftLavenderLightColorScheme = lightColorScheme(
    primary = Color(0xFF4A4E69),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF2E9E4),
    onPrimaryContainer = Color(0xFF22223B),
    secondary = Color(0xFF9A8C98),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEBE6E3),
    onSecondaryContainer = Color(0xFF22223B),
    tertiary = Color(0xFFC9ADA7),
    onTertiary = Color.White,
    background = Color(0xFFF7F7FA),
    surface = Color(0xFFF2E9E4),
    onBackground = Color(0xFF22223B),
    onSurface = Color(0xFF22223B),
    surfaceVariant = Color(0xFFDFD7D3),
    onSurfaceVariant = Color(0xFF4A4E69)
)

private val SoftLavenderDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC9ADA7),
    onPrimary = Color(0xFF22223B),
    primaryContainer = Color(0xFF4A4E69),
    onPrimaryContainer = Color(0xFFF2E9E4),
    secondary = Color(0xFF9A8C98),
    onSecondary = Color(0xFF22223B),
    secondaryContainer = Color(0xFF22223B),
    onSecondaryContainer = Color(0xFFF2E9E4),
    tertiary = Color(0xFFF2E9E4),
    onTertiary = Color(0xFF22223B),
    background = Color(0xFF22223B),
    surface = Color(0xFF1C1C32),
    onBackground = Color(0xFFF2E9E4),
    onSurface = Color(0xFFF2E9E4),
    surfaceVariant = Color(0xFF4A4E69),
    onSurfaceVariant = Color(0xFFC9ADA7)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  themeCombo: String = "default",
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val actualDynamic = dynamicColor && (themeCombo == "default")

  val colorScheme =
    when {
      actualDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      themeCombo == "summer" -> {
        if (darkTheme) SummerDarkColorScheme else SummerLightColorScheme
      }

      themeCombo == "cozy" -> {
        if (darkTheme) CozyDarkColorScheme else CozyLightColorScheme
      }

      themeCombo == "winter" -> {
        if (darkTheme) WinterDarkColorScheme else WinterLightColorScheme
      }

      themeCombo == "serenity" -> {
        if (darkTheme) SerenityDarkColorScheme else SerenityLightColorScheme
      }

      themeCombo == "green_serenity" -> {
        if (darkTheme) GreenSerenityDarkColorScheme else GreenSerenityLightColorScheme
      }

      themeCombo == "blood_moon" -> {
        if (darkTheme) BloodMoonDarkColorScheme else BloodMoonLightColorScheme
      }

      themeCombo == "earthy_harmony" -> {
        if (darkTheme) EarthyHarmonyDarkColorScheme else EarthyHarmonyLightColorScheme
      }

      themeCombo == "chocolate_sunset" -> {
        if (darkTheme) ChocolateSunsetDarkColorScheme else ChocolateSunsetLightColorScheme
      }

      themeCombo == "red_sunburst" -> {
        if (darkTheme) RedSunburstDarkColorScheme else RedSunburstLightColorScheme
      }

      themeCombo == "earthy_forest" -> {
        if (darkTheme) EarthyForestDarkColorScheme else EarthyForestLightColorScheme
      }

      themeCombo == "deep_sea" -> {
        if (darkTheme) DeepSeaDarkColorScheme else DeepSeaLightColorScheme
      }

      themeCombo == "soft_lavender" -> {
        if (darkTheme) SoftLavenderDarkColorScheme else SoftLavenderLightColorScheme
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
