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
    primary = Color(0xFF1E6BFF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7FF),
    onPrimaryContainer = Color(0xFF001A41),
    secondary = Color(0xFF4F5D78),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1E8F5),
    onSecondaryContainer = Color(0xFF0B1B33),
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
    surfaceVariant = Color(0xFFE3E7EF),
    onSurfaceVariant = Color(0xFF454C5C),
    outline = Color(0xFF7A8294),
    outlineVariant = Color(0xFFD3D8E2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8FB),
    surfaceContainer = Color(0xFFF1F3F8),
    surfaceContainerHigh = Color(0xFFEBEEF4),
    surfaceContainerHighest = Color(0xFFE4E8F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5B9BFF),
    onPrimary = Color(0xFF002A66),
    primaryContainer = Color(0xFF133C8C),
    onPrimaryContainer = Color(0xFFD9E6FF),
    secondary = Color(0xFFB7C4DE),
    onSecondary = Color(0xFF1E2B44),
    secondaryContainer = Color(0xFF2B3850),
    onSecondaryContainer = Color(0xFFDDE6F7),
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
    surfaceVariant = Color(0xFF2A303B),
    onSurfaceVariant = Color(0xFFB5BDCC),
    outline = Color(0xFF7F8798),
    outlineVariant = Color(0xFF2E3440),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF0C0E12),
    surfaceContainer = Color(0xFF12151B),
    surfaceContainerHigh = Color(0xFF191D24),
    surfaceContainerHighest = Color(0xFF21262F),
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
