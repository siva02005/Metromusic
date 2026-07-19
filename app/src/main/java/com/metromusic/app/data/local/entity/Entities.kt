package com.metromusic.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_tracks")
data class CachedTrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val thumbnailUrl: String,
    val localPath: String?,
    val source: String,
    val addedAt: Long
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val trackIds: String,
    val thumbnailUrl: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "playlist_track_cross_ref")
data class PlaylistTrackEntity(
    val playlistId: String,
    val trackId: String,
    val position: Int
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
