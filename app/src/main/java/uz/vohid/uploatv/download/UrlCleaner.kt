package uz.vohid.uploatv.download

/**
 * Havoladagi kuzatuv qismlarini (?si=…, utm_…, fbclid va h.k.) olib tashlaydi.
 * Video aniqlanishiga ta'sir qiladigan qismlar (masalan YouTube'dagi v=, list=, t=) saqlanadi.
 */
object UrlCleaner {

    /** Barcha saytlarda ma'nosiz kuzatuv parametrlari. */
    private val TRACKING = setOf(
        "fbclid", "gclid", "dclid", "msclkid", "yclid", "igshid", "igsh",
        "mc_cid", "mc_eid", "ref_src", "ref_url", "_ga",
    )

    /** YouTube'da faqat shu parametrlar qoladi. */
    private val YOUTUBE_KEEP = setOf("v", "list", "t", "index", "start")

    private val YT_PATH_ID = Regex("""^/(?:shorts|embed|live|v)/([\w-]{6,})""")

    internal fun hostOf(url: String): String =
        url.substringAfter("://", url).substringBefore('/').substringBefore('?').substringBefore('#')
            .substringAfterLast('@').substringBefore(':').lowercase()
            .removePrefix("www.").removePrefix("m.")

    private fun isYoutube(host: String) = host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com")

    /** Instagram, TikTok, X: video havolada (yo'lda) bo'ladi, so'rov qismi faqat kuzatuv uchun. */
    private fun dropsAllParams(host: String) =
        host == "instagram.com" || host.endsWith(".instagram.com") ||
            host == "tiktok.com" || host.endsWith(".tiktok.com") ||
            host == "x.com" || host == "twitter.com"

    fun clean(url: String): String {
        val u = url.trim()
        val hash = u.indexOf('#').takeIf { it >= 0 }
        val noFragment = if (hash != null) u.substring(0, hash) else u
        val fragment = if (hash != null) u.substring(hash) else ""
        val q = noFragment.indexOf('?')
        if (q < 0) return u
        val base = noFragment.substring(0, q)
        val host = hostOf(base)
        val kept = noFragment.substring(q + 1).split('&').filter { param ->
            if (param.isEmpty()) return@filter false
            val name = param.substringBefore('=').lowercase()
            when {
                name.startsWith("utm_") || name in TRACKING -> false
                isYoutube(host) -> name in YOUTUBE_KEEP
                dropsAllParams(host) -> false
                else -> true
            }
        }
        return base + (if (kept.isEmpty()) "" else "?" + kept.joinToString("&")) + fragment
    }

    private fun queryParam(url: String, name: String): String? =
        url.substringAfter('?', "").substringBefore('#').split('&')
            .firstOrNull { it.substringBefore('=') == name }
            ?.substringAfter('=', "")?.takeIf { it.isNotEmpty() }

    /**
     * Bir xil video/sahifa uchun bir xil kalit: youtu.be/ID va youtube.com/watch?v=ID bir xil hisoblanadi.
     * "Avval yuklangan" va "allaqachon yuklanmoqda" tekshiruvlari shu kalit bilan ishlaydi.
     */
    fun key(url: String): String {
        val c = clean(url)
        val host = hostOf(c)
        if (isYoutube(host)) {
            val path = "/" + c.substringAfter("://", c).substringAfter('/', "").substringBefore('?').substringBefore('#')
            val id = when {
                host == "youtu.be" -> path.removePrefix("/").substringBefore('/').takeIf { it.isNotEmpty() }
                path == "/watch" -> queryParam(c, "v")
                else -> YT_PATH_ID.find(path)?.groupValues?.get(1)
            }
            if (id != null) return "yt:$id"
            if (path == "/playlist") queryParam(c, "list")?.let { return "ytlist:$it" }
        }
        val rest = c.substringAfter("://", c).substringAfter('/', "").substringBefore('#').trimEnd('/')
        return "$host/$rest"
    }
}
