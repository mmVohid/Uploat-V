package uz.vohid.uploatv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import uz.vohid.uploatv.ui.GlassScaffold
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.LocalContext
import uz.vohid.uploatv.download.AppUpdater
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.vohid.uploatv.data.AppSettings
import uz.vohid.uploatv.data.SettingsRepository
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.NavType
import androidx.navigation.navArgument
import uz.vohid.uploatv.ui.CookiesScreen
import uz.vohid.uploatv.ui.DownloadsScreen
import uz.vohid.uploatv.ui.GuideScreen
import uz.vohid.uploatv.ui.PrivacyScreen
import uz.vohid.uploatv.ui.LoginWebScreen
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import uz.vohid.uploatv.ui.AboutScreen
import uz.vohid.uploatv.ui.AppearanceScreen
import uz.vohid.uploatv.ui.DownloadSettingsScreen
import uz.vohid.uploatv.ui.EngineScreen
import uz.vohid.uploatv.ui.HomeScreen
import uz.vohid.uploatv.ui.HomeViewModel
import uz.vohid.uploatv.ui.SettingsMainScreen
import uz.vohid.uploatv.ui.SettingsRoutes
import uz.vohid.uploatv.ui.SettingsViewModel
import uz.vohid.uploatv.ui.theme.UploatTheme

private const val HOME = "home"

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        val settingsRepo = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            UploatTheme(themeMode = settings.themeMode) {
                GlassScaffold {
                    AppRoot(sharedUrl = sharedUrl, onSharedConsumed = { sharedUrl = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        sharedUrl = URL_REGEX.find(text)?.value
    }

    private companion object {
        val URL_REGEX = Regex("""https?://\S+""")
    }
}

@Composable
private fun OnboardingDialog(onAccept: () -> Unit, onSkip: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        title = { Text("Boshlashdan oldin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "yt-dlp yangilash",
                    text = "Sayt o'zgarganda video yuklanishi uchun dvigatel kuniga bir marta " +
                        "avtomatik yangilanadi (internet sarflaydi). Sozlamalardan o'chirish mumkin.",
                )
                PermissionRow(
                    icon = Icons.Rounded.Cookie,
                    title = "Cookie'larni avtomatik boshqarish",
                    text = "Instagram kabi saytlar uchun kirish belgilari avtomatik olinadi va yangilab turiladi. " +
                        "Parol saqlanmaydi, ma'lumot telefoningizda qoladi.",
                )
            }
        },
        confirmButton = { Button(onClick = onAccept) { Text("Ruxsat berish") } },
        dismissButton = { TextButton(onClick = onSkip) { Text("Hozir emas") } },
    )
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppRoot(sharedUrl: String?, onSharedConsumed: () -> Unit) {
    val homeVm: HomeViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val snackbar = remember { SnackbarHostState() }
    val nav = rememberNavController()
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    // Birinchi ochilish: 0 = bildirishnoma (tizim oynasi), 1 = batareya (tizim oynasi), 2 = bizning oyna
    var step by rememberSaveable { mutableIntStateOf(0) }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { step = 1 }
    val batteryRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { step = 2 }
    val appContext = LocalContext.current
    val onboarding = settings.loaded && !settings.onboardingDone

    LaunchedEffect(onboarding, step) {
        if (!onboarding) return@LaunchedEffect
        when (step) {
            0 -> {
                val needed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needed) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else step = 1
            }
            1 -> {
                val pm = appContext.getSystemService(PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(appContext.packageName)) {
                    step = 2
                } else {
                    val launched = runCatching {
                        batteryRequest.launch(
                            Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${appContext.packageName}"),
                            ),
                        )
                    }.isSuccess
                    if (!launched) step = 2
                }
            }
        }
    }
    if (onboarding && step >= 2) {
        OnboardingDialog(
            onAccept = { settingsVm.finishOnboarding(accepted = true) },
            onSkip = { settingsVm.finishOnboarding(accepted = false) },
        )
    }

    // Uploat-V yangilanishini avtomatik tekshirish (yoqilgan bo'lsa, kuniga bir marta)
    val context = LocalContext.current
    LaunchedEffect(settings.loaded, settings.autoUpdateApp) {
        if (settings.loaded && settings.autoUpdateApp) AppUpdater.checkIfDue(context.applicationContext)
    }
    val available by AppUpdater.available.collectAsStateWithLifecycle()
    LaunchedEffect(available?.tag) {
        val update = available ?: return@LaunchedEffect
        val action = snackbar.showSnackbar(
            message = "Uploat-V yangi versiyasi bor: ${update.tag}",
            actionLabel = "Yuklab olish",
            duration = SnackbarDuration.Long,
        )
        if (action == SnackbarResult.ActionPerformed) {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(update.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            // Sozlamalar ochiq bo'lsa ham, ulashilgan havola Bosh sahifada ko'rinsin
            nav.popBackStack(HOME, inclusive = false)
            homeVm.onUrlChange(it, immediate = true)
            onSharedConsumed()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary,
                )
            }
        },
    ) { _ ->
        // Sahifalar o'ng tomondan suriladi; chapdan o'ngga surish (orqaga ishorasi) bitta qadam qaytaradi
        val slide = tween<IntOffset>(320)
        val fade = tween<Float>(320)
        NavHost(
            navController = nav,
            startDestination = HOME,
            enterTransition = { slideInHorizontally(slide) { it } + fadeIn(fade) },
            exitTransition = { slideOutHorizontally(slide) { -it / 4 } + fadeOut(fade) },
            popEnterTransition = { slideInHorizontally(slide) { -it / 4 } + fadeIn(fade) },
            popExitTransition = { slideOutHorizontally(slide) { it } + fadeOut(fade) },
        ) {
            composable(HOME) {
                HomeScreen(
                    vm = homeVm,
                    snackbar = snackbar,
                    onOpenDownloads = { nav.navigate("downloads") },
                    onOpenSettings = { nav.navigate(SettingsRoutes.MAIN) },
                )
            }
            composable("downloads") { DownloadsScreen(snackbar) { nav.popBackStack() } }
            composable(SettingsRoutes.MAIN) {
                SettingsMainScreen(
                    vm = settingsVm,
                    onNavigate = { nav.navigate(it) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(SettingsRoutes.APPEARANCE) { AppearanceScreen(settingsVm) { nav.popBackStack() } }
            composable(SettingsRoutes.ENGINE) { EngineScreen(settingsVm) { nav.popBackStack() } }
            composable(SettingsRoutes.DOWNLOAD) { DownloadSettingsScreen(settingsVm) { nav.popBackStack() } }
            composable(SettingsRoutes.ABOUT) {
                AboutScreen(settingsVm, onOpenGuide = { nav.navigate(SettingsRoutes.GUIDE) }) { nav.popBackStack() }
            }
            composable(SettingsRoutes.PRIVACY) {
                PrivacyScreen(
                    settingsVm,
                    onOpenCookies = { nav.navigate(SettingsRoutes.COOKIES) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(SettingsRoutes.GUIDE) { GuideScreen { nav.popBackStack() } }
            composable(SettingsRoutes.COOKIES) {
                CookiesScreen(
                    vm = settingsVm,
                    onOpenLogin = { nav.navigate(SettingsRoutes.login(it)) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(
                SettingsRoutes.LOGIN,
                arguments = listOf(navArgument("url") { type = NavType.StringType }),
            ) { entry ->
                LoginWebScreen(startUrl = entry.arguments?.getString("url").orEmpty()) { nav.popBackStack() }
            }
        }
    }
}
