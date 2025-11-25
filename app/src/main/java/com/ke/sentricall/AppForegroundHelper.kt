package com.ke.sentricall

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

object AppForegroundHelper {

    private const val TAG = "AppForegroundHelper"

    /**
     * True if the app has Usage Access permission.
     * Uses AppOpsManager, which is the recommended way.
     */
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )

        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> {
                Log.d(TAG, "hasUsageAccess: MODE_ALLOWED")
                true
            }
            AppOpsManager.MODE_DEFAULT -> {
                // Fallback – check if the permission itself is granted
                val granted =
                    context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                            PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "hasUsageAccess: MODE_DEFAULT, permissionGranted=$granted")
                granted
            }
            else -> {
                Log.d(TAG, "hasUsageAccess: mode=$mode (NOT allowed)")
                false
            }
        }
    }

    /**
     * Opens the system screen where the user can grant Usage Access.
     * We cannot auto-grant this – Android forces a manual toggle.
     */
    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Returns the package name of the most recent app that moved to foreground.
     * Requires Usage Access to be granted.
     */
    fun getCurrentForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000L // last 10 seconds

        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var lastPackage: String? = null

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }

        Log.d(TAG, "Foreground package = $lastPackage")
        return lastPackage
    }
}