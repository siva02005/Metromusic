package com.metromusic.shared.data.remote.api

import com.metromusic.shared.data.remote.dto.PipedSearchResponse
import com.metromusic.shared.data.remote.dto.PipedStreamResponse
import com.metromusic.shared.data.remote.dto.PipedTrendingResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object PipedApiRegistry {
    val FALLBACK_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.r4fo.com",
        "https://pipedapi.12a.app",
        "https://pipedapi.smnz.de",
        "https://api.piped.privacydev.net",
        "https://pipedapi.frontendfriendly.xyz",
        "https://pipedapi.ggtyler.dev",
    )
}

class PipedApi(
    private val client: HttpClient,
    private var baseUrl: String = PipedApiRegistry.FALLBACK_INSTANCES.first()
) {
    private val fallbackInstances = PipedApiRegistry.FALLBACK_INSTANCES.toMutableList()

    fun getBaseUrl(): String = baseUrl

    fun setBaseUrl(url: String) {
        baseUrl = url
        fallbackInstances.remove(url)
        fallbackInstances.add(0, url)
    }

    suspend fun getStream(videoId: String): PipedStreamResponse {
        return tryGet { client.get("$it/streams/$videoId").body() }
    }

    suspend fun search(query: String): PipedSearchResponse {
        return tryGet {
            client.get("$it/search") {
                parameter("q", query)
                parameter("filter", "music_songs")
            }.body()
        }
    }

    suspend fun getTrending(region: String = "US"): PipedTrendingResponse {
        return tryGet {
            client.get("$it/trending") {
                parameter("region", region)
            }.body()
        }
    }

    private suspend fun <T> tryGet(block: suspend (String) -> T): T {
        val ordered = listOf(baseUrl) + fallbackInstances.filter { it != baseUrl }
        var lastException: Exception? = null
        for (instance in ordered) {
            try {
                val result = block(instance)
                if (instance != baseUrl) {
                    baseUrl = instance
                }
                return result
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: Exception("No Piped instances available")
    }
}
