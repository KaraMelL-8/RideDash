package com.example.ridedash.stream

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.ridedash.MainActivity
import com.example.ridedash.link.FileDashLink
import com.example.ridedash.source.AaSource
import com.example.ridedash.source.OwnRenderSource
import com.example.ridedash.source.SourceMode
import com.example.ridedash.source.SourcePrefs
import com.example.ridedash.source.VideoSource

/**
 * Owns the whole pipeline (VideoSource -> DashLink) so it does NOT depend on the Activity or on the
 * phone screen being on. The source is chosen by the user: own rendering or Android Auto.
 */
class StreamService : Service() {
    companion object {
        const val ACTION_STOP = "com.example.ridedash.STOP"
        private const val CHANNEL = "ridedash_stream"
        private const val NOTIF_ID = 1001
        const val W = 800; const val H = 480; const val DPI = 160 // TODO: real dash resolution
    }

    private var source: VideoSource? = null
    private var link: FileDashLink? = null
    private var cpuLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var rxRegistered = false

    private val screenRx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> StreamStats.setScreenOff(true, System.currentTimeMillis())
                Intent.ACTION_SCREEN_ON -> StreamStats.setScreenOff(false, System.currentTimeMillis())
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopStream(); stopSelf(); return START_NOT_STICKY }
        if (!StreamStats.running) startStream()
        return START_NOT_STICKY
    }

    private fun startStream() {
        StreamStats.reset(System.currentTimeMillis())
        try {
            ServiceCompat.startForeground(
                this, NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } catch (e: Exception) {
            StreamStats.error = "startForeground: ${e.message}"; stopSelf(); return
        }
        acquireLocks()
        val pm = getSystemService(PowerManager::class.java)
        StreamStats.setScreenOff(!pm.isInteractive, System.currentTimeMillis())
        val filter = IntentFilter().apply { addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON) }
        ContextCompat.registerReceiver(this, screenRx, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        rxRegistered = true
        try {
            val l = FileDashLink(this).also { it.connect() }
            link = l; StreamStats.filePath = l.file.path
            val src: VideoSource = if (SourcePrefs.get(this) == SourceMode.AA) AaSource()
            else OwnRenderSource(W, H, DPI) { StreamStats.onTick(System.currentTimeMillis()) }
            source = src
            val err = src.start(this) { bytes, key -> l.sendFrame(bytes, key); StreamStats.onFrame(bytes.size) }
            if (err != null) { StreamStats.error = err; stopStream(); stopSelf(); return }
            StreamStats.running = true
        } catch (t: Throwable) {
            StreamStats.error = t.message ?: t.javaClass.simpleName
            stopStream(); stopSelf()
        }
    }

    private fun stopStream() {
        runCatching { source?.stop() }; source = null
        runCatching { link?.close() }; link = null
        if (rxRegistered) { runCatching { unregisterReceiver(screenRx) }; rxRegistered = false }
        releaseLocks()
        StreamStats.stop(System.currentTimeMillis())
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() { stopStream(); super.onDestroy() }

    private fun acquireLocks() {
        val pm = getSystemService(PowerManager::class.java)
        cpuLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RideDash:stream").apply {
            setReferenceCounted(false); acquire(8 * 60 * 60 * 1000L)
        }
        // TODO(verify on device): behaviour of this lock while the screen is off differs by Android version.
        val wm = applicationContext.getSystemService(WifiManager::class.java)
        wifiLock = wm?.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "RideDash:wifi")?.apply {
            setReferenceCounted(false); acquire()
        }
    }

    private fun releaseLocks() {
        runCatching { cpuLock?.takeIf { it.isHeld }?.release() }; cpuLock = null
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }; wifiLock = null
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "RideDash stream", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(
            this, 1, Intent(this, StreamService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("RideDash: ทดสอบสตรีม (ยังไม่ต่อจอรถ)")
            .setContentText("ปิดหน้าจอมือถือได้ · แตะเพื่อเปิดแอป")
            .setContentIntent(open).addAction(0, "หยุด", stop).setOngoing(true).build()
    }
}
