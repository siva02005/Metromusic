package com.metromusic.shared.domain.model

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
    val source: TrackSource = TrackSource.ONLINE,
    val addedAt: Long = 0L
)

enum class TrackSource { ONLINE, LOCAL, CACHE }

@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val description: String = "",
    val trackIds: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class SearchResult(
    val tracks: List<Track>,
    val hasNextPage: Boolean = false,
    val continuationToken: String? = null
)

data class MeshPeer(
    val deviceId: String,
    val deviceName: String,
    val isConnected: Boolean = false,
    val signalStrength: Int = 0,
    val lastSeen: Long = 0L
)

data class SyncState(
    val trackId: String,
    val positionMs: Long,
    val isPlaying: Boolean,
    val timestamp: Long,
    val hostDeviceId: String
)

enum class SpatialPreset { JBL_STAGE, DOLBY_ATMOS, OFF }
