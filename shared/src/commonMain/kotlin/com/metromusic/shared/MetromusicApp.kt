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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.metromusic.shared.domain.model.Track
import com.metromusic.shared.ui.components.MiniPlayerBar
import com.metromusic.shared.ui.friends.FriendsScreen
import com.metromusic.shared.ui.home.HomeScreen
import com.metromusic.shared.ui.library.LibraryScreen
import com.metromusic.shared.ui.navigation.Screen
import com.metromusic.shared.ui.player.PlayerScreen
import com.metromusic.shared.ui.search.SearchScreen

expect fun homeIcon(): ImageVector
expect fun searchIcon(): ImageVector
expect fun friendsIcon(): ImageVector
expect fun libraryIcon(): ImageVector

data class NavItem(val screen: Screen, val label: String, val icon: ImageVector)

@Composable
fun MetromusicApp(
    trendingTracks: List<Track>,
    searchResults: List<Track>,
    isLoading: Boolean,
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
    searchQuery: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = listOf(
        NavItem(Screen.HOME, "Home", homeIcon()),
        NavItem(Screen.SEARCH, "Search", searchIcon()),
        NavItem(Screen.FRIENDS, "FriendsZone", friendsIcon()),
        NavItem(Screen.LIBRARY, "Library", libraryIcon())
    )

    var isMeshActive by remember { mutableStateOf(false) }
    var isHost by remember { mutableStateOf(false) }
    var peers by remember { mutableStateOf(emptyList<com.metromusic.shared.domain.model.MeshPeer>()) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hasRoute(item.screen::class) == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                popUpTo(Screen.HOME.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.HOME.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.HOME.route) {
                    HomeScreen(tracks = trendingTracks, onTrackClick = {
                        onTrackClick(it)
                        navController.navigate(Screen.PLAYER.route)
                    })
                }
                composable(Screen.SEARCH.route) {
                    SearchScreen(
                        searchResults = searchResults,
                        trending = trendingTracks,
                        isLoading = isLoading,
                        query = searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onTrackClick = {
                            onTrackClick(it)
                            navController.navigate(Screen.PLAYER.route)
                        }
                    )
                }
                composable(Screen.FRIENDS.route) {
                    FriendsScreen(
                        isMeshActive = isMeshActive,
                        isHost = isHost,
                        peers = peers,
                        onStartSession = { isMeshActive = true; isHost = true },
                        onLeaveSession = { isMeshActive = false; isHost = false; peers = emptyList() }
                    )
                }
                composable(Screen.LIBRARY.route) {
                    LibraryScreen(tracks = emptyList(), onTrackClick = {
                        onTrackClick(it)
                        navController.navigate(Screen.PLAYER.route)
                    })
                }
                composable(Screen.PLAYER.route) {
                    PlayerScreen(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            if (currentDestination?.hasRoute(Screen.PLAYER::class) != true) {
                MiniPlayerBar(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                    onPlayPause = onPlayPause,
                    onClick = { navController.navigate(Screen.PLAYER.route) },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter).padding(bottom = 80.dp)
                )
            }
        }
    }
}
