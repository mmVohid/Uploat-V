package uz.vohid.uploatv.download

import java.util.Locale

fun formatEta(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m >= 60) {
        String.format(Locale.US, "%d:%02d:%02d", m / 60, m % 60, s)
    } else {
        String.format(Locale.US, "%d:%02d", m, s)
    }
}

fun formatDuration(seconds: Long): String = formatEta(seconds)

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
    else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024 * 1024))
}

const val DEFAULT_FOLDER = "Download/Uploat-V"

/** SAF papka manzilini o'qiladigan ko'rinishga keltiradi: "Ichki xotira/Movies/Videolar". */
fun describeTree(tree: android.net.Uri): String {
    val docId = runCatching { android.provider.DocumentsContract.getTreeDocumentId(tree) }.getOrNull()
        ?: return tree.toString()
    val volume = docId.substringBefore(':')
    val path = docId.substringAfter(':', "")
    val root = if (volume == "primary") "Ichki xotira" else "SD karta"
    return if (path.isEmpty()) root else "$root/$path"
}

/** Xatodan keyin foydalanuvchiga taklif qilinadigan harakat (tugma sifatida ko'rsatiladi). */
enum class ErrorAction(val label: String) {
    NONE(""),
    OPEN_COOKIES("Cookies'ni ochish"),
    UPDATE_ENGINE("Yuklovchini yangilash"),
    BEST_QUALITY("Eng yaxshi sifatda urinish"),
}

private class ErrorKind(val text: String, val action: ErrorAction = ErrorAction.NONE)

private val DRM_WORD = Regex("""\bdrm\b""")

/** yt-dlp xato matnini tanib, o'zbekcha izoh va tavsiya qilinadigan harakatni qaytaradi. */
private fun classify(msg: String): ErrorKind? {
    val low = msg.lowercase()
    return when {
        isLoginError(msg) ->
            ErrorKind("Bu video uchun akkauntga kirish kerak. Sozlamalar → Maxfiylik → Cookies bo'limidan saytga kiring", ErrorAction.OPEN_COOKIES)
        "unsupported url" in low -> ErrorKind("Bu havola qo'llab-quvvatlanmaydi")
        isNetworkText(low) -> ErrorKind("Internet aloqasini tekshiring")
        "no space left" in low -> ErrorKind("Telefon xotirasi to'ldi. Joy bo'shatib qayta urinib ko'ring")
        "private video" in low || "this video is private" in low -> ErrorKind("Bu video yopiq (maxfiy)")
        "confirm your age" in low || "age-restricted" in low || "age restricted" in low ->
            ErrorKind("Video yoshga cheklangan. Cookies orqali akkauntga kiring", ErrorAction.OPEN_COOKIES)
        "sign in to confirm" in low ->
            ErrorKind("Sayt tasdiqlashni talab qilmoqda. Cookies'ni yoqing yoki yuklovchini yangilang", ErrorAction.OPEN_COOKIES)
        "not available in your country" in low || "geo" in low && "restrict" in low || "blocked it in your country" in low ->
            ErrorKind("Bu video sizning mintaqangizda ko'rish uchun yopilgan")
        "members-only" in low || "members only" in low || "join this channel" in low ->
            ErrorKind("Bu video faqat kanal a'zolari uchun")
        DRM_WORD.containsMatchIn(low) -> ErrorKind("Video DRM bilan himoyalangan, uni yuklab bo'lmaydi")
        "requires payment" in low || "payment required" in low || "rent or buy" in low ->
            ErrorKind("Bu video pullik: avval sotib oling yoki ijaraga oling")
        "has been removed" in low || "been deleted" in low || "no longer available" in low || "video unavailable" in low ->
            ErrorKind("Video o'chirilgan yoki mavjud emas")
        "copyright" in low -> ErrorKind("Mualliflik huquqi sababli video bloklangan")
        "premieres in" in low || "this live event will begin" in low -> ErrorKind("Bu video hali boshlanmagan (premyera)")
        "is live" in low || "live stream" in low -> ErrorKind("Jonli efirni yuklab bo'lmaydi")
        "http error 429" in low || "too many requests" in low ->
            ErrorKind("Server ko'p so'rovdan to'xtatdi. Birozdan keyin qayta urinib ko'ring")
        "http error 404" in low -> ErrorKind("Video topilmadi (404). Havolani tekshiring")
        "http error 403" in low -> ErrorKind("Server ruxsat bermadi (403). Yuklovchini yangilab ko'ring", ErrorAction.UPDATE_ENGINE)
        "unable to extract" in low || "nsig extraction failed" in low || "signature extraction failed" in low ->
            ErrorKind("Sayt o'zgargan bo'lishi mumkin. Yuklovchini yangilab ko'ring", ErrorAction.UPDATE_ENGINE)
        "requested format is not available" in low ->
            ErrorKind("Tanlangan sifat mavjud emas. Boshqa sifatni tanlang", ErrorAction.BEST_QUALITY)
        "not available" in low || "unavailable" in low -> ErrorKind("Video mavjud emas")
        else -> null
    }
}

/** yt-dlp xatosini foydalanuvchiga tushunarli qisqa matnga aylantiradi. */
fun friendlyError(e: Throwable): String {
    if (e is OfflineException || e is StorageLowException || e is NetworkModeException) return e.message.orEmpty()
    val msg = e.message.orEmpty()
    return classify(msg)?.text ?: msg.lineSequence()
        .lastOrNull { it.contains("ERROR", ignoreCase = true) }
        ?.substringAfter("ERROR:")
        ?.trim()
        ?.take(140)
        ?.ifBlank { null }
        ?: "Yuklab bo'lmadi"
}

/** Saqlangan texnik xato matniga qarab, xato yonida ko'rsatiladigan harakat. */
fun errorAction(rawDetail: String?): ErrorAction =
    rawDetail?.let { classify(it)?.action } ?: ErrorAction.NONE

private fun isNetworkText(low: String) =
    "unable to resolve host" in low || "network is unreachable" in low || "temporary failure" in low ||
        "timed out" in low || "no address associated" in low || "connection reset" in low

/** Xato internet bilan bog'liqmi (shunda internet qaytganda yuklash o'zi davom etadi). */
fun isNetworkFailure(e: Throwable): Boolean =
    e is OfflineException || e is NetworkModeException || isNetworkText(e.message.orEmpty().lowercase())

/** Sayt akkaunt/cookie talab qilgan xato belgilari. */
fun isLoginError(msg: String): Boolean {
    val low = msg.lowercase()
    return "empty media response" in low || "login required" in low || "log in" in low ||
        "logged-in" in low || "rate-limit reached" in low || "--cookies" in low
}

/** Texnik xato matni (nusxalash uchun): oxirgi qismi, qisqartirilgan. */
fun rawError(e: Throwable): String = e.message.orEmpty().trim().takeLast(1500)
