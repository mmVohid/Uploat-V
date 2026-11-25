package uz.vohid.uploatv.download

import uz.vohid.uploatv.data.MediaInfo
import java.io.File

/**
 * Faqat testlar uchun: o'rnatilsa, haqiqiy yt-dlp o'rniga soxta dvigatel ishlaydi.
 * Oddiy ilovada doim null.
 */
interface TestEngine {
    suspend fun fetch(url: String): MediaInfo

    /** Dvigatelning "eng so'nggi" versiyasi; null bo'lsa yangilash taklifi ko'rsatilmaydi. */
    suspend fun engineVersion(): String? = null

    /** [dir] ichiga natija faylini yozadi; xato bo'lsa istisno tashlaydi. */
    suspend fun download(url: String, dir: File, onProgress: (Float) -> Unit)
}

object EngineHook {
    @Volatile
    var engine: TestEngine? = null
}
