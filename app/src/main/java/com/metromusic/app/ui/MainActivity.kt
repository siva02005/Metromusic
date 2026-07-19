package com.metromusic.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.metromusic.shared.MetromusicApp
import com.metromusic.shared.data.remote.api.PipedApi
import com.metromusic.shared.data.repository.MusicRepositoryImpl
import com.metromusic.shared.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        val repository = MusicRepositoryImpl(PipedApi(client))

        setContent {
            val scope = rememberCoroutineScope()
            var trending by remember { mutableStateOf(emptyList<Track>()) }
            var searchResults by remember { mutableStateOf(emptyList<Track>()) }
            var searchQuery by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var currentTrack by remember { mutableStateOf<Track?>(null) }
            var isPlaying by remember { mutableStateOf(false) }
            var positionMs by remember { mutableStateOf(0L) }
            var durationMs by remember { mutableStateOf(0L) }

            LaunchedEffect(Unit) {
                launch(Dispatchers.IO) { trending = repository.getTrending() }
            }

            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotBlank()) {
                    launch(Dispatchers.IO) {
                        isLoading = true
                        searchResults = repository.search(searchQuery).tracks
                        isLoading = false
                    }
                }
            }

            MetromusicApp(
                trendingTracks = trending,
                searchResults = searchResults,
                isLoading = isLoading,
                currentTrack = currentTrack,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                onSearchQueryChange = { searchQuery = it },
                onTrackClick = { currentTrack = it },
                onPlayPause = { isPlaying = !isPlaying },
                onNext = {},
                onPrevious = {},
                onSeek = { positionMs = it },
                searchQuery = searchQuery
            )
        }
    }
}
