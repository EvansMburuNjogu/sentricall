package com.ke.sentricall

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that:
 * - Polls the current foreground app
 * - If it is in the protected list -> opens Sentricall lock screen
 *
 * Runs as a FOREGROUND SERVICE on Android 8+ with a persistent notification,
 * using foregroundServiceType="dataSync" in the manifest.
 */
class AppLockService : Service() {

    companion object {
        private const val TAG = "AppLockService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "club_mode_watcher"

        @Volatile
        var protectedPackages: Set<String> = emptySet()
    }

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.Default + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        createNotificationChannelIfNeeded()
        val notification = buildNotification()
        // IMPORTANT: This is what triggered the MissingForegroundServiceTypeException before.
        // Now the manifest has foregroundServiceType="dataSync" so it's valid.
        startForeground(NOTIFICATION_ID, notification)

        startWatching()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand, protectedPackages = $protectedPackages")
        // If the service is restarted, the watcher loop is still active from onCreate.
        // Just return sticky so the system restarts it if killed.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
        serviceJob.cancel()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sentricall Club mode",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Watches which apps are open to protect Club mode apps."
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // you can change this icon
            .setContentTitle("Sentricall Club mode")
            .setContentText("Club mode is protecting your apps.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun startWatching() {
        scope.launch {
            var lastBlockedPkg: String? = null

            while (isActive) {
                val hasAccess = AppForegroundHelper.hasUsageAccess(this@AppLockService)
                Log.d(TAG, "hasUsageAccess = $hasAccess, protectedPackages = $protectedPackages")

                if (!hasAccess) {
                    // No permission → wait and retry
                    delay(2_000)
                    continue
                }

                val pkg = AppForegroundHelper.getCurrentForegroundPackage(this@AppLockService)
                Log.d(TAG, "current foreground pkg = $pkg")

                if (
                    pkg != null &&
                    pkg != packageName &&                  // ignore Sentricall itself
                    protectedPackages.contains(pkg) &&
                    pkg != lastBlockedPkg                  // prevent spamming same app
                ) {
                    lastBlockedPkg = pkg
                    Log.d(TAG, "Blocking app: $pkg")
                    openLockScreenForPackage(pkg)
                }

                delay(800) // polling interval
            }
        }
    }

    private fun openLockScreenForPackage(blockedPackage: String) {
        val intent = Intent(this, LockGateActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra("blocked_package", blockedPackage)
        }
        startActivity(intent)
    }
}