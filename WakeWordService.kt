package com.example.jarvisrealme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.PorcupineManager
import java.io.File
import java.io.FileOutputStream

class WakeWordService : Service() {
    private var porcupine: PorcupineManager? = null
    private val channelId = "jarvis_wake_service"
    private val notificationId = 220

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pending = openIntent?.let {
            PendingIntent.getActivity(this, 1, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("J.A.R.V.I.S. wake word active")
            .setContentText("Microphone is active for the Jarvis keyword. Tap to open.")
            .setContentIntent(pending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(notificationId, notification)
        initializeDetector()
    }

    private fun initializeDetector() {
        try {
            val accessKey = BuildConfig.PICOVOICE_ACCESS_KEY
            if (accessKey.isBlank()) {
                stopSelf()
                return
            }
            val model = File(cacheDir, "jarvis.ppn")
            assets.open("jarvis.ppn").use { input ->
                FileOutputStream(model).use { output -> input.copyTo(output) }
            }
            porcupine = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeywordPath(model.absolutePath)
                .build(this) {
                    val wakeIntent = Intent(this, MainActivity::class.java).apply {
                        action = MainActivity.ACTION_WAKE_DETECTED
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    startActivity(wakeIntent)
                }
            porcupine?.start()
        } catch (_: Exception) {
            stopSelf()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "JARVIS wake-word service", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int =
        START_NOT_STICKY

    override fun onDestroy() {
        try { porcupine?.stop(); porcupine?.delete() } catch (_: Exception) {}
        porcupine = null
        getSharedPreferences("jarvis_v2", MODE_PRIVATE).edit()
            .putBoolean("wake_enabled", false).apply()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
