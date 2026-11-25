package uz.vohid.uploatv.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Yangi yt-dlp bor bo'lganda ko'rinadigan sariq taklif. Sariq faqat shu kartochka uchun ishlatiladi,
 * shuning uchun u diqqatni tortadi, lekin asosiy palitrani (ko'k/yashil/qizil) o'zgartirmaydi.
 */
@Composable
fun EngineBanner(
    state: EngineBannerUi,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val container = if (dark) Color(0xFF2E2500) else Color(0xFFFFF4CC)
    val accent = if (dark) Color(0xFFFFC53D) else Color(0xFFF2A900)
    val onContainer = if (dark) Color(0xFFFFE9A8) else Color(0xFF4A3700)
    val onAccent = Color(0xFF241A00)
    val done = state.phase == BannerPhase.DONE
    val busy = state.phase == BannerPhase.UPDATING

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            when {
                busy -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = accent)
                done -> Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary)
                else -> Icon(Icons.Rounded.Bolt, null, tint = accent)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    when (state.phase) {
                        BannerPhase.UPDATING -> "Yangilanmoqda…"
                        BannerPhase.DONE -> "Yangilandi"
                        BannerPhase.FAILED -> "Yangilab bo'lmadi"
                        else -> "Dvigatelni yangilang"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = onContainer,
                    maxLines = 1,
                )
                Text(
                    when (state.phase) {
                        BannerPhase.UPDATING -> "Bir necha soniya kuting"
                        BannerPhase.DONE -> "Endi yuklash barqarorroq ishlaydi"
                        BannerPhase.FAILED -> state.message.ifBlank { "Internetni tekshirib qayta urinib ko'ring" }
                        else -> "Yuklash barqaror va sifatli bo'lishi uchun"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = onContainer.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.phase == BannerPhase.AVAILABLE || state.phase == BannerPhase.FAILED) {
                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                ) {
                    Text(if (state.phase == BannerPhase.FAILED) "Qayta" else "Yangilash", maxLines = 1)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Yopish", tint = onContainer)
                }
            }
        }
    }
}
