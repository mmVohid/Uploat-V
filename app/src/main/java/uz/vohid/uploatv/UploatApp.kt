package uz.vohid.uploatv

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.EngineUpdater

class UploatApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createChannels()
        uz.vohid.uploatv.download.CookieStore.init(this)
        uz.vohid.uploatv.download.Preflight.init(this)
        uz.vohid.uploatv.download.ErrorLog.init(this)
        DownloadManager.init(this)
        EngineUpdater.init(this)
        uz.vohid.uploatv.ui.EngineWatch.init(this)

        // Dvigatel (Python + yt-dlp + ffmpeg) birinchi ishga tushishda ochiladi, shu sabab fonda
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@UploatApp)
                FFmpeg.getInstance().init(this@UploatApp)
                engineReady.complete(Unit)
            } catch (e: Throwable) {
                engineReady.completeExceptionally(e)
                return@launch
            }
            // Foydalanuvchi ruxsat bergan bo'lsa, yt-dlp kuniga bir marta yangilanadi
            runCatching { EngineUpdater.autoUpdateIfDue(this@UploatApp) }
        }
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PROGRESS, "Yuklash jarayoni", NotificationManager.IMPORTANCE_LOW)
        )
        // Eski "done" kanali (oddiy muhimlik) o'chiriladi: yangisi ekranda banner bo'lib chiqadi (SMS kabi)
        nm.deleteNotificationChannel("done")
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Yuklash natijasi", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    companion object {
        const val CHANNEL_PROGRESS = "progress"
        const val CHANNEL_DONE = "done_v2"

        /** Dvigatel tayyor bo'lganda yakunlanadi. */
        val engineReady = CompletableDeferred<Unit>()
    }
}
