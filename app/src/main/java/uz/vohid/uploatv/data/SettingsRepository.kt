package uz.vohid.uploatv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val AUDIO = booleanPreferencesKey("default_audio")
        val QUALITY = stringPreferencesKey("default_quality")
        val AUDIO_FORMAT = stringPreferencesKey("audio_format")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME].toEnum(ThemeMode.SYSTEM),
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            defaultAudio = p[Keys.AUDIO] ?: false,
            defaultQuality = p[Keys.QUALITY].toEnum(Quality.P720),
            audioFormat = p[Keys.AUDIO_FORMAT].toEnum(AudioFormat.M4A),
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDynamicColor(on: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = on }
    suspend fun setDefaultAudio(on: Boolean) = context.dataStore.edit { it[Keys.AUDIO] = on }
    suspend fun setDefaultQuality(q: Quality) = context.dataStore.edit { it[Keys.QUALITY] = q.name }
    suspend fun setAudioFormat(f: AudioFormat) = context.dataStore.edit { it[Keys.AUDIO_FORMAT] = f.name }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
