package com.metromusic.app.di

import android.content.Context
import com.metromusic.app.data.local.MetromusicDatabase
import com.metromusic.app.data.remote.api.PipedApi
import com.metromusic.app.service.audio.processor.MetromusicAudioPipeline
import com.metromusic.app.service.mesh.MeshController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MetromusicDatabase {
        return MetromusicDatabase.create(context)
    }

    @Provides
    fun provideTrackDao(db: MetromusicDatabase) = db.trackDao()

    @Provides
    fun providePlaylistDao(db: MetromusicDatabase) = db.playlistDao()

    @Provides
    fun provideSearchHistoryDao(db: MetromusicDatabase) = db.searchHistoryDao()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.metromusic.app.BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun providePipedApi(client: OkHttpClient): PipedApi {
        return Retrofit.Builder()
            .baseUrl("https://pipedapi.kavin.rocks/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PipedApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMeshController(@ApplicationContext context: Context): MeshController {
        return MeshController(context)
    }
}
