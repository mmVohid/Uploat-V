package uz.vohid.uploatv.download

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.SettingsRepository

/** yt-dlp dvigatelini yangilaydi. Yangilash paytida yuklash va ma'lumot olish kutib turadi. */
object EngineUpdater {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Yangilash tugaguncha kutadi (yangilanmayotgan bo'lsa darhol qaytadi). */
    suspend fun waitIdle() = mutex.withLock { }

    suspend fun update(context: Context): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            runCatching {
                UploatApp.engineReady.await()
                val status = YoutubeDL.getInstance().updateYoutubeDL(context)
                SettingsRepository(context).setLastUpdate(System.currentTimeMillis())
                if (status?.toString()?.contains("ALREADY", ignoreCase = true) == true) {
                    "Allaqachon eng so'nggi versiya"
                } else {
                    "Yangilandi"
                }
            }.getOrElse { "Yangilab bo'lmadi. Internetni tekshiring" }
        }
    }

    fun updateInBackground(context: Context) {
        val app = context.applicationContext
        scope.launch { update(app) }
    }

    /** Ruxsat berilgan bo'lsa va oxirgi yangilanishdan 24 soat o'tgan bo'lsa yangilaydi. */
    suspend fun autoUpdateIfDue(context: Context) {
        val s = SettingsRepository(context).settings.first()
        if (!s.autoUpdateYtdlp) return
        if (System.currentTimeMillis() - s.lastUpdateMs < DAY_MS) return
        update(context)
    }
}
