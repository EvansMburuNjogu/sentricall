// app/src/main/java/com/ke/sentricall/data/local/ClubSettingsDao.kt
package com.ke.sentricall.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClubSettingsDao {

    @Query("SELECT * FROM club_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): ClubSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: ClubSettingsEntity)

    @Query("UPDATE club_settings SET pinHash = :pinHash, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updatePin(pinHash: String?, updatedAt: Long)

    @Query("UPDATE club_settings SET clubModeEnabled = :enabled, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateClubMode(enabled: Boolean, updatedAt: Long)
}