package com.wearamp.di

import com.wearamp.data.api.PlexAuthApi
import com.wearamp.data.api.PlexMediaApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    const val PLEX_AUTH_BASE_URL = "https://plex.tv/api/v2/"
    const val PLEX_MEDIA_PLACEHOLDER_URL = "http://localhost:32400/"

    @Provides
    @Singleton
    @Named("auth")
    fun provideAuthOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("media")
    fun provideMediaOkHttpClient(
        serverUrlInterceptor: PlexServerUrlInterceptor
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(serverUrlInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun providePlexAuthApi(@Named("auth") okHttpClient: OkHttpClient): PlexAuthApi {
        return Retrofit.Builder()
            .baseUrl(PLEX_AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlexAuthApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlexMediaApi(@Named("media") okHttpClient: OkHttpClient): PlexMediaApi {
        return Retrofit.Builder()
            .baseUrl(PLEX_MEDIA_PLACEHOLDER_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlexMediaApi::class.java)
    }
}
