package uz.vohid.uploatv.download

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File

/** Tugagan yuklashlar tarixini oddiy JSON fayl sifatida saqlaydi. */
object HistoryStore {
    private val lock = Mutex()

    private fun file(context: Context) = File(context.filesDir, "history.json")

    suspend fun load(context: Context): List<DownloadItem> = lock.withLock {
        runCatching {
            val arr = JSONArray(file(context).readText())
            (0 until arr.length()).mapNotNull {
                runCatching { DownloadItem.fromJson(arr.getJSONObject(it)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    suspend fun save(context: Context, items: List<DownloadItem>) = lock.withLock {
        runCatching {
            val arr = JSONArray()
            items.forEach { arr.put(it.toJson()) }
            file(context).writeText(arr.toString())
        }
        Unit
    }
}
