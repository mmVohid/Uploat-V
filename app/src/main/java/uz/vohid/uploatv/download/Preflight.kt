package uz.vohid.uploatv.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.StatFs

class OfflineException : Exception("Internet yo'q. Ulanishni tekshiring")

class StorageLowException(freeMb: Long) :
    Exception("Xotira yetarli emas: ${freeMb} MB bo'sh qoldi. Joy bo'shatib qayta urinib ko'ring")

/** Yuklashdan oldingi tekshiruvlar: internet va bo'sh joy. */
object Preflight {
    private const val MIN_FREE_BYTES = 300L * 1024 * 1024
    private lateinit var app: Context

    fun init(context: Context) {
        app = context.applicationContext
    }

    fun requireOnline() {
        val cm = app.getSystemService(ConnectivityManager::class.java)
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        if (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) throw OfflineException()
    }

    fun requireStorage() {
        val dir = app.getExternalFilesDir(null) ?: app.filesDir
        val free = StatFs(dir.path).availableBytes
        if (free < MIN_FREE_BYTES) throw StorageLowException(free / (1024 * 1024))
    }
}
