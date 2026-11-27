package uz.vohid.uploatv.download

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

/**
 * Tugagan va yarim qolgan yuklashlar tarixini JSON fayl sifatida saqlaydi.
 * Yozish xavfsiz: avval vaqtinchalik faylga yoziladi, keyin bir zumda asl fayl o'rniga almashtiriladi.
 * Shuning uchun yozish paytida ilova o'chib qolsa ham tarix butun holda qoladi.
 */
object HistoryStore {
    private val lock = Mutex()

    private fun file(context: Context) = File(context.filesDir, "history.json")
    private fun tmpFile(context: Context) = File(context.filesDir, "history.json.tmp")

    private fun parse(f: File): List<DownloadItem>? = runCatching {
        val arr = JSONArray(f.readText())
        (0 until arr.length()).mapNotNull {
            runCatching { DownloadItem.fromJson(arr.getJSONObject(it)) }.getOrNull()
        }
    }.getOrNull()

    suspend fun load(context: Context): List<DownloadItem> = lock.withLock {
        // Asl fayl buzuq bo'lsa (eski versiyadan qolgan), to'liq yozilgan vaqtinchalik nusxadan tiklanadi
        parse(file(context)) ?: parse(tmpFile(context)) ?: emptyList()
    }

    suspend fun save(context: Context, items: List<DownloadItem>) = lock.withLock {
        runCatching {
            val arr = JSONArray()
            items.forEach { arr.put(it.toJson()) }
            val tmp = tmpFile(context)
            FileOutputStream(tmp).use { out ->
                out.write(arr.toString().toByteArray(Charsets.UTF_8))
                out.fd.sync() // diskka haqiqatan yozilganini kutadi
            }
            if (!tmp.renameTo(file(context))) {
                file(context).delete()
                tmp.renameTo(file(context))
            }
        }
        Unit
    }
}
