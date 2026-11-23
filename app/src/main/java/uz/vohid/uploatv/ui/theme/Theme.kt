package uz.vohid.uploatv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import uz.vohid.uploatv.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF006A65),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1EA),
    onPrimaryContainer = Color(0xFF00201E),
    secondary = Color(0xFF4A6361),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E5),
    onSecondaryContainer = Color(0xFF05201F),
    tertiary = Color(0xFF3F6837),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFF0B0),
    onTertiaryContainer = Color(0xFF002200),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4FBF9),
    onBackground = Color(0xFF161D1C),
    surface = Color(0xFFF4FBF9),
    onSurface = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDAE5E2),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3E9E7),
    surfaceContainerHighest = Color(0xFFDDE4E1),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5CE),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF00504C),
    onPrimaryContainer = Color(0xFF9CF1EA),
    secondary = Color(0xFFB1CCC9),
    onSecondary = Color(0xFF1C3533),
    secondaryContainer = Color(0xFF324B49),
    onSecondaryContainer = Color(0xFFCCE8E5),
    tertiary = Color(0xFFA4D397),
    onTertiary = Color(0xFF0A3909),
    tertiaryContainer = Color(0xFF25511F),
    onTertiaryContainer = Color(0xFFBFF0B0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1413),
    onBackground = Color(0xFFDDE4E2),
    surface = Color(0xFF0B1413),
    onSurface = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF899392),
    outlineVariant = Color(0xFF3F4948),
    surfaceContainerLowest = Color(0xFF060E0D),
    surfaceContainerLow = Color(0xFF121A19),
    surfaceContainer = Color(0xFF16201F),
    surfaceContainerHigh = Color(0xFF212B2A),
    surfaceContainerHighest = Color(0xFF2C3635),
)

@Composable
fun UploatTheme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    // Status va navigatsiya paneli ikonkalari mavzuga mos bo'lsin
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(colorScheme = scheme, content = content)
}
