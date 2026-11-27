package uz.vohid.uploatv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import uz.vohid.uploatv.download.EngineUpdater
import kotlinx.coroutines.launch
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.Status
import uz.vohid.uploatv.download.InfoFetcher
import uz.vohid.uploatv.download.friendlyError
import uz.vohid.uploatv.download.rawError
import java.util.UUID

data class HomeUi(
    val url: String = "",
    val fetching: Boolean = false,
    val info: MediaInfo? = null,
    val error: String? = null,
    val errorDetail: String? = null,
    val quality: Quality = Quality.BEST,
    val wholePlaylist: Boolean = false,
    /** Bu havola avval yuklangan: foydalanuvchidan tasdiq so'raladi. */
    val duplicate: Boolean = false,
) {
    /** Havolada ham video, ham playlist bo'lsa (watch?v=…&list=…) foydalanuvchi tanlaydi. */
    val hasVideoAndList: Boolean
        get() = url.contains("v=") && url.contains("list=")

    val availableQualities: List<Quality>
        get() {
            val max = info?.maxHeight ?: return Quality.entries
            return Quality.entries.filter { it.height == null || it.height <= max }
        }
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SettingsRepository(app)
    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui.asStateFlow()
    private var fetchJob: Job? = null

    /** Oxirgi havolalar ma'lumoti (xotirada, 10 daqiqa): bir xil havola qayta kiritilsa, bir zumda chiqadi. */
    private val infoCache = object : LinkedHashMap<String, Pair<Long, MediaInfo>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<Long, MediaInfo>>) = size > 10
    }
    private var incognito = false

    val banner: StateFlow<EngineBannerUi> = EngineWatch.banner

    init {
        viewModelScope.launch {
            var first = true
            var onboarded = false
            settings.settings.collect { s ->
                incognito = s.incognito
                if (first) {
                    first = false
                    _ui.update { it.copy(quality = s.defaultQuality) }
                }
                // Ruxsatlar va kelishuv oynalaridan keyin dvigatel yangiligi tekshiriladi
                if (s.onboardingDone && !onboarded) {
                    onboarded = true
                    EngineWatch.check(force = true)
                }
            }
        }
        // Yuklash dvigatel/sayt bilan bog'liq sabab bilan yiqilsa, yangi versiya bor-yo'qligi darhol tekshiriladi
        viewModelScope.launch {
            var prev = 0
            DownloadManager.items.collect { list ->
                val failed = list.count { it.status == Status.FAILED && !DownloadManager.willAutoRetry(it.id) }
                if (failed > prev) EngineWatch.check(force = true)
                prev = failed
            }
        }
    }

    fun updateEngineNow() = EngineWatch.update()

    fun dismissBanner() = EngineWatch.dismiss()

    fun onUrlChange(value: String, immediate: Boolean = false) {
        fetchJob?.cancel()
        InfoFetcher.cancel()
        _ui.update { it.copy(url = value, info = null, error = null, fetching = false, wholePlaylist = false) }
        if (!looksLikeUrl(value.trim())) return
        startFetch(immediate)
    }

    fun retryFetch() {
        if (looksLikeUrl(_ui.value.url.trim())) startFetch(true)
    }

    fun setWholePlaylist(on: Boolean) {
        fetchJob?.cancel()
        InfoFetcher.cancel()
        _ui.update { it.copy(wholePlaylist = on, info = null, error = null) }
        startFetch(true)
    }

    fun setQuality(q: Quality) = _ui.update { it.copy(quality = q) }

    fun dismissDuplicate() = _ui.update { it.copy(duplicate = false) }

    fun download(force: Boolean = false): Boolean {
        val s = _ui.value
        val info = s.info ?: return false
        val url = s.url.trim()
        if (!force && DownloadManager.items.value.any { it.url == url && it.status == Status.DONE }) {
            _ui.update { it.copy(duplicate = true) }
            return false
        }
        _ui.update { it.copy(duplicate = false) }
        DownloadManager.enqueue(
            DownloadItem(
                id = UUID.randomUUID().toString(),
                url = s.url.trim(),
                title = info.title,
                thumbnail = info.thumbnail,
                uploader = info.uploader,
                quality = s.quality,
                playlist = info.isPlaylist,
                incognito = incognito,
            )
        )
        _ui.update { it.copy(url = "", info = null, error = null, fetching = false, wholePlaylist = false) }
        return true
    }

    private fun applyInfo(info: MediaInfo) {
        _ui.update { cur ->
            val max = info.maxHeight
            val allowed = Quality.entries.filter { q ->
                val h = q.height
                h == null || max == null || h <= max
            }
            val q = if (cur.quality in allowed) cur.quality else allowed.firstOrNull { it.height != null } ?: Quality.BEST
            cur.copy(fetching = false, info = info, quality = q)
        }
    }

    private fun startFetch(immediate: Boolean) {
        fetchJob = viewModelScope.launch {
            val s = _ui.value
            val url = s.url.trim()
            val noPlaylist = s.hasVideoAndList && !s.wholePlaylist
            val key = "$url|$noPlaylist"
            infoCache[key]?.let { (time, cached) ->
                if (System.currentTimeMillis() - time < INFO_TTL_MS) {
                    applyInfo(cached)
                    return@launch
                }
            }
            if (!immediate) delay(700)
            _ui.update { it.copy(fetching = true, error = null) }
            try {
                val info = InfoFetcher.fetch(url, noPlaylist = noPlaylist)
                infoCache[key] = System.currentTimeMillis() to info
                applyInfo(info)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Yangi kiritish tufayli bekor qilingan bo'lsa, xato ko'rsatilmaydi
                currentCoroutineContext().ensureActive()
                _ui.update { it.copy(fetching = false, error = friendlyError(e), errorDetail = rawError(e)) }
            }
        }
    }

    private companion object {
        const val INFO_TTL_MS = 10 * 60 * 1000L
    }

    private fun looksLikeUrl(text: String) =
        (text.startsWith("http://") || text.startsWith("https://")) && !text.contains(' ') && text.length > 10
}
