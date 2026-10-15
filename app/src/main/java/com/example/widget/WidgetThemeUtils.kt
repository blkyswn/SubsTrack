package com.example.widget

import android.content.Context
import android.content.res.Configuration
import com.example.data.User
import java.util.Locale

data class WidgetThemeColors(
    val primary: Int,
    val onPrimary: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int
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
                    onSurfaceVariant = 0xFFCCD5AE.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD4A373.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFEFAE0.toInt(),
                    onSurface = 0xFF231C0C.toInt(),
                    surfaceVariant = 0xFFFAEDCD.toInt(),
                    onSurfaceVariant = 0xFF512E00.toInt()
                )
            }
            "cozy" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFEBAF94.toInt(),
                    onPrimary = 0xFF3E1705.toInt(),
                    surface = 0xFF211D17.toInt(),
                    onSurface = 0xFFF7F3DC.toInt(),
                    surfaceVariant = 0xFF3D2A1F.toInt(),
                    onSurfaceVariant = 0xFFF2CBB2.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD19E82.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF7F3DC.toInt(),
                    onSurface = 0xFF352C16.toInt(),
                    surfaceVariant = 0xFFF7D3AD.toInt(),
                    onSurfaceVariant = 0xFF5E2E10.toInt()
                )
            }
            "winter" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFDDD3C1.toInt(),
                    onPrimary = 0xFF241C10.toInt(),
                    surface = 0xFF201F1D.toInt(),
                    onSurface = 0xFFFBFAF8.toInt(),
                    surfaceVariant = 0xFF38352F.toInt(),
                    onSurfaceVariant = 0xFFC4B69E.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFC4B69E.toInt(),
                    onPrimary = 0xFF000000.toInt(),
                    surface = 0xFFFAF7F0.toInt(),
                    onSurface = 0xFF24221E.toInt(),
                    surfaceVariant = 0xFFEFE8DB.toInt(),
                    onSurfaceVariant = 0xFF4A453A.toInt()
                )
            }
            "serenity" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFE6D7AB.toInt(),
                    onPrimary = 0xFF332A02.toInt(),
                    surface = 0xFF222019.toInt(),
                    onSurface = 0xFFFCFCF2.toInt(),
                    surfaceVariant = 0xFF3C3725.toInt(),
                    onSurfaceVariant = 0xFFD4C294.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFD4C294.toInt(),
                    onPrimary = 0xFF3E3106.toInt(),
                    surface = 0xFFF8F4E3.toInt(),
                    onSurface = 0xFF3E3106.toInt(),
                    surfaceVariant = 0xFFF3ECD3.toInt(),
                    onSurfaceVariant = 0xFF5A4D21.toInt()
                )
            }
            "green_serenity" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFA1CCA5.toInt(),
                    onPrimary = 0xFF112E15.toInt(),
                    surface = 0xFF1A221A.toInt(),
                    onSurface = 0xFFEBF5EC.toInt(),
                    surfaceVariant = 0xFF273828.toInt(),
                    onSurfaceVariant = 0xFFA1CCA5.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF588157.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF2F7F2.toInt(),
                    onSurface = 0xFF1E281F.toInt(),
                    surfaceVariant = 0xFFE0EFE2.toInt(),
                    onSurfaceVariant = 0xFF334B32.toInt()
                )
            }
            "blood_moon" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFF9DC7C8.toInt(),
                    onPrimary = 0xFF173031.toInt(),
                    surface = 0xFF231717.toInt(),
                    onSurface = 0xFFF7EBEB.toInt(),
                    surfaceVariant = 0xFF3D2525.toInt(),
                    onSurfaceVariant = 0xFF9DC7C8.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF6A3937.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFDF5F5.toInt(),
                    onSurface = 0xFF2C1E1E.toInt(),
                    surfaceVariant = 0xFFF2DEDE.toInt(),
                    onSurfaceVariant = 0xFF543837.toInt()
                )
            }
            "earthy_harmony" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFDDC8C4.toInt(),
                    onPrimary = 0xFF3A2B29.toInt(),
                    surface = 0xFF211A19.toInt(),
                    onSurface = 0xFFF7EFEB.toInt(),
                    surfaceVariant = 0xFF3A2D2C.toInt(),
                    onSurfaceVariant = 0xFFDDC8C4.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF896A67.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFBF6F4.toInt(),
                    onSurface = 0xFF2B2120.toInt(),
                    surfaceVariant = 0xFFEFE4E1.toInt(),
                    onSurfaceVariant = 0xFF5A4644.toInt()
                )
            }
            "chocolate_sunset" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFCB9173.toInt(),
                    onPrimary = 0xFF3C1F10.toInt(),
                    surface = 0xFF211614.toInt(),
                    onSurface = 0xFFF9EDE5.toInt(),
                    surfaceVariant = 0xFF3C2420.toInt(),
                    onSurfaceVariant = 0xFFCB9173.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF8E443D.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFDF6F0.toInt(),
                    onSurface = 0xFF2D1E1B.toInt(),
                    surfaceVariant = 0xFFF5E4D7.toInt(),
                    onSurfaceVariant = 0xFF5A312D.toInt()
                )
            }
            "red_sunburst" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFCE4257.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFF231315.toInt(),
                    onSurface = 0xFFFFEAEA.toInt(),
                    surfaceVariant = 0xFF401C21.toInt(),
                    onSurfaceVariant = 0xFFFFB2B9.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFFCE4257.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFFF5F5.toInt(),
                    onSurface = 0xFF2F171A.toInt(),
                    surfaceVariant = 0xFFFFE0E2.toInt(),
                    onSurfaceVariant = 0xFF662731.toInt()
                )
            }
            "earthy_forest" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFA3B18A.toInt(),
                    onPrimary = 0xFF1F2B1A.toInt(),
                    surface = 0xFF161E1A.toInt(),
                    onSurface = 0xFFE8EFEA.toInt(),
                    surfaceVariant = 0xFF24352B.toInt(),
                    onSurfaceVariant = 0xFFA3B18A.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF344E41.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF4F7F2.toInt(),
                    onSurface = 0xFF18221D.toInt(),
                    surfaceVariant = 0xFFE0EAE0.toInt(),
                    onSurfaceVariant = 0xFF314339.toInt()
                )
            }
            "deep_sea" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFF778DA9.toInt(),
                    onPrimary = 0xFF182330.toInt(),
                    surface = 0xFF161C22.toInt(),
                    onSurface = 0xFFE7EDF3.toInt(),
                    surfaceVariant = 0xFF23303D.toInt(),
                    onSurfaceVariant = 0xFF778DA9.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF415A77.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF2F5F8.toInt(),
                    onSurface = 0xFF1B232D.toInt(),
                    surfaceVariant = 0xFFDFE6EE.toInt(),
                    onSurfaceVariant = 0xFF324254.toInt()
                )
            }
            "soft_lavender" -> if (isDark) {
                WidgetThemeColors(
                    primary = 0xFFC9ADA7.toInt(),
                    onPrimary = 0xFF2C2423.toInt(),
                    surface = 0xFF1C1C24.toInt(),
                    onSurface = 0xFFEDE8EC.toInt(),
                    surfaceVariant = 0xFF32303D.toInt(),
                    onSurfaceVariant = 0xFFC9ADA7.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF4A4E69.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFF6F3F5.toInt(),
                    onSurface = 0xFF212028.toInt(),
                    surfaceVariant = 0xFFE8E1E6.toInt(),
                    onSurfaceVariant = 0xFF3A3C4D.toInt()
                )
            }
            else -> if (isDark) { // default theme
                WidgetThemeColors(
                    primary = 0xFFD0BCFF.toInt(),
                    onPrimary = 0xFF381E72.toInt(),
                    surface = 0xFF1C1A22.toInt(),
                    onSurface = 0xFFE6E1E5.toInt(),
                    surfaceVariant = 0xFF3B3545.toInt(),
                    onSurfaceVariant = 0xFFCAC4D0.toInt()
                )
            } else {
                WidgetThemeColors(
                    primary = 0xFF6650A4.toInt(),
                    onPrimary = 0xFFFFFFFF.toInt(),
                    surface = 0xFFFDFCFF.toInt(),
                    onSurface = 0xFF1D1B20.toInt(),
                    surfaceVariant = 0xFFEADBFF.toInt(),
                    onSurfaceVariant = 0xFF49454E.toInt()
                )
            }
        }
    }
}
