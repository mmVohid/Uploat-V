package uz.vohid.uploatv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/** Shisha panellar orqasidagi fonni xiralashtirish uchun umumiy holat. */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Butun ilova uchun fon: gradient + yumshoq rangli "nur dog'lari".
 * Aynan shu qatlam xiralashtiriladi (hazeSource), kontent esa uning ustida turadi.
 */
@Composable
fun GlassScaffold(content: @Composable BoxScope.() -> Unit) {
    val hazeState = remember { HazeState() }
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val glow = if (dark) 0.40f else 0.30f
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .background(scheme.surface)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        Brush.radialGradient(
                            listOf(scheme.primary.copy(alpha = glow), Color.Transparent),
                            center = Offset(w * 0.1f, h * 0.08f),
                            radius = w * 1.1f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(scheme.tertiary.copy(alpha = glow), Color.Transparent),
                            center = Offset(w * 0.95f, h * 0.45f),
                            radius = w * 1.0f,
                        ),
                    )
                    drawRect(
                        Brush.radialGradient(
                            listOf(scheme.secondary.copy(alpha = glow * 0.9f), Color.Transparent),
                            center = Offset(w * 0.2f, h * 0.98f),
                            radius = w * 1.1f,
                        ),
                    )
                },
        )
        androidx.compose.runtime.CompositionLocalProvider(LocalHazeState provides hazeState) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

/** Shisha effekti: xiralashgan fon + yarim shaffof rang + yorug' chegara. */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tintAlpha: Float = 0.5f,
): Modifier {
    val state = LocalHazeState.current
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val rim = if (dark) Color.White else Color.White
    val base = this.clip(shape)
    val blurred = if (state != null) {
        base.hazeEffect(state) {
            blurRadius = 28.dp
            backgroundColor = scheme.surface
            tints = listOf(HazeTint(tint.copy(alpha = tintAlpha)))
            noiseFactor = 0.04f
        }
    } else {
        base.background(tint.copy(alpha = 0.85f))
    }
    return blurred.border(
        1.dp,
        Brush.linearGradient(listOf(rim.copy(alpha = if (dark) 0.28f else 0.8f), rim.copy(alpha = if (dark) 0.06f else 0.25f))),
        shape,
    )
}

/** Shisha kartochka ichidagi Card: o'zi shaffof, effekt modifier orqali beriladi. */
@Composable
fun glassCardColors(content: Color = Color.Unspecified) =
    CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = content)
