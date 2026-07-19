package com.metromusic.shared.data.repository

import com.metromusic.shared.data.remote.api.PipedApi
import com.metromusic.shared.data.remote.dto.PipedSearchItem
import com.metromusic.shared.domain.model.SearchResult
import com.metromusic.shared.domain.model.Track
import com.metromusic.shared.domain.model.TrackSource
import com.metromusic.shared.domain.repository.MusicRepository

class MusicRepositoryImpl(
    private val pipedApi: PipedApi
) : MusicRepository {

    override suspend fun search(query: String): SearchResult {
        return try {
            val response = pipedApi.search(query)
            val tracks = response.items
                .filter { it.type == "stream" || it.url.contains("/watch") }
                .map { it.toDomainTrack() }
            SearchResult(tracks = tracks)
        } catch (e: Exception) {
            SearchResult(tracks = emptyList())
        }
    }

    override suspend fun getTrack(videoId: String): Track? {
        return try {
            val response = pipedApi.getStream(videoId)
            val bestAudio = response.audioStreams
                .filter { it.mimeType.startsWith("audio/") }
                .maxByOrNull { it.bitrate }

            Track(
                id = videoId,
                title = response.title,
                artist = response.uploaderName,
                durationMs = response.duration * 1000,
                streamUrl = bestAudio?.url ?: "",
                thumbnailUrl = response.thumbnailUrl,
                source = TrackSource.ONLINE
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getTrending(): List<Track> {
        return try {
            val response = pipedApi.getTrending()
            response.items
                .filter { it.type == "stream" }
                .take(50)
                .map { it.toDomainTrack() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getStreamUrl(trackId: String): Result<String> {
        return try {
            val response = pipedApi.getStream(trackId)
            val bestAudio = response.audioStreams
                .filter { it.mimeType.startsWith("audio/") }
                .maxByOrNull { it.bitrate }

            if (bestAudio != null) Result.success(bestAudio.url)
            else Result.failure(Exception("No audio stream found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSuggestions(query: String): List<String> {
        return try {
            val response = pipedApi.search(query)
            response.items.take(10).map { it.title }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun PipedSearchItem.toDomainTrack(): Track {
        val videoId = url.removePrefix("/watch?v=").removePrefix("/")
        return Track(
            id = videoId,
            title = title,
            artist = uploaderName,
            durationMs = duration * 1000,
            streamUrl = "https://pipedapi.kavin.rocks/streams/$videoId",
            thumbnailUrl = thumbnail,
            source = TrackSource.ONLINE
        )
    }
}
