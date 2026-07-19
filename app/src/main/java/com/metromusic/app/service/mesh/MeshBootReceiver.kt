package com.metromusic.app.service.mesh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MeshBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Mesh sessions are not persisted across reboots for security.
            // Users must manually re-initiate FriendsZone sessions.
        }
    }
}
