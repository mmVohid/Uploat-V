package uz.vohid.uploatv.quick

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Tajribaviy avtomatik rejim. Foydalanuvchi uni Maxsus imkoniyatlar sozlamalarida o'zi yoqadi.
 * Xizmat ekran tarkibini o'qimaydi: faqat bufer o'zgarganda ichida havola bor-yo'qligini tekshiradi
 * va bor bo'lsa "Yuklaymi?" bildirishnomasini chiqaradi. Hech narsa saqlanmaydi va yuborilmaydi.
 */
class ClipboardWatcherService : AccessibilityService() {

    private var clipboard: ClipboardManager? = null
    private var lastStamp: String? = null
    private val handler = Handler(Looper.getMainLooper())

    private val listener = ClipboardManager.OnPrimaryClipChangedListener { check() }

    override fun onServiceConnected() {
        clipboard = getSystemService(ClipboardManager::class.java)
        clipboard?.addPrimaryClipChangedListener(listener)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // "Nusxalash" bosilgandan keyin bufer biroz keyin yangilanadi; tinglovchi ishlamagan telefonlar uchun zaxira
        if (event?.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({ check() }, 500)
        }
    }

    override fun onInterrupt() {}

    private fun check() {
        val url = ClipLink.read(this) ?: return
        val stamp = ClipLink.stamp(this, url)
        if (stamp == lastStamp) return
        lastStamp = stamp
        QuickNotifications.postAsk(this, url)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        clipboard?.removePrimaryClipChangedListener(listener)
        super.onDestroy()
    }
}
