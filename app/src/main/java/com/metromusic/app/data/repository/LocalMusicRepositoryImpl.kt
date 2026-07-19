package com.metromusic.app.data.repository

import com.metromusic.app.data.local.dao.TrackDao
import com.metromusic.app.data.local.entity.CachedTrackEntity
import com.metromusic.app.domain.model.Track
import com.metromusic.app.domain.model.TrackSource
import com.metromusic.app.domain.repository.LocalMusicRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMusicRepositoryImpl @Inject constructor(
    private val trackDao: TrackDao
) : LocalMusicRepository {

    override suspend fun getAllTracks(): List<Track> {
        return trackDao.getAllTracks().map { it.toDomain() }
    }

    override suspend fun getTrackById(id: String): Track? {
        return trackDao.getTrackById(id)?.toDomain()
    }

    override suspend fun saveTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
    }

    override suspend fun deleteTrack(id: String) {
        trackDao.deleteTrack(id)
    }

    override suspend fun searchTracks(query: String): List<Track> {
        return trackDao.searchTracks(query).map { it.toDomain() }
    }

    private fun CachedTrackEntity.toDomain() = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = streamUrl,
        thumbnailUrl = thumbnailUrl,
        localPath = localPath,
        source = TrackSource.entries.find { it.name == source } ?: TrackSource.CACHE
    )

    private fun Track.toEntity() = CachedTrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        streamUrl = streamUrl,
        thumbnailUrl = thumbnailUrl,
        localPath = localPath,
        source = source.name,
        addedAt = addedAt
    )
}
