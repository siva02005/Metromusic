package com.metromusic.desktop

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.metromusic.shared.MetromusicApp
import com.metromusic.shared.data.remote.api.PipedApi
import com.metromusic.shared.data.repository.MusicRepositoryImpl
import com.metromusic.shared.domain.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun main() = application {
    val client = remember {
        HttpClient(Java) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }

    val repository = remember { MusicRepositoryImpl(PipedApi(client)) }
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
        scope.launch(Dispatchers.IO) {
            trending = repository.getTrending()
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            scope.launch(Dispatchers.IO) {
                isLoading = true
                searchResults = repository.search(searchQuery).tracks
                isLoading = false
            }
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Metromusic",
        state = WindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(1200.dp, 800.dp)
        )
    ) {
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
