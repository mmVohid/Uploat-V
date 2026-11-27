package uz.vohid.uploatv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import uz.vohid.uploatv.BuildConfig
import uz.vohid.uploatv.data.BulkMode
import uz.vohid.uploatv.download.compressIndices
import uz.vohid.uploatv.update.UpdateChecker
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

/** Bir nechta havola joylanganda ro'yxat holati. [selectable] bo'lsa, foydalanuvchi belgilaydi. */
data class BulkUi(val urls: List<String>, val selected: Set<Int>, val selectable: Boolean)

data class UpdateUi(val version: String, val url: String)

private val URL_FIND = Regex("""https?://[^\s]+""")

/** Matndagi barcha havolalar (takrorlarsiz, oxiridagi tinish belgilarsiz). */
internal fun extractUrls(text: String): List<String> =
    URL_FIND.findAll(text)
        .map { it.value.trimEnd('.', ',', ';', '"', '\'') }
        .filter { it.length > 10 }
        .distinct()
        .toList()

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
    /** Playlistdan tanlangan videolar raqami; null = hammasi. */
    val selectedEntries: Set<Int>? = null,
    val bulk: BulkUi? = null,
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
    private var bulkMode = BulkMode.MANUAL

    private val _events = Channel<String>(Channel.BUFFERED)
    /** Qisqa xabarlar (snackbar uchun). */
    val events = _events.receiveAsFlow()

    /** Yangi versiya chiqqan bo'lsa (va foydalanuvchi yopmagan bo'lsa) banner ma'lumoti. */
    val update: StateFlow<UpdateUi?> = settings.settings
        .map { s ->
            val v = s.latestVersion
            val u = s.latestUrl
            if (s.updateCheck && v != null && u != null && v != s.dismissedVersion &&
                UpdateChecker.isNewer(v, BuildConfig.VERSION_NAME)
            ) UpdateUi(v, u) else null
        }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val banner: StateFlow<EngineBannerUi> = EngineWatch.banner

    init {
        viewModelScope.launch {
            var first = true
            var onboarded = false
            settings.settings.collect { s ->
                incognito = s.incognito
                bulkMode = s.bulkMode
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
        // Yangi versiyani tekshirish: ko'pi bilan 6 soatda bir marta, xato bo'lsa jim o'tadi
        viewModelScope.launch {
            val s = settings.settings.first()
            if (s.updateCheck && System.currentTimeMillis() - s.lastUpdateCheck > UPDATE_INTERVAL_MS) {
                UpdateChecker.fetchLatest()?.let { settings.setLatestRelease(it.version, it.url) }
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

    fun dismissUpdate() {
        val v = update.value?.version ?: return
        viewModelScope.launch { settings.dismissUpdate(v) }
    }

    /** Bufer matnidan havola(lar) olinadi. Havola topilmasa false. */
    fun onPaste(text: String): Boolean {
        val urls = extractUrls(text)
        return when {
            urls.isEmpty() -> false
            urls.size == 1 -> {
                onUrlChange(urls[0], immediate = true)
                true
            }
            else -> {
                startBulk(urls)
                true
            }
        }
    }

    private fun startBulk(urls: List<String>) {
        fetchJob?.cancel()
        InfoFetcher.cancel()
        _ui.update {
            it.copy(url = "", info = null, error = null, fetching = false, wholePlaylist = false, selectedEntries = null, bulk = null)
        }
        when (bulkMode) {
            BulkMode.INSTANT -> enqueueUrls(urls)
            BulkMode.CONFIRM -> _ui.update { it.copy(bulk = BulkUi(urls, urls.indices.toSet(), selectable = false)) }
            BulkMode.MANUAL -> _ui.update { it.copy(bulk = BulkUi(urls, emptySet(), selectable = true)) }
        }
    }

    fun toggleBulk(index: Int) = _ui.update { cur ->
        val b = cur.bulk ?: return@update cur
        cur.copy(bulk = b.copy(selected = if (index in b.selected) b.selected - index else b.selected + index))
    }

    fun setAllBulk(on: Boolean) = _ui.update { cur ->
        val b = cur.bulk ?: return@update cur
        cur.copy(bulk = b.copy(selected = if (on) b.urls.indices.toSet() else emptySet()))
    }

    fun cancelBulk() = _ui.update { it.copy(bulk = null) }

    fun downloadBulk() {
        val b = _ui.value.bulk ?: return
        val urls = b.urls.filterIndexed { i, _ -> i in b.selected }
        _ui.update { it.copy(bulk = null) }
        enqueueUrls(urls)
    }

    private fun enqueueUrls(urls: List<String>) {
        val active = DownloadManager.items.value.filter { it.isActive }.map { it.url }.toSet()
        val fresh = urls.filter { it !in active }
        fresh.forEach { DownloadManager.quickDownload(it) }
        _events.trySend(
            if (fresh.isEmpty()) "Bu havolalar allaqachon yuklanmoqda" else "${fresh.size} ta havola navbatga qo'shildi",
        )
    }

    fun setSelectedEntries(selected: Set<Int>?) = _ui.update { it.copy(selectedEntries = selected) }

    fun onUrlChange(value: String, immediate: Boolean = false) {
        val urls = extractUrls(value)
        if (urls.size >= 2) {
            startBulk(urls)
            return
        }
        fetchJob?.cancel()
        InfoFetcher.cancel()
        _ui.update {
            it.copy(url = value, info = null, error = null, fetching = false, wholePlaylist = false, selectedEntries = null, bulk = null)
        }
        if (!looksLikeUrl(value.trim())) return
        startFetch(immediate)
    }

    fun retryFetch() {
        if (looksLikeUrl(_ui.value.url.trim())) startFetch(true)
    }

    fun setWholePlaylist(on: Boolean) {
        fetchJob?.cancel()
        InfoFetcher.cancel()
        _ui.update { it.copy(wholePlaylist = on, info = null, error = null, selectedEntries = null) }
        startFetch(true)
    }

    fun setQuality(q: Quality) = _ui.update { it.copy(quality = q) }

    fun dismissDuplicate() = _ui.update { it.copy(duplicate = false) }

    fun download(force: Boolean = false): Boolean {
        val s = _ui.value
        val info = s.info ?: return false
        val url = s.url.trim()
        val picked = s.selectedEntries
        if (info.isPlaylist && picked != null && picked.isEmpty()) return false
        val partial = info.isPlaylist && picked != null && picked.size < info.entryCount
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
                playlistItems = if (partial) compressIndices(picked.orEmpty()) else null,
                incognito = incognito,
            )
        )
        _ui.update { it.copy(url = "", info = null, error = null, fetching = false, wholePlaylist = false, selectedEntries = null) }
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
        const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L
    }

    private fun looksLikeUrl(text: String) =
        (text.startsWith("http://") || text.startsWith("https://")) && !text.contains(' ') && text.length > 10
}
