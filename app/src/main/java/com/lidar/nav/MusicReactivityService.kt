package com.lidar.nav

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MusicReactivityService : Service() {

    private var visualizer: Visualizer? = null

    companion object {
        const val ACTION_MUSIC_INTENSITY_UPDATE = "com.lidar.nav.MUSIC_INTENSITY_UPDATE"
        const val EXTRA_INTENSITY = "intensity"
        private const val CHANNEL_ID = "music_reactivity"
        private const val NOTIF_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundWithNotification()
        setupVisualizer()
    }

    private fun startForegroundWithNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Music Reactivity",
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Meridian")
            .setContentText("Audio reactive grid active")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setSilent(true)
            .build()
        startForeground(NOTIF_ID, notification)
    }

    private fun setupVisualizer() {
        try {
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, rate: Int) {}
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, rate: Int) {
                        fft?.let { processFftData(it) }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                enabled = true
            }
            Log.d("MusicReactivity", "Visualizer started on global mix")
        } catch (e: Exception) {
            Log.e("MusicReactivity", "Visualizer init failed — RECORD_AUDIO granted?", e)
        }
    }

    private fun processFftData(fft: ByteArray) {
        var mag = 0f
        for (i in 2 until fft.size / 4 step 2) {
            val r = fft[i].toFloat()
            val im = fft[i + 1].toFloat()
            mag += Math.hypot(r.toDouble(), im.toDouble()).toFloat()
        }
        val intensity = (mag / 8000f).coerceIn(0f, 1f)
        LocalBroadcastManager.getInstance(this).sendBroadcast(
            Intent(ACTION_MUSIC_INTENSITY_UPDATE).putExtra(EXTRA_INTENSITY, intensity)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer?.enabled = false
        visualizer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
