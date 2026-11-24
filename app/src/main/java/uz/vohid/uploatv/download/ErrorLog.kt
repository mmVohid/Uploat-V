package uz.vohid.uploatv.download

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Oxirgi yuklash xatolari jurnali: muammo bo'lganda nusxalab yuborish uchun. */
object ErrorLog {
    private val lock = Any()
    private lateinit var file: File

    fun init(context: Context) {
        file = File(context.filesDir, "errors.log")
    }

    fun add(url: String, raw: String) = synchronized(lock) {
        runCatching {
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            file.appendText("[$stamp] $url\n${raw.trim()}\n\n")
            if (file.length() > 60_000) file.writeText(file.readText().takeLast(40_000))
        }
        Unit
    }

    fun read(): String = synchronized(lock) { runCatching { file.readText() }.getOrDefault("") }

    fun clear() = synchronized(lock) { runCatching { file.delete() }; Unit }
}
