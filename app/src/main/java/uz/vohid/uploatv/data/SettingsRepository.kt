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
        val QUALITY = stringPreferencesKey("default_quality")
        val SAVE_TREE = stringPreferencesKey("save_tree_uri")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME].toEnum(ThemeMode.SYSTEM),
            dynamicColor = p[Keys.DYNAMIC] ?: true,
            defaultQuality = p[Keys.QUALITY].toEnum(Quality.BEST),
            saveTreeUri = p[Keys.SAVE_TREE],
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDynamicColor(on: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = on }
    suspend fun setDefaultQuality(q: Quality) = context.dataStore.edit { it[Keys.QUALITY] = q.name }

    /** null — standart papka (Download/Uploat-V). */
    suspend fun setSaveTree(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.SAVE_TREE) else it[Keys.SAVE_TREE] = uri
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
