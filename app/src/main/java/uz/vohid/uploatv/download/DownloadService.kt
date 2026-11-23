package uz.vohid.uploatv.download

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import uz.vohid.uploatv.data.SettingsRepository
import kotlinx.coroutines.launch
import uz.vohid.uploatv.MainActivity
import uz.vohid.uploatv.R
import uz.vohid.uploatv.UploatApp

/** Ilova yopilganda ham yuklash davom etishi uchun foreground xizmat. */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private val seen = mutableSetOf<String>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIF_ID,
            build("Yuklash boshlanmoqda…", null, 0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (job == null) {
            job = scope.launch { DownloadManager.items.collect { onItems(it) } }
        }
        return START_NOT_STICKY
    }

    private suspend fun onItems(items: List<DownloadItem>) {
        val nm = getSystemService(NotificationManager::class.java)
        val active = items.filter { it.isActive }
        seen += active.map { it.id }

        if (active.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            val done = items.count { it.id in seen && it.status == Status.DONE }
            val failed = items.count { it.id in seen && it.status == Status.FAILED }
            val notify = SettingsRepository(this).settings.first().notifyOnDone
            if (notify && done + failed > 0) {
                val text = buildString {
                    if (done > 0) append("$done ta yuklash tayyor")
                    if (failed > 0) append(if (done > 0) ", $failed ta xatolik" else "$failed ta xatolik")
                }
                nm.notify(DONE_ID, build("Uploat-V", text, -1, done = true))
            }
            stopSelf()
        } else {
            val current = active.firstOrNull { it.status == Status.RUNNING } ?: active.first()
            val pct = (current.progress * 100).toInt()
            val title = if (active.size > 1) "Yuklanmoqda (${active.size} ta)" else "Yuklanmoqda"
            nm.notify(NOTIF_ID, build(title, current.title, pct))
        }
    }

    private fun build(title: String, text: String?, percent: Int, done: Boolean = false) =
        NotificationCompat.Builder(this, if (done) UploatApp.CHANNEL_DONE else UploatApp.CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(!done)
            .setAutoCancel(done)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .apply { if (!done) setProgress(100, percent.coerceAtLeast(0), percent <= 0) }
            .build()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val NOTIF_ID = 1
        const val DONE_ID = 2
    }
}
