// app/src/main/java/com/ke/sentricall/data/local/ProtectedAppsDao.kt
package com.ke.sentricall.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProtectedAppsDao {

    @Query("SELECT * FROM protected_apps ORDER BY label ASC")
    suspend fun getAll(): List<ProtectedAppEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ProtectedAppEntity>)

    @Delete
    suspend fun delete(item: ProtectedAppEntity)

    @Query("DELETE FROM protected_apps")
    suspend fun clearAll()
}