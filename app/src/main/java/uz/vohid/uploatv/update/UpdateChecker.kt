package uz.vohid.uploatv.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** GitHub Releases'dagi oxirgi barqaror versiyani tekshiradi. */
object UpdateChecker {

    data class Release(val version: String, val url: String)

    private const val API = "https://api.github.com/repos/mmVohid/Uploat-V/releases/latest"

    /** Oxirgi reliz; tarmoq yo'q yoki xato bo'lsa null (xatolik foydalanuvchiga ko'rsatilmaydi). */
    suspend fun fetchLatest(): Release? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(API).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "Uploat-V")
                if (conn.responseCode != 200) return@runCatching null
                parseRelease(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    internal fun parseRelease(json: String): Release? {
        val o = JSONObject(json)
        if (o.optBoolean("draft") || o.optBoolean("prerelease")) return null
        val tag = o.optString("tag_name").removePrefix("v").takeIf { it.isNotBlank() } ?: return null
        // APK biriktirilgan bo'lsa to'g'ridan-to'g'ri yuklab olish havolasi, bo'lmasa reliz sahifasi
        val apk = o.optJSONArray("assets")?.let { arr ->
            (0 until arr.length()).firstNotNullOfOrNull { i ->
                arr.optJSONObject(i)?.takeIf { it.optString("name").endsWith(".apk") }
                    ?.optString("browser_download_url")?.takeIf { it.isNotBlank() }
            }
        }
        val url = apk ?: o.optString("html_url").takeIf { it.isNotBlank() } ?: return null
        return Release(tag, url)
    }

    /** "1.2.0" kabi versiyani sonlarga ajratadi; "dev.128" kabi sinov versiyalari uchun null. */
    internal fun parseVersion(v: String): List<Int>? {
        val parts = v.removePrefix("v").split('.')
        val nums = parts.map { it.toIntOrNull() ?: return null }
        return nums.takeIf { it.isNotEmpty() }
    }

    /** [latest] joriy versiyadan yangimi. Sinov versiyalarida (tahlil qilib bo'lmasa) doim false. */
    fun isNewer(latest: String, current: String): Boolean {
        val a = parseVersion(latest) ?: return false
        val b = parseVersion(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
