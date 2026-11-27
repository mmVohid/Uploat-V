package uz.vohid.uploatv.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import uz.vohid.uploatv.data.AppSettings
import kotlinx.coroutines.flow.first
import uz.vohid.uploatv.UploatApp
import kotlinx.coroutines.delay
import uz.vohid.uploatv.data.SettingsRepository
import java.io.File
import java.io.FileOutputStream
import java.io.FileInputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Yuklashlar navbati. Bir vaqtda nechta yuklash ishlashi sozlamalardan olinadi (1–3, standart 2).
 * Natija telefonning `Download/Uploat-V` papkasiga saqlanadi.
 */
object DownloadManager {

    private const val HISTORY_LIMIT = 100
    private const val UI_INTERVAL_MS = 500L
    private val ITEM_REGEX = Regex("""Downloading item (\d+) of (\d+)""")
    private val SPEED_REGEX = Regex("""\bat\s+(\d+(?:\.\d+)?\s?[KMGT]?i?B/s)""")

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val running = MutableStateFlow(0)

    /** Ko'p havola birdan qo'shilganda ma'lumot olish jarayonlari bir vaqtda 2 tadan oshmasin. */
    private val infoGate = Semaphore(2)
    private val jobs = ConcurrentHashMap<String, Job>()

    /** Bo'sh joy kutayotgan yuklashlar tartibi (boshidagisi birinchi navbatda yuklanadi). */
    private val waitQueue = MutableStateFlow<List<String>>(emptyList())
    val queue: StateFlow<List<String>> = waitQueue.asStateFlow()

    /** Navbatdagi yuklashni boshqa o'ringa o'tkazadi (sudrab tartiblash uchun). */
    fun moveInQueue(id: String, toIndex: Int) = waitQueue.update { list ->
        if (id !in list) return@update list
        val m = list.toMutableList()
        m.remove(id)
        m.add(toIndex.coerceIn(0, m.size), id)
        m
    }

    /** Yuklashni navbatning eng boshiga olib chiqadi: bo'sh joy chiqishi bilan birinchi bo'lib yuklanadi. */
    fun moveToFront(id: String) = moveInQueue(id, 0)

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private val netWaiting = ConcurrentHashMap.newKeySet<String>()
    private val autoRetries = ConcurrentHashMap<String, Int>()

    /** Internet yo'qligidan to'xtagan, tarmoq qaytganda o'zi davom etadigan yuklashmi. */
    fun willAutoRetry(id: String) = id in netWaiting

    fun init(context: Context) {
        app = context.applicationContext
        scope.launch {
            val saved = HistoryStore.load(app)
                .map { if (it.isActive) it.copy(status = Status.QUEUED, detail = "Davom ettirilmoqda…") else it }
            val fresh = saved.filter { s -> _items.value.none { it.id == s.id } }
            _items.update { current -> current + fresh }
            // Ilova o'chib qolgan bo'lsa, yarim qolgan yuklashlar qayta boshlanadi (tmp'dagi fayllardan davom etadi)
            val resume = fresh.filter { it.isActive }
            if (resume.isNotEmpty()) {
                runCatching { startService() }
                resume.forEach { launchJob(it.id) }
            }
            cleanup()
        }
        runCatching {
            app.getSystemService(ConnectivityManager::class.java)
                .registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = resumeWaiting()
                })
        }
    }

    /** Internet (yoki Wi‑Fi) qaytganda kutib turgan yuklashlarni qayta ishga tushiradi. */
    private fun resumeWaiting() {
        if (netWaiting.isEmpty()) return
        scope.launch {
            delay(2000)
            netWaiting.toList().forEach { id ->
                netWaiting.remove(id)
                if (get(id)?.status == Status.FAILED) {
                    autoRetries.merge(id, 1) { a, b -> a + b }
                    retry(id)
                }
            }
        }
    }

    /** Eski yozuvlarni (sozlama bo'yicha) va egasiz vaqtinchalik fayllarni tozalaydi. */
    fun cleanup() {
        scope.launch {
            val now = System.currentTimeMillis()
            val period = SettingsRepository(app).settings.first().historyRetention
            period.millis?.let { ms ->
                val old = _items.value.filter { !it.isActive && it.createdAt < now - ms }
                if (old.isNotEmpty()) {
                    val ids = old.map { it.id }.toSet()
                    _items.update { list -> list.filterNot { it.id in ids } }
                    ids.forEach { tmpDir(it).deleteRecursively() }
                    persist()
                }
            }
            val day = 24L * 3600 * 1000
            val live = _items.value.associateBy { it.id }
            File(app.getExternalFilesDir(null) ?: app.filesDir, "tmp").listFiles()?.forEach { dir ->
                val item = live[dir.name]
                val stale = when {
                    item == null -> dir.lastModified() < now - day              // egasiz
                    item.status == Status.FAILED -> dir.lastModified() < now - 7 * day // uzoq yotgan xatolik
                    else -> false
                }
                if (stale) dir.deleteRecursively()
            }
        }
    }

    // ---------- Ommaviy API ----------

    fun enqueue(item: DownloadItem) {
        _items.update { listOf(item) + it }
        persist()
        startService()
        launchJob(item.id)
    }

    /**
     * "Tezkor yuklash": havola ulashilganda ilova ochilmasdan, sozlamalardagi standart
     * tur/sifat bilan orqa fonda yuklashni boshlaydi.
     */
    fun quickDownload(rawUrl: String) {
        val url = UrlCleaner.clean(rawUrl)
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
                incognito = s.incognito,
                detail = "Ma'lumot olinmoqda…",
            )
            _items.update { listOf(item) + it }
            persist()
            startService()
            try {
                val noPlaylist = url.contains("v=") && url.contains("list=")
                // processId = id: shu id bilan "Bekor qilish" ma'lumot olishni ham to'xtatadi
                val info = infoGate.withPermit {
                    if (get(id)?.status == Status.CANCELED) return@launch
                    InfoFetcher.fetch(
                        url, noPlaylist, processId = id,
                        cancelled = { get(id)?.status == Status.CANCELED },
                    )
                }
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
                    mutate(id) { it.copy(status = Status.FAILED, error = friendlyError(e), errorDetail = rawError(e), detail = "") }
                    if (get(id)?.incognito != true) ErrorLog.add(url, rawError(e))
                    persist()
                }
                return@launch
            }
            launchJob(id)
        }
    }

    fun cancel(id: String) {
        netWaiting.remove(id)
        mutate(id) { it.copy(status = Status.CANCELED, detail = "", error = null) }
        runCatching { YoutubeDL.destroyProcessById(id) }
        jobs[id]?.cancel()
        persist()
    }

    /** [quality] berilsa, qayta urinish shu sifatda bo'ladi (masalan, tanlangan sifat mavjud bo'lmaganda). */
    fun retry(id: String, quality: uz.vohid.uploatv.data.Quality? = null) {
        netWaiting.remove(id)
        loginRetried.remove(id)
        mutate(id) {
            it.copy(
                status = Status.QUEUED, progress = 0f, detail = "", error = null,
                quality = quality ?: it.quality,
            )
        }
        startService()
        launchJob(id)
    }

    private fun tmpDir(id: String) = File(app.getExternalFilesDir(null) ?: app.filesDir, "tmp/$id")

    fun remove(id: String) {
        if (get(id)?.isActive == true) cancel(id)
        netWaiting.remove(id)
        attempts.remove(id)
        autoRetries.remove(id)
        scope.launch { tmpDir(id).deleteRecursively() }
        _items.update { list -> list.filterNot { it.id == id } }
        persist()
    }

    /** "O'chirildi" xabaridagi "Qaytarish": element ro'yxatga joyiga qaytadi. */
    fun restore(item: DownloadItem) {
        _items.update { list ->
            if (list.any { it.id == item.id }) list else (list + item).sortedByDescending { it.createdAt }
        }
        persist()
    }

    fun clearFinished() {
        _items.value.filter { !it.isActive }.forEach { netWaiting.remove(it.id); attempts.remove(it.id) }
        _items.value.filter { !it.isActive }.forEach { item -> scope.launch { tmpDir(item.id).deleteRecursively() } }
        _items.update { list -> list.filter { it.isActive } }
        persist()
    }

    // ---------- Ichki ishlash ----------

    private fun get(id: String) = _items.value.firstOrNull { it.id == id }

    private fun mutate(id: String, transform: (DownloadItem) -> DownloadItem) {
        _items.update { list -> list.map { if (it.id == id) transform(it) else it } }
    }

    private fun startService() {
        // Ilova orqa fonda bo'lsa (masalan, tarmoq qaytganda) tizim xizmatni boshlashga ruxsat bermasligi mumkin
        runCatching { ContextCompat.startForegroundService(app, Intent(app, DownloadService::class.java)) }
    }

    private fun persist() {
        scope.launch {
            val finished = _items.value.filter { !it.incognito }.take(HISTORY_LIMIT)
            HistoryStore.save(app, finished)
        }
    }

    private fun launchJob(id: String) {
        jobs[id] = scope.launch {
            try {
                acquireSlot(id)
                try {
                    process(id)
                } finally {
                    releaseSlot()
                }
            } finally {
                jobs.remove(id)
            }
        }
    }

    /** Bo'sh joy chiqquncha kutadi. Chegara sozlamalardan o'qiladi, shuning uchun o'zgarsa darrov hisobga olinadi. */
    private suspend fun acquireSlot(id: String) {
        waitQueue.update { if (id in it) it else it + id }
        try {
            val maxFlow = SettingsRepository(app).settings.map { it.maxParallel }
            while (true) {
                // Faqat navbatda oldinda turganlar (bo'sh joylar soniga teng) joy oladi: tartib saqlanadi
                val max = combine(running, maxFlow, waitQueue) { r, m, q -> Triple(r, m, q) }
                    .first { (r, m, q) -> r < m && q.indexOf(id) in 0 until (m - r) }
                    .second
                val cur = running.value
                if (cur < max && running.compareAndSet(cur, cur + 1)) return
            }
        } finally {
            waitQueue.update { it - id }
        }
    }

    private fun releaseSlot() {
        running.update { (it - 1).coerceAtLeast(0) }
    }

    /** Yuklangan faylni telefondan o'chiradi. Ro'yxatdan olib tashlash alohida [remove] bilan. */
    suspend fun deleteFile(id: String): Boolean = withContext(Dispatchers.IO) {
        val uri = get(id)?.savedUri?.let(Uri::parse) ?: return@withContext false
        runCatching {
            if (DocumentsContract.isDocumentUri(app, uri)) {
                DocumentsContract.deleteDocument(app.contentResolver, uri)
            } else {
                app.contentResolver.delete(uri, null, null) > 0
            }
        }.getOrDefault(false)
    }

    private val loginRetried = ConcurrentHashMap.newKeySet<String>()

    /** Login/cookie xatosida: cookie'ni majburan yangilab, bir marta qayta uriniladi. */
    private suspend fun process(id: String) {
        processOnce(id)
        val item = get(id) ?: return
        if (item.status == Status.FAILED && isLoginError(item.errorDetail.orEmpty()) && loginRetried.add(id)) {
            val enabled = SettingsRepository(app).settings.first().autoCookies
            if (enabled) {
                runCatching { CookieStore.prepareIfEnabled(item.url, force = true) }
                processOnce(id)
            }
        }
    }

    private val attempts = ConcurrentHashMap<String, Int>()

    private suspend fun processOnce(id: String) {
        val item = get(id) ?: return
        if (item.status == Status.CANCELED) return

        netWaiting.remove(id)
        mutate(id) { it.copy(status = Status.RUNNING, progress = 0f, detail = "Boshlanmoqda…", error = null) }
        val dir = File(app.getExternalFilesDir(null) ?: app.filesDir, "tmp/$id")

        // 1-urinish: noldan; 2-urinish: yarim fayldan davom; 3-urinish: eskisi buzuq bo'lishi mumkin, noldan
        val attempt = (attempts[id] ?: 0) + 1
        attempts[id] = attempt
        if (attempt >= 3) {
            dir.deleteRecursively()
            attempts[id] = 1
        }

        try {
            val s = SettingsRepository(app).settings.first()
            Preflight.requireOnline(s.networkMode)
            Preflight.requireStorage()
            UploatApp.engineReady.await()
            EngineUpdater.waitIdle()
            CookieStore.prepareIfEnabled(item.url)

            // yt-dlp eskirgan bo'lsa (sayt o'zgargan), dvigatel yangilanib bir marta qayta uriniladi
            EngineUpdater.healing(cancelled = { get(id)?.status == Status.CANCELED }) {
                dir.mkdirs()
                mutate(id) { it.copy(progress = 0f, detail = "Boshlanmoqda…") }

                var index = 1
                var total = 1
                var speed: String? = null
                var lastUiMs = 0L // ekran yangilanishini sekundiga ~2 martaga cheklaydi
                YoutubeDL.getInstance().execute(buildRequest(item, dir, s), id) { percent, eta, line ->
                    ITEM_REGEX.find(line)?.let {
                        index = it.groupValues[1].toInt()
                        total = it.groupValues[2].toInt()
                    }
                    SPEED_REGEX.find(line)?.let { speed = it.groupValues[1].replace("iB", "B") }
                    val nowMs = android.os.SystemClock.elapsedRealtime()
                    if (percent >= 0f && (nowMs - lastUiMs >= UI_INTERVAL_MS || percent >= 100f)) {
                        lastUiMs = nowMs
                        val overall = if (total > 1) ((index - 1) + percent / 100f) / total else percent / 100f
                        val parts = buildList {
                            if (total > 1) add("$index/$total")
                            speed?.let { add(it) }
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
            val treeUri = s.saveTreeUri
            val saved = if (treeUri != null) saveToTree(dir, Uri.parse(treeUri)) else saveToDownloads(dir)
            if (saved.isEmpty()) throw IllegalStateException("Fayl topilmadi")

            autoRetries.remove(id)
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
                if (isNetworkFailure(e)) {
                    // Internet muammosi: fayllar saqlanadi, urinish hisoblanmaydi, tarmoq qaytganda o'zi davom etadi.
                    // Holat FAILED bo'lishidan oldin belgilanadi: xizmat va xabarlar shunga qarab ishlaydi.
                    attempts[id] = attempt - 1
                    if ((autoRetries[id] ?: 0) < 5) netWaiting.add(id)
                }
                if (!item.incognito) ErrorLog.add(item.url, rawError(e))
                mutate(id) { it.copy(status = Status.FAILED, error = friendlyError(e), errorDetail = rawError(e)) }
            }
        } finally {
            // Xato bo'lsa yarim fayllar saqlanadi: "Qayta urinish" qolgan joyidan davom etadi
            if (get(id)?.status != Status.FAILED) {
                dir.deleteRecursively()
                attempts.remove(id)
            }
            persist()
        }
    }

    private fun buildRequest(item: DownloadItem, dir: File, s: AppSettings): YoutubeDLRequest =
        YoutubeDLRequest(item.url).apply {
            // Playlistda bir xil sarlavhalar bir-birini bosib ketmasligi uchun shablonda [id] bo'lmasa, qo'shiladi
            val base = s.fileNameTemplate.pattern
            val name = if (item.playlist && "%(id)s" !in base) "$base [%(id)s]" else base
            addOption("-o", "${dir.absolutePath}/$name.%(ext)s")
            s.speedLimit.arg?.let { addOption("--limit-rate", it) }
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--concurrent-fragments", s.fragmentThreads.toString())
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--socket-timeout", "20")
            CookieStore.fileFor(item.url)?.let { addOption("--cookies", it.absolutePath) }
            if (item.playlist) {
                addOption("--yes-playlist")
                item.playlistItems?.let { addOption("--playlist-items", it) }
            } else {
                addOption("--no-playlist")
            }

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
            copyFast(file, target)
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
            copyFast(file, uri)
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            uri.toString() to mime
        }
    }

    /** Faylni kernel darajasida (transferTo) ko'chiradi: oddiy oqim nusxasidan bir necha baravar tez. */
    private fun copyFast(file: File, target: Uri) {
        val resolver = app.contentResolver
        val size = file.length()
        var done = false
        runCatching {
            resolver.openFileDescriptor(target, "w")?.use { pfd ->
                FileOutputStream(pfd.fileDescriptor).use { out ->
                    FileInputStream(file).use { input ->
                        val src = input.channel
                        val dst = out.channel
                        var pos = 0L
                        while (pos < size) {
                            val n = src.transferTo(pos, size - pos, dst)
                            if (n <= 0) break
                            pos += n
                        }
                        done = pos >= size
                    }
                }
            }
        }
        if (done) return
        // Zaxira yo'l: katta bufer bilan oddiy nusxa
        resolver.openOutputStream(target, "wt")?.use { out ->
            file.inputStream().use { it.copyTo(out, 1 shl 20) }
        } ?: throw IllegalStateException("Faylni yozib bo'lmadi")
    }
}
