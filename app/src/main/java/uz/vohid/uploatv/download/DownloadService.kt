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
    private val notified = mutableSetOf<String>()

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
        // qayta urinilgan yuklash tugaganda yana xabar berilishi uchun
        notified -= active.map { it.id }.toSet()

        // Har bir video tugashi bilan alohida xabar: qaysi biri yuklandi, qaysi biri yo'q
        val finished = items.filter {
            it.id in seen && it.id !in notified && (it.status == Status.DONE || it.status == Status.FAILED) &&
                !(it.status == Status.FAILED && DownloadManager.willAutoRetry(it.id))
        }
        if (finished.isNotEmpty()) {
            val notify = SettingsRepository(this).settings.first().notifyOnDone
            finished.forEach { item ->
                notified += item.id
                if (notify) nm.notify(resultId(item.id), buildResult(item))
            }
        }

        val waiting = items.count { it.status == Status.FAILED && DownloadManager.willAutoRetry(it.id) }
        if (active.isEmpty() && waiting == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } else if (active.isEmpty()) {
            nm.notify(NOTIF_ID, build("Internet kutilmoqda", "$waiting ta yuklash aloqa qaytganda davom etadi", 0))
        } else {
            val current = active.firstOrNull { it.status == Status.RUNNING } ?: active.first()
            val pct = (current.progress * 100).toInt()
            val title = if (active.size > 1) "Yuklanmoqda (${active.size} ta)" else "Yuklanmoqda"
            nm.notify(NOTIF_ID, build(title, current.title, pct))
        }
    }

    private fun resultId(id: String) = 1000 + (id.hashCode() and 0x7fffffff) % 100000

    private fun buildResult(item: DownloadItem): android.app.Notification {
        val ok = item.status == Status.DONE
        // Inkognito yuklashda video nomi bildirishnomada ko'rsatilmaydi
        val name = if (item.incognito) "Video" else item.title
        val text = if (ok) name else "$name\n${item.error ?: "Yuklab bo'lmadi"}"
        return NotificationCompat.Builder(this, UploatApp.CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_penguin)
            .setContentTitle(if (ok) "Yuklandi" else "Yuklab bo'lmadi")
            .setContentText(name)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .build()
    }

    private fun build(title: String, text: String?, percent: Int, done: Boolean = false) =
        NotificationCompat.Builder(this, if (done) UploatApp.CHANNEL_DONE else UploatApp.CHANNEL_PROGRESS)
            .setSmallIcon(R.drawable.ic_stat_penguin)
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
    }
}
