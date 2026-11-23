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
        DownloadManager.init(this)

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
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Yuklash yakunlandi", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    companion object {
        const val CHANNEL_PROGRESS = "progress"
        const val CHANNEL_DONE = "done"

        /** Dvigatel tayyor bo'lganda yakunlanadi. */
        val engineReady = CompletableDeferred<Unit>()
    }
}
