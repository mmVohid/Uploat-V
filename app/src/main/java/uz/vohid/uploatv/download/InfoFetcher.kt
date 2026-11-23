package uz.vohid.uploatv.download

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.MediaInfo

/** Yuklashdan oldin havola haqida ma'lumot (nom, rasm, davomiylik, mavjud sifat) olish. */
object InfoFetcher {

    private const val PROCESS_ID = "uploatv-info"

    suspend fun fetch(url: String, noPlaylist: Boolean, processId: String = PROCESS_ID): MediaInfo = withContext(Dispatchers.IO) {
        UploatApp.engineReady.await()
        val request = YoutubeDLRequest(url).apply {
            addOption("--dump-single-json")
            addOption("--flat-playlist")
            addOption("--no-warnings")
            if (noPlaylist) addOption("--no-playlist")
        }
        val out = YoutubeDL.getInstance().execute(request, processId).out
        parse(out)
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
        )
    }

    private fun JSONObject.str(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONObject.thumbnail(): String? =
        str("thumbnail") ?: optJSONArray("thumbnails")?.let { arr ->
            (arr.length() - 1 downTo 0).firstNotNullOfOrNull { arr.optJSONObject(it)?.str("url") }
        }
}
