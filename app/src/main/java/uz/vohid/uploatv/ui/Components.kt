package uz.vohid.uploatv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import uz.vohid.uploatv.download.DownloadManager
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import android.widget.Toast
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import uz.vohid.uploatv.download.ErrorAction
import uz.vohid.uploatv.download.errorAction
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
    onErrorAction: (ErrorAction) -> Unit = {},
    /** Navbatdagi o'rni (1 dan); null bo'lsa ko'rsatilmaydi. */
    queuePos: Int? = null,
    /** Berilsa, "Ko'rish" ilova ichida ijro etadi; "Ochish" esa menyuga o'tadi. */
    onPlay: (() -> Unit)? = null,
    onMoveToFront: (() -> Unit)? = null,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
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
                    StatusLine(item, queuePos)
                    if (item.status == Status.QUEUED || item.status == Status.RUNNING) {
                        val bar by animateFloatAsState(item.progress, label = "bar")
                        val barModifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                        if (item.status == Status.RUNNING && item.progress > 0f) {
                            LinearProgressIndicator(
                                progress = { bar },
                                modifier = barModifier,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = barModifier,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            // Xato sababiga mos tavsiya: bir bosishda to'g'ri joyga olib boradi
            val action = if (item.status == Status.FAILED) errorAction(item.errorDetail) else ErrorAction.NONE
            if (action != ErrorAction.NONE) {
                FilledTonalButton(
                    onClick = { onErrorAction(action) },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                ) { Text(action.label) }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (item.status) {
                    Status.QUEUED, Status.RUNNING -> {
                        if (onMoveToFront != null && queuePos != null && queuePos > 1) {
                            TextButton(onClick = onMoveToFront) {
                                Icon(Icons.Rounded.ArrowUpward, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Birinchi")
                            }
                        }
                        TextButton(onClick = onCancel) {
                            Icon(Icons.Rounded.Close, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Bekor qilish")
                        }
                        dragHandle?.invoke()
                    }
                    Status.DONE -> if (item.savedUri == null) {
                        // Yangi video bo'lmagan: ochiladigan fayl yo'q, faqat ro'yxatdan olib tashlash
                        IconButton(onClick = onRemove) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Ro'yxatdan olib tashlash")
                        }
                    } else {
                        var menuOpen by remember { mutableStateOf(false) }
                        var confirmDelete by remember { mutableStateOf(false) }
                        val scope = rememberCoroutineScope()
                        FilledTonalButton(onClick = onPlay ?: onOpen) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (onPlay != null) "Ko'rish" else "Ochish")
                        }
                        IconButton(onClick = { shareFile(context, item) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Ulashish")
                        }
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "Boshqa amallar")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (onPlay != null) {
                                    DropdownMenuItem(
                                        text = { Text("Boshqa ilovada ochish") },
                                        leadingIcon = { Icon(Icons.Rounded.OpenInNew, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            onOpen()
                                        },
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Ro'yxatdan olib tashlash") },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        onRemove()
                                    },
                                )
                                if (!item.playlist) {
                                    DropdownMenuItem(
                                        text = { Text("Telefondan ham o'chirish") },
                                        leadingIcon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null) },
                                        onClick = {
                                            menuOpen = false
                                            confirmDelete = true
                                        },
                                    )
                                }
                            }
                        }
                        if (confirmDelete) {
                            AlertDialog(
                                onDismissRequest = { confirmDelete = false },
                                title = { Text("Faylni o'chirasizmi?") },
                                text = { Text("«${item.title}» telefondan butunlay o'chadi. Buni qaytarib bo'lmaydi.") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        confirmDelete = false
                                        scope.launch {
                                            val ok = DownloadManager.deleteFile(item.id)
                                            Toast.makeText(
                                                context,
                                                if (ok) "Fayl o'chirildi" else "Faylni o'chirib bo'lmadi",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                            if (ok) onRemove()
                                        }
                                    }) { Text("O'chirish", color = MaterialTheme.colorScheme.error) }
                                },
                                dismissButton = {
                                    TextButton(onClick = { confirmDelete = false }) { Text("Bekor qilish") }
                                },
                            )
                        }
                    }
                    Status.FAILED, Status.CANCELED -> {
                        if (item.status == Status.FAILED && !item.errorDetail.isNullOrBlank()) {
                            TextButton(onClick = {
                                clipboard.setText(AnnotatedString("${item.url}\n${item.errorDetail}"))
                                Toast.makeText(context, "Xato matni nusxalandi", Toast.LENGTH_SHORT).show()
                            }) { Text("Nusxalash") }
                        }
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

/**
 * Ixcham 16:9 rasm (yumaloq burchakli to'rtburchak). Ustida: yuklanayotganda foiz, tugagach yashil ✓, xatoda qizil belgi.
 * Coil rasmni o'zi ko'rsatiladigan o'lchamgacha kichraytirib yuklaydi.
 */
@Composable
private fun ProgressThumb(item: DownloadItem) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(96.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
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
        when (item.status) {
            Status.QUEUED, Status.RUNNING -> Box(
                Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                if (item.status == Status.RUNNING && item.progress > 0f) {
                    Text(
                        "${(item.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
            Status.DONE -> StatusBadge(
                icon = Icons.Rounded.Check,
                container = scheme.tertiary,
                content = scheme.onTertiary,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
            Status.FAILED -> StatusBadge(
                icon = Icons.Rounded.Close,
                container = scheme.error,
                content = scheme.onError,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
            Status.CANCELED -> {}
        }
    }
}

@Composable
private fun StatusBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, container: Color, content: Color, modifier: Modifier) {
    Box(
        modifier = modifier.padding(4.dp).size(20.dp).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = content)
    }
}

@Composable
private fun StatusLine(item: DownloadItem, queuePos: Int? = null) {
    val scheme = MaterialTheme.colorScheme
    val (text, color) = when (item.status) {
        Status.QUEUED -> (if (queuePos != null) "Navbatda · $queuePos-o'rin" else "Navbatda") to scheme.onSurfaceVariant
        Status.RUNNING -> {
            val pct = "${(item.progress * 100).toInt()}%"
            (if (item.detail.isBlank()) pct else "$pct · ${item.detail}") to scheme.primary
        }
        Status.DONE -> if (item.savedUri == null) {
            item.detail.ifBlank { "Tayyor" } to scheme.onSurfaceVariant
        } else {
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
                Modifier.size(14.dp), tint = scheme.tertiary,
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
