package uz.vohid.uploatv.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
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
 * Butun ilova uchun toza fon (yorug' rejimda oq, qorong'uda qora).
 * Kartochkalar shu qatlamni xiralashtiradi (hazeSource).
 */
@Composable
fun GlassScaffold(content: @Composable BoxScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize().background(scheme.surface)) {
        CompositionLocalProvider(LocalContentColor provides scheme.onSurface) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

/** Bosilganda yumshoq kichrayish (spring) effekti. */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.95f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) pressedScale else 1f,
        spring(dampingRatio = 0.55f, stiffness = 600f),
        label = "press",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Shisha effekti: xiralashgan fon + yarim shaffof rang + yorug' chegara. */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    tintAlpha: Float = 0.9f,
    hazeState: HazeState? = null,
): Modifier {
    val state = hazeState
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    val rim = if (dark) Color.White else Color.Black
    val base = this.clip(shape)
    val blurred = if (state != null) {
        base.hazeEffect(state) {
            blurRadius = 28.dp
            backgroundColor = scheme.surface
            tints = listOf(HazeTint(tint.copy(alpha = tintAlpha)))
            noiseFactor = 0.04f
        }
    } else {
        base.background(tint.copy(alpha = tintAlpha))
    }
    return blurred.border(
        1.dp,
        Brush.linearGradient(listOf(rim.copy(alpha = if (dark) 0.22f else 0.12f), rim.copy(alpha = if (dark) 0.06f else 0.04f))),
        shape,
    )
}

/** Shisha kartochka ichidagi Card: o'zi shaffof, effekt modifier orqali beriladi. */
@Composable
fun glassCardColors(content: Color = Color.Unspecified) =
    CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = content)
