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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.download.EngineUpdater
import uz.vohid.uploatv.data.AppSettings
import uz.vohid.uploatv.data.Quality
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.data.ThemeMode

data class EngineUi(
    val version: String? = null,
    val updating: Boolean = false,
    val message: String? = null,
    val messageOk: Boolean = true,
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
    fun setDefaultQuality(q: Quality) = viewModelScope.launch { repo.setDefaultQuality(q) }
    fun setNotifyOnDone(on: Boolean) = viewModelScope.launch { repo.setNotifyOnDone(on) }
    fun setSaveTree(uri: String?) = viewModelScope.launch { repo.setSaveTree(uri) }

    private fun loadVersion() {
        viewModelScope.launch {
            // Ilova o'zi yangilagan versiya bo'lsa shuni, bo'lmasa ilova ichidagi versiyani ko'rsatamiz
            val own = repo.settings.first().ytdlpVersion
            val v = own ?: withContext(Dispatchers.IO) {
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
            val result = EngineUpdater.update(getApplication())
            _engine.update { it.copy(updating = false, message = result.message, messageOk = result.ok) }
            loadVersion()
        }
    }

    fun setAutoUpdate(on: Boolean) = viewModelScope.launch { repo.setAutoUpdate(on) }

    /**
     * Birinchi ishga tushishdagi so'rov javobi. [accepted] bo'lsa yt-dlp avtomatik yangilanadi
     * va hozir ham bir marta yangilanadi.
     */
    fun finishOnboarding(accepted: Boolean) {
        viewModelScope.launch { repo.finishOnboarding(autoUpdate = accepted) }
        if (accepted) EngineUpdater.updateInBackground(getApplication())
    }
}
