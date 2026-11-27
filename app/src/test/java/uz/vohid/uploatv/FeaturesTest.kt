package uz.vohid.uploatv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.vohid.uploatv.download.ErrorAction
import uz.vohid.uploatv.download.UrlCleaner
import uz.vohid.uploatv.download.compressIndices
import uz.vohid.uploatv.download.errorAction
import uz.vohid.uploatv.download.friendlyError
import uz.vohid.uploatv.ui.extractUrls
import uz.vohid.uploatv.update.UpdateChecker

class FeaturesTest {

    @Test
    fun compressIndices_groupsRanges() {
        assertEquals("1-3,5", compressIndices(listOf(1, 2, 3, 5)))
        assertEquals("2", compressIndices(listOf(2)))
        assertEquals("1,3,5", compressIndices(listOf(5, 3, 1)))
        assertEquals("4-6,9-10", compressIndices(setOf(10, 9, 6, 5, 4)))
        assertEquals("", compressIndices(emptyList()))
    }

    @Test
    fun extractUrls_findsAllDistinct() {
        val text = "Mana: https://youtu.be/abc123xyz, va https://www.instagram.com/reel/XYZ/ \nhttps://youtu.be/abc123xyz"
        assertEquals(
            listOf("https://youtu.be/abc123xyz", "https://www.instagram.com/reel/XYZ/"),
            extractUrls(text),
        )
        assertTrue(extractUrls("havola yo'q").isEmpty())
    }

    @Test
    fun isNewer_comparesVersions() {
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.2.0"))
        // sinov versiyalarida (dev.N) hech qachon "yangi" deb hisoblanmaydi
        assertFalse(UpdateChecker.isNewer("1.1.0", "dev.128"))
        assertNull(UpdateChecker.parseVersion("dev.128"))
    }

    @Test
    fun parseRelease_prefersApkAndSkipsPrerelease() {
        val json = """{"tag_name":"v1.1.0","html_url":"https://github.com/x/releases/tag/v1.1.0",
            "assets":[{"name":"Uploat-V.apk","browser_download_url":"https://github.com/x/Uploat-V.apk"}]}"""
        val r = UpdateChecker.parseRelease(json)
        assertEquals("1.1.0", r?.version)
        assertEquals("https://github.com/x/Uploat-V.apk", r?.url)
        assertNull(UpdateChecker.parseRelease("""{"tag_name":"v2.0.0","prerelease":true}"""))
    }

    @Test
    fun urlCleaner_removesTrackingOnly() {
        assertEquals("https://youtu.be/abc12345678?t=30", UrlCleaner.clean("https://youtu.be/abc12345678?si=XYZ&t=30"))
        assertEquals(
            "https://www.youtube.com/watch?v=abc12345678&list=PL1",
            UrlCleaner.clean("https://www.youtube.com/watch?v=abc12345678&list=PL1&feature=share&pp=ygU"),
        )
        assertEquals("https://www.instagram.com/reel/AbC/", UrlCleaner.clean("https://www.instagram.com/reel/AbC/?igsh=abc&utm_source=x"))
        assertEquals("https://vm.tiktok.com/ZM123/", UrlCleaner.clean("https://vm.tiktok.com/ZM123/?_r=1&_t=abc"))
        assertEquals("https://site.com/v?id=5", UrlCleaner.clean("https://site.com/v?id=5&utm_campaign=a&fbclid=z"))
        assertEquals("https://site.com/v", UrlCleaner.clean("https://site.com/v?utm_source=a"))
        assertEquals("https://site.com/page#part", UrlCleaner.clean("https://site.com/page?gclid=1#part"))
        // o'zgarishsiz havola o'zgarmaydi
        assertEquals("https://site.com/a?b=1", UrlCleaner.clean("https://site.com/a?b=1"))
    }

    @Test
    fun urlCleaner_keyMatchesSameVideo() {
        val a = UrlCleaner.key("https://youtu.be/abc12345678?si=1")
        val b = UrlCleaner.key("https://www.youtube.com/watch?v=abc12345678&feature=share")
        val c = UrlCleaner.key("https://m.youtube.com/shorts/abc12345678")
        assertEquals(a, b)
        assertEquals(a, c)
        assertFalse(a == UrlCleaner.key("https://youtu.be/zzz12345678"))
        assertEquals(UrlCleaner.key("https://site.com/a/b/"), UrlCleaner.key("https://www.site.com/a/b?utm_x=1"))
    }

    @Test
    fun errors_haveHintsAndActions() {
        fun err(m: String) = RuntimeException(m)
        assertEquals("Bu video faqat kanal a'zolari uchun", friendlyError(err("ERROR: Join this channel to get access")))
        assertEquals("Video DRM bilan himoyalangan, uni yuklab bo'lmaydi", friendlyError(err("ERROR: This video is DRM protected")))
        assertEquals(ErrorAction.OPEN_COOKIES, errorAction("ERROR: Sign in to confirm you're not a bot"))
        assertEquals(ErrorAction.OPEN_COOKIES, errorAction("This video may be inappropriate. Confirm your age"))
        assertEquals(ErrorAction.UPDATE_ENGINE, errorAction("ERROR: Unable to extract uploader id"))
        assertEquals(ErrorAction.UPDATE_ENGINE, errorAction("HTTP Error 403: Forbidden"))
        assertEquals(ErrorAction.BEST_QUALITY, errorAction("ERROR: Requested format is not available"))
        assertEquals(ErrorAction.NONE, errorAction("ERROR: Private video"))
        assertEquals(ErrorAction.NONE, errorAction(null))
        // eski xatti-harakat saqlanadi
        assertEquals("Bu video yopiq (maxfiy)", friendlyError(err("ERROR: Private video. Sign in")))
    }
}
