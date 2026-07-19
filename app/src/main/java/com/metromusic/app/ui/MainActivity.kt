package com.metromusic.app.ui

import android.content.ComponentName
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.metromusic.app.domain.model.Track
import com.metromusic.app.service.audio.session.MetromusicMediaService
import com.metromusic.app.ui.components.MiniPlayerBar
import com.metromusic.app.ui.friends.FriendsScreen
import com.metromusic.app.ui.friends.FriendsViewModel
import com.metromusic.app.ui.home.HomeScreen
import com.metromusic.app.ui.library.LibraryScreen
import com.metromusic.app.ui.navigation.Screen
import com.metromusic.app.ui.navigation.bottomNavItems
import com.metromusic.app.ui.player.PlayerScreen
import com.metromusic.app.ui.player.PlayerViewModel
import com.metromusic.app.ui.search.SearchScreen
import com.metromusic.app.ui.search.SearchViewModel
import com.metromusic.app.ui.theme.MetromusicTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var mediaControllerFuture: ListenableFuture<MediaController>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MetromusicTheme {
                MetromusicApp()
            }
        }

        initializeMediaController()
    }

    private fun initializeMediaController() {
        val sessionToken = SessionToken(
            this,
            ComponentName(this, MetromusicMediaService::class.java)
        )
        mediaControllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaController.releaseFuture(mediaControllerFuture)
    }
}

@Composable
fun MetromusicApp() {
    val navController = rememberNavController()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val friendsViewModel: FriendsViewModel = hiltViewModel()

    val searchResults by searchViewModel.searchResults.collectAsState()
    val trending by searchViewModel.trending.collectAsState()
    val isSearchLoading by searchViewModel.isLoading.collectAsState()

    val currentTrack by playerViewModel.currentTrack.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val positionMs by playerViewModel.positionMs.collectAsState()
    val durationMs by playerViewModel.durationMs.collectAsState()
    val volumeBoost by playerViewModel.volumeBoost.collectAsState()
    val spatialPreset by playerViewModel.spatialPreset.collectAsState()

    val isMeshActive by friendsViewModel.isMeshActive.collectAsState()
    val isHost by friendsViewModel.isHost.collectAsState()
    val peers by friendsViewModel.peers.collectAsState()
    val uiMessage by friendsViewModel.uiMessage.collectAsState()

    var showPlayer by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            screen.icon?.let {
                                Icon(imageVector = it, contentDescription = screen.title)
                            }
                        },
                        label = { Text(screen.title) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { saveState = true }
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
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        tracks = trending,
                        onTrackClick = { track ->
                            playerViewModel.loadTrack(track.id)
                            showPlayer = true
                        }
                    )
                }

                composable(Screen.Search.route) {
                    SearchScreen(
                        searchResults = searchResults,
                        trending = trending,
                        isLoading = isSearchLoading,
                        onQueryChange = searchViewModel::onQueryChange,
                        onTrackClick = { track ->
                            playerViewModel.loadTrack(track.id)
                            showPlayer = true
                        }
                    )
                }

                composable(Screen.FriendsZone.route) {
                    FriendsScreen(
                        isMeshActive = isMeshActive,
                        isHost = isHost,
                        peers = peers,
                        uiMessage = uiMessage,
                        onStartSession = friendsViewModel::startSession,
                        onJoinSession = friendsViewModel::joinSession,
                        onLeaveSession = friendsViewModel::leaveSession
                    )
                }

                composable(Screen.Library.route) {
                    LibraryScreen(
                        tracks = emptyList(),
                        onTrackClick = { track ->
                            playerViewModel.loadTrack(track.id)
                            showPlayer = true
                        }
                    )
                }

                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("trackId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val trackId = backStackEntry.arguments?.getString("trackId") ?: return@composable
                    LaunchedEffect(trackId) {
                        playerViewModel.loadTrack(trackId)
                    }
                    PlayerScreen(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        volumeBoost = volumeBoost,
                        spatialPreset = spatialPreset,
                        onPlayPause = { playerViewModel.setVolumeBoost(volumeBoost) },
                        onNext = {},
                        onPrevious = {},
                        onSeek = {},
                        onVolumeBoostChange = playerViewModel::setVolumeBoost,
                        onSpatialPresetChange = playerViewModel::setSpatialPreset,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Mini Player
            MiniPlayerBar(
                track = currentTrack,
                isPlaying = isPlaying,
                progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
                onPlayPause = { playerViewModel.setVolumeBoost(volumeBoost) },
                onClick = {
                    currentTrack?.let {
                        navController.navigate(Screen.Player.createRoute(it.id))
                    }
                },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }

    // FriendsZone message snackbar
    LaunchedEffect(uiMessage) {
        uiMessage?.let {
            kotlinx.coroutines.delay(3000)
            friendsViewModel.clearMessage()
        }
    }
}
