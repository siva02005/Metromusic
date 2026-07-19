package com.metromusic.shared.domain.repository

import com.metromusic.shared.domain.model.SearchResult
import com.metromusic.shared.domain.model.Track

interface MusicRepository {
    suspend fun search(query: String): SearchResult
    suspend fun getTrack(videoId: String): Track?
    suspend fun getTrending(): List<Track>
    suspend fun getStreamUrl(trackId: String): Result<String>
    suspend fun getSuggestions(query: String): List<String>
}
