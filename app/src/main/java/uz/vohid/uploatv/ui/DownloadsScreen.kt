package uz.vohid.uploatv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.remember
import uz.vohid.uploatv.download.Status
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.ErrorAction

/** Barcha yuklashlar: faol va tugaganlar. Tugaganlarni chapga surib o'chirish mumkin. */
@Composable
fun DownloadsScreen(
    snackbar: SnackbarHostState,
    onErrorAction: (ErrorAction, String) -> Unit,
    onPlay: (String) -> Unit,
    onBack: () -> Unit,
) {
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val queue by DownloadManager.queue.collectAsStateWithLifecycle()
    val currentQueue by rememberUpdatedState(queue)
    val ordered = remember(items, queue) { orderForScreen(items, queue) }
    val listState = rememberLazyListState()
    var draggedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Sudralayotgan qator boshqa navbatdagi qator ustiga yetganda ular o'rin almashadi
    val onDragBy: (String, Float) -> Unit = { id, dy ->
        dragOffset += dy
        val infos = listState.layoutInfo.visibleItemsInfo
        val me = infos.firstOrNull { it.key == id }
        if (me != null) {
            val center = me.offset + me.size / 2f + dragOffset
            val q = currentQueue
            val target = infos.firstOrNull { info ->
                val k = info.key as? String
                k != null && k != id && k in q && center >= info.offset && center <= info.offset + info.size
            }
            if (target != null) {
                DownloadManager.moveInQueue(id, q.indexOf(target.key as String))
                dragOffset -= (target.offset - me.offset)
            }
        }
    }
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                }
                Text(
                    text = "Yuklashlar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (items.any { !it.isActive }) {
                    TextButton(onClick = DownloadManager::clearFinished) { Text("Tozalash") }
                }
            }
        },
    ) { pad ->
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Text("Hozircha yuklashlar yo'q", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Yuklangan videolar shu yerda chiqadi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            val side = adaptiveSidePadding()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = side,
                    end = side,
                    top = pad.calculateTopPadding() + 8.dp,
                    bottom = 24.dp + navBottom,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(ordered, key = { _, it -> it.id }, contentType = { _, _ -> "download" }) { index, item ->
                    val pos = queue.indexOf(item.id).takeIf { it >= 0 && item.status == Status.QUEUED }?.plus(1)
                    val dragged = draggedId == item.id
                    val handle: (@Composable () -> Unit)? = if (pos != null && queue.size > 1) {
                        {
                            DragHandleBox(
                                onStart = {
                                    draggedId = item.id
                                    dragOffset = 0f
                                },
                                onDrag = { onDragBy(item.id, it) },
                                onEnd = {
                                    draggedId = null
                                    dragOffset = 0f
                                },
                            )
                        }
                    } else {
                        null
                    }
                    SwipeDownloadRow(
                        item = item,
                        index = index,
                        snackbar = snackbar,
                        onErrorAction = { onErrorAction(it, item.id) },
                        onPlay = { onPlay(item.id) },
                        queuePos = pos,
                        onMoveToFront = { DownloadManager.moveToFront(item.id) },
                        dragHandle = handle,
                        modifier = if (dragged) {
                            Modifier.zIndex(1f).graphicsLayer {
                                translationY = dragOffset
                                scaleX = 1.03f
                                scaleY = 1.03f
                            }
                        } else {
                            Modifier.animateItem()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeDownloadRow(
    item: DownloadItem,
    index: Int,
    snackbar: SnackbarHostState,
    onErrorAction: (ErrorAction) -> Unit,
    queuePos: Int?,
    onPlay: () -> Unit,
    onMoveToFront: () -> Unit,
    dragHandle: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart && !item.isActive) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                DownloadManager.remove(item.id)
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = "O'chirildi",
                        actionLabel = "Qaytarish",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) DownloadManager.restore(item)
                }
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !item.isActive,
        backgroundContent = {
            // Karta yarim shaffof: fon faqat surish paytida chizilsin, aks holda orqadan ko'rinib turadi
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "O'chirish",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        },
    ) {
        var shown by rememberSaveable(item.id) { mutableStateOf(index > 6) }
        LaunchedEffect(item.id) {
            if (!shown) {
                delay(index * 60L)
                shown = true
            }
        }
        AnimatedVisibility(
            visible = shown,
            enter = fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 4 },
        ) {
            DownloadRow(
                item = item,
                onCancel = { DownloadManager.cancel(item.id) },
                onRetry = { DownloadManager.retry(item.id) },
                onOpen = { openFile(context, item) },
                onRemove = { DownloadManager.remove(item.id) },
                onErrorAction = onErrorAction,
                // Bitta video fayli ilova ichida ijro etiladi; playlist yoki boshqa turdagi fayllar tashqi ilovada
                onPlay = if (!item.playlist && item.mime?.startsWith("video") == true) onPlay else null,
                queuePos = queuePos,
                onMoveToFront = onMoveToFront,
                dragHandle = dragHandle,
            )
        }
    }
}

/** Ekrandagi tartib: avval ishlayotganlar, keyin navbat tartibida kutayotganlar, so'ng tugaganlar. */
private fun orderForScreen(items: List<DownloadItem>, queue: List<String>): List<DownloadItem> {
    val byId = items.associateBy { it.id }
    val running = items.filter { it.status == Status.RUNNING }
    val queued = queue.mapNotNull { byId[it] }.filter { it.status == Status.QUEUED }
    val queuedIds = queued.map { it.id }.toSet()
    // Ma'lumoti olinayotgan (hali navbatga kirmagan) yuklashlar navbat oxirida turadi
    val pending = items.filter { it.status == Status.QUEUED && it.id !in queuedIds }
    val rest = items.filter { !it.isActive }
    return running + queued + pending + rest
}

/** Navbatdagi qatorni ushlab sudrash uchun tutqich. */
@Composable
private fun DragHandleBox(onStart: () -> Unit, onDrag: (Float) -> Unit, onEnd: () -> Unit) {
    val start by rememberUpdatedState(onStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    Box(
        modifier = Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start() },
                    onDrag = { change, amount ->
                        change.consume()
                        drag(amount.y)
                    },
                    onDragEnd = { end() },
                    onDragCancel = { end() },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.DragHandle,
            contentDescription = "Tartibni o'zgartirish (sudrang)",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
