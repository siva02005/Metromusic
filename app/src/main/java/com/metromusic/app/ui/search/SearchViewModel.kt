package com.metromusic.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromusic.app.domain.model.Track
import com.metromusic.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _trending = MutableStateFlow<List<Track>>(emptyList())
    val trending: StateFlow<List<Track>> = _trending.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadTrending()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _suggestions.value = emptyList()
            return
        }
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = musicRepository.search(query)
                _searchResults.value = result.tracks
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun loadTrending() {
        viewModelScope.launch {
            _isLoading.value = true
            _trending.value = musicRepository.getTrending()
            _isLoading.value = false
        }
    }
}
