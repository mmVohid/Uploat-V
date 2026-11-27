package uz.vohid.uploatv.download

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import uz.vohid.uploatv.data.DoneVibration

/** Tebranish signali (yuklashlar tugaganda va sozlamalarda namuna sifatida). */
object VibrationPlayer {
    fun play(context: Context, mode: DoneVibration) {
        val pattern = mode.pattern ?: return
        runCatching {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
            if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        }
    }
}
