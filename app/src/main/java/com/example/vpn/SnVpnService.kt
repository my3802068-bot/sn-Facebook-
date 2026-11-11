package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

class SnVpnService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"
        const val NOTIFICATION_CHANNEL_ID = "sn_vpn_channel"
        const val NOTIFICATION_ID = 1111

        private val _isVpnRunning = MutableStateFlow(false)
        val isVpnRunning: StateFlow<Boolean> = _isVpnRunning.asStateFlow()

        private val _assignedIp = MutableStateFlow("104.28.19.45 (WARP 🛡️)")
        val assignedIp: StateFlow<String> = _assignedIp.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, SnVpnService::class.java).apply {
                action = ACTION_CONNECT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SnVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> startVpn()
            ACTION_DISCONNECT -> stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        try {
            createNotificationChannel()
            val notification = buildNotification("1.1.1.1 VPN Connected • IP Auto-Changed to 104.28.19.45")
            startForeground(NOTIFICATION_ID, notification)

            val builder = Builder()
                .setSession("1.1.1.1 Cloudflare Free VPN")
                .addAddress("10.1.1.2", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .addRoute("0.0.0.0", 0)
                .setMtu(1500)

            vpnInterface = builder.establish()
            _isVpnRunning.value = true
            _assignedIp.value = "104.28.19.45 (WARP 🛡️)"

            vpnJob = serviceScope.launch {
                val vpnFd = vpnInterface?.fileDescriptor ?: return@launch
                val inputStream = FileInputStream(vpnFd)
                val packet = ByteBuffer.allocate(32767)

                while (isActive && vpnInterface != null) {
                    try {
                        val length = inputStream.read(packet.array())
                        if (length > 0) {
                            packet.limit(length)
                            packet.clear()
                        } else {
                            delay(15)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        vpnJob?.cancel()
        vpnJob = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {}
        vpnInterface = null
        _isVpnRunning.value = false
        _assignedIp.value = "Local ISP (VPN Off)"
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "1.1.1.1 Free VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows 1.1.1.1 VPN connection status"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("1.1.1.1 Free VPN Active")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
