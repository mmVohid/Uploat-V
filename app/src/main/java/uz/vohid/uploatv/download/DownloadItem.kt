package uz.vohid.uploatv.download

import org.json.JSONObject
import uz.vohid.uploatv.data.AudioFormat
import uz.vohid.uploatv.data.Quality

enum class Status { QUEUED, RUNNING, DONE, FAILED, CANCELED }

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String?,
    val uploader: String?,
    val audio: Boolean,
    val quality: Quality,
    val audioFormat: AudioFormat,
    val playlist: Boolean,
    val status: Status = Status.QUEUED,
    val progress: Float = 0f,
    val detail: String = "",
    val error: String? = null,
    val savedUri: String? = null,
    val mime: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val isActive get() = status == Status.QUEUED || status == Status.RUNNING

    val optionLabel get() = if (audio) audioFormat.label else quality.label

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("url", url)
        put("title", title)
        put("thumbnail", thumbnail)
        put("uploader", uploader)
        put("audio", audio)
        put("quality", quality.name)
        put("audioFormat", audioFormat.name)
        put("playlist", playlist)
        put("status", status.name)
        put("detail", detail)
        put("error", error)
        put("savedUri", savedUri)
        put("mime", mime)
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): DownloadItem {
            fun s(k: String): String? = if (o.isNull(k)) null else o.optString(k).takeIf { it.isNotEmpty() }
            return DownloadItem(
                id = o.getString("id"),
                url = o.getString("url"),
                title = o.optString("title", "Nomsiz"),
                thumbnail = s("thumbnail"),
                uploader = s("uploader"),
                audio = o.optBoolean("audio"),
                quality = runCatching { Quality.valueOf(o.optString("quality")) }.getOrDefault(Quality.P720),
                audioFormat = runCatching { AudioFormat.valueOf(o.optString("audioFormat")) }
                    .getOrDefault(AudioFormat.M4A),
                playlist = o.optBoolean("playlist"),
                status = runCatching { Status.valueOf(o.optString("status")) }.getOrDefault(Status.FAILED),
                progress = if (o.optString("status") == Status.DONE.name) 1f else 0f,
                detail = o.optString("detail"),
                error = s("error"),
                savedUri = s("savedUri"),
                mime = s("mime"),
                createdAt = o.optLong("createdAt"),
            )
        }
    }
}
