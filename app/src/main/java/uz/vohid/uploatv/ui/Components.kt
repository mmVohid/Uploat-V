package uz.vohid.uploatv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import uz.vohid.uploatv.download.DEFAULT_FOLDER
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.Status

/** 16:9 rasm; rasm bo'lmasa tur ikonkasi ko'rsatiladi. */
@Composable
fun Thumbnail(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Videocam,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun DownloadRow(
    item: DownloadItem,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProgressThumb(item)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StatusLine(item)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (item.status) {
                    Status.QUEUED, Status.RUNNING -> TextButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Bekor qilish")
                    }
                    Status.DONE -> {
                        FilledTonalButton(onClick = onOpen) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ochish")
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Ro'yxatdan olib tashlash")
                        }
                    }
                    Status.FAILED, Status.CANCELED -> {
                        TextButton(onClick = onRetry) {
                            Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Qayta urinish")
                        }
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Ro'yxatdan olib tashlash")
                        }
                    }
                }
            }
        }
    }
}

/** Yumaloq rasm atrofida gradient progress halqasi; tugagach yashil porlaydi. */
@Composable
private fun ProgressThumb(item: DownloadItem) {
    val scheme = MaterialTheme.colorScheme
    val progress by animateFloatAsState(item.progress, label = "ring")
    val done = item.status == Status.DONE
    val failed = item.status == Status.FAILED
    Box(
        modifier = Modifier
            .size(72.dp)
            .drawBehind {
                if (done) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(scheme.tertiary.copy(alpha = 0.45f), Color.Transparent),
                            radius = size.minDimension * 0.75f,
                        ),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (item.thumbnail != null) {
                AsyncImage(
                    model = item.thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            } else {
                Icon(Icons.Rounded.Videocam, null, tint = scheme.onSurfaceVariant)
            }
            if (item.status == Status.RUNNING && item.progress > 0f) {
                Box(
                    Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }
        }
        val track = scheme.outlineVariant.copy(alpha = 0.5f)
        when {
            item.status == Status.QUEUED || (item.status == Status.RUNNING && item.progress <= 0f) ->
                CircularProgressIndicator(
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 4.dp,
                    color = scheme.primary,
                    trackColor = track,
                    strokeCap = StrokeCap.Round,
                )
            item.status == Status.RUNNING ->
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 4.dp,
                    color = scheme.primary,
                    trackColor = track,
                    strokeCap = StrokeCap.Round,
                )
            else ->
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 3.dp,
                    color = when {
                        done -> scheme.tertiary
                        failed -> scheme.error
                        else -> track
                    },
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                )
        }
    }
}

@Composable
private fun StatusLine(item: DownloadItem) {
    val scheme = MaterialTheme.colorScheme
    val (text, color) = when (item.status) {
        Status.QUEUED -> "Navbatda" to scheme.onSurfaceVariant
        Status.RUNNING -> {
            val pct = "${(item.progress * 100).toInt()}%"
            (if (item.detail.isBlank()) pct else "$pct · ${item.detail}") to scheme.primary
        }
        Status.DONE -> {
            val base = "${item.quality.label} · ${item.savedTo ?: DEFAULT_FOLDER}"
            (if (item.detail.isBlank()) base else "$base · ${item.detail}") to scheme.onSurfaceVariant
        }
        Status.FAILED -> (item.error ?: "Xatolik") to scheme.error
        Status.CANCELED -> "Bekor qilindi" to scheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (item.status) {
            Status.DONE -> Icon(
                Icons.Rounded.CheckCircle, null,
                Modifier.size(14.dp), tint = scheme.primary,
            )
            Status.FAILED -> Icon(
                Icons.Rounded.ErrorOutline, null,
                Modifier.size(14.dp), tint = scheme.error,
            )
            else -> {}
        }
        if (item.status == Status.DONE || item.status == Status.FAILED) Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
