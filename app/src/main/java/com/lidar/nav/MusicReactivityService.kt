package com.lidar.nav

import android.app.Service
import android.content.Intent
import android.media.audiofx.Visualizer
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MusicReactivityService : Service() {

    private var visualizer: Visualizer? = null

    companion object {
        const val ACTION_MUSIC_INTENSITY_UPDATE = "com.lidar.nav.MUSIC_INTENSITY_UPDATE"
        const val EXTRA_INTENSITY = "intensity"
    }

    override fun onCreate() {
        super.onCreate()
        setupVisualizer()
    }

    private fun setupVisualizer() {
        try {
            // session 0 implies the global output mix
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(
                        visualizer: Visualizer?,
                        waveform: ByteArray?,
                        samplingRate: Int
                    ) {
                        waveform?.let { processAudioData(it) }
                    }

                    override fun onFftDataCapture(
                        visualizer: Visualizer?,
                        fft: ByteArray?,
                        samplingRate: Int
                    ) {
                        // Using Fast Fourier Transform data is generally better for beat detection
                        fft?.let { processFftData(it) }
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true)
                
                enabled = true
            }
            Log.d("MusicReactivity", "Visualizer initialized on global mix.")
        } catch (e: Exception) {
            Log.e("MusicReactivity", "Failed to initialize visualizer. Check RECORD_AUDIO permissions.", e)
        }
    }

    private fun processAudioData(waveform: ByteArray) {
        // Fallback if needed, but we rely on FFT.
    }

    private fun processFftData(fft: ByteArray) {
        var preProcessedMag = 0f
        val n: Int = fft.size
        
        // Summing magnitudes of lower frequencies (bass/beats)
        for (i in 2 until n / 4 step 2) {
            val r = fft[i]
            val i_val = fft[i + 1]
            val mag = Math.hypot(r.toDouble(), i_val.toDouble()).toFloat()
            preProcessedMag += mag
        }

        // Normalize
        val intensity = (preProcessedMag / 8000f).coerceIn(0f, 1f)
        
        val intent = Intent(ACTION_MUSIC_INTENSITY_UPDATE).apply {
            putExtra(EXTRA_INTENSITY, intensity)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        visualizer?.enabled = false
        visualizer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
