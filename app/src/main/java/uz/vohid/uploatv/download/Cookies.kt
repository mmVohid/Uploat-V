package uz.vohid.uploatv.download

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import java.io.File

/** Login talab qiladigan sayt: kirish sahifasi va cookie domenlari. */
data class Account(
    val key: String,
    val name: String,
    val loginUrl: String,
    val domains: List<String>,
)

/**
 * Akkaunt cookie'lari (yt-dlp `--cookies`). Foydalanuvchi ilova ichidagi oynada saytga kiradi,
 * cookie Netscape formatida filesDir/cookies/<sayt>.txt ga yoziladi. Parol saqlanmaydi.
 */
object CookieStore {
    val accounts = listOf(
        Account("instagram", "Instagram", "https://www.instagram.com/accounts/login/", listOf("instagram.com")),
        Account("facebook", "Facebook", "https://m.facebook.com/login", listOf("facebook.com", "fb.watch")),
        Account("tiktok", "TikTok", "https://www.tiktok.com/login", listOf("tiktok.com")),
        Account("x", "X (Twitter)", "https://x.com/i/flow/login", listOf("x.com", "twitter.com")),
    )

    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun byKey(key: String): Account? = accounts.firstOrNull { it.key == key }

    private fun file(account: Account) = File(File(app.filesDir, "cookies").apply { mkdirs() }, "${account.key}.txt")

    fun isLoggedIn(account: Account): Boolean = file(account).let { it.exists() && it.length() > 40 }

    fun loggedCount(): Int = accounts.count { isLoggedIn(it) }

    /** Havola shu saytlardan biriga tegishli bo'lsa va kirilgan bo'lsa, cookie fayl. */
    fun fileFor(url: String): File? {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
        val account = accounts.firstOrNull { a ->
            a.domains.any { d -> host == d || host.endsWith(".$d") }
        } ?: return null
        return file(account).takeIf { isLoggedIn(account) }
    }

    /** WebView'dagi cookie'larni yt-dlp formatiga saqlaydi. Cookie topilmasa false. */
    fun saveFromWebView(account: Account): Boolean {
        val cm = CookieManager.getInstance()
        cm.flush()
        val expiry = System.currentTimeMillis() / 1000 + 60L * 60 * 24 * 90
        val seen = LinkedHashMap<String, String>()
        val lines = StringBuilder("# Netscape HTTP Cookie File\n")
        var count = 0
        for (d in account.domains) {
            for (host in listOf("https://www.$d", "https://$d", "https://m.$d")) {
                val raw = cm.getCookie(host) ?: continue
                for (pair in raw.split(";")) {
                    val p = pair.trim()
                    val eq = p.indexOf('=')
                    if (eq <= 0) continue
                    val key = "$d|${p.substring(0, eq)}"
                    if (seen.containsKey(key)) continue
                    seen[key] = p
                    lines.append(".$d\tTRUE\t/\tTRUE\t$expiry\t${p.substring(0, eq)}\t${p.substring(eq + 1)}\n")
                    count++
                }
            }
        }
        if (count == 0) return false
        file(account).writeText(lines.toString())
        return true
    }

    fun logout(account: Account) {
        file(account).delete()
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
    }
}
