package uz.vohid.uploatv.quick

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import uz.vohid.uploatv.data.SettingsRepository

/** Qurilma qayta yoqilganda doimiy "Buferdan yuklash" tugmasini qaytaradi. */
class ClipBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val on = runBlocking { SettingsRepository(context).settings.first().clipShortcut }
        if (on) QuickNotifications.postShortcut(context)
    }
}
