package uz.vohid.uploatv.ui

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.vohid.uploatv.data.SettingsRepository
import uz.vohid.uploatv.download.EngineUpdater

enum class BannerPhase { HIDDEN, AVAILABLE, UPDATING, DONE, FAILED }

data class EngineBannerUi(val phase: BannerPhase = BannerPhase.HIDDEN, val tag: String = "", val message: String = "")

/**
 * Yangi yt-dlp bor-yo'qligini kuzatadi. Bosh sahifa va Sozlamalardagi sariq taklif
 * bitta holatdan foydalanadi: biridan yopilsa yoki yangilansa, ikkinchisida ham shunday bo'ladi.
 */
object EngineWatch {
    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _banner = MutableStateFlow(EngineBannerUi())
    val banner: StateFlow<EngineBannerUi> = _banner.asStateFlow()
    private var lastCheck = 0L

    fun init(context: Context) {
        app = context.applicationContext
    }

    /** Yangi versiya bor bo'lsa taklifni ko'rsatadi (majburiy bo'lmasa 6 soatda bir martadan ko'p tekshirmaydi). */
    fun check(force: Boolean = false) {
        scope.launch {
            val now = System.currentTimeMillis()
            if (_banner.value.phase == BannerPhase.UPDATING) return@launch
            if (!force && now - lastCheck < 6L * 3600 * 1000) return@launch
            val s = SettingsRepository(app).settings.first()
            if (!s.onboardingDone) return@launch
            val latest = EngineUpdater.latestVersion() ?: return@launch
            lastCheck = now
            if (latest == s.ytdlpVersion || latest == s.engineBannerDismissed) {
                if (_banner.value.phase == BannerPhase.AVAILABLE) _banner.value = EngineBannerUi()
            } else if (_banner.value.phase == BannerPhase.HIDDEN || _banner.value.phase == BannerPhase.FAILED) {
                _banner.value = EngineBannerUi(BannerPhase.AVAILABLE, latest)
            }
        }
    }

    fun update() {
        val tag = _banner.value.tag
        if (_banner.value.phase == BannerPhase.UPDATING) return
        _banner.value = EngineBannerUi(BannerPhase.UPDATING, tag)
        scope.launch {
            val r = EngineUpdater.update(app)
            if (r.ok) {
                _banner.value = EngineBannerUi(BannerPhase.DONE, tag)
                delay(2500)
                _banner.value = EngineBannerUi()
            } else {
                _banner.value = EngineBannerUi(BannerPhase.FAILED, tag, r.message)
            }
        }
    }

    fun dismiss() {
        val tag = _banner.value.tag
        _banner.value = EngineBannerUi()
        scope.launch { SettingsRepository(app).setEngineBannerDismissed(tag) }
    }
}
