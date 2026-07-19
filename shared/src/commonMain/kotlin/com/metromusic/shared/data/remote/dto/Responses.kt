package com.metromusic.shared.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PipedStreamResponse(
    val title: String = "",
    @SerialName("uploaderName") val uploaderName: String = "",
    @SerialName("thumbnailUrl") val thumbnailUrl: String = "",
    val duration: Long = 0,
    @SerialName("audioStreams") val audioStreams: List<PipedAudioStream> = emptyList()
)

@Serializable
data class PipedAudioStream(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    @SerialName("mimeType") val mimeType: String = "",
    val bitrate: Int = 0
)

@Serializable
data class PipedSearchResponse(
    val items: List<PipedSearchItem> = emptyList()
)

@Serializable
data class PipedSearchItem(
    val url: String = "",
    val title: String = "",
    val thumbnail: String = "",
    @SerialName("uploaderName") val uploaderName: String = "",
    val duration: Long = 0,
    val type: String = ""
)

@Serializable
data class PipedTrendingResponse(
    val items: List<PipedSearchItem> = emptyList()
)
