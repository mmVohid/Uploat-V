package uz.vohid.uploatv.download

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.data.Quality

/** Yuklashdan oldin havola haqida ma'lumot (nom, rasm, davomiylik, mavjud sifat) olish. */
object InfoFetcher {

    private const val PROCESS_ID = "uploatv-info"

    suspend fun fetch(
        url: String,
        noPlaylist: Boolean,
        processId: String = PROCESS_ID,
        cancelled: () -> Boolean = { false },
    ): MediaInfo = withContext(Dispatchers.IO) {
        UploatApp.engineReady.await()
        EngineUpdater.waitIdle()
        val ctx = currentCoroutineContext()
        // yt-dlp eskirgan bo'lsa dvigatel yangilanib, bir marta qayta uriniladi
        EngineUpdater.healing(cancelled = { cancelled() || !ctx.isActive }) {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
                if (noPlaylist) addOption("--no-playlist")
            }
            parse(YoutubeDL.getInstance().execute(request, processId).out)
        }
    }

    fun cancel() {
        runCatching { YoutubeDL.destroyProcessById(PROCESS_ID) }
    }

    private fun parse(raw: String): MediaInfo {
        val json = raw.lineSequence().map { it.trim() }.lastOrNull { it.startsWith("{") }
            ?: throw IllegalStateException("Ma'lumot olinmadi")
        val o = JSONObject(json)
        val isPlaylist = o.optString("_type") == "playlist"
        val entries = o.optJSONArray("entries")

        val thumb = o.thumbnail() ?: entries?.optJSONObject(0)?.thumbnail()
        val maxHeight = o.optJSONArray("formats")?.let { arr ->
            (0 until arr.length()).maxOfOrNull { arr.optJSONObject(it)?.optInt("height", 0) ?: 0 }
        }?.takeIf { it > 0 }

        return MediaInfo(
            title = o.str("title") ?: "Nomsiz",
            uploader = o.str("uploader") ?: o.str("channel"),
            thumbnail = thumb,
            durationSec = if (o.isNull("duration")) null else o.optDouble("duration").toLong(),
            isPlaylist = isPlaylist,
            entryCount = if (isPlaylist) (entries?.length() ?: o.optInt("playlist_count")) else 1,
            maxHeight = maxHeight,
            sizes = if (isPlaylist) emptyMap() else estimateSizes(o.optJSONArray("formats")),
        )
    }

    /** Formatlar ro'yxatidan har sifat uchun taxminiy hajmni (video + audio) hisoblaydi. */
    private fun estimateSizes(formats: org.json.JSONArray?): Map<Quality, Long> {
        if (formats == null) return emptyMap()
        class F(val height: Int, val size: Long, val hasAudio: Boolean)

        val videos = mutableListOf<F>()
        var bestAudio = 0L
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val size = f.optLong("filesize", 0L).takeIf { it > 0 } ?: f.optLong("filesize_approx", 0L)
            if (size <= 0) continue
            val v = f.optString("vcodec", "none")
            val a = f.optString("acodec", "none")
            val height = f.optInt("height", 0)
            when {
                v != "none" && height > 0 -> videos += F(height, size, a != "none")
                v == "none" && a != "none" -> bestAudio = maxOf(bestAudio, size)
            }
        }
        if (videos.isEmpty()) return emptyMap()

        return buildMap {
            for (q in Quality.entries) {
                val limit = q.height ?: Int.MAX_VALUE
                val candidates = videos.filter { it.height <= limit }
                val top = candidates.maxOfOrNull { it.height } ?: continue
                val size = candidates.filter { it.height == top }
                    .maxOf { it.size + if (it.hasAudio) 0L else bestAudio }
                put(q, size)
            }
        }
    }

    private fun JSONObject.str(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.thumbnail(): String? =
        str("thumbnail") ?: optJSONArray("thumbnails")?.let { arr ->
            (arr.length() - 1 downTo 0).firstNotNullOfOrNull { arr.optJSONObject(it)?.str("url") }
        }
}
