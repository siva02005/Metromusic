package com.metromusic.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.metromusic.app.data.local.dao.PlaylistDao
import com.metromusic.app.data.local.dao.SearchHistoryDao
import com.metromusic.app.data.local.dao.TrackDao
import com.metromusic.app.data.local.entity.CachedTrackEntity
import com.metromusic.app.data.local.entity.PlaylistEntity
import com.metromusic.app.data.local.entity.PlaylistTrackEntity
import com.metromusic.app.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        CachedTrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MetromusicDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        const val DATABASE_NAME = "metromusic.db"

        fun create(context: Context): MetromusicDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MetromusicDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
