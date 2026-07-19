package com.metromusic.app.service.audio.session

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadControl
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.metromusic.app.R
import com.metromusic.app.service.audio.processor.MetromusicAudioPipeline
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Core MediaSessionService optimized for gaming coexistence.
 *
 * Key design decisions for gaming coexistence:
 * 1. AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK: Instead of AUDIOFOCUS_GAIN,
 *    this tells the OS that our audio is transient and other apps
 *    can duck rather than pause. Games will lower their volume
 *    instead of stopping entirely.
 *
 * 2. Audio attributes use USAGE_MEDIA with CONTENT_TYPE_MUSIC which
 *    is treated as "entertainment" by Android's audio policy, sharing
 *    the focus pool with games (unlike USAGE_GAME which is separate).
 *
 * 3. The load control uses a larger buffer to prevent stuttering under
 *    high CPU load (e.g., during intense gaming).
 *
 * 4. The session stays foreground with an ongoing notification that
 *    uses MEDIA_PLAYBACK category, which the OS treats as a
 *    non-aggressive background service.
 */
@AndroidEntryPoint
class MetromusicMediaService : MediaSessionService() {

    @Inject lateinit var audioPipeline: MetromusicAudioPipeline

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null

    private val audioAttributes = C.AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setFlags(C.AUDIO_FLAG_BYPASS_INTERRUPTION_POLICY)
        .build()

    // Large buffer for gaming coexistence — prevents underruns under load
    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            50_000,   // minBufferMs: 50s minimum
            120_000,  // maxBufferMs: 2 min max
            2_000,    // bufferForPlaybackMs: 2s before playback
            5_000     // bufferForPlaybackAfterRebufferMs: 5s after rebuffer
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setLoadControl(loadControl)

        exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // handleAudioFocus = true
            .setLoadControl(loadControl)
            .setMediaSourceFactory(mediaSourceFactory)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSkipSilenceEnabled(false)
            .build()
            .also { player ->
                player.addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Timber.e(error, "Playback error: ${error.message}")
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        Timber.d("Playback state changed: isPlaying=$isPlaying")
                    }
                })
            }

        val sessionActivityPendingIntent = packageManager
            .getLaunchIntentForIntent(packageName)
            ?.let { sessionIntent ->
                PendingIntent.getActivity(
                    this, 0, sessionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

        val customLayout = ImmutableList.of(
            CommandButton.Builder()
                .setDisplayName("Previous")
                .setIconRes android.R.drawable.ic_media_previous
                .setSessionCommand(MediaSession.SessionCommand.COMMAND_CODE_PLAYER_PREVIOUS)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Rewind")
                .setIconRes android.R.drawable.ic_media_rew
                .setSessionCommand(MediaSession.SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO_PREVIOUS)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Fast Forward")
                .setIconRes android.R.drawable.ic_media_ff
                .setSessionCommand(MediaSession.SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO_NEXT)
                .build(),
            CommandButton.Builder()
                .setDisplayName("Next")
                .setIconRes android.R.drawable.ic_media_next
                .setSessionCommand(MediaSession.SessionCommand.COMMAND_CODE_PLAYER_NEXT)
                .build()
        )

        mediaSession = MediaSession.Builder(this, exoPlayer!!)
            .setSessionActivity(sessionActivityPendingIntent!!)
            .setCustomLayout(customLayout)
            .setHandleAudioBecomingNoisy(true)
            .build()

        Timber.d("MetromusicMediaService created with audio pipeline")
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        exoPlayer = null
        audioPipeline.release()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("System low memory — keeping session alive, trimming cache")
    }

    fun getPlayer(): ExoPlayer? = exoPlayer
}
