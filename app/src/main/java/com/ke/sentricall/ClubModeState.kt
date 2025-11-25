package com.ke.sentricall

object ClubModeState {
    @Volatile
    var enabled: Boolean = false

    @Volatile
    var protectedPackages: Set<String> = emptySet()

    // simple in-memory “temporary unlock” list
    private val tempUnlocks = mutableSetOf<String>()

    @Synchronized
    fun markTemporarilyUnlocked(pkg: String) {
        tempUnlocks.add(pkg)
    }

    @Synchronized
    fun isTemporarilyUnlocked(pkg: String): Boolean {
        return tempUnlocks.contains(pkg)
    }

    @Synchronized
    fun clearTemporaryUnlock(pkg: String) {
        tempUnlocks.remove(pkg)
    }
}