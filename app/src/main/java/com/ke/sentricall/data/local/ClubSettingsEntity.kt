package com.ke.sentricall.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "club_settings")
data class ClubSettingsEntity(
    @PrimaryKey
    val id: Long = 1L,
    val clubModeEnabled: Boolean = false,
    val pinHash: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)