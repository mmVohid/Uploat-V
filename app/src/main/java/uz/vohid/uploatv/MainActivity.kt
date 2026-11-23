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
import androidx.compose.foundation.layout.WindowInsets
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

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* natija talab qilinmaydi */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        requestNotificationPermission()

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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        val URL_REGEX = Regex("""https?://\S+""")
    }
}

@Composable
private fun AppRoot(sharedUrl: String?, onSharedConsumed: () -> Unit) {
    val homeVm: HomeViewModel = viewModel()
    val settingsVm: SettingsViewModel = viewModel()
    val snackbar = remember { SnackbarHostState() }

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
