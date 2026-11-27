package uz.vohid.uploatv.data

import androidx.compose.runtime.Immutable

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

enum class NetworkMode(val label: String, val hint: String) {
    ANY("Wi‑Fi va mobil internet", "Qaysi biri ulangan bo'lsa, o'sha orqali yuklanadi"),
    WIFI("Faqat Wi‑Fi", "Mobil internet sarflanmaydi. Wi‑Fi'ga ulansangiz yuklash davom etadi"),
    MOBILE("Faqat mobil internet", "Wi‑Fi'da yuklanmaydi. Mobil internet yoqilganda davom etadi"),
}

enum class Retention(val label: String, val millis: Long?) {
    FOREVER("Cheksiz", null),
    DAYS_30("30 kun", 30L * 24 * 3600 * 1000),
    DAYS_7("7 kun", 7L * 24 * 3600 * 1000),
    HOURS_24("24 soat", 24L * 3600 * 1000),
}

/** Har bir yuklash uchun tezlik chegarasi (yt-dlp --limit-rate). */
enum class SpeedLimit(val label: String, val arg: String?) {
    NONE("Cheksiz", null),
    MB5("5 MB/s", "5M"),
    MB2("2 MB/s", "2M"),
    MB1("1 MB/s", "1M"),
    KB500("500 KB/s", "500K"),
}

/** Fayl nomi shabloni (yt-dlp chiqish shabloni, kengaytmasiz). */
enum class FileNameTemplate(val label: String, val example: String, val pattern: String) {
    TITLE_ID("Sarlavha [id]", "Video nomi [dQw4w9WgXcQ].mp4", "%(title).80s [%(id)s]"),
    TITLE("Faqat sarlavha", "Video nomi.mp4", "%(title).80s"),
    UPLOADER_TITLE("Muallif – Sarlavha", "Kanal nomi - Video nomi.mp4", "%(uploader|Noma'lum).40s - %(title).80s"),
    DATE_TITLE("Sana – Sarlavha", "2026-11-27 - Video nomi.mp4", "%(upload_date>%Y-%m-%d|Sanasiz)s - %(title).80s"),
}

/** Ko'p havola joylanganda nima qilinishi. */
enum class BulkMode(val label: String, val hint: String) {
    INSTANT("Darrov", "Topilgan hamma havola darrov navbatga qo'shiladi"),
    CONFIRM("Tasdiq bilan", "Ro'yxat chiqadi, tasdiqlasangiz hammasi yuklanadi"),
    MANUAL("Qo'lda", "Ro'yxatdan kerakli havolalarni o'zingiz tanlaysiz"),
}

/** Playlistdagi bitta video (tanlash oynasi uchun). [index] 1 dan boshlanadi. */
@Immutable
data class PlaylistEntry(val index: Int, val title: String, val durationSec: Long?)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
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
    /** Cookie'larni avtomatik boshqarish. */
    val autoCookies: Boolean = false,
    val ytdlpNightly: Boolean = false,
    /** Bildirishnoma panelida doimiy "Buferdan yuklash" tugmasi. */
    val clipShortcut: Boolean = false,
    /** Foydalanuvchi yopib qo'ygan dvigatel versiyasi: shu versiya uchun taklif qayta chiqmaydi. */
    val engineBannerDismissed: String = "",
    /** Yuklash qaysi tarmoq orqali bo'lishi. */
    val networkMode: NetworkMode = NetworkMode.ANY,
    /** Tugagan yuklashlar ro'yxati qancha vaqt saqlanadi. */
    val historyRetention: Retention = Retention.FOREVER,
    /** Bir vaqtda nechta video yuklanadi (qolganlari navbatda kutadi). */
    val maxParallel: Int = 2,
    val speedLimit: SpeedLimit = SpeedLimit.NONE,
    val fileNameTemplate: FileNameTemplate = FileNameTemplate.TITLE_ID,
    val bulkMode: BulkMode = BulkMode.MANUAL,
    /** Yangi versiya haqida xabar berish (GitHub Releases tekshiriladi). */
    val updateCheck: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    val latestVersion: String? = null,
    val latestUrl: String? = null,
    /** GitHub javobining ETag'i: o'zgarmagan bo'lsa qayta yuklanmaydi. */
    val latestEtag: String? = null,
    /** Foydalanuvchi yopib qo'ygan versiya: shu versiya haqida qayta ko'rsatilmaydi. */
    val dismissedVersion: String? = null,
)

/** Havola bo'yicha yt-dlp qaytargan qisqa ma'lumot. */
@Immutable
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
    /** Playlist videolari ro'yxati (playlist bo'lmasa bo'sh). */
    val entries: List<PlaylistEntry> = emptyList(),
)
