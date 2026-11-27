package uz.vohid.uploatv.download

import android.content.Context
import java.io.File

/**
 * Playlistdan yuklangan videolar yodlanadigan ro'yxat (yt-dlp `--download-archive`).
 * Playlist qayta yuklanganda yodda borlar o'tkazib yuboriladi, faqat yangilari tushadi.
 */
object PlaylistArchive {
    fun file(context: Context): File = File(context.filesDir, "playlist-archive.txt")

    /** Yodlangan videolar soni. */
    fun count(context: Context): Int = runCatching {
        file(context).useLines { lines -> lines.count { it.isNotBlank() } }
    }.getOrDefault(0)

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }
}
