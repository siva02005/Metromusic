package com.metromusic.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    data object Home : Screen("home", "Home", Icons.Default.Home)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object FriendsZone : Screen("friends", "FriendsZone", Icons.Default.Person)
    data object Library : Screen("library", "Library", Icons.Default.Home)
    data object Player : Screen("player/{trackId}", "Player") {
        fun createRoute(trackId: String) = "player/$trackId"
    }
    data object Settings : Screen("settings", "Settings")
    data object Queue : Screen("queue", "Queue")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Search,
    Screen.FriendsZone,
    Screen.Library
)
