package com.metromusic.app.service.audio.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import androidx.media3.session.MediaButtonReceiver

class MediaButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
            MediaButtonReceiver.handleIntent(
                context.sessionManager?.getMediaSessionById(0),
                intent
            )
        }
    }

    private val Context.sessionManager: android.media.session.MediaManager?
        get() = getSystemService(Context.MEDIA_SESSION_SERVICE) as? android.media.session.MediaManager
}
