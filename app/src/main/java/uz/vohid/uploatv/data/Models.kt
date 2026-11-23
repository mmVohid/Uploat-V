package uz.vohid.uploatv.data

enum class ThemeMode(val label: String) {
    SYSTEM("Tizim"),
    LIGHT("Yorug'"),
    DARK("Qorong'i"),
}

enum class Quality(val label: String, val height: Int?) {
    BEST("Eng yaxshi", null),
    P1080("1080p", 1080),
    P720("720p", 720),
    P480("480p", 480),
    P360("360p", 360),
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val defaultQuality: Quality = Quality.BEST,
    /** Foydalanuvchi tanlagan papka (SAF tree URI). null bo'lsa Download/Uploat-V ishlatiladi. */
    val saveTreeUri: String? = null,
)

/** Havola bo'yicha yt-dlp qaytargan qisqa ma'lumot. */
data class MediaInfo(
    val title: String,
    val uploader: String?,
    val thumbnail: String?,
    val durationSec: Long?,
    val isPlaylist: Boolean,
    val entryCount: Int,
    val maxHeight: Int?,
)
