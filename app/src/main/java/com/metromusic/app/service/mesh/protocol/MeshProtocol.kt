package com.metromusic.app.service.mesh.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber

/**
 * Binary protocol for FriendsZone sync messages.
 *
 * Uses Protocol Buffers for compact, efficient serialization.
 * Messages are kept minimal to fit within Wi-Fi Aware
 * service info limits (~255 bytes for raw, larger for data paths).
 *
 * Message types:
 *   BEACON      — Periodic broadcast announcing peer presence
 *   SYNC_REQUEST  — Host requests all peers to jump to position
 *   SYNC_UPDATE   — Periodic position update from host
 *   TRACK_CHANGE  — Host changed track, peers follow
 *   PEER_JOIN     — New peer wants to join the session
 *   PEER_LEAVE    — Peer leaving the mesh
 */
@Serializable
data class MeshMessage(
    @ProtoNumber(1) val type: MessageType,
    @ProtoNumber(2) val sessionId: String,
    @ProtoNumber(3) val senderDeviceId: String,
    @ProtoNumber(4) val timestamp: Long,
    @ProtoNumber(5) val syncState: SyncPayload? = null,
    @ProtoNumber(6) val trackChange: TrackChangePayload? = null,
    @ProtoNumber(7) val peerInfo: PeerInfoPayload? = null
)

@Serializable
enum class MessageType(@ProtoNumber val value: Int) {
    BEACON(1),
    SYNC_REQUEST(2),
    SYNC_UPDATE(3),
    TRACK_CHANGE(4),
    PEER_JOIN(5),
    PEER_LEAVE(6),
    PING(7),
    PONG(8)
}

@Serializable
data class SyncPayload(
    @ProtoNumber(1) val trackId: String,
    @ProtoNumber(2) val positionMs: Long,
    @ProtoNumber(3) val isPlaying: Boolean,
    @ProtoNumber(4) val playbackSpeed: Float = 1.0f
)

@Serializable
data class TrackChangePayload(
    @ProtoNumber(1) val trackId: String,
    @ProtoNumber(2) val streamUrl: String,
    @ProtoNumber(3) val title: String,
    @ProtoNumber(4) val artist: String,
    @ProtoNumber(5) val thumbnailUrl: String = "",
    @ProtoNumber(6) val startPositionMs: Long = 0
)

@Serializable
data class PeerInfoPayload(
    @ProtoNumber(1) val deviceName: String,
    @ProtoNumber(2) val deviceModel: String,
    @ProtoNumber(3) val isHost: Boolean = false,
    @ProtoNumber(4) val appVersion: String = "1.0"
)

/**
 * Clock offset estimation for cross-device sync accuracy.
 *
 * Uses a simple NTP-like exchange:
 *   Device A -> B: t1
 *   Device B -> A: t2 (t1 + processing + network)
 *   RTT = t2 - t1 (approximate)
 *   offset = (t2_local - t1) - RTT/2
 */
@Serializable
data class ClockSyncPacket(
    @ProtoNumber(1) val sequenceId: Long,
    @ProtoNumber(2) val t1: Long,
    @ProtoNumber(3) val t2: Long = 0,
    @ProtoNumber(4) val senderId: String
) {
    companion object {
        fun estimateOffset(localT1: Long, remoteT2: Long, localT3: Long): Long {
            val rtt = localT3 - localT1
            return remoteT2 - localT1 - rtt / 2
        }
    }
}

object MeshCodec {
    fun encode(message: MeshMessage): ByteArray {
        return ProtoBuf.encodeToByteArray(MeshMessage.serializer(), message)
    }

    fun decode(data: ByteArray): MeshMessage? {
        return try {
            ProtoBuf.decodeFromByteArray(MeshMessage.serializer(), data)
        } catch (e: Exception) {
            null
        }
    }

    fun encodeClockSync(packet: ClockSyncPacket): ByteArray {
        return ProtoBuf.encodeToByteArray(ClockSyncPacket.serializer(), packet)
    }

    fun decodeClockSync(data: ByteArray): ClockSyncPacket? {
        return try {
            ProtoBuf.decodeFromByteArray(ClockSyncPacket.serializer(), data)
        } catch (e: Exception) {
            null
        }
    }
}
