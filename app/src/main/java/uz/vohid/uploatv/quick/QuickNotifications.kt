package uz.vohid.uploatv.quick

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import androidx.core.app.NotificationCompat
import uz.vohid.uploatv.MainActivity
import uz.vohid.uploatv.R

/** "Buferdan yuklash" bildirishnomalari: savol (Ha / Yo'q) va doimiy tugma. */
object QuickNotifications {

    const val CHANNEL_ASK = "ask_link"
    const val CHANNEL_SHORTCUT = "clip_shortcut"

    const val ACTION_YES = "uz.vohid.uploatv.action.CLIP_YES"
    const val ACTION_NO = "uz.vohid.uploatv.action.CLIP_NO"
    const val EXTRA_URL = "url"

    const val ID_ASK = 7001
    const val ID_SHORTCUT = 7002

    fun createChannels(nm: NotificationManager) {
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ASK, "Havola so'rovi", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Nusxalangan havolani yuklashni taklif qiladi"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SHORTCUT, "Buferdan yuklash tugmasi", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Bildirishnoma panelidagi doimiy tugma"
            }
        )
    }

    /** Ekran tepasida ixcham bildirishnoma: "Salom, olgan havolangizni yuklaymi?" */
    fun postAsk(context: Context, url: String) {
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val yes = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ClipAskReceiver::class.java).setAction(ACTION_YES).putExtra(EXTRA_URL, url),
            flags,
        )
        val no = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, ClipAskReceiver::class.java).setAction(ACTION_NO),
            flags,
        )
        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            flags,
        )
        val text = "Salom, olgan havolangizni yuklaymi?"
        val n = NotificationCompat.Builder(context, CHANNEL_ASK)
            .setSmallIcon(R.drawable.ic_stat_penguin)
            .setLargeIcon(logo(context))
            .setContentTitle("Uploat-V")
            .setContentText(text)
            .setSubText(ClipLink.host(url))
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$url"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(open)
            .addAction(0, "Ha, yuklashni boshla", yes)
            .addAction(0, "Yo'q, hozir emas", no)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(ID_ASK, n)
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

    /** Maxsus imkoniyatlar ro'yxatida Uploat-V xizmati yoqilganmi. */
    fun isWatcherEnabled(context: Context): Boolean {
        val mine = ComponentName(context, ClipboardWatcherService::class.java)
        val list = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return list.split(':').any { ComponentName.unflattenFromString(it) == mine }
    }

    private fun logo(context: Context): Bitmap? = runCatching {
        val src = BitmapFactory.decodeResource(context.resources, R.drawable.vohid_icon)
        Bitmap.createScaledBitmap(src, 128, 128, true)
    }.getOrNull()
}
