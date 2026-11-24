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

/** yt-dlp xatosini foydalanuvchiga tushunarli qisqa matnga aylantiradi. */
fun friendlyError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        isLoginError(msg) ->
            "Bu video uchun akkauntga kirish kerak. Sozlamalar → Maxfiylik va xavfsizlik → Cookies bo'limidan saytga kiring"
        "Unsupported URL" in msg -> "Bu havola qo'llab-quvvatlanmaydi"
        "Unable to resolve host" in msg || "Network is unreachable" in msg ||
            "Temporary failure" in msg || "timed out" in msg -> "Internet aloqasini tekshiring"
        "Sign in to confirm" in msg || "confirm your age" in msg -> "Video kirishni yoki yoshni tasdiqlashni talab qiladi"
        "Private video" in msg -> "Bu video yopiq"
        "not available" in msg || "unavailable" in msg -> "Video mavjud emas"
        "HTTP Error 403" in msg -> "Server ruxsat bermadi (403). Dvigatelni yangilab ko'ring"
        else -> msg.lineSequence()
            .lastOrNull { it.contains("ERROR", ignoreCase = true) }
            ?.substringAfter("ERROR:")
            ?.trim()
            ?.take(140)
            ?.ifBlank { null }
            ?: "Yuklab bo'lmadi"
    }
}

/** Sayt akkaunt/cookie talab qilgan xato belgilari. */
fun isLoginError(msg: String): Boolean {
    val low = msg.lowercase()
    return "empty media response" in low || "login required" in low || "log in" in low ||
        "logged-in" in low || "rate-limit reached" in low || "--cookies" in low
}

/** Texnik xato matni (nusxalash uchun): oxirgi qismi, qisqartirilgan. */
fun rawError(e: Throwable): String = e.message.orEmpty().trim().takeLast(1500)
