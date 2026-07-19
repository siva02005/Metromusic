package com.metromusic.app.data.remote.api

import com.metromusic.app.data.remote.dto.PipedSearchResponse
import com.metromusic.app.data.remote.dto.PipedStreamResponse
import com.metromusic.app.data.remote.dto.PipedTrendingResponse
import com.metromusic.app.data.remote.dto.YtSearchResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PipedApi {

    @GET("streams/{videoId}")
    suspend fun getStream(@Path("videoId") videoId: String): PipedStreamResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("filter") filter: String = "music_songs"
    ): PipedSearchResponse

    @GET("trending")
    suspend fun getTrending(
        @Query("region") region: String = "US"
    ): PipedTrendingResponse
}

interface InnerTubeApi {

    @GET("youtubei/v1/search")
    suspend fun search(@Query("prettyPrint") pretty: Boolean = false): YtSearchResponse

    @GET("youtubei/v1/browse/{browseId}")
    suspend fun getBrowse(@Path("browseId") browseId: String)
}
