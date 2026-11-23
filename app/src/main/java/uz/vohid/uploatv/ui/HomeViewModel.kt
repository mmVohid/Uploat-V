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
import kotlinx.coroutines.launch
import uz.vohid.uploatv.data.AudioFormat
import uz.vohid.uploatv.data.MediaInfo
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.download.DownloadItem
import uz.vohid.uploatv.download.DownloadManager
import uz.vohid.uploatv.download.InfoFetcher
import uz.vohid.uploatv.download.friendlyError
import java.util.UUID

data class HomeUi(
    val url: String = "",
    val fetching: Boolean = false,
    val info: MediaInfo? = null,
    val error: String? = null,
    val audio: Boolean = false,
    val quality: Quality = Quality.P720,
    val audioFormat: AudioFormat = AudioFormat.M4A,
    val wholePlaylist: Boolean = false,
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

    init {
        viewModelScope.launch {
            val s = settings.settings.first()
            _ui.update { it.copy(audio = s.defaultAudio, quality = s.defaultQuality, audioFormat = s.audioFormat) }
        }
    }

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

    fun setAudio(audio: Boolean) = _ui.update { it.copy(audio = audio) }
    fun setQuality(q: Quality) = _ui.update { it.copy(quality = q) }
    fun setAudioFormat(f: AudioFormat) = _ui.update { it.copy(audioFormat = f) }

    fun download(): Boolean {
        val s = _ui.value
        val info = s.info ?: return false
        DownloadManager.enqueue(
            DownloadItem(
                id = UUID.randomUUID().toString(),
                url = s.url.trim(),
                title = info.title,
                thumbnail = info.thumbnail,
                uploader = info.uploader,
                audio = s.audio,
                quality = s.quality,
                audioFormat = s.audioFormat,
                playlist = info.isPlaylist,
            )
        )
        _ui.update { it.copy(url = "", info = null, error = null, fetching = false, wholePlaylist = false) }
        return true
    }

    private fun startFetch(immediate: Boolean) {
        fetchJob = viewModelScope.launch {
            if (!immediate) delay(700)
            val s = _ui.value
            val url = s.url.trim()
            _ui.update { it.copy(fetching = true, error = null) }
            try {
                val info = InfoFetcher.fetch(url, noPlaylist = s.hasVideoAndList && !s.wholePlaylist)
                _ui.update { cur ->
                    val max = info.maxHeight
                    val allowed = Quality.entries.filter { q ->
                        val h = q.height
                        h == null || max == null || h <= max
                    }
                    val q = if (cur.quality in allowed) cur.quality else allowed.firstOrNull { it.height != null } ?: Quality.BEST
                    cur.copy(fetching = false, info = info, quality = q)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Yangi kiritish tufayli bekor qilingan bo'lsa, xato ko'rsatilmaydi
                currentCoroutineContext().ensureActive()
                _ui.update { it.copy(fetching = false, error = friendlyError(e)) }
            }
        }
    }

    private fun looksLikeUrl(text: String) =
        (text.startsWith("http://") || text.startsWith("https://")) && !text.contains(' ') && text.length > 10
}
