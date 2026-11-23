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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import uz.vohid.uploatv.data.AudioFormat
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: HomeViewModel, bottomPadding: Dp, snackbar: SnackbarHostState) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .nestedScroll(scroll.nestedScrollConnection)
            .padding(bottom = bottomPadding)
            .consumeWindowInsets(PaddingValues(bottom = bottomPadding)),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("Uploat-V") },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 4.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "url") {
                OutlinedTextField(
                    value = ui.url,
                    onValueChange = { vm.onUrlChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    label = { Text("Video yoki audio havolasi") },
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
                                        ui.audio -> "Audioni yuklash"
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
                item(key = "empty") { EmptyState() }
            } else {
                items(items, key = { it.id }) { item ->
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

@Composable
private fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(info.thumbnail, audio = false, modifier = Modifier.size(width = 128.dp, height = 72.dp))
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !ui.audio,
                    onClick = { vm.setAudio(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("Video") }
                SegmentedButton(
                    selected = ui.audio,
                    onClick = { vm.setAudio(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Audio") }
            }

            Text(
                text = if (ui.audio) "Format" else "Sifat",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (ui.audio) {
                    AudioFormat.entries.forEach { f ->
                        FilterChip(
                            selected = ui.audioFormat == f,
                            onClick = { vm.setAudioFormat(f) },
                            label = { Text(f.label) },
                        )
                    }
                } else {
                    ui.availableQualities.forEach { q ->
                        FilterChip(
                            selected = ui.quality == q,
                            onClick = { vm.setQuality(q) },
                            label = { Text(q.label) },
                        )
                    }
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

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .then(Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.outline,
            )
        }
        Text("Hozircha yuklashlar yo'q", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Havolani yuqoridagi maydonga joylang yoki boshqa ilovadan «Ulashish» orqali yuboring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
