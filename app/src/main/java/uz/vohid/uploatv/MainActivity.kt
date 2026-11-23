package uz.vohid.uploatv

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import uz.vohid.uploatv.data.AppSettings
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.ui.HomeScreen
import uz.vohid.uploatv.ui.HomeViewModel
import uz.vohid.uploatv.ui.SettingsViewModel
import uz.vohid.uploatv.ui.theme.UploatTheme

class MainActivity : ComponentActivity() {

    private var sharedUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        val settingsRepo = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
            UploatTheme(themeMode = settings.themeMode, dynamicColor = settings.dynamicColor) {
                AppRoot(sharedUrl = sharedUrl, onSharedConsumed = { sharedUrl = null })
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
        title = { Text("Boshlashdan oldin") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PermissionRow(
                    icon = Icons.Rounded.Notifications,
                    title = "Bildirishnomalar",
                    text = "Yuklash jarayoni va natijasini ko'rsatish uchun.",
                )
                PermissionRow(
                    icon = Icons.Rounded.SystemUpdate,
                    title = "yt-dlp yangilash",
                    text = "Sayt o'zgarganda video yuklanishi uchun dvigatel kuniga bir marta " +
                        "avtomatik yangilanadi (internet sarflaydi). Sozlamalardan o'chirish mumkin.",
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
    val settings by settingsVm.settings.collectAsStateWithLifecycle()

    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* natija talab qilinmaydi */ }

    // Birinchi ochilishda: bildirishnoma + yt-dlp yangilash uchun bitta ruxsat so'rovi
    if (settings.loaded && !settings.onboardingDone) {
        OnboardingDialog(
            onAccept = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                settingsVm.finishOnboarding(accepted = true)
            },
            onSkip = { settingsVm.finishOnboarding(accepted = false) },
        )
    }

    LaunchedEffect(sharedUrl) {
        sharedUrl?.let {
            homeVm.onUrlChange(it, immediate = true)
            onSharedConsumed()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { _ ->
        HomeScreen(vm = homeVm, settingsVm = settingsVm, snackbar = snackbar)
    }
}
