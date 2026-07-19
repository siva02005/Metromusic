package com.metromusic.app.ui.friends

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.metromusic.app.domain.model.MeshPeer
import com.metromusic.app.domain.model.SyncState
import com.metromusic.app.service.mesh.MeshController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    application: Application,
    private val meshController: MeshController
) : AndroidViewModel(application) {

    val isMeshActive: StateFlow<Boolean> = meshController.isMeshActive
    val isHost: StateFlow<Boolean> = meshController.isHost
    val peers: StateFlow<List<MeshPeer>> = meshController.peers
    val currentSync: StateFlow<SyncState?> = meshController.currentSync

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    init {
        viewModelScope.launch {
            meshController.peerEvents.collectLatest { peer ->
                _uiMessage.value = "${peer.deviceName} ${if (peer.isConnected) "joined" else "left"}"
            }
        }
    }

    fun startSession() {
        meshController.startAsHost()
        _uiMessage.value = "Session started! Waiting for friends to join..."
    }

    fun joinSession(sessionId: String) {
        meshController.joinSession(sessionId)
        _uiMessage.value = "Joining session..."
    }

    fun syncPlayback(trackId: String, positionMs: Long, isPlaying: Boolean) {
        meshController.broadcastSync(trackId, positionMs, isPlaying)
    }

    fun changeTrack(
        trackId: String,
        streamUrl: String,
        title: String,
        artist: String,
        thumbnailUrl: String = ""
    ) {
        meshController.broadcastTrackChange(trackId, streamUrl, title, artist, thumbnailUrl)
    }

    fun leaveSession() {
        meshController.stopMesh()
        _uiMessage.value = "Left session"
    }

    fun clearMessage() {
        _uiMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        meshController.stopMesh()
    }
}
