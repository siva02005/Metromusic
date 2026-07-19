package com.metromusic.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.metromusic.shared.MetromusicApp
import com.metromusic.shared.data.local.SettingsStore
import com.metromusic.shared.data.remote.api.PipedApi
import com.metromusic.shared.data.remote.api.PipedApiRegistry
import com.metromusic.shared.data.repository.MusicRepositoryImpl
import com.metromusic.shared.domain.model.Track
import com.metromusic.shared.ui.theme.MetromusicTheme
import com.metromusic.shared.ui.theme.ThemeMode
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

        val settingsStore = SettingsStore(applicationContext)
        val client = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }

        val savedInstance = settingsStore.getPipedInstance()
        val baseUrl = if (savedInstance.isNotEmpty()) savedInstance else PipedApiRegistry.FALLBACK_INSTANCES.first()
        val pipedApi = PipedApi(client, baseUrl)
        val repository = MusicRepositoryImpl(pipedApi)

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
            var errorMessage by remember { mutableStateOf<String?>(null) }

            var themeMode by remember {
                mutableStateOf(
                    when (settingsStore.getThemeMode()) {
                        "light" -> ThemeMode.LIGHT
                        "dark" -> ThemeMode.DARK
                        else -> ThemeMode.SYSTEM
                    }
                )
            }
            var volumeBoost by remember { mutableStateOf(settingsStore.getVolumeBoost()) }
            var spatialPreset by remember { mutableStateOf(settingsStore.getSpatialAudioPreset()) }

            val loadTrending: () -> Unit = {
                scope.launch(Dispatchers.IO) {
                    try {
                        errorMessage = null
                        trending = repository.getTrending()
                        if (trending.isEmpty()) {
                            errorMessage = "No music found. Check your connection or try a different source in Settings."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Failed to load: ${e.message ?: "Unknown error"}. Try another source in Settings."
                    }
                }
            }

            LaunchedEffect(Unit) { loadTrending() }

            LaunchedEffect(searchQuery) {
                if (searchQuery.isNotBlank()) {
                    launch(Dispatchers.IO) {
                        isLoading = true
                        try {
                            searchResults = repository.search(searchQuery).tracks
                        } catch (_: Exception) {}
                        isLoading = false
                    }
                }
            }

            MetromusicTheme(themeMode = themeMode) {
                MetromusicApp(
                    trendingTracks = trending,
                    searchResults = searchResults,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
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
                    onRetry = { loadTrending() },
                    searchQuery = searchQuery,
                    currentThemeMode = themeMode,
                    onThemeModeChange = { mode ->
                        themeMode = mode
                        settingsStore.setThemeMode(mode.name.lowercase())
                    },
                    currentPipedInstance = pipedApi.getBaseUrl(),
                    onPipedInstanceChange = { url ->
                        pipedApi.setBaseUrl(url)
                        settingsStore.setPipedInstance(url)
                        loadTrending()
                    },
                    volumeBoostEnabled = volumeBoost,
                    onVolumeBoostChange = {
                        volumeBoost = it
                        settingsStore.setVolumeBoost(it)
                    },
                    spatialPreset = spatialPreset,
                    onSpatialPresetChange = { preset ->
                        spatialPreset = preset
                        settingsStore.setSpatialAudioPreset(preset)
                    }
                )
            }
        }
    }
}
