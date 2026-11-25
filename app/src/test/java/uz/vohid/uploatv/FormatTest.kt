package uz.vohid.uploatv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.vohid.uploatv.download.OfflineException
import uz.vohid.uploatv.data.NetworkMode
import uz.vohid.uploatv.download.NetworkModeException
import uz.vohid.uploatv.download.formatBytes
import uz.vohid.uploatv.download.formatEta
import uz.vohid.uploatv.download.friendlyError
import uz.vohid.uploatv.download.isLoginError
import uz.vohid.uploatv.download.isNetworkFailure
import uz.vohid.uploatv.download.rawError

class FormatTest {

    private fun err(msg: String) = friendlyError(RuntimeException(msg))

    @Test fun etaFormats() {
        assertEquals("0:05", formatEta(5))
        assertEquals("1:05", formatEta(65))
        assertEquals("1:01:01", formatEta(3661))
    }

    @Test fun bytesFormats() {
        assertEquals("512 B", formatBytes(512))
        assertEquals("2 KB", formatBytes(2048))
        assertEquals("1.5 MB", formatBytes((1.5 * 1024 * 1024).toLong()))
        assertEquals("2.00 GB", formatBytes(2L * 1024 * 1024 * 1024))
    }

    @Test fun loginErrorsDetected() {
        assertTrue(isLoginError("ERROR: [Instagram] X: Instagram sent an empty media response"))
        assertTrue(isLoginError("ERROR: login required"))
        assertTrue(isLoginError("Use --cookies-from-browser or --cookies for the authentication"))
        assertFalse(isLoginError("ERROR: Private video"))
    }

    @Test fun loginErrorMessageMentionsCookies() {
        assertTrue(err("ERROR: empty media response").contains("Cookies"))
    }

    @Test fun commonErrorsAreFriendly() {
        assertEquals("Bu video yopiq (maxfiy)", err("ERROR: [youtube] abc: Private video. Sign in if you've been granted access"))
        assertEquals("Bu havola qo'llab-quvvatlanmaydi", err("ERROR: Unsupported URL: https://x.y"))
        assertEquals("Video o'chirilgan yoki mavjud emas", err("ERROR: This video has been removed by the uploader"))
        assertEquals("Mualliflik huquqi sababli video bloklangan", err("ERROR: blocked on copyright grounds"))
        assertEquals("Tanlangan sifat mavjud emas. Boshqa sifatni tanlang", err("ERROR: Requested format is not available"))
        assertEquals("Server ko'p so'rovdan to'xtatdi. Birozdan keyin qayta urinib ko'ring", err("ERROR: HTTP Error 429: Too Many Requests"))
        assertEquals("Video topilmadi (404). Havolani tekshiring", err("ERROR: HTTP Error 404: Not Found"))
        assertEquals("Telefon xotirasi to'ldi. Joy bo'shatib qayta urinib ko'ring", err("OSError: [Errno 28] No space left on device"))
    }

    @Test fun ageAndGeoErrors() {
        assertTrue(err("ERROR: Sign in to confirm your age").contains("yoshga"))
        assertTrue(err("ERROR: The uploader has not made this video available in your country (geo restriction)").contains("mintaqa"))
    }

    @Test fun networkErrorsAreDetected() {
        assertEquals("Internet aloqasini tekshiring", err("Unable to resolve host \"x\""))
        assertTrue(isNetworkFailure(RuntimeException("Connection reset by peer")))
        assertTrue(isNetworkFailure(OfflineException()))
        assertTrue(isNetworkFailure(NetworkModeException(NetworkMode.WIFI))
        assertTrue(isNetworkFailure(NetworkModeException(NetworkMode.MOBILE))))
        assertFalse(isNetworkFailure(RuntimeException("ERROR: Private video")))
    }

    @Test fun preflightMessagesPassThrough() {
        assertEquals("Internet yo'q. Ulanishni tekshiring", friendlyError(OfflineException()))
    }

    @Test fun unknownErrorUsesLastErrorLine() {
        assertEquals("kutilmagan holat", err("WARNING: x\nERROR: kutilmagan holat"))
        assertEquals("Yuklab bo'lmadi", err(""))
    }

    @Test fun rawErrorIsTruncated() {
        assertEquals(1500, rawError(RuntimeException("a".repeat(5000))).length)
    }
}
