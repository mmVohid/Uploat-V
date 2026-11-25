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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.vohid.uploatv.data.MediaInfo
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.runtime.produceState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: HomeViewModel,
    snackbar: SnackbarHostState,
    onOpenDownloads: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val banner by vm.banner.collectAsStateWithLifecycle()
    if (ui.duplicate) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = vm::dismissDuplicate,
            title = { Text("Bu video avval yuklangan") },
            text = { Text("Shu havola ilgari yuklab olingan. Yana yuklaysizmi?") },
            confirmButton = { TextButton(onClick = { vm.download(force = true) }) { Text("Yana yuklash") } },
            dismissButton = { TextButton(onClick = vm::dismissDuplicate) { Text("Bekor qilish") } },
        )
    }
    val items by DownloadManager.items.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    // Ma'lumot kelganda klaviatura yopiladi: "Yuklash" tugmasi va sifat tanlovi ko'rinib qoladi
    LaunchedEffect(ui.info != null) {
        if (ui.info != null) {
            keyboard?.hide()
            focus.clearFocus()
        }
    }
    val haptic = LocalHapticFeedback.current
    val engineState by produceState(0) {
        value = if (UploatApp.engineReady.isCompleted) 1 else 0
        value = if (runCatching { UploatApp.engineReady.await() }.isSuccess) 1 else 2
    }
    val activeItems = items.filter { it.isActive }
    val activeCount = activeItems.size
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

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
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (url != null) {
            vm.onUrlChange(url, immediate = true)
        } else {
            scope.launch { snackbar.showSnackbar("Buferda havola yo'q") }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { HomeHeader(engineState = engineState, activeCount = activeCount) },
    ) { pad ->
      val listHaze = remember { HazeState() }
      Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(listHaze)
                .imePadding(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = pad.calculateTopPadding() + 20.dp,
                bottom = 104.dp + navBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (banner.phase != BannerPhase.HIDDEN) {
                item(key = "engine") {
                    EngineBanner(
                        state = banner,
                        onUpdate = vm::updateEngineNow,
                        onDismiss = vm::dismissBanner,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
            item(key = "url") {
                OutlinedTextField(
                    value = ui.url,
                    onValueChange = { vm.onUrlChange(it) },
                    modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(20.dp)),
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                    ),
                    label = { Text("Video havolasi") },
                    placeholder = { Text("https://…") },
                    leadingIcon = { PlatformBadge(ui.url) },
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
                    ErrorCard(message = ui.error.orEmpty(), detail = ui.errorDetail, url = ui.url, onRetry = vm::retryFetch)
                }
            }

            item(key = "info") {
                AnimatedVisibility(visible = ui.info != null) {
                    ui.info?.let { info ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            InfoCard(info)
                            OptionsCard(ui = ui, vm = vm)
                            val dlSource = remember { MutableInteractionSource() }
                            Button(
                                interactionSource = dlSource,
                                onClick = {
                                    focus.clearFocus()
                                    keyboard?.hide()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    vm.download()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .pressScale(dlSource),
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

            if (activeItems.isNotEmpty()) {
                item(key = "active-header") {
                    Text(
                        text = "Faol yuklashlar",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
                items(activeItems, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onCancel = { DownloadManager.cancel(item.id) },
                        onRetry = { DownloadManager.retry(item.id) },
                        onOpen = { openFile(context, item) },
                        onRemove = { DownloadManager.remove(item.id) },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else if (ui.info == null && !ui.fetching && ui.error == null) {
                item(key = "empty") { EmptyState() }
            }
        }
        AnimatedVisibility(
            visible = !imeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            FloatingBar(
                onPaste = pasteFromClipboard,
                onDownloads = onOpenDownloads,
                onSettings = onOpenSettings,
                activeCount = activeCount,
                hazeState = listHaze,
            )
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
private fun ErrorCard(message: String, detail: String?, url: String, onRetry: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
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
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onRetry) { Text("Qayta") }
                if (!detail.isNullOrBlank()) {
                    TextButton(onClick = {
                        clipboard.setText(androidx.compose.ui.text.AnnotatedString("$url\n$detail"))
                        Toast.makeText(context, "Xato matni nusxalandi", Toast.LENGTH_SHORT).show()
                    }) { Text("Nusxalash") }
                }
            }
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
private fun HomeHeader(engineState: Int, activeCount: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Uploat-V",
            style = MaterialTheme.typography.headlineLarge.copy(
                brush = Brush.linearGradient(listOf(scheme.primary, Color(0xFF38BDF8))),
                fontWeight = FontWeight.ExtraBold,
            ),
            modifier = Modifier.weight(1f),
        )
        val (label, dot) = when {
            activeCount > 0 -> "$activeCount ta yuklash" to scheme.primary
            engineState == 1 -> "Tayyor" to scheme.tertiary
            engineState == 2 -> "Dvigatel xatosi" to scheme.error
            else -> "Yuklanmoqda…" to scheme.outline
        }
        Row(
            modifier = Modifier
                .glass(RoundedCornerShape(50), tintAlpha = 0.45f)
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FloatingBar(
    onPaste: () -> Unit,
    onDownloads: () -> Unit,
    onSettings: () -> Unit,
    activeCount: Int,
    hazeState: HazeState,
) {
    val pasteSource = remember { MutableInteractionSource() }
    val setSource = remember { MutableInteractionSource() }
    val dlSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .glass(RoundedCornerShape(32.dp), tintAlpha = 0.6f, hazeState = hazeState)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onPaste,
            interactionSource = pasteSource,
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .pressScale(pasteSource),
        ) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Havolani joylash", style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
        FilledTonalIconButton(
            onClick = onDownloads,
            interactionSource = dlSource,
            modifier = Modifier
                .size(52.dp)
                .pressScale(dlSource),
        ) {
            BadgedBox(badge = { if (activeCount > 0) Badge { Text("$activeCount") } }) {
                Icon(Icons.Rounded.Download, contentDescription = "Yuklashlar")
            }
        }
        FilledTonalIconButton(
            onClick = onSettings,
            interactionSource = setSource,
            modifier = Modifier
                .size(52.dp)
                .pressScale(setSource),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Sozlamalar")
        }
    }
}

/** Havola domeniga qarab platforma belgisi; noma'lum bo'lsa oddiy havola ikonkasi. */
@Composable
private fun PlatformBadge(url: String) {
    val scheme = MaterialTheme.colorScheme
    val host = if (url.isBlank()) null else runCatching { Uri.parse(url.trim()).host?.lowercase() }.getOrNull()
    val (color, icon) = when {
        host == null -> null to Icons.Rounded.Link
        "youtube" in host || "youtu.be" in host -> Color(0xFFFF0033) to Icons.Rounded.PlayArrow
        "instagram" in host -> Color(0xFFE1306C) to Icons.Rounded.CameraAlt
        "tiktok" in host -> Color(0xFF00BFB3) to Icons.Rounded.MusicNote
        "facebook" in host || "fb.watch" in host -> Color(0xFF1877F2) to Icons.Rounded.ThumbUp
        "twitter" in host || host == "x.com" || host.endsWith(".x.com") -> Color(0xFF1D9BF0) to Icons.Rounded.Tag
        else -> scheme.primary to Icons.Rounded.Public
    }
    if (color == null) {
        Icon(icon, contentDescription = null)
    } else {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(color),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(uz.vohid.uploatv.R.drawable.vohid_logo),
            contentDescription = null,
            modifier = Modifier.size(132.dp),
            // Piksel uslubi tiniq qolishi uchun yumshatilmaydi
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
        )
        Spacer(Modifier.height(4.dp))
        Text("Havola qo'shing", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Havolani nusxalang va pastdagi tugmani bosing yoki boshqa ilovadan «Ulashish» orqali yuboring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
    }
}

internal fun openFile(context: android.content.Context, item: DownloadItem) {
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
