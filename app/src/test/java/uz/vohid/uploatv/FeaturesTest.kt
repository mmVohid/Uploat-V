package uz.vohid.uploatv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.vohid.uploatv.download.compressIndices
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
}
