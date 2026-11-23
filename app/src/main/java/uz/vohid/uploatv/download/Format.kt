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

/** yt-dlp xatosini foydalanuvchiga tushunarli qisqa matnga aylantiradi. */
fun friendlyError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
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
