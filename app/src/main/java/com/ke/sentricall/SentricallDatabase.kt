package com.ke.sentricall

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ke.sentricall.data.local.ClubSettingsDao
import com.ke.sentricall.data.local.ClubSettingsEntity
import com.ke.sentricall.data.local.ProtectedAppEntity
import com.ke.sentricall.data.local.ProtectedAppsDao

@Database(
    entities = [
        ProtectedAppEntity::class,
        ClubSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SentricallDatabase : RoomDatabase() {

    abstract fun protectedAppsDao(): ProtectedAppsDao
    abstract fun clubSettingsDao(): ClubSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SentricallDatabase? = null

        fun getInstance(context: Context): SentricallDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SentricallDatabase::class.java,
                    "sentricall.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}