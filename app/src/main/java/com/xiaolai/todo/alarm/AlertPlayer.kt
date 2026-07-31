package com.xiaolai.todo.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object AlertPlayer {
    @Volatile
    private var player: MediaPlayer? = null

    fun play(context: Context, durationMs: Long = 4_000L) {
        stop()
        vibrate(context)

        val appContext = context.applicationContext
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return

        runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(appContext, uri)
                isLooping = true
                setOnPreparedListener {
                    start()
                    android.os.Handler(appContext.mainLooper).postDelayed({
                        stop()
                    }, durationMs)
                }
                prepareAsync()
                player = this
            }
        }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 600), -1),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 400, 200, 400, 200, 600), -1)
        }
    }
}
