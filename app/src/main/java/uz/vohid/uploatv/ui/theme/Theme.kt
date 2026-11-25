package uz.vohid.uploatv.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import uz.vohid.uploatv.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF111318),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE9EAEE),
    onPrimaryContainer = Color(0xFF111318),
    secondary = Color(0xFF55585F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEDEEF1),
    onSecondaryContainer = Color(0xFF1B1D22),
    tertiary = Color(0xFF1B9E5A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC8F4DC),
    onTertiaryContainer = Color(0xFF00391C),
    error = Color(0xFFE5383B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410006),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0B0D12),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B0D12),
    surfaceVariant = Color(0xFFE6E7EB),
    onSurfaceVariant = Color(0xFF494C53),
    outline = Color(0xFF7B7E86),
    outlineVariant = Color(0xFFD8D9DE),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F8FA),
    surfaceContainer = Color(0xFFF3F3F6),
    surfaceContainerHigh = Color(0xFFEDEDF0),
    surfaceContainerHighest = Color(0xFFE7E7EB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF111318),
    primaryContainer = Color(0xFF2C2E34),
    onPrimaryContainer = Color(0xFFF2F3F6),
    secondary = Color(0xFFC4C6CC),
    onSecondary = Color(0xFF1F2126),
    secondaryContainer = Color(0xFF2A2C32),
    onSecondaryContainer = Color(0xFFE3E4E8),
    tertiary = Color(0xFF4CD38A),
    onTertiary = Color(0xFF00391C),
    tertiaryContainer = Color(0xFF0F5132),
    onTertiaryContainer = Color(0xFFC8F4DC),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF5C0008),
    errorContainer = Color(0xFF8C1D1F),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF050608),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF050608),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF2B2D33),
    onSurfaceVariant = Color(0xFFB8BAC1),
    outline = Color(0xFF868991),
    outlineVariant = Color(0xFF2F3138),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0D0E10),
    surfaceContainer = Color(0xFF131417),
    surfaceContainerHigh = Color(0xFF1A1B1F),
    surfaceContainerHighest = Color(0xFF222327),
)

@Composable
fun UploatTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) DarkColors else LightColors

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
