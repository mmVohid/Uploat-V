package uz.vohid.uploatv.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uz.vohid.uploatv.UploatApp
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Yuklashlar navbati. Bir vaqtda 2 tagacha yuklash ishlaydi.
 * Natija telefonning `Download/Uploat-V` papkasiga saqlanadi.
 */
object DownloadManager {

    private const val MAX_PARALLEL = 2
    private const val HISTORY_LIMIT = 100
    private val ITEM_REGEX = Regex("""Downloading item (\d+) of (\d+)""")

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(MAX_PARALLEL)
    private val jobs = ConcurrentHashMap<String, Job>()

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    fun init(context: Context) {
        app = context.applicationContext
        scope.launch {
            val saved = HistoryStore.load(app)
            _items.update { current -> current + saved.filter { s -> current.none { it.id == s.id } } }
        }
    }

    // ---------- Ommaviy API ----------

    fun enqueue(item: DownloadItem) {
        _items.update { listOf(item) + it }
        startService()
        launchJob(item.id)
    }

    fun cancel(id: String) {
        mutate(id) { it.copy(status = Status.CANCELED, detail = "", error = null) }
        runCatching { YoutubeDL.destroyProcessById(id) }
        jobs[id]?.cancel()
        persist()
    }

    fun retry(id: String) {
        mutate(id) { it.copy(status = Status.QUEUED, progress = 0f, detail = "", error = null) }
        startService()
        launchJob(id)
    }

    fun remove(id: String) {
        if (get(id)?.isActive == true) cancel(id)
        _items.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    fun clearFinished() {
        _items.update { list -> list.filter { it.isActive } }
        persist()
    }

    // ---------- Ichki ishlash ----------

    private fun get(id: String) = _items.value.firstOrNull { it.id == id }

    private fun mutate(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun startService() {
        ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java))
    }

    private fun persist() {
        scope.launch {
            val finished = _items.value.filter { !it.isActive }.take(HISTORY_LIMIT)
            HistoryStore.save(app, finished)
        }
    }

    private fun launchJob(id: String) {
        jobs[id] = scope.launch {
            try {
                gate.withPermit { process(id) }
            } finally {
                jobs.remove(id)
            }
        }
    }

    private suspend fun process(id: String) {
        val item = get(id) ?: return
        if (item.status == Status.CANCELED) return

        mutate(id) { it.copy(status = Status.RUNNING, progress = 0f, detail = "Boshlanmoqda…", error = null) }
        val dir = File(app.getExternalFilesDir(null) ?: app.filesDir, "tmp/$id")

        try {
            UploatApp.engineReady.await()
            dir.deleteRecursively()
            dir.mkdirs()

            var index = 1
            var total = 1
            YoutubeDL.getInstance().execute(buildRequest(item, dir), id) { percent, eta, line ->
                ITEM_REGEX.find(line)?.let {
                    index = it.groupValues[1].toInt()
                    total = it.groupValues[2].toInt()
                }
                if (percent >= 0f) {
                    val overall = if (total > 1) ((index - 1) + percent / 100f) / total else percent / 100f
                    val parts = buildList {
                        if (total > 1) add("$index/$total")
                        if (eta > 0) add("${formatEta(eta)} qoldi")
                    }
                    mutate(id) {
                        it.copy(
                            progress = maxOf(it.progress, overall.coerceIn(0f, 1f)),
                            detail = parts.joinToString(" · "),
                        )
                    }
                }
            }

            if (get(id)?.status == Status.CANCELED) return

            mutate(id) { it.copy(detail = "Saqlanmoqda…", progress = 1f) }
            val saved = saveToDownloads(dir)
            if (saved.isEmpty()) throw IllegalStateException("Fayl topilmadi")

            mutate(id) {
                it.copy(
                    status = Status.DONE,
                    progress = 1f,
                    detail = if (saved.size > 1) "${saved.size} ta fayl" else "",
                    savedUri = saved.first().first,
                    mime = saved.first().second,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (get(id)?.status != Status.CANCELED) {
                mutate(id) { it.copy(status = Status.FAILED, error = friendlyError(e)) }
            }
        } finally {
            dir.deleteRecursively()
            persist()
        }
    }

    private fun buildRequest(item: DownloadItem, dir: File): YoutubeDLRequest =
        YoutubeDLRequest(item.url).apply {
            addOption("-o", "${dir.absolutePath}/%(title).80s [%(id)s].%(ext)s")
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--concurrent-fragments", "4")
            addOption(if (item.playlist) "--yes-playlist" else "--no-playlist")

            if (item.audio) {
                val selector = if (item.audioFormat.ext == "m4a") "bestaudio[ext=m4a]/bestaudio/best" else "bestaudio/best"
                addOption("-f", selector)
                addOption("-x")
                addOption("--audio-format", item.audioFormat.ext)
                addOption("--audio-quality", "0")
            } else {
                val h = item.quality.height
                val selector = if (h == null) "bv*+ba/b" else "bv*[height<=$h]+ba/b[height<=$h]/b"
                addOption("-f", selector)
                // Telefonlarda keng qo'llab-quvvatlanadigan h264/aac afzal ko'riladi
                addOption("-S", "res,vcodec:h264,acodec:m4a")
                addOption("--merge-output-format", "mp4")
            }
        }

    /** Vaqtinchalik papkadagi tayyor fayllarni MediaStore orqali Download/Uploat-V ga ko'chiradi. */
    private fun saveToDownloads(dir: File): List<Pair<String, String>> {
        val skip = setOf("part", "ytdl", "temp", "tmp", "json", "jpg", "jpeg", "png", "webp", "vtt", "srt")
        val files = dir.listFiles { f -> f.isFile && f.extension.lowercase() !in skip }
            ?.sortedBy { it.name }
            .orEmpty()

        val resolver = app.contentResolver
        return files.map { file ->
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
                ?: "application/octet-stream"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/Uploat-V")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Faylni saqlab bo'lmadi")
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IllegalStateException("Faylni yozib bo'lmadi")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            uri.toString() to mime
        }
    }
}
