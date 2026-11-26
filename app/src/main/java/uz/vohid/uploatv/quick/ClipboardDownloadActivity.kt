package uz.vohid.uploatv.quick

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import uz.vohid.uploatv.data.AppSettings
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.ui.theme.UploatTheme

/**
 * Plitka yoki doimiy tugma bosilganda ochiladi: buferdagi havolani o'qiydi va
 * "Havolani yuklaymi?" deb so'raydi. Bufer faqat oyna fokus olgandan keyin o'qiladi (Android 10+ qoidasi).
 */
class ClipboardDownloadActivity : ComponentActivity() {

    private var url by mutableStateOf<String?>(null)
    private var checked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by remember { SettingsRepository(applicationContext).settings }.collectAsState(AppSettings())
            UploatTheme(themeMode = settings.themeMode) {
                val u = url
                if (u != null) {
                    AlertDialog(
                        onDismissRequest = { finish() },
                        title = { Text("Havolani yuklaymi?") },
                        text = { Text("Salom, olgan havolangizni yuklaymi?\n\n${ClipLink.host(u)}") },
                        confirmButton = {
                            TextButton(onClick = {
                                DownloadManager.quickDownload(u)
                                Toast.makeText(this@ClipboardDownloadActivity, "Yuklash boshlandi", Toast.LENGTH_SHORT).show()
                                finish()
                            }) { Text("Ha, yuklashni boshla") }
                        },
                        dismissButton = {
                            TextButton(onClick = { finish() }) { Text("Yo'q, hozir emas") }
                        },
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || checked) return
        checked = true
        val found = ClipLink.read(this)
        if (found == null) {
            Toast.makeText(this, "Buferda havola yo'q. Avval videoning havolasini nusxalang", Toast.LENGTH_LONG).show()
            finish()
        } else {
            url = found
        }
    }
}
