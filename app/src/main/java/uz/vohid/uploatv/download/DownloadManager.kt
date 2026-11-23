package uz.vohid.uploatv.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
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
import kotlinx.coroutines.flow.first
import uz.vohid.uploatv.UploatApp
import uz.vohid.uploatv.data.SettingsRepository
import java.io.File
import java.util.UUID
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

    /**
     * "Быстрая загрузка": havola ulashilganda ilova ochilmasdan, sozlamalardagi standart
     * tur/sifat bilan orqa fonda yuklashni boshlaydi.
     */
    fun quickDownload(url: String) {
        val id = UUID.randomUUID().toString()
        scope.launch {
            val s = SettingsRepository(app).settings.first()
            val item = DownloadItem(
                id = id,
                url = url,
                title = url,
                thumbnail = null,
                uploader = null,
                quality = s.defaultQuality,
                playlist = false,
                detail = "Ma'lumot olinmoqda…",
            )
            _items.update { listOf(item) + it }
            startService()
            try {
                val noPlaylist = url.contains("v=") && url.contains("list=")
                // processId = id: shu id bilan "Bekor qilish" ma'lumot olishni ham to'xtatadi
                val info = InfoFetcher.fetch(
                    url, noPlaylist, processId = id,
                    cancelled = { get(id)?.status == Status.CANCELED },
                )
                if (get(id)?.status == Status.CANCELED) return@launch
                mutate(id) {
                    it.copy(
                        title = info.title,
                        thumbnail = info.thumbnail,
                        uploader = info.uploader,
                        playlist = info.isPlaylist,
                        detail = "",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                if (get(id)?.status != Status.CANCELED) {
                    mutate(id) { it.copy(status = Status.FAILED, error = friendlyError(e), detail = "") }
                    persist()
                }
                return@launch
            }
            launchJob(id)
        }
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
            EngineUpdater.waitIdle()

            // yt-dlp eskirgan bo'lsa (sayt o'zgargan), dvigatel yangilanib bir marta qayta uriniladi
            EngineUpdater.healing(cancelled = { get(id)?.status == Status.CANCELED }) {
                dir.deleteRecursively()
                dir.mkdirs()
                mutate(id) { it.copy(progress = 0f, detail = "Boshlanmoqda…") }

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
            }

            if (get(id)?.status == Status.CANCELED) return

            mutate(id) { it.copy(detail = "Saqlanmoqda…", progress = 1f) }
            val treeUri = SettingsRepository(app).settings.first().saveTreeUri
            val saved = if (treeUri != null) saveToTree(dir, Uri.parse(treeUri)) else saveToDownloads(dir)
            if (saved.isEmpty()) throw IllegalStateException("Fayl topilmadi")

            mutate(id) {
                it.copy(
                    status = Status.DONE,
                    progress = 1f,
                    detail = if (saved.size > 1) "${saved.size} ta fayl" else "",
                    savedUri = saved.first().first,
                    mime = saved.first().second,
                    savedTo = if (treeUri != null) describeTree(Uri.parse(treeUri)) else DEFAULT_FOLDER,
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

            val h = item.quality.height
            val selector = if (h == null) "bv*+ba/b" else "bv*[height<=$h]+ba/b[height<=$h]/b"
            addOption("-f", selector)
            // Telefonlarda keng qo'llab-quvvatlanadigan h264/aac afzal ko'riladi
            addOption("-S", "res,vcodec:h264,acodec:m4a")
            addOption("--merge-output-format", "mp4")
        }

    private fun listResultFiles(dir: File): List<File> {
        val skip = setOf("part", "ytdl", "temp", "tmp", "json", "jpg", "jpeg", "png", "webp", "vtt", "srt")
        return dir.listFiles { f -> f.isFile && f.extension.lowercase() !in skip }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    private fun mimeOf(file: File): String =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension.lowercase())
            ?: "application/octet-stream"

    /** Foydalanuvchi tanlagan papkaga (SAF) ko'chiradi. */
    private fun saveToTree(dir: File, tree: Uri): List<Pair<String, String>> {
        val resolver = app.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        return listResultFiles(dir).map { file ->
            val mime = mimeOf(file)
            val target = DocumentsContract.createDocument(resolver, parent, mime, file.name)
                ?: throw IllegalStateException("Tanlangan papkaga yozib bo'lmadi. Papkani qayta tanlang")
            resolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: throw IllegalStateException("Faylni yozib bo'lmadi")
            target.toString() to mime
        }
    }

    /** Vaqtinchalik papkadagi tayyor fayllarni MediaStore orqali Download/Uploat-V ga ko'chiradi. */
    private fun saveToDownloads(dir: File): List<Pair<String, String>> {
        val resolver = app.contentResolver
        return listResultFiles(dir).map { file ->
            val mime = mimeOf(file)
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
