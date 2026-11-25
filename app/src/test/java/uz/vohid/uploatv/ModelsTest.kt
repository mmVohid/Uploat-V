package uz.vohid.uploatv

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.Retention
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.InfoFetcher
import uz.vohid.uploatv.download.Status

class ModelsTest {

    @Test fun retentionOrderAndValues() {
        assertNull(Retention.FOREVER.millis)
        assertEquals(24L * 3600 * 1000, Retention.HOURS_24.millis)
        assertTrue(Retention.DAYS_7.millis!! < Retention.DAYS_30.millis!!)
        assertEquals(listOf("Cheksiz", "30 kun", "7 kun", "24 soat"), Retention.entries.map { it.label })
    }

    @Test fun downloadItemJsonRoundTrip() {
        val item = DownloadItem(
            id = "1", url = "https://a.b/c", title = "Sarlavha", thumbnail = null, uploader = "U",
            quality = Quality.P720, playlist = false, status = Status.FAILED,
            error = "xato", errorDetail = "texnik", createdAt = 123L,
        )
        val back = DownloadItem.fromJson(JSONObject(item.toJson().toString()))
        assertEquals(item.id, back.id)
        assertEquals(Quality.P720, back.quality)
        assertEquals(Status.FAILED, back.status)
        assertEquals("texnik", back.errorDetail)
        assertEquals(123L, back.createdAt)
        assertNull(back.thumbnail)
    }

    @Test fun activeStatusFlags() {
        val base = DownloadItem("1", "u", "t", null, null, Quality.BEST, false)
        assertTrue(base.copy(status = Status.QUEUED).isActive)
        assertTrue(base.copy(status = Status.RUNNING).isActive)
        assertTrue(!base.copy(status = Status.DONE).isActive)
        assertTrue(!base.copy(status = Status.FAILED).isActive)
    }

    @Test fun parseSingleVideoAndSizes() {
        val json = """{"title":"Test","uploader":"Kanal","thumbnail":"http://t","duration":65.5,
          "formats":[
            {"vcodec":"avc1","acodec":"none","height":1080,"filesize":9000000},
            {"vcodec":"avc1","acodec":"none","height":720,"filesize":5000000},
            {"vcodec":"none","acodec":"mp4a","filesize":1000000}
          ]}"""
        val info = InfoFetcher.parse("WARNING: noise\n" + json.replace("\n", ""))
        assertEquals("Test", info.title)
        assertEquals("Kanal", info.uploader)
        assertEquals(65L, info.durationSec)
        assertEquals(1080, info.maxHeight)
        assertEquals(10_000_000L, info.sizes[Quality.BEST])
        assertEquals(6_000_000L, info.sizes[Quality.P720])
        assertTrue(Quality.P360 !in info.sizes)
    }

    @Test fun parsePlaylist() {
        val info = InfoFetcher.parse("""{"_type":"playlist","title":"PL","entries":[{"title":"a"},{"title":"b"}]}""")
        assertTrue(info.isPlaylist)
        assertEquals(2, info.entryCount)
        assertTrue(info.sizes.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun parseWithoutJsonFails() {
        InfoFetcher.parse("hech narsa")
    }
}
