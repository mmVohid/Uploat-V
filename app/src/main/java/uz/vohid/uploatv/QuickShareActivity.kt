package uz.vohid.uploatv

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import uz.vohid.uploatv.download.DownloadManager

/**
 * Ulashish menyusidagi "Быстрая загрузка" varianti. Ekran ko'rsatmaydi:
 * havolani oladi, orqa fonda yuklashni boshlaydi va darhol yopiladi.
 */
class QuickShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = if (intent?.action == Intent.ACTION_SEND) intent.getStringExtra(Intent.EXTRA_TEXT) else null
        val url = text?.let { URL_REGEX.find(it)?.value }

        if (url == null) {
            Toast.makeText(this, "Havola topilmadi", Toast.LENGTH_SHORT).show()
        } else {
            DownloadManager.quickDownload(url)
            Toast.makeText(this, "Yuklash boshlandi", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    private companion object {
        val URL_REGEX = Regex("""https?://\S+""")
    }
}
