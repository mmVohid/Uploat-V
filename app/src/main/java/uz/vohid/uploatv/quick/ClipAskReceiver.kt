package uz.vohid.uploatv.quick

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.vohid.uploatv.download.DownloadManager

/** Bildirishnomadagi "Ha, yuklashni boshla" / "Yo'q, hozir emas" tugmalari. */
class ClipAskReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSystemService(NotificationManager::class.java).cancel(QuickNotifications.ID_ASK)
        if (intent.action == QuickNotifications.ACTION_YES) {
            intent.getStringExtra(QuickNotifications.EXTRA_URL)?.let { DownloadManager.quickDownload(it) }
        }
    }
}
