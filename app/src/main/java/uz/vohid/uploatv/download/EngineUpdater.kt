package uz.vohid.uploatv.download

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.SettingsRepository
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException

data class UpdateResult(val message: String, val ok: Boolean, val updated: Boolean)

/**
 * yt-dlp dvigatelini yangilaydi.
 *
 * Kutubxonaning o'z yangilashi GitHub API'ga (soatiga 60 so'rov limiti) va juda qisqa timeout'larga
 * tayanadi, shu sabab ko'p foydalanuvchi bor tarmoqlarda tez-tez yiqiladi. Bu yerda esa fayl
 * to'g'ridan-to'g'ri release havolasidan, uzoqroq timeout va qayta urinishlar bilan yuklanadi.
 * To'g'ridan-to'g'ri yo'l yiqilsa, kutubxonaning o'z yangilashi zaxira sifatida ishlatiladi.
 */
object EngineUpdater {

    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val RELEASE_PAGE = "https://github.com/yt-dlp/yt-dlp/releases/latest"
    private const val ASSET_URL = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp"
    private const val NIGHTLY_PAGE = "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest"
    private const val NIGHTLY_ASSET_URL = "https://github.com/yt-dlp/yt-dlp-nightly-builds/releases/latest/download/yt-dlp"
    private const val MIN_SIZE = 1_000_000L

    private lateinit var app: Context
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Har muvaffaqiyatli yangilanishda oshadi; parallel yuklashlar qayta yangilamasligi uchun. */
    @Volatile
    private var epoch = 0

    fun init(context: Context) {
        app = context.applicationContext
    }

    /** Yangilash tugaguncha kutadi (yangilanmayotgan bo'lsa darhol qaytadi). */
    suspend fun waitIdle() = mutex.withLock { }

    suspend fun update(context: Context): UpdateResult = mutex.withLock {
        withContext(Dispatchers.IO) {
            val repo = SettingsRepository(context)
            try {
                UploatApp.engineReady.await()
                updateDirect(context, repo)
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                UpdateResult("Yangilab bo'lmadi: ${reason(e)}", ok = false, updated = false)
            }
        }
    }

    fun updateInBackground(context: Context) {
        val appContext = context.applicationContext
        scope.launch { update(appContext) }
    }

    /** Ruxsat berilgan bo'lsa va oxirgi yangilanishdan 24 soat o'tgan bo'lsa yangilaydi. */
    suspend fun autoUpdateIfDue(context: Context) {
        val s = SettingsRepository(context).settings.first()
        if (!s.autoUpdateYtdlp) return
        if (System.currentTimeMillis() - s.lastUpdateMs < DAY_MS) return
        update(context)
    }

    /**
     * [block] yiqilsa (masalan, yt-dlp eskirib sayt o'zgargan bo'lsa), dvigatelni yangilab bir marta
     * qayta urinadi. Foydalanuvchi bekor qilgan bo'lsa ([cancelled]) yoki tarmoq xatosi bo'lsa urinmaydi.
     */
    suspend fun <T> healing(cancelled: () -> Boolean = { false }, block: suspend () -> T): T {
        val startEpoch = epoch
        return try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (cancelled() || isNetworkError(e) || !updateAllowed()) throw e
            if (epoch != startEpoch) {
                // Boshqa yuklash dvigatelni allaqachon yangilab bo'ldi
                block()
            } else {
                val result = update(app)
                if (!result.updated) throw e
                block()
            }
        }
    }

    private suspend fun updateAllowed(): Boolean {
        val s = SettingsRepository(app).settings.first()
        return !s.onboardingDone || s.autoUpdateYtdlp
    }

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = e.message.orEmpty()
        return "Unable to resolve host" in msg || "Network is unreachable" in msg ||
            "timed out" in msg || "No address associated" in msg || "Connection reset" in msg
    }

    // ---------- To'g'ridan-to'g'ri yangilash ----------

    private suspend fun updateDirect(context: Context, repo: SettingsRepository): UpdateResult {
        val nightly = repo.settings.first().ytdlpNightly
        val latest = latestTag(nightly)
        val current = repo.settings.first().ytdlpVersion
        val target = File(context.noBackupFilesDir, "youtubedl-android/yt-dlp/yt-dlp")

        if (current == latest && target.exists()) {
            repo.setLastUpdate(System.currentTimeMillis())
            return UpdateResult("Allaqachon eng so'nggi versiya ($latest)", ok = true, updated = false)
        }

        val dir = target.parentFile ?: throw IOException("Dvigatel papkasi topilmadi")
        dir.mkdirs()
        val tmp = File(dir, "yt-dlp.new")

        var last: Throwable? = null
        for (attempt in 1..3) {
            try {
                download(if (nightly) NIGHTLY_ASSET_URL else ASSET_URL, tmp)
                if (!tmp.renameTo(target)) {
                    tmp.copyTo(target, overwrite = true)
                    tmp.delete()
                }
                repo.setYtdlpVersion(latest)
                repo.setLastUpdate(System.currentTimeMillis())
                epoch++
                return UpdateResult("Yangilandi: $latest", ok = true, updated = true)
            } catch (e: CancellationException) {
                tmp.delete()
                throw e
            } catch (e: Throwable) {
                tmp.delete()
                last = e
                if (attempt < 3) delay(1500L * attempt)
            }
        }
        throw last ?: IOException("Noma'lum xato")
    }

    private fun latestTag(nightly: Boolean): String {
        val c = URL(if (nightly) NIGHTLY_PAGE else RELEASE_PAGE).openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects = false
            c.requestMethod = "HEAD"
            c.connectTimeout = 15_000
            c.readTimeout = 15_000
            val location = c.getHeaderField("Location")
                ?: throw IOException("Versiya aniqlanmadi (HTTP ${c.responseCode})")
            return location.substringAfterLast('/')
        } finally {
            c.disconnect()
        }
    }

    private fun download(url: String, out: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.instanceFollowRedirects = true
            c.connectTimeout = 20_000
            c.readTimeout = 30_000
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            val expected = c.contentLengthLong
            c.inputStream.use { input -> out.outputStream().use { input.copyTo(it) } }
            val size = out.length()
            if (size < MIN_SIZE || (expected > 0 && size != expected)) {
                throw IOException("Fayl to'liq yuklanmadi ($size bayt)")
            }
        } finally {
            c.disconnect()
        }
    }

    private fun reason(e: Throwable): String = when (e) {
        is UnknownHostException -> "GitHub'ga ulanib bo'lmadi (internet yoki DNS)"
        else -> "${e::class.simpleName}: ${e.message.orEmpty()}".trim().take(140)
    }
}
