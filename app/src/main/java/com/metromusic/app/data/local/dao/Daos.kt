package com.metromusic.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.metromusic.app.data.local.entity.CachedTrackEntity
import com.metromusic.app.data.local.entity.PlaylistEntity
import com.metromusic.app.data.local.entity.SearchHistoryEntity

@Dao
interface TrackDao {

    @Query("SELECT * FROM cached_tracks ORDER BY addedAt DESC")
    suspend fun getAllTracks(): List<CachedTrackEntity>

    @Query("SELECT * FROM cached_tracks WHERE id = :id")
    suspend fun getTrackById(id: String): CachedTrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: CachedTrackEntity)

    @Query("DELETE FROM cached_tracks WHERE id = :id")
    suspend fun deleteTrack(id: String)

    @Query("SELECT * FROM cached_tracks WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%'")
    suspend fun searchTracks(query: String): List<CachedTrackEntity>
}

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylist(id: String)
}

@Dao
interface SearchHistoryDao {

    @Insert
    suspend fun insertQuery(entity: SearchHistoryEntity)

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentQueries(): List<SearchHistoryEntity>

    @Query("DELETE FROM search_history")
    suspend fun clearHistory()
}
