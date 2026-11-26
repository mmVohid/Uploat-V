package uz.vohid.uploatv.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val QUALITY = stringPreferencesKey("default_quality")
        val SAVE_TREE = stringPreferencesKey("save_tree_uri")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update_ytdlp")
        val LAST_UPDATE = longPreferencesKey("last_update_ms")
        val YTDLP_VERSION = stringPreferencesKey("ytdlp_version")
        val NOTIFY_DONE = booleanPreferencesKey("notify_on_done")
        val INCOGNITO = booleanPreferencesKey("incognito")
        val AUTO_COOKIES = booleanPreferencesKey("auto_cookies")
        val YTDLP_NIGHTLY = booleanPreferencesKey("ytdlp_nightly")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only") // eski sozlama: faqat ko'chirish uchun
        val NETWORK_MODE = stringPreferencesKey("network_mode")
        val ENGINE_BANNER_DISMISSED = stringPreferencesKey("engine_banner_dismissed")
        val RETENTION = stringPreferencesKey("history_retention")
        val CLIP_SHORTCUT = booleanPreferencesKey("clip_shortcut")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[Keys.THEME].toEnum(ThemeMode.SYSTEM),
            defaultQuality = p[Keys.QUALITY].toEnum(Quality.BEST),
            saveTreeUri = p[Keys.SAVE_TREE],
            loaded = true,
            onboardingDone = p[Keys.ONBOARDING] ?: false,
            autoUpdateYtdlp = p[Keys.AUTO_UPDATE] ?: false,
            lastUpdateMs = p[Keys.LAST_UPDATE] ?: 0L,
            ytdlpVersion = p[Keys.YTDLP_VERSION],
            notifyOnDone = p[Keys.NOTIFY_DONE] ?: true,
            incognito = p[Keys.INCOGNITO] ?: false,
            autoCookies = p[Keys.AUTO_COOKIES] ?: false,
            ytdlpNightly = p[Keys.YTDLP_NIGHTLY] ?: false,
            networkMode = p[Keys.NETWORK_MODE].toEnum(
                if (p[Keys.WIFI_ONLY] == true) NetworkMode.WIFI else NetworkMode.ANY,
            ),
            engineBannerDismissed = p[Keys.ENGINE_BANNER_DISMISSED] ?: "",
            historyRetention = p[Keys.RETENTION].toEnum(Retention.FOREVER),
            clipShortcut = p[Keys.CLIP_SHORTCUT] ?: false,
        )
    }

    /** Birinchi ishga tushishdagi so'rov yakunlandi; [autoUpdate] — yt-dlp yangilashga ruxsat. */
    suspend fun finishOnboarding(autoUpdate: Boolean, autoCookies: Boolean) = context.dataStore.edit {
        it[Keys.ONBOARDING] = true
        it[Keys.AUTO_UPDATE] = autoUpdate
        it[Keys.AUTO_COOKIES] = autoCookies
    }
    suspend fun setYtdlpNightly(on: Boolean) = context.dataStore.edit { it[Keys.YTDLP_NIGHTLY] = on }
    suspend fun setEngineBannerDismissed(tag: String) = context.dataStore.edit { it[Keys.ENGINE_BANNER_DISMISSED] = tag }
    suspend fun setNetworkMode(m: NetworkMode) = context.dataStore.edit { it[Keys.NETWORK_MODE] = m.name }
    suspend fun setRetention(r: Retention) = context.dataStore.edit { it[Keys.RETENTION] = r.name }
    suspend fun setClipShortcut(on: Boolean) = context.dataStore.edit { it[Keys.CLIP_SHORTCUT] = on }
    suspend fun setAutoCookies(on: Boolean) = context.dataStore.edit { it[Keys.AUTO_COOKIES] = on }

    suspend fun setAutoUpdate(on: Boolean) = context.dataStore.edit { it[Keys.AUTO_UPDATE] = on }
    suspend fun setLastUpdate(ms: Long) = context.dataStore.edit { it[Keys.LAST_UPDATE] = ms }
    suspend fun setIncognito(on: Boolean) = context.dataStore.edit { it[Keys.INCOGNITO] = on }
    suspend fun setNotifyOnDone(on: Boolean) = context.dataStore.edit { it[Keys.NOTIFY_DONE] = on }
    suspend fun setYtdlpVersion(v: String) = context.dataStore.edit { it[Keys.YTDLP_VERSION] = v }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }
    suspend fun setDefaultQuality(q: Quality) = context.dataStore.edit { it[Keys.QUALITY] = q.name }

    /** null — standart papka (Download/Uploat-V). */
    suspend fun setSaveTree(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.SAVE_TREE) else it[Keys.SAVE_TREE] = uri
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T =
    this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: default
