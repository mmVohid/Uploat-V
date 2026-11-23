package uz.vohid.uploatv.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import uz.vohid.uploatv.download.formatBytes
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, snackbar: SnackbarHostState, onOpenSettings: () -> Unit) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current

    // Buferdagi havolani aniqlash (Android faqat oyna fokusda bo'lganda o'qishga ruxsat beradi)
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    var clipUrl by remember { mutableStateOf<String?>(null) }
    var dismissedClip by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(windowFocused) {
        if (windowFocused) clipUrl = URL_REGEX.find(clipboard.getText()?.text.orEmpty())?.value
    }
    val showClip = clipUrl != null && clipUrl != dismissedClip &&
        ui.url.isEmpty() && ui.info == null && !ui.fetching

    val pasteFromClipboard: () -> Unit = {
        val url = URL_REGEX.find(clipboard.getText()?.text.orEmpty())?.value
        if (url != null) {
            vm.onUrlChange(url, immediate = true)
        } else {
            scope.launch { snackbar.showSnackbar("Buferda havola yo'q") }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HomeHeader(onOpenSettings) },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = 24.dp + navBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "url") {
                OutlinedTextField(
                    value = ui.url,
                    onValueChange = { vm.onUrlChange(it) },
                    modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp), tintAlpha = 0.4f),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    label = { Text("Video havolasi") },
                    placeholder = { Text("https://…") },
                    leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                    trailingIcon = {
                        if (ui.url.isEmpty()) {
                            IconButton(onClick = {
                                val text = clipboard.getText()?.text?.trim().orEmpty()
                                if (text.isNotEmpty()) vm.onUrlChange(text, immediate = true)
                            }) {
                                Icon(Icons.Rounded.ContentPaste, contentDescription = "Joylash")
                            }
                        } else {
                            IconButton(onClick = { vm.onUrlChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Tozalash")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focus.clearFocus()
                        vm.onUrlChange(ui.url, immediate = true)
                    }),
                )
            }

            item(key = "clip") {
                AnimatedVisibility(visible = showClip) {
                    clipUrl?.let { url ->
                        ClipCard(
                            url = url,
                            onUse = {
                                dismissedClip = url
                                vm.onUrlChange(url, immediate = true)
                            },
                            onDismiss = { dismissedClip = url },
                        )
                    }
                }
            }

            item(key = "loading") {
                AnimatedVisibility(visible = ui.fetching) { LoadingCard() }
            }

            item(key = "error") {
                AnimatedVisibility(visible = ui.error != null) {
                    ErrorCard(message = ui.error.orEmpty(), onRetry = vm::retryFetch)
                }
            }

            item(key = "info") {
                AnimatedVisibility(visible = ui.info != null) {
                    ui.info?.let { info ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoCard(info)
                            OptionsCard(ui = ui, vm = vm)
                            Button(
                                onClick = {
                                    focus.clearFocus()
                                    if (vm.download()) {
                                        scope.launch { snackbar.showSnackbar("Yuklash navbatga qo'shildi") }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                            ) {
                                Icon(Icons.Rounded.Download, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        info.isPlaylist -> "Playlistni yuklash (${info.entryCount})"
                                        else -> "Videoni yuklash"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }
            }

            item(key = "header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Yuklashlar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    if (items.any { !it.isActive }) {
                        TextButton(onClick = DownloadManager::clearFinished) { Text("Tozalash") }
                    }
                }
            }

            if (items.isEmpty()) {
                item(key = "empty") { EmptyState(onPaste = pasteFromClipboard) }
            } else {
                items(items, key = { it.id }) { item ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart && !item.isActive) {
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
                        modifier = Modifier.animateItem(),
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = !item.isActive,
                        backgroundContent = {
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
                        },
                    ) {
                        DownloadRow(
                            item = item,
                            onCancel = { DownloadManager.cancel(item.id) },
                            onRetry = { DownloadManager.retry(item.id) },
                            onOpen = { openFile(context, item) },
                            onRemove = { DownloadManager.remove(item.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
            Spacer(Modifier.width(14.dp))
            Text("Ma'lumot olinmoqda…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().glass(
            RoundedCornerShape(20.dp),
            tint = MaterialTheme.colorScheme.errorContainer,
            tintAlpha = 0.65f,
        ),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(MaterialTheme.colorScheme.onErrorContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry) { Text("Qayta") }
        }
    }
}

@Composable
private fun InfoCard(info: MediaInfo) {
    Card(
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(info.thumbnail, modifier = Modifier.size(width = 128.dp, height = 72.dp))
            Spacer(Modifier.width(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                info.uploader?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val meta = if (info.isPlaylist) {
                    "Playlist · ${info.entryCount} ta video"
                } else {
                    info.durationSec?.let { formatDuration(it) }
                }
                meta?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionsCard(ui: HomeUi, vm: HomeViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "Video sifati",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ui.availableQualities.forEach { q ->
                    val size = ui.info?.sizes?.get(q)
                    FilterChip(
                        selected = ui.quality == q,
                        onClick = { vm.setQuality(q) },
                        label = { Text(if (size != null) "${q.label} · ~${formatBytes(size)}" else q.label) },
                    )
                }
            }

            if (ui.hasVideoAndList) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Butun playlistni yuklash", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Havola playlist ichidagi videoga tegishli",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = ui.wholePlaylist, onCheckedChange = vm::setWholePlaylist)
                }
            }
        }
    }
}

private val URL_REGEX = Regex("""https?://\S+""")

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Uploat-V",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Rounded.Settings, contentDescription = "Sozlamalar")
        }
    }
}

@Composable
private fun ClipCard(url: String, onUse: () -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().glass(
            RoundedCornerShape(20.dp),
            tint = MaterialTheme.colorScheme.secondaryContainer,
            tintAlpha = 0.65f,
        ),
        shape = RoundedCornerShape(20.dp),
        colors = glassCardColors(MaterialTheme.colorScheme.onSecondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.ContentPaste,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Buferda havola bor",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = Uri.parse(url).host ?: url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onUse) { Text("Ishlatish") }
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Yopish",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onPaste: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Hozircha yuklashlar yo'q", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Havolani nusxalang va joylang yoki boshqa ilovadan «Ulashish» orqali yuboring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onPaste, modifier = Modifier.height(52.dp)) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Havolani joylash", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun openFile(context: android.content.Context, item: DownloadItem) {
    val uri = item.savedUri?.let(Uri::parse) ?: return
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, item.mime ?: "*/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Faylni ochadigan ilova topilmadi", Toast.LENGTH_SHORT).show()
    }
}
