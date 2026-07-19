package com.metromusic.app.ui.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metromusic.app.domain.model.Track
import com.metromusic.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    application: Application,
    private val musicRepository: MusicRepository
) : AndroidViewModel(application) {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volumeBoost = MutableStateFlow(1.0f)
    val volumeBoost: StateFlow<Float> = _volumeBoost.asStateFlow()

    private val _spatialPreset = MutableStateFlow("OFF")
    val spatialPreset: StateFlow<String> = _spatialPreset.asStateFlow()

    fun loadTrack(trackId: String) {
        viewModelScope.launch {
            val track = musicRepository.getTrack(trackId)
            _currentTrack.value = track
        }
    }

    fun setVolumeBoost(factor: Float) {
        _volumeBoost.value = factor.coerceIn(0f, 2f)
    }

    fun setSpatialPreset(preset: String) {
        _spatialPreset.value = preset
    }
}
