// app/src/main/java/com/ke/sentricall/SentricallAccessibilityService.kt
package com.ke.sentricall

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.ke.sentricall.data.local.ClubSettingsDao
import com.ke.sentricall.data.local.ProtectedAppsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Accessibility service that enforces Sentricall Club Mode.
 * - Listens to app changes
 * - If Club Mode is ON and app is protected, shows LockGateActivity
 */
class SentricallAccessibilityService : AccessibilityService() {

    // Scope for background DB work
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Lazy DB + DAOs
    private val db by lazy { SentricallDatabase.getInstance(applicationContext) }
    private val clubSettingsDao: ClubSettingsDao by lazy { db.clubSettingsDao() }
    private val protectedAppsDao: ProtectedAppsDao by lazy { db.protectedAppsDao() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Optionally log or toast here
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // We mostly care about window state changes (foreground app changes)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkgName = event.packageName?.toString() ?: return

        // Ignore our own app so we don't block ourselves
        if (pkgName == applicationContext.packageName) return

        // All DB + heavy work must be in coroutine
        serviceScope.launch {
            // 1. Read club mode settings
            val settings = clubSettingsDao.getSettings()

            // If nothing saved yet or club mode is off, do nothing
            if (settings == null || !settings.clubModeEnabled) {
                return@launch
            }

            // 2. If this package was temporarily unlocked, skip locking
            if (ClubModeState.isTemporarilyUnlocked(pkgName)) {
                return@launch
            }

            // 3. Check if this app is protected
            val protectedApps = protectedAppsDao.getAll()
            val isProtected = protectedApps.any { app ->
                app.packageName == pkgName
            }

            if (!isProtected) {
                // Not in protected list → do nothing
                return@launch
            }

            // 4. If protected and club mode is ON → show lock gate
            withContext(Dispatchers.Main) {
                showLockGate(pkgName)
            }
        }
    }

    override fun onInterrupt() {
        // Required override, nothing special here for now
    }

    private fun showLockGate(blockedPkg: String) {
        val intent = Intent(this, LockGateActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
            putExtra("blocked_package", blockedPkg)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel coroutines when service is destroyed
        serviceScope.cancel()
    }
}