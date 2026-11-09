package com.bumbaiya.lanplay.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the host's game alive while the screen is off or the app is in the background.
 * Android kills background apps quickly; a foreground service with a notification is how a
 * server is allowed to keep running.
 */
class HostService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val room = intent?.getStringExtra(EXTRA_ROOM) ?: "?"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Hosting a game", NotificationManager.IMPORTANCE_LOW))
        val note: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hosting room $room")
            .setContentText("LanPlay is keeping your game running")
            .setOngoing(true)
            .build()
        runCatching { startForeground(1, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE) }
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL = "hosting"
        private const val EXTRA_ROOM = "room"

        fun start(context: Context, room: String) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, HostService::class.java).putExtra(EXTRA_ROOM, room)) }
        }
        fun stop(context: Context) { context.stopService(Intent(context, HostService::class.java)) }
    }
}
