package com.metromusic.app.data.remote

import com.metromusic.app.data.remote.api.PipedApi
import com.metromusic.app.data.remote.dto.PipedSearchItem
import com.metromusic.app.domain.model.SearchResult
import com.metromusic.app.domain.model.Track
import com.metromusic.app.domain.model.TrackSource
import com.metromusic.app.domain.repository.MusicRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val pipedApi: PipedApi
) : MusicRepository {

    override suspend fun search(query: String, page: Int): SearchResult {
        return try {
            val response = pipedApi.search(query)
            val tracks = response.items
                .filter { it.type == "stream" || it.url.contains("/watch") }
                .map { it.toDomainTrack() }
            SearchResult(tracks = tracks)
        } catch (e: Exception) {
            Timber.e(e, "Search failed for query: $query")
            SearchResult(tracks = emptyList())
        }
    }

    override suspend fun getTrack(streamUrl: String): Track? {
        val videoId = extractVideoId(streamUrl) ?: return null
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
            Timber.e(e, "Failed to get track: $videoId")
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
            Timber.e(e, "Failed to fetch trending")
            emptyList()
        }
    }

    override suspend fun getCharts(): List<Track> = getTrending()

    override suspend fun getArtistTracks(artistId: String): List<Track> = emptyList()
    override suspend fun getAlbumTracks(albumId: String): List<Track> = emptyList()

    override suspend fun getStreamUrl(trackId: String): Result<String> {
        return try {
            val response = pipedApi.getStream(trackId)
            val bestAudio = response.audioStreams
                .filter { it.mimeType.startsWith("audio/") }
                .maxByOrNull { it.bitrate }

            if (bestAudio != null) {
                Result.success(bestAudio.url)
            } else {
                Result.failure(Exception("No audio stream found"))
            }
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

    private fun extractVideoId(url: String): String? {
        val patterns = listOf(
            Regex("[?&]v=([\\w-]{11})"),
            Regex("youtu\\.be/([\\w-]{11})"),
            Regex("embed/([\\w-]{11})")
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) return match.groupValues[1]
        }
        return if (url.length == 11 && url.all { it.isLetterOrDigit() || it == '_' || it == '-' }) url else null
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
