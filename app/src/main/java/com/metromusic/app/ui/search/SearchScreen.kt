package com.metromusic.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metromusic.app.domain.model.Track
import com.metromusic.app.ui.components.TrackItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchResults: List<Track>,
    trending: List<Track>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onTrackClick: (Track) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = {
                query = it
                onQueryChange(it)
            },
            onSearch = { isActive = false },
            active = isActive,
            onActiveChange = { isActive = it },
            placeholder = { Text("Search songs, artists...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        onQueryChange("")
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isActive) 0.dp else 16.dp)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(searchResults, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        onClick = {
                            onTrackClick(track)
                            isActive = false
                        }
                    )
                }
            }
        }

        if (!isActive) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLoading) {
                    item {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        )
                    }
                }

                item {
                    Text(
                        text = "Trending",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(trending, key = { it.id }) { track ->
                    TrackItem(
                        track = track,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }
    }
}
