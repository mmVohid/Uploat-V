package uz.vohid.uploatv

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.EngineHook
import uz.vohid.uploatv.download.TestEngine
import java.io.File

/**
 * Ilovaning to'liq foydalanuvchi yo'li (E2E): haqiqiy ekranlar, haqiqiy navbat va saqlash,
 * faqat yt-dlp o'rniga soxta dvigatel ishlaydi (tarmoqsiz, barqaror).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AppE2ETest {

    @get:Rule val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null

    private val fake = object : TestEngine {
        override suspend fun fetch(url: String): MediaInfo {
            delay(200)
            if (url.contains("bad")) throw RuntimeException("ERROR: [youtube] x: Private video")
            return MediaInfo(
                title = "Sinov video", uploader = "Sinov kanal", thumbnail = null,
                durationSec = 30, isPlaylist = false, entryCount = 1, maxHeight = 720,
            )
        }

        override suspend fun download(url: String, dir: File, onProgress: (Float) -> Unit) {
            if (url.contains("fail")) throw RuntimeException("ERROR: [youtube] x: Private video")
            for (p in listOf(0.25f, 0.5f, 1f)) {
                delay(100)
                onProgress(p)
            }
            File(dir, "sinov-${System.nanoTime()}.mp4").writeBytes(ByteArray(2048) { it.toByte() })
        }
    }

    @Before fun setUp() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        runBlocking {
            // Birinchi ochilish oynalari testga xalaqit bermasligi uchun onboarding tugagan deb belgilanadi
            SettingsRepository(ctx).finishOnboarding(autoUpdate = false, autoCookies = false)
        }
        DownloadManager.clearFinished()
        EngineHook.engine = fake
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun tearDown() {
        EngineHook.engine = null
        scenario?.close()
        DownloadManager.clearFinished()
    }

    /** Belgi (badge) bor paytda tugma tavsifi birlashib ketadi, shuning uchun birlashmagan daraxtdan qidiriladi. */
    private fun openDownloads() {
        // Klaviatura yopilguncha pastki panel ko'rinmasligi mumkin: paydo bo'lishini kutamiz
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithContentDescription("Yuklashlar", substring = true, useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("'Yuklashlar' tugmasi yo'q. Ekran:\n" + rule.onRoot(useUnmergedTree = true).printToString(), e)
        }
        rule.onNodeWithContentDescription("Yuklashlar", substring = true, useUnmergedTree = true).performClick()
    }

    private fun enterUrl(url: String) {
        rule.onNodeWithText("Video havolasi").performTextInput(url)
    }

    private fun waitText(text: String, timeoutMs: Long = 15_000) {
        try {
            rule.waitUntil(timeoutMs) {
                rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("'$text' topilmadi. Ekran:\n" + rule.onRoot(useUnmergedTree = true).printToString(), e)
        }
    }

    @Test fun homeShowsEmptyState() {
        waitText("Havola qo'shing")
        rule.onNodeWithText("Havolani joylash").assertExists()
    }

    @Test fun downloadFlowEndsInDownloadsList() {
        enterUrl("https://example.com/watch?v=ok1")
        waitText("Sinov video")
        rule.onNodeWithText("Videoni yuklash").performClick()

        openDownloads()
        waitText("Sinov video")
        // Tugagach holat qatorida saqlangan papka ko'rsatiladi
        waitText("Download/Uploat-V")
    }

    @Test fun duplicateUrlAsksForConfirmation() {
        enterUrl("https://example.com/watch?v=dup1")
        waitText("Sinov video")
        rule.onNodeWithText("Videoni yuklash").performClick()
        openDownloads()
        waitText("Download/Uploat-V")

        // Bosh sahifaga qaytib, shu havolani yana kiritamiz
        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        enterUrl("https://example.com/watch?v=dup1")
        waitText("Sinov video")
        rule.onNodeWithText("Videoni yuklash").performClick()
        waitText("Bu video avval yuklangan")
        rule.onNodeWithText("Bekor qilish").performClick()
    }

    @Test fun failedDownloadShowsFriendlyError() {
        enterUrl("https://example.com/watch?v=fail1")
        waitText("Sinov video")
        rule.onNodeWithText("Videoni yuklash").performClick()
        openDownloads()
        waitText("Bu video yopiq (maxfiy)")
    }

    @Test fun infoErrorIsShownOnHome() {
        enterUrl("https://example.com/watch?v=bad1")
        waitText("Bu video yopiq (maxfiy)")
    }

    @Test fun settingsScreensOpen() {
        rule.onNodeWithContentDescription("Sozlamalar").performClick()
        waitText("Maxfiylik va xavfsizlik")
        rule.onNodeWithText("Ma'lumotlar va xotira").performClick()
        waitText("Xotira tozaligi")
        waitText("24 soat")
        waitText("Faqat Wi‑Fi'da yuklash")
    }
}
