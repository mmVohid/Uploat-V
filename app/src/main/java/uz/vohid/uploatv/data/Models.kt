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
    val dynamicColor: Boolean = false,
    val defaultQuality: Quality = Quality.BEST,
    /** Foydalanuvchi tanlagan papka (SAF tree URI). null bo'lsa Download/Uploat-V ishlatiladi. */
    val saveTreeUri: String? = null,
    /** Sozlamalar diskdan o'qib bo'lingani (birinchi kadrda noto'g'ri dialog ko'rsatmaslik uchun). */
    val loaded: Boolean = false,
    /** Birinchi ishga tushishdagi ruxsat so'rovi ko'rsatib bo'lingan. */
    val onboardingDone: Boolean = false,
    /** Foydalanuvchi yt-dlp'ni avtomatik yangilashga ruxsat bergan. */
    val autoUpdateYtdlp: Boolean = false,
    val lastUpdateMs: Long = 0L,
    /** Ilova o'zi yuklab o'rnatgan yt-dlp versiyasi (null — ilova ichidagi birinchi versiya). */
    val ytdlpVersion: String? = null,
    /** Barcha yuklashlar tugaganda bildirishnoma ko'rsatish. */
    val notifyOnDone: Boolean = true,
    /** Inkognito: yuklab olish tarixi saqlanmaydi. */
    val incognito: Boolean = false,
    /** Uploat-V yangi versiyasini GitHub'dan avtomatik tekshirish. */
    val autoUpdateApp: Boolean = false,
    val autoCookies: Boolean = false,
    val ytdlpNightly: Boolean = false,
    val lastAppCheckMs: Long = 0L,
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
    /** Har sifat uchun taxminiy fayl hajmi (bayt); ma'lum bo'lmasa kalit yo'q. */
    val sizes: Map<Quality, Long> = emptyMap(),
)
