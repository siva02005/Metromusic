package com.metromusic.shared.data.remote.api

import com.metromusic.shared.data.remote.dto.PipedSearchResponse
import com.metromusic.shared.data.remote.dto.PipedStreamResponse
import com.metromusic.shared.data.remote.dto.PipedTrendingResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PipedApi(private val client: HttpClient) {

    private val baseUrl = "https://pipedapi.kavin.rocks"

    suspend fun getStream(videoId: String): PipedStreamResponse {
        return client.get("$baseUrl/streams/$videoId").body()
    }

    suspend fun search(query: String): PipedSearchResponse {
        return client.get("$baseUrl/search") {
            parameter("q", query)
            parameter("filter", "music_songs")
        }.body()
    }

    suspend fun getTrending(region: String = "US"): PipedTrendingResponse {
        return client.get("$baseUrl/trending") {
            parameter("region", region)
        }.body()
    }
}
