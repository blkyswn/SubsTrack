package com.example.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import com.example.data.User
import java.util.Locale

data class WidgetThemeColors(
    val primary: Int,
    val onPrimary: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val primaryContainer: Int = primary,
    val onPrimaryContainer: Int = onPrimary,
    val tertiary: Int = primary,
    val onTertiary: Int = onPrimary,
    val tertiaryContainer: Int = surfaceVariant,
    val onTertiaryContainer: Int = onSurfaceVariant,
    val eventHighlightTint: Int = tertiary,
    val counterBg: Int = tertiary,
    val counterText: Int = onTertiary,
    val eventHighlightBg: Int = 0x33FF80AB.toInt()
)

object WidgetThemeUtils {

    fun isDarkTheme(context: Context, user: User?): Boolean {
        return when (user?.themeMode?.lowercase(Locale.getDefault())) {
            "dark" -> true
            "light" -> false
            else -> {
                val nightModeFlags = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    fun getThemeColors(context: Context, user: User?): WidgetThemeColors {
        val isDark = isDarkTheme(context, user)
        val combo = user?.themeCombo?.lowercase(Locale.getDefault()) ?: "default"

        return when (combo) {
            "summer" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFE0B48A.toInt(),
                    onPrimary = 0xFF3D1F00.toInt(),
                    surface = 0xFF1E1B14.toInt(),
                    onSurface = 0xFFE9EDC9.toInt(),
                    surfaceVariant = 0xFF3A3425.toInt(),
                    onSurfaceVariant = 0xFFCCD5AE.toInt(),
                    primaryContainer = 0xFF5A391A.toInt(),
                    onPrimaryContainer = 0xFFFFDDBA.toInt(),
                    tertiary = 0xFFCCD5AE.toInt(),
                    onTertiary = 0xFF242F0B.toInt(),
                    tertiaryContainer = 0xFF3B4422.toInt(),
                    onTertiaryContainer = 0xFFE9EDC9.toInt(),
                    eventHighlightTint = 0xFFCCD5AE.toInt(),
                    counterBg = 0xFFCCD5AE.toInt(),
                    counterText = 0xFF242F0B.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD4A373.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFEFAE0.toInt(),
                    onSurface = 0xFF231C0C.toInt(),
                    surfaceVariant = 0xFFFAEDCD.toInt(),
                    onSurfaceVariant = 0xFF512E00.toInt(),
                    primaryContainer = 0xFFFAEDCD.toInt(),
                    onPrimaryContainer = 0xFF512E00.toInt(),
                    tertiary = 0xFFD4A373.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFFAEDCD.toInt(),
                    onTertiaryContainer = 0xFF512E00.toInt(),
                    eventHighlightTint = 0xFFD4A373.toInt(),
                    counterBg = 0xFFD4A373.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "cozy" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFEBAF94.toInt(),
                    onPrimary = 0xFF3E1705.toInt(),
                    surface = 0xFF211D17.toInt(),
                    onSurface = 0xFFF7F3DC.toInt(),
                    surfaceVariant = 0xFF3D2A1F.toInt(),
                    onSurfaceVariant = 0xFFF2CBB2.toInt(),
                    primaryContainer = 0xFF5E2E19.toInt(),
                    onPrimaryContainer = 0xFFFFDBCF.toInt(),
                    tertiary = 0xFFEBAF94.toInt(),
                    onTertiary = 0xFF3E1705.toInt(),
                    tertiaryContainer = 0xFF5E3A25.toInt(),
                    onTertiaryContainer = 0xFFFFE3D3.toInt(),
                    eventHighlightTint = 0xFFEBAF94.toInt(),
                    counterBg = 0xFFEBAF94.toInt(),
                    counterText = 0xFF3E1705.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD19E82.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF7F3DC.toInt(),
                    onSurface = 0xFF352C16.toInt(),
                    surfaceVariant = 0xFFF7D3AD.toInt(),
                    onSurfaceVariant = 0xFF5E2E10.toInt(),
                    primaryContainer = 0xFFF7D3AD.toInt(),
                    onPrimaryContainer = 0xFF52280A.toInt(),
                    tertiary = 0xFFD19E82.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFF7D3AD.toInt(),
                    onTertiaryContainer = 0xFF52280A.toInt(),
                    eventHighlightTint = 0xFFD19E82.toInt(),
                    counterBg = 0xFFD19E82.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "winter" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFDDD3C1.toInt(),
                    onPrimary = 0xFF241C10.toInt(),
                    surface = 0xFF201F1D.toInt(),
                    onSurface = 0xFFFBFAF8.toInt(),
                    surfaceVariant = 0xFF38352F.toInt(),
                    onSurfaceVariant = 0xFFC4B69E.toInt(),
                    primaryContainer = 0xFF4A4132.toInt(),
                    onPrimaryContainer = 0xFFFAF7F0.toInt(),
                    tertiary = 0xFFE7DECD.toInt(),
                    onTertiary = 0xFF241C10.toInt(),
                    tertiaryContainer = 0xFF3A3427.toInt(),
                    onTertiaryContainer = 0xFFEFE8DB.toInt(),
                    eventHighlightTint = 0xFFE7DECD.toInt(),
                    counterBg = 0xFFE7DECD.toInt(),
                    counterText = 0xFF241C10.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFC4B69E.toInt(),
                    onPrimary = 0xFF000000.toInt(),
                    surface = 0xFFFAF7F0.toInt(),
                    onSurface = 0xFF24221E.toInt(),
                    surfaceVariant = 0xFFEFE8DB.toInt(),
                    onSurfaceVariant = 0xFF4A453A.toInt(),
                    primaryContainer = 0xFFFAF7F0.toInt(),
                    onPrimaryContainer = 0xFF332918.toInt(),
                    tertiary = 0xFFC4B69E.toInt(),
                    onTertiary = 0xFF000000.toInt(),
                    tertiaryContainer = 0xFFEFE8DB.toInt(),
                    onTertiaryContainer = 0xFF332918.toInt(),
                    eventHighlightTint = 0xFFC4B69E.toInt(),
                    counterBg = 0xFFC4B69E.toInt(),
                    counterText = 0xFF000000.toInt()
                )
            }
            "serenity" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFE6D7AB.toInt(),
                    onPrimary = 0xFF332A02.toInt(),
                    surface = 0xFF222019.toInt(),
                    onSurface = 0xFFFCFCF2.toInt(),
                    surfaceVariant = 0xFF3C3725.toInt(),
                    onSurfaceVariant = 0xFFD4C294.toInt(),
                    primaryContainer = 0xFF4F431B.toInt(),
                    onPrimaryContainer = 0xFFFCFCF2.toInt(),
                    tertiary = 0xFFEEE4C3.toInt(),
                    onTertiary = 0xFF332A02.toInt(),
                    tertiaryContainer = 0xFF423B1E.toInt(),
                    onTertiaryContainer = 0xFFF8F4E3.toInt(),
                    eventHighlightTint = 0xFFEEE4C3.toInt(),
                    counterBg = 0xFFEEE4C3.toInt(),
                    counterText = 0xFF332A02.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD4C294.toInt(),
                    onPrimary = 0xFF3E3106.toInt(),
                    surface = 0xFFF8F4E3.toInt(),
                    onSurface = 0xFF3E3106.toInt(),
                    surfaceVariant = 0xFFF3ECD3.toInt(),
                    onSurfaceVariant = 0xFF5A4D21.toInt(),
                    primaryContainer = 0xFFF3ECD3.toInt(),
                    onPrimaryContainer = 0xFF3E3106.toInt(),
                    tertiary = 0xFFD4C294.toInt(),
                    onTertiary = 0xFF3E3106.toInt(),
                    tertiaryContainer = 0xFFF3ECD3.toInt(),
                    onTertiaryContainer = 0xFF3E3106.toInt(),
                    eventHighlightTint = 0xFFD4C294.toInt(),
                    counterBg = 0xFFD4C294.toInt(),
                    counterText = 0xFF3E3106.toInt()
                )
            }
            "green_serenity" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFA1CCA5.toInt(),
                    onPrimary = 0xFF112E15.toInt(),
                    surface = 0xFF1A221A.toInt(),
                    onSurface = 0xFFEBF5EC.toInt(),
                    surfaceVariant = 0xFF273828.toInt(),
                    onSurfaceVariant = 0xFFA1CCA5.toInt(),
                    primaryContainer = 0xFF415D43.toInt(),
                    onPrimaryContainer = 0xFFE9F5EB.toInt(),
                    tertiary = 0xFF709775.toInt(),
                    onTertiary = 0xFF111D13.toInt(),
                    tertiaryContainer = 0xFF415D43.toInt(),
                    onTertiaryContainer = 0xFFE9F5EB.toInt(),
                    eventHighlightTint = 0xFF709775.toInt(),
                    counterBg = 0xFF709775.toInt(),
                    counterText = 0xFF111D13.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF588157.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF2F7F2.toInt(),
                    onSurface = 0xFF1E281F.toInt(),
                    surfaceVariant = 0xFFE0EFE2.toInt(),
                    onSurfaceVariant = 0xFF334B32.toInt(),
                    primaryContainer = 0xFFA1CCA5.toInt(),
                    onPrimaryContainer = 0xFF112E15.toInt(),
                    tertiary = 0xFF415D43.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFA1CCA5.toInt(),
                    onTertiaryContainer = 0xFF112E15.toInt(),
                    eventHighlightTint = 0xFF588157.toInt(),
                    counterBg = 0xFF415D43.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "blood_moon" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFF9DC7C8.toInt(),
                    onPrimary = 0xFF173031.toInt(),
                    surface = 0xFF231717.toInt(),
                    onSurface = 0xFFF7EBEB.toInt(),
                    surfaceVariant = 0xFF3D2525.toInt(),
                    onSurfaceVariant = 0xFF9DC7C8.toInt(),
                    primaryContainer = 0xFF6A3937.toInt(),
                    onPrimaryContainer = 0xFFFFF0EF.toInt(),
                    tertiary = 0xFF706563.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFF6A3937.toInt(),
                    onTertiaryContainer = 0xFFFFF0EF.toInt(),
                    eventHighlightTint = 0xFF9DC7C8.toInt(),
                    counterBg = 0xFF706563.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF6A3937.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFDF5F5.toInt(),
                    onSurface = 0xFF2C1E1E.toInt(),
                    surfaceVariant = 0xFFF2DEDE.toInt(),
                    onSurfaceVariant = 0xFF543837.toInt(),
                    primaryContainer = 0xFFFFF0EF.toInt(),
                    onPrimaryContainer = 0xFF3B0D11.toInt(),
                    tertiary = 0xFF748386.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFFFF0EF.toInt(),
                    onTertiaryContainer = 0xFF3B0D11.toInt(),
                    eventHighlightTint = 0xFF6A3937.toInt(),
                    counterBg = 0xFF748386.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "earthy_harmony" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFDDC8C4.toInt(),
                    onPrimary = 0xFF3A2B29.toInt(),
                    surface = 0xFF211A19.toInt(),
                    onSurface = 0xFFF7EFEB.toInt(),
                    surfaceVariant = 0xFF3A2D2C.toInt(),
                    onSurfaceVariant = 0xFFDDC8C4.toInt(),
                    primaryContainer = 0xFF896A67.toInt(),
                    onPrimaryContainer = 0xFFEFF9F0.toInt(),
                    tertiary = 0xFF896A67.toInt(),
                    onTertiary = 0xFFEFF9F0.toInt(),
                    tertiaryContainer = 0xFF896A67.toInt(),
                    onTertiaryContainer = 0xFFEFF9F0.toInt(),
                    eventHighlightTint = 0xFFDDC8C4.toInt(),
                    counterBg = 0xFF896A67.toInt(),
                    counterText = 0xFFEFF9F0.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF896A67.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFBF6F4.toInt(),
                    onSurface = 0xFF2B2120.toInt(),
                    surfaceVariant = 0xFFEFE4E1.toInt(),
                    onSurfaceVariant = 0xFF5A4644.toInt(),
                    primaryContainer = 0xFFDDC8C4.toInt(),
                    onPrimaryContainer = 0xFF24151C.toInt(),
                    tertiary = 0xFFDDC8C4.toInt(),
                    onTertiary = 0xFF24151C.toInt(),
                    tertiaryContainer = 0xFFDDC8C4.toInt(),
                    onTertiaryContainer = 0xFF24151C.toInt(),
                    eventHighlightTint = 0xFF896A67.toInt(),
                    counterBg = 0xFF896A67.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "chocolate_sunset" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFCB9173.toInt(),
                    onPrimary = 0xFF3C1F10.toInt(),
                    surface = 0xFF211614.toInt(),
                    onSurface = 0xFFF9EDE5.toInt(),
                    surfaceVariant = 0xFF3C2420.toInt(),
                    onSurfaceVariant = 0xFFCB9173.toInt(),
                    primaryContainer = 0xFF8E443D.toInt(),
                    onPrimaryContainer = 0xFFFFF1EB.toInt(),
                    tertiary = 0xFF511730.toInt(),
                    onTertiary = 0xFFFFECEF.toInt(),
                    tertiaryContainer = 0xFF8E443D.toInt(),
                    onTertiaryContainer = 0xFFFFF1EB.toInt(),
                    eventHighlightTint = 0xFFCB9173.toInt(),
                    counterBg = 0xFF511730.toInt(),
                    counterText = 0xFFFFECEF.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF8E443D.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFDF6F0.toInt(),
                    onSurface = 0xFF2D1E1B.toInt(),
                    surfaceVariant = 0xFFF5E4D7.toInt(),
                    onSurfaceVariant = 0xFF5A312D.toInt(),
                    primaryContainer = 0xFFFFF5F0.toInt(),
                    onPrimaryContainer = 0xFF320A28.toInt(),
                    tertiary = 0xFFE0D68A.toInt(),
                    onTertiary = 0xFF320A28.toInt(),
                    tertiaryContainer = 0xFFFFF5F0.toInt(),
                    onTertiaryContainer = 0xFF320A28.toInt(),
                    eventHighlightTint = 0xFF8E443D.toInt(),
                    counterBg = 0xFFE0D68A.toInt(),
                    counterText = 0xFF320A28.toInt()
                )
            }
            "red_sunburst" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFCE4257.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFF231315.toInt(),
                    onSurface = 0xFFFFEAEA.toInt(),
                    surfaceVariant = 0xFF401C21.toInt(),
                    onSurfaceVariant = 0xFFFFB2B9.toInt(),
                    primaryContainer = 0xFF720026.toInt(),
                    onPrimaryContainer = 0xFFFFEBEC.toInt(),
                    tertiary = 0xFFFF9B54.toInt(),
                    onTertiary = 0xFF4F000B.toInt(),
                    tertiaryContainer = 0xFF720026.toInt(),
                    onTertiaryContainer = 0xFFFFEBEC.toInt(),
                    eventHighlightTint = 0xFFFF9B54.toInt(),
                    counterBg = 0xFFFF9B54.toInt(),
                    counterText = 0xFF4F000B.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFCE4257.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFFF5F5.toInt(),
                    onSurface = 0xFF2F171A.toInt(),
                    surfaceVariant = 0xFFFFE0E2.toInt(),
                    onSurfaceVariant = 0xFF662731.toInt(),
                    primaryContainer = 0xFFFFEBEC.toInt(),
                    onPrimaryContainer = 0xFF4F000B.toInt(),
                    tertiary = 0xFFFF9B54.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFFFEBEC.toInt(),
                    onTertiaryContainer = 0xFF4F000B.toInt(),
                    eventHighlightTint = 0xFFCE4257.toInt(),
                    counterBg = 0xFFFF9B54.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "earthy_forest" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFA3B18A.toInt(),
                    onPrimary = 0xFF1F2B1A.toInt(),
                    surface = 0xFF161E1A.toInt(),
                    onSurface = 0xFFE8EFEA.toInt(),
                    surfaceVariant = 0xFF24352B.toInt(),
                    onSurfaceVariant = 0xFFA3B18A.toInt(),
                    primaryContainer = 0xFF344E41.toInt(),
                    onPrimaryContainer = 0xFFDAD7CD.toInt(),
                    tertiary = 0xFF3A5A40.toInt(),
                    onTertiary = 0xFFDAD7CD.toInt(),
                    tertiaryContainer = 0xFF344E41.toInt(),
                    onTertiaryContainer = 0xFFDAD7CD.toInt(),
                    eventHighlightTint = 0xFFA3B18A.toInt(),
                    counterBg = 0xFF3A5A40.toInt(),
                    counterText = 0xFFDAD7CD.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF344E41.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF4F7F2.toInt(),
                    onSurface = 0xFF18221D.toInt(),
                    surfaceVariant = 0xFFE0EAE0.toInt(),
                    onSurfaceVariant = 0xFF314339.toInt(),
                    primaryContainer = 0xFFDAD7CD.toInt(),
                    onPrimaryContainer = 0xFF1E2F26.toInt(),
                    tertiary = 0xFF3A5A40.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFDAD7CD.toInt(),
                    onTertiaryContainer = 0xFF1E2F26.toInt(),
                    eventHighlightTint = 0xFF344E41.toInt(),
                    counterBg = 0xFF3A5A40.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "deep_sea" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFF778DA9.toInt(),
                    onPrimary = 0xFF182330.toInt(),
                    surface = 0xFF161C22.toInt(),
                    onSurface = 0xFFE7EDF3.toInt(),
                    surfaceVariant = 0xFF23303D.toInt(),
                    onSurfaceVariant = 0xFF778DA9.toInt(),
                    primaryContainer = 0xFF1B263B.toInt(),
                    onPrimaryContainer = 0xFFE0E1DD.toInt(),
                    tertiary = 0xFF415A77.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFF1B263B.toInt(),
                    onTertiaryContainer = 0xFFE0E1DD.toInt(),
                    eventHighlightTint = 0xFF778DA9.toInt(),
                    counterBg = 0xFF415A77.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF415A77.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF2F5F8.toInt(),
                    onSurface = 0xFF1B232D.toInt(),
                    surfaceVariant = 0xFFDFE6EE.toInt(),
                    onSurfaceVariant = 0xFF324254.toInt(),
                    primaryContainer = 0xFFEAF0F6.toInt(),
                    onPrimaryContainer = 0xFF011B2A.toInt(),
                    tertiary = 0xFF1B263B.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFEAF0F6.toInt(),
                    onTertiaryContainer = 0xFF011B2A.toInt(),
                    eventHighlightTint = 0xFF415A77.toInt(),
                    counterBg = 0xFF1B263B.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            "soft_lavender" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFC9ADA7.toInt(),
                    onPrimary = 0xFF2C2423.toInt(),
                    surface = 0xFF1C1C24.toInt(),
                    onSurface = 0xFFEDE8EC.toInt(),
                    surfaceVariant = 0xFF32303D.toInt(),
                    onSurfaceVariant = 0xFFC9ADA7.toInt(),
                    primaryContainer = 0xFF22223B.toInt(),
                    onPrimaryContainer = 0xFFF2E9E4.toInt(),
                    tertiary = 0xFFF2E9E4.toInt(),
                    onTertiary = 0xFF22223B.toInt(),
                    tertiaryContainer = 0xFF22223B.toInt(),
                    onTertiaryContainer = 0xFFF2E9E4.toInt(),
                    eventHighlightTint = 0xFFC9ADA7.toInt(),
                    counterBg = 0xFFC9ADA7.toInt(),
                    counterText = 0xFF2C2423.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF4A4E69.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF6F3F5.toInt(),
                    onSurface = 0xFF212028.toInt(),
                    surfaceVariant = 0xFFE8E1E6.toInt(),
                    onSurfaceVariant = 0xFF3A3C4D.toInt(),
                    primaryContainer = 0xFFF2E9E4.toInt(),
                    onPrimaryContainer = 0xFF22223B.toInt(),
                    tertiary = 0xFF4A4E69.toInt(),
                    onTertiary = 0xFFFFFFFF.toInt(),
                    tertiaryContainer = 0xFFF2E9E4.toInt(),
                    onTertiaryContainer = 0xFF22223B.toInt(),
                    eventHighlightTint = 0xFF4A4E69.toInt(),
                    counterBg = 0xFF4A4E69.toInt(),
                    counterText = 0xFFFFFFFF.toInt()
                )
            }
            else -> {
                // When app theme uses system colors ("default" or "system"), use dynamic system colors on Android 12+ (API 31+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamicScheme = if (isDark) {
                        androidx.compose.material3.dynamicDarkColorScheme(context)
                    } else {
                        androidx.compose.material3.dynamicLightColorScheme(context)
                    }
                    WidgetThemeColors(
                        primary = dynamicScheme.primary.toArgb(),
                        onPrimary = dynamicScheme.onPrimary.toArgb(),
                        surface = dynamicScheme.surface.toArgb(),
                        onSurface = dynamicScheme.onSurface.toArgb(),
                        surfaceVariant = dynamicScheme.surfaceVariant.toArgb(),
                        onSurfaceVariant = dynamicScheme.onSurfaceVariant.toArgb(),
                        primaryContainer = dynamicScheme.primaryContainer.toArgb(),
                        onPrimaryContainer = dynamicScheme.onPrimaryContainer.toArgb(),
                        tertiary = dynamicScheme.tertiary.toArgb(),
                        onTertiary = dynamicScheme.onTertiary.toArgb(),
                        tertiaryContainer = dynamicScheme.tertiaryContainer.toArgb(),
                        onTertiaryContainer = dynamicScheme.onTertiaryContainer.toArgb(),
                        eventHighlightTint = dynamicScheme.tertiary.toArgb(),
                        counterBg = dynamicScheme.tertiary.toArgb(),
                        counterText = dynamicScheme.onTertiary.toArgb()
                    )
                } else if (isDark) { // Fallback Classic Purple dark theme
                    WidgetThemeColors(
                        primary = 0xFFD0BCFF.toInt(),
                        onPrimary = 0xFF381E72.toInt(),
                        surface = 0xFF1C1A22.toInt(),
                        onSurface = 0xFFE6E1E5.toInt(),
                        surfaceVariant = 0xFF3B3545.toInt(),
                        onSurfaceVariant = 0xFFCAC4D0.toInt(),
                        primaryContainer = 0xFF4F378B.toInt(),
                        onPrimaryContainer = 0xFFEADDFF.toInt(),
                        tertiary = 0xFFEFB8C8.toInt(),
                        onTertiary = 0xFF492532.toInt(),
                        tertiaryContainer = 0xFF633B48.toInt(),
                        onTertiaryContainer = 0xFFFFD8E4.toInt(),
                        eventHighlightTint = 0xFFEFB8C8.toInt(),
                        counterBg = 0xFFEFB8C8.toInt(),
                        counterText = 0xFF492532.toInt()
                    )
                } else { // Fallback Classic Purple light theme
                    WidgetThemeColors(
                        primary = 0xFF6650A4.toInt(),
                        onPrimary = 0xFFFFFFFF.toInt(),
                        surface = 0xFFFDFCFF.toInt(),
                        onSurface = 0xFF1D1B20.toInt(),
                        surfaceVariant = 0xFFEADBFF.toInt(),
                        onSurfaceVariant = 0xFF49454E.toInt(),
                        primaryContainer = 0xFFEADDFF.toInt(),
                        onPrimaryContainer = 0xFF21005D.toInt(),
                        tertiary = 0xFF7D5260.toInt(),
                        onTertiary = 0xFFFFFFFF.toInt(),
                        tertiaryContainer = 0xFFFFD8E4.toInt(),
                        onTertiaryContainer = 0xFF31111D.toInt(),
                        eventHighlightTint = 0xFF7D5260.toInt(),
                        counterBg = 0xFF7D5260.toInt(),
                        counterText = 0xFFFFFFFF.toInt()
                    )
                }
            }
        }
    }
}
