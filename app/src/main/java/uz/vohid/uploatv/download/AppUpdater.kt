package uz.vohid.uploatv.download

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.BuildConfig
import uz.vohid.uploatv.data.SettingsRepository
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdate(val tag: String, val code: Int, val url: String)

data class AppCheck(val message: String, val ok: Boolean, val update: AppUpdate?)

/**
 * Uploat-V'ning o'zini yangilash: GitHub'dagi so'nggi release'ni tekshiradi.
 * GitHub API ishlatilmaydi (limitsiz): `/releases/latest` manzilining yo'naltirishi
 * ("…/tag/v1.0.45") o'qiladi, oxirgi raqam — build raqami (versionCode).
 * Faqat repo ochiq (public) bo'lganda ishlaydi.
 */
object AppUpdater {

    private const val REPO = "https://github.com/mmVohid/Uploat-V"
    private const val DAY_MS = 24L * 60 * 60 * 1000

    private val _available = MutableStateFlow<AppUpdate?>(null)
    val available: StateFlow<AppUpdate?> = _available.asStateFlow()

    suspend fun check(context: Context): AppCheck = withContext(Dispatchers.IO) {
        val conn = URL("$REPO/releases/latest").openConnection() as HttpURLConnection
        try {
            conn.instanceFollowRedirects = false
            conn.requestMethod = "HEAD"
            conn.connectTimeout = 15_000
            conn.readTimeout = 15_000

            val location = conn.getHeaderField("Location")
            val tag = location?.takeIf { "/tag/" in it }?.substringAfterLast('/')
            val code = tag?.substringAfterLast('.')?.toIntOrNull()
            if (tag == null || code == null) {
                return@withContext AppCheck(
                    "Tekshirib bo'lmadi: repo yopiq (private) yoki hali release yo'q (HTTP ${conn.responseCode})",
                    ok = false,
                    update = null,
                )
            }

            SettingsRepository(context).setLastAppCheck(System.currentTimeMillis())
            if (code > BuildConfig.VERSION_CODE) {
                val update = AppUpdate(tag, code, "$REPO/releases/download/$tag/Uploat-V.apk")
                _available.value = update
                AppCheck("Yangi versiya bor: $tag", ok = true, update = update)
            } else {
                _available.value = null
                AppCheck("Eng so'nggi versiya o'rnatilgan", ok = true, update = null)
            }
        } catch (e: Throwable) {
            AppCheck("Tekshirib bo'lmadi: ${e.message ?: e::class.simpleName}".take(160), ok = false, update = null)
        } finally {
            conn.disconnect()
        }
    }

    /** Yoqilgan bo'lsa va oxirgi tekshiruvdan 24 soat o'tgan bo'lsa tekshiradi. */
    suspend fun checkIfDue(context: Context) {
        val s = SettingsRepository(context).settings.first()
        if (!s.autoUpdateApp) return
        if (System.currentTimeMillis() - s.lastAppCheckMs < DAY_MS) return
        check(context)
    }
}
