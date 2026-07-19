package com.metromusic.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PipedStreamResponse(
    val title: String = "",
    val uploaderName: String = "",
    val uploaderUrl: String = "",
    val thumbnailUrl: String = "",
    val duration: Long = 0,
    @SerializedName("videoStreams")
    val videoStreams: List<PipedVideoStream> = emptyList(),
    @SerializedName("audioStreams")
    val audioStreams: List<PipedAudioStream> = emptyList()
)

data class PipedAudioStream(
    val url: String = "",
    val format: String = "",
    val quality: String = "",
    val mimeType: String = "",
    val bitrate: Int = 0
)

data class PipedVideoStream(
    val url: String = "",
    val quality: String = "",
    val mimeType: String = ""
)

data class PipedSearchResponse(
    val items: List<PipedSearchItem> = emptyList()
)

data class PipedSearchItem(
    val url: String = "",
    val title: String = "",
    val thumbnail: String = "",
    val uploaderName: String = "",
    val uploaderUrl: String = "",
    val duration: Long = 0,
    val type: String = ""
)

data class PipedTrendingResponse(
    val items: List<PipedSearchItem> = emptyList()
)

data class YtSearchResponse(
    val contents: YtContents? = null
)

data class YtContents(
    val tabbedSearchResultsRenderer: Any? = null
)
