package uz.vohid.uploatv.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Card
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.rounded.AccountCircle
import uz.vohid.uploatv.download.CookieStore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uz.vohid.uploatv.BuildConfig
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.ThemeMode
import uz.vohid.uploatv.download.DEFAULT_FOLDER
import uz.vohid.uploatv.download.describeTree
import uz.vohid.uploatv.download.formatBytes

/** Sozlamalar sahifalarining yo'llari (navigatsiya manzillari). */
object SettingsRoutes {
    const val MAIN = "settings"
    const val NOTIFICATIONS = "settings/notifications"
    const val APPEARANCE = "settings/appearance"
    const val ENGINE = "settings/engine"
    const val DOWNLOAD = "settings/download"
    const val ABOUT = "settings/about"
    const val ACCOUNTS = "settings/accounts"
    const val LOGIN = "settings/login/{site}"
    fun login(key: String) = "settings/login/$key"
}

// ======================= Asosiy ro'yxat =======================

@Composable
fun SettingsMainScreen(vm: SettingsViewModel, onNavigate: (String) -> Unit, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()

    SettingsPage(title = "Sozlamalar", onBack = onBack) {
        Card(
            modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp),
            colors = glassCardColors(),
        ) {
            Column {
                CategoryRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Bildirishnomalar",
                    subtitle = if (settings.notifyOnDone) "Ruxsat va xabarlar" else "Yuklash tugaganda xabar o'chiq",
                    onClick = { onNavigate(SettingsRoutes.NOTIFICATIONS) },
                )
                CategoryDivider()
                CategoryRow(
                    icon = Icons.Rounded.Palette,
                    title = "Ko'rinish",
                    subtitle = "Mavzu: ${settings.themeMode.label}",
                    onClick = { onNavigate(SettingsRoutes.APPEARANCE) },
                )
                CategoryDivider()
                CategoryRow(
                    icon = Icons.Rounded.Memory,
                    title = "Dvigatel",
                    subtitle = "yt-dlp ${engine.version ?: ""}".trim(),
                    onClick = { onNavigate(SettingsRoutes.ENGINE) },
                )
                CategoryDivider()
                CategoryRow(
                    icon = Icons.Rounded.AccountCircle,
                    title = "Akkauntlar",
                    subtitle = CookieStore.loggedCount().let { if (it == 0) "Instagram, Facebook, TikTok, X" else "$it ta akkaunt ulangan" },
                    onClick = { onNavigate(SettingsRoutes.ACCOUNTS) },
                )
                CategoryDivider()
                CategoryRow(
                    icon = Icons.Rounded.Download,
                    title = "Yuklash",
                    subtitle = "${settings.defaultQuality.label} · " +
                        (settings.saveTreeUri?.let { describeTree(Uri.parse(it)) } ?: DEFAULT_FOLDER),
                    onClick = { onNavigate(SettingsRoutes.DOWNLOAD) },
                )
                CategoryDivider()
                CategoryRow(
                    icon = Icons.Rounded.Info,
                    title = "Ilova haqida",
                    subtitle = "Versiya ${BuildConfig.VERSION_NAME}",
                    onClick = { onNavigate(SettingsRoutes.ABOUT) },
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun CategoryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 76.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

// ======================= Bildirishnomalar =======================

@Composable
fun NotificationsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }

    // Tizim sozlamalaridan qaytganda holat yangilansin
    LifecycleResumeEffect(Unit) {
        enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    SettingsPage(title = "Bildirishnomalar", onBack = onBack) {
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Bildirishnoma ruxsati", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (enabled) "Yoqilgan" else "O'chirilgan: yuklash jarayoni ko'rinmaydi",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                FilledTonalButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }) { Text("Sozlash") }
            }
            SwitchRow(
                title = "Yuklash tugaganda xabar berish",
                subtitle = "Barcha yuklashlar tugagach bildirishnoma chiqadi",
                checked = settings.notifyOnDone,
                onChange = vm::setNotifyOnDone,
            )
        }
    }
}

// ======================= Ko'rinish =======================

@Composable
fun AppearanceScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    SettingsPage(title = "Ko'rinish", onBack = onBack) {
        SettingsCard {
            LabeledRow("Mavzu") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { vm.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) { Text(mode.label) }
                    }
                }
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                SwitchRow(
                    title = "Dinamik ranglar",
                    subtitle = "Rangni telefon fon rasmiga moslash",
                    checked = settings.dynamicColor,
                    onChange = vm::setDynamicColor,
                )
            }
        }
    }
}

// ======================= Dvigatel =======================

@Composable
fun EngineScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val engine by vm.engine.collectAsStateWithLifecycle()

    SettingsPage(title = "Dvigatel", onBack = onBack) {
        SettingsCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("yt-dlp", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Versiya: ${engine.version ?: "…"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    engine.message?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (engine.messageOk) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                }
                if (engine.updating) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    FilledTonalButton(onClick = vm::updateEngine) { Text("Yangilash") }
                }
            }
            SwitchRow(
                title = "Avtomatik yangilash",
                subtitle = "Kuniga bir marta, internet sarflaydi",
                checked = settings.autoUpdateYtdlp,
                onChange = vm::setAutoUpdate,
            )
        }
        Text(
            text = "Sayt o'zgarib video yuklanmay qolsa, avval dvigatelni yangilang",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

// ======================= Yuklash =======================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadSettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            // Ilova qayta ishga tushsa ham papkaga yozish huquqi saqlansin
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            vm.setSaveTree(uri.toString())
        }
    }

    val cache by vm.cache.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshCache() }

    SettingsPage(title = "Yuklash", onBack = onBack) {
        SettingsCard {
            LabeledRow("Standart video sifati") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Quality.entries.forEach { q ->
                        FilterChip(
                            selected = settings.defaultQuality == q,
                            onClick = { vm.setDefaultQuality(q) },
                            label = { Text(q.label) },
                        )
                    }
                }
            }
            val treeUri = settings.saveTreeUri
            ClickableRow(
                title = "Saqlash joyi",
                value = treeUri?.let { describeTree(Uri.parse(it)) } ?: DEFAULT_FOLDER,
                hint = if (treeUri == null) "Standart · o'zgartirish uchun bosing" else "O'zgartirish uchun bosing",
                onClick = { pickFolder.launch(null) },
            )
            if (treeUri != null) {
                TextButton(
                    onClick = { vm.setSaveTree(null) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) { Text("Standart papkaga qaytarish") }
            }
            SwitchRow(
                title = "Inkognito",
                subtitle = "Yuklab olish tarixini o'chirib qo'ying: yuklashlar tarixga saqlanmaydi",
                checked = settings.incognito,
                onChange = vm::setIncognito,
            )
        }

        SectionLabel("Fon rejimi")
        SettingsCard {
            val pm = remember { context.getSystemService(android.os.PowerManager::class.java) }
            var unrestricted by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName)) }
            LifecycleResumeEffect(Unit) {
                unrestricted = pm.isIgnoringBatteryOptimizations(context.packageName)
                onPauseOrDispose { }
            }
            ClickableRow(
                title = "Batareya cheklovisiz ishlash",
                value = if (unrestricted) "Ruxsat berilgan" else "Cheklangan",
                hint = "Ekran o'chiq paytda yuklash uzilib qolmasligi uchun ruxsat bering",
                onClick = {
                    if (!unrestricted) {
                        val request = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}"),
                        )
                        runCatching { context.startActivity(request) }.onFailure {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }
                    }
                },
            )
        }

        SectionLabel("Ma'lumotlar va xotira")
        SettingsCard {
            ClickableRow(
                title = "Keshni tozalash",
                value = when {
                    cache.busy -> "Tozalanmoqda…"
                    cache.message != null -> cache.message.orEmpty()
                    cache.bytes != null -> "Hajmi: ${formatBytes(cache.bytes ?: 0L)}"
                    else -> "Hajmi hisoblanmoqda…"
                },
                hint = "Vaqtinchalik fayllar o'chadi, yuklab olingan videolar saqlanib qoladi",
                onClick = vm::clearCache,
            )
        }
    }
}

// ======================= Ilova haqida =======================

@Composable
fun AboutScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val update by vm.appUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsPage(title = "Ilova haqida", onBack = onBack) {
        SettingsCard {
            InfoRow("Uploat-V", "Versiya ${BuildConfig.VERSION_NAME}")
            InfoRow("Dvigatel", "yt-dlp asosida video yuklovchi")
        }

        SettingsCard {
            SwitchRow(
                title = "Avtomatik yangilash – Uploat-V",
                subtitle = "GitHub'dan so'nggi versiyani avtomatik ravishda tekshiring.",
                checked = settings.autoUpdateApp,
                onChange = vm::setAutoUpdateApp,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val result = update.result
                Text(
                    text = result?.message ?: "Hali tekshirilmagan",
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        result == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        result.ok -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                if (update.checking) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                } else {
                    val found = result?.update
                    if (found != null) {
                        FilledTonalButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(found.url))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) { Text("Yuklab olish") }
                    } else {
                        TextButton(onClick = vm::checkAppNow) { Text("Hozir tekshirish") }
                    }
                }
            }
        }
    }
}

// ======================= Umumiy qismlar =======================

/**
 * Har bir sozlamalar sahifasining asosi: katta sarlavha, ← tugma va chap chetdan o'ngga surib
 * orqaga qaytish. (Tizimning orqaga qaytish ishorasi navigatsiya orqali alohida ishlaydi.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = pad.calculateTopPadding())
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
            // Tugmali navigatsiyada ham ishlashi uchun: chap chetdan o'ngga surish = orqaga
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(20.dp)
                    .pointerInput(Unit) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragCancel = { total = 0f },
                            onDragEnd = { if (total > 120f) onBack() },
                            onHorizontalDrag = { _, delta -> total += delta },
                        )
                    },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().glass(RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = glassCardColors(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

@Composable
private fun LabeledRow(label: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ClickableRow(title: String, value: String, hint: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


// ======================= Akkauntlar =======================

@Composable
fun AccountsScreen(onOpenLogin: (String) -> Unit, onBack: () -> Unit) {
    var refresh by remember { mutableStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose { }
    }
    SettingsPage(title = "Akkauntlar", onBack = onBack) {
        Text(
            "Ba'zi saytlar (masalan Instagram) akkauntsiz video bermaydi. Shu yerda kirsangiz, " +
                "ilova sizning nomingizdan yuklaydi. Parol saqlanmaydi, faqat kirish belgisi (cookie).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        SettingsCard {
            CookieStore.accounts.forEachIndexed { i, account ->
                if (i > 0) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                val logged = remember(refresh) { CookieStore.isLoggedIn(account) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(account.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (logged) "Kirilgan" else "Kirilmagan",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (logged) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (logged) {
                        TextButton(onClick = {
                            CookieStore.logout(account)
                            refresh++
                        }) { Text("Chiqish", color = MaterialTheme.colorScheme.error) }
                    }
                    FilledTonalButton(onClick = { onOpenLogin(account.key) }) {
                        Text(if (logged) "Qayta kirish" else "Kirish")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginWebScreen(siteKey: String, onDone: () -> Unit) {
    val account = CookieStore.byKey(siteKey)
    val context = LocalContext.current
    var failed by remember { mutableStateOf(false) }
    if (account == null) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(account.name) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (CookieStore.saveFromWebView(account)) onDone() else failed = true
                    }) { Text("Tayyor") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            if (failed) {
                Text(
                    "Hali kirilmagan ko'rinadi. Avval saytga kiring, so'ng \"Tayyor\" ni bosing.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 4.dp),
                )
            }
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        val cm = android.webkit.CookieManager.getInstance()
                        cm.setAcceptCookie(true)
                        cm.setAcceptThirdPartyCookies(this, true)
                        webViewClient = android.webkit.WebViewClient()
                        loadUrl(account.loginUrl)
                    }
                },
                onRelease = { it.destroy() },
            )
        }
    }
}
