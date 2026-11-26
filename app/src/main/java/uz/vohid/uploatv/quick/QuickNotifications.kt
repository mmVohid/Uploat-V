package uz.vohid.uploatv.quick

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import uz.vohid.uploatv.R

/** "Buferdan yuklash" bildirishnomalari: savol (Ha / Yo'q) va doimiy tugma. */
object QuickNotifications {

    const val CHANNEL_SHORTCUT = "clip_shortcut"

    const val ID_SHORTCUT = 7002

    fun createChannels(nm: NotificationManager) {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SHORTCUT, "Buferdan yuklash tugmasi", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Bildirishnoma panelidagi doimiy tugma"
            }
        )
    }

    /** Bildirishnoma panelidagi doimiy "Buferdan yuklash" tugmasi. */
    fun postShortcut(context: Context) {
        val go = PendingIntent.getActivity(
            context, 4,
            Intent(context, ClipboardDownloadActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_SHORTCUT)
            .setSmallIcon(R.drawable.ic_stat_penguin)
            .setContentTitle("Uploat-V")
            .setContentText("Havolani nusxalang va shu yerni bosing")
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(go)
            .addAction(0, "Buferdan yuklash", go)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_SHORTCUT, n)
    }

    fun syncShortcut(context: Context, enabled: Boolean) {
        if (enabled) postShortcut(context)
        else context.getSystemService(NotificationManager::class.java).cancel(ID_SHORTCUT)
    }
}
