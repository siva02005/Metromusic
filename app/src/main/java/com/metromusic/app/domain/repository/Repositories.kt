package com.metromusic.app.domain.repository

import com.metromusic.app.domain.model.AlbumInfo
import com.metromusic.app.domain.model.ArtistInfo
import com.metromusic.app.domain.model.Playlist
import com.metromusic.app.domain.model.SearchResult
import com.metromusic.app.domain.model.Track

interface MusicRepository {
    suspend fun search(query: String, page: Int = 1): SearchResult
    suspend fun getTrack(streamUrl: String): Track?
    suspend fun getTrending(): List<Track>
    suspend fun getCharts(): List<Track>
    suspend fun getArtistTracks(artistId: String): List<Track>
    suspend fun getAlbumTracks(albumId: String): List<Track>
    suspend fun getStreamUrl(trackId: String): Result<String>
    suspend fun getSuggestions(query: String): List<String>
}

interface LocalMusicRepository {
    suspend fun getAllTracks(): List<Track>
    suspend fun getTrackById(id: String): Track?
    suspend fun saveTrack(track: Track)
    suspend fun deleteTrack(id: String)
    suspend fun searchTracks(query: String): List<Track>
}

interface PlaylistRepository {
    suspend fun getAllPlaylists(): List<Playlist>
    suspend fun getPlaylist(id: String): Playlist?
    suspend fun createPlaylist(name: String, description: String = ""): Playlist
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(id: String)
    suspend fun addTrackToPlaylist(playlistId: String, trackId: String)
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}

interface SettingsRepository {
    suspend fun getVolumeBoostLevel(): Float
    suspend fun setVolumeBoostLevel(level: Float)
    suspend fun isSpatialAudioEnabled(): Boolean
    suspend fun setSpatialAudioEnabled(enabled: Boolean)
    suspend fun getSpatialAudioPreset(): SpatialPreset
    suspend fun setSpatialAudioPreset(preset: SpatialPreset)
    suspend fun getEqualizerPreset(): Int
    suspend fun setEqualizerPreset(preset: Int)
}

enum class SpatialPreset { JBL_STAGE, DOLBY_ATMOS, OFF }
