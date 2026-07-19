package com.metromusic.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.metromusic.shared.domain.model.MeshPeer
import com.metromusic.shared.domain.model.Track
import com.metromusic.shared.ui.components.MiniPlayerBar
import com.metromusic.shared.ui.friends.FriendsScreen
import com.metromusic.shared.ui.home.HomeScreen
import com.metromusic.shared.ui.library.LibraryScreen
import com.metromusic.shared.ui.player.PlayerScreen
import com.metromusic.shared.ui.search.SearchScreen
import com.metromusic.shared.ui.settings.SettingsScreen
import com.metromusic.shared.ui.theme.ThemeMode

expect fun homeIcon(): ImageVector
expect fun searchIcon(): ImageVector
expect fun friendsIcon(): ImageVector
expect fun libraryIcon(): ImageVector
expect fun settingsIcon(): ImageVector

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MetromusicApp(
    trendingTracks: List<Track>,
    searchResults: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    currentTrack: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSearchQueryChange: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    searchQuery: String,
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    currentPipedInstance: String,
    onPipedInstanceChange: (String) -> Unit,
    volumeBoostEnabled: Boolean,
    onVolumeBoostChange: (Boolean) -> Unit,
    spatialPreset: String,
    onSpatialPresetChange: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }

    var isMeshActive by remember { mutableStateOf(false) }
    var isHost by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf(emptyList<MeshPeer>()) }

    val navItems = listOf(
        NavItem("Home", homeIcon()),
        NavItem("Search", searchIcon()),
        NavItem("FriendsZone", friendsIcon()),
        NavItem("Library", libraryIcon()),
        NavItem("Settings", settingsIcon())
    )

    if (showPlayer) {
        PlayerScreen(
            track = currentTrack,
            isPlaying = isPlaying,
            positionMs = positionMs,
            durationMs = durationMs,
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek,
            onBack = { showPlayer = false }
        )
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    navItems.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selectedTab == index,
                            onClick = { selectedTab = index }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (selectedTab) {
                    0 -> HomeScreen(
                        tracks = trendingTracks,
                        errorMessage = errorMessage,
                        onTrackClick = {
                            onTrackClick(it)
                            showPlayer = true
                        },
                        onRetry = onRetry
                    )
                    1 -> SearchScreen(
                        searchResults = searchResults,
                        trending = trendingTracks,
                        isLoading = isLoading,
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onTrackClick = {
                            onTrackClick(it)
                            showPlayer = true
                        }
                    )
                    2 -> FriendsScreen(
                        isMeshActive = isMeshActive,
                        isHost = isHost,
                        peers = peers,
                        onStartSession = { isMeshActive = true; isHost = true },
                        onLeaveSession = { isMeshActive = false; isHost = false; peers = emptyList() }
                    )
                    3 -> LibraryScreen(
                        tracks = emptyList(),
                        onTrackClick = {
                            onTrackClick(it)
                            showPlayer = true
                        }
                    )
                    4 -> SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = onThemeModeChange,
                        currentPipedInstance = currentPipedInstance,
                        onPipedInstanceChange = onPipedInstanceChange,
                        volumeBoostEnabled = volumeBoostEnabled,
                        onVolumeBoostChange = onVolumeBoostChange,
                        spatialPreset = spatialPreset,
                        onSpatialPresetChange = onSpatialPresetChange
                    )
                }

                if (currentTrack != null && selectedTab != 4) {
                    MiniPlayerBar(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                        onPlayPause = onPlayPause,
                        onClick = { showPlayer = true },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                    )
                }
            }
        }
    }
}
