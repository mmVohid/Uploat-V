package uz.vohid.uploatv.quick

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

/** Bufer (clipboard) ichidagi havolani topish yordamchilari. */
object ClipLink {

    private val URL_REGEX = Regex("""https?://\S+""")

    fun find(text: CharSequence?): String? =
        text?.let { URL_REGEX.find(it)?.value?.trimEnd('.', ',', ';', ')') }

    /**
     * Bufer matnidagi birinchi havola. Android 10+ da bufer faqat oldingi planda turgan ilova
     * (yoki yoqilgan Maxsus imkoniyatlar xizmati) uchun o'qiladi, aks holda null qaytadi.
     */
    fun read(context: Context): String? = runCatching {
        val cm = context.getSystemService(ClipboardManager::class.java)
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        find(clip.getItemAt(0).coerceToText(context))
    }.getOrNull()

    /** Bufer holatining belgisi: bir xil havola qayta nusxalansa ham o'zgaradi. */
    fun stamp(context: Context, url: String): String {
        val ts = runCatching {
            context.getSystemService(ClipboardManager::class.java).primaryClipDescription?.timestamp ?: 0L
        }.getOrDefault(0L)
        return "$url|$ts"
    }

    fun host(url: String): String =
        runCatching { Uri.parse(url).host?.removePrefix("www.") }.getOrNull() ?: url
}
