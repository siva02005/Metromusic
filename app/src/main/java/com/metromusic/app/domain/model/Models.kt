package com.metromusic.app.domain.model

import android.graphics.Bitmap
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val streamUrl: String,
    val thumbnailUrl: String = "",
    val isLocal: Boolean = false,
    val localPath: String? = null,
    val source: TrackSource = TrackSource.ONLINE,
    val addedAt: Long = System.currentTimeMillis()
)

enum class TrackSource { ONLINE, LOCAL, CACHE }

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class SearchResult(
    val tracks: List<Track>,
    val hasNextPage: Boolean = false,
    val continuationToken: String? = null
)

data class ArtistInfo(
    val name: String,
    val thumbnailUrl: String = "",
    val browseId: String = ""
)

data class AlbumInfo(
    val title: String,
    val artist: String,
    val thumbnailUrl: String = "",
    val browseId: String = "",
    val year: Int = 0
)

@Serializable
data class MeshPeer(
    val deviceId: String,
    val deviceName: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)

data class SyncState(
    val trackId: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val timestamp: Long,
    val hostDeviceId: String
)
