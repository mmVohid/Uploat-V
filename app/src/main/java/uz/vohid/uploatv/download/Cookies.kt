package uz.vohid.uploatv.download

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uz.vohid.uploatv.data.SettingsRepository
import kotlin.coroutines.resume
import java.io.File

/** Tez kirish uchun tayyor saytlar (foydalanuvchi istalgan boshqa manzilni ham kiritishi mumkin). */
data class QuickSite(val name: String, val url: String)

/**
 * Cookie'lar (yt-dlp `--cookies`). Foydalanuvchi ilova ichidagi oynada saytga kiradi,
 * cookie Netscape formatida filesDir/cookies.txt ga yoziladi. Parol saqlanmaydi.
 */
object CookieStore {
    val quickSites = listOf(
        QuickSite("Instagram", "https://www.instagram.com/accounts/login/"),
        QuickSite("Facebook", "https://m.facebook.com/login"),
        QuickSite("TikTok", "https://www.tiktok.com/login"),
        QuickSite("X (Twitter)", "https://x.com/i/flow/login"),
    )

    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    private fun file() = File(app.filesDir, "cookies.txt")

    /** fayldagi cookie qatorlari: "domen|nom" -> to'liq qator */
    private fun read(): LinkedHashMap<String, String> {
        val map = LinkedHashMap<String, String>()
        val f = file()
        if (!f.exists()) return map
        f.readLines().forEach { line ->
            val parts = line.split("\t")
            if (parts.size >= 7 && !line.startsWith("#")) map["${parts[0]}|${parts[5]}"] = line
        }
        return map
    }

    fun count(): Int = read().size

    fun domains(): List<String> = read().keys.map { it.substringBefore('|').removePrefix(".") }.distinct().sorted()

    private fun baseDomain(host: String): String {
        val labels = host.lowercase().split(".")
        return if (labels.size <= 2) host.lowercase() else labels.takeLast(2).joinToString(".")
    }

    /** Havola domeniga tegishli cookie bo'lsa, fayl; aks holda null. */
    fun fileFor(url: String): File? {
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return null
        val has = domains().any { d -> host == d || host.endsWith(".$d") }
        return if (has) file() else null
    }

    /** WebView'dagi shu saytning cookie'larini mavjudlariga qo'shadi. Cookie topilmasa false. */
    fun saveFromWebView(pageUrl: String): Boolean {
        val host = runCatching { Uri.parse(pageUrl).host }.getOrNull() ?: return false
        val base = baseDomain(host)
        val cm = CookieManager.getInstance()
        cm.flush()
        val expiry = System.currentTimeMillis() / 1000 + 60L * 60 * 24 * 90
        val all = read()
        var added = 0
        for (h in listOf("https://$host", "https://$base", "https://www.$base", "https://m.$base").distinct()) {
            val raw = cm.getCookie(h) ?: continue
            for (pair in raw.split(";")) {
                val p = pair.trim()
                val eq = p.indexOf('=')
                if (eq <= 0) continue
                val name = p.substring(0, eq)
                all[".$base|$name"] = ".$base\tTRUE\t/\tTRUE\t$expiry\t$name\t${p.substring(eq + 1)}"
                added++
            }
        }
        if (added == 0) return false
        file().writeText("# Netscape HTTP Cookie File\n" + all.values.joinToString("\n") + "\n")
        return true
    }

    private val lastRefresh = HashMap<String, Long>()

    /**
     * "Cookie'larni avtomatik boshqarish" yoqilgan bo'lsa: saytni yashirin WebView'da ochib,
     * mehmon (yoki kirilgan) cookie'larni yangilaydi. Har sayt uchun 6 soatda bir marta.
     */
    suspend fun prepareIfEnabled(url: String, force: Boolean = false) {
        if (!SettingsRepository(app).settings.first().autoCookies) return
        val host = runCatching { Uri.parse(url).host?.lowercase() }.getOrNull() ?: return
        val base = baseDomain(host)
        val known = quickSites.any { baseDomain(Uri.parse(it.url).host.orEmpty()) == base } || base in domains()
        if (!known) return
        val now = System.currentTimeMillis()
        if (!force && now - (lastRefresh[base] ?: 0L) < 6L * 60 * 60 * 1000 && fileFor(url) != null) return
        lastRefresh[base] = now

        withContext(Dispatchers.Main) {
            withTimeoutOrNull(15_000) {
                suspendCancellableCoroutine<Unit> { cont ->
                    val web = WebView(app)
                    var finished = false
                    fun finish() {
                        if (finished) return
                        finished = true
                        runCatching { web.stopLoading(); web.destroy() }
                        if (cont.isActive) cont.resume(Unit)
                    }
                    web.settings.javaScriptEnabled = true
                    web.settings.domStorageEnabled = true
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)
                    web.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, pageUrl: String) {
                            // sahifa skriptlari cookie o'rnatib ulgurishi uchun biroz kutiladi
                            view.postDelayed({
                                if (!finished) {
                                    runCatching { saveFromWebView(pageUrl) }
                                    finish()
                                }
                            }, 2000)
                        }
                    }
                    cont.invokeOnCancellation { Handler(Looper.getMainLooper()).post { finish() } }
                    web.loadUrl("https://www.$base/")
                }
            }
        }
    }

    fun clear() {
        file().delete()
        val cm = CookieManager.getInstance()
        cm.removeAllCookies(null)
        cm.flush()
    }
}
