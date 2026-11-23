package uz.vohid.uploatv.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.AppSettings
import uz.vohid.uploatv.data.AudioFormat
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.data.ThemeMode

data class EngineUi(
    val version: String? = null,
    val updating: Boolean = false,
    val message: String? = null,
)

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SettingsRepository(app)

    val settings: StateFlow<AppSettings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    private val _engine = MutableStateFlow(EngineUi())
    val engine: StateFlow<EngineUi> = _engine.asStateFlow()

    init {
        loadVersion()
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { repo.setTheme(mode) }
    fun setDynamicColor(on: Boolean) = viewModelScope.launch { repo.setDynamicColor(on) }
    fun setDefaultAudio(on: Boolean) = viewModelScope.launch { repo.setDefaultAudio(on) }
    fun setDefaultQuality(q: Quality) = viewModelScope.launch { repo.setDefaultQuality(q) }
    fun setAudioFormat(f: AudioFormat) = viewModelScope.launch { repo.setAudioFormat(f) }

    private fun loadVersion() {
        viewModelScope.launch {
            val v = withContext(Dispatchers.IO) {
                runCatching {
                    UploatApp.engineReady.await()
                    YoutubeDL.getInstance().version(getApplication())
                }.getOrNull()
            }
            _engine.update { it.copy(version = v ?: "noma'lum") }
        }
    }

    fun updateEngine() {
        if (_engine.value.updating) return
        _engine.update { it.copy(updating = true, message = null) }
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) {
                runCatching {
                    UploatApp.engineReady.await()
                    val status = YoutubeDL.getInstance().updateYoutubeDL(getApplication())
                    if (status?.toString()?.contains("ALREADY", ignoreCase = true) == true) {
                        "Allaqachon eng so'nggi versiya"
                    } else {
                        "Yangilandi"
                    }
                }.getOrElse { "Yangilab bo'lmadi. Internetni tekshiring" }
            }
            _engine.update { it.copy(updating = false, message = msg) }
            loadVersion()
        }
    }
}
