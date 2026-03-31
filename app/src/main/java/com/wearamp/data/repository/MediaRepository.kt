package com.wearamp.data.repository

import com.wearamp.data.api.PlexMediaApi
import com.wearamp.data.api.model.PlexLibrarySection
import com.wearamp.data.api.model.PlexMetadata
import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val plexMediaApi: PlexMediaApi,
    private val userPreferences: UserPreferences
) {

    private suspend fun token(): String =
        userPreferences.authToken.firstOrNull()
            ?: throw IllegalStateException("Not signed in")

    private suspend fun serverUrl(): String =
        userPreferences.serverUrl.firstOrNull() ?: "(not set)"

    private suspend fun <T> apiCall(block: suspend () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: IOException) {
            val server = serverUrl()
            Result.failure(
                IOException("Connection failed.\nServer: $server\n${e.message}", e)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMusicLibrarySections(): Result<List<PlexLibrarySection>> = apiCall {
        plexMediaApi.getLibrarySections(token())
            .mediaContainer
            .sections
            ?.filter { it.type == "artist" }
            ?: emptyList()
    }

    suspend fun getArtists(sectionId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getArtists(sectionId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun getAlbums(artistId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getAlbums(artistId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun getTracks(albumId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getTracks(albumId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun getRecentlyAdded(sectionId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getRecentlyAdded(sectionId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun rateTrack(ratingKey: String, starred: Boolean): Result<Unit> = apiCall {
        plexMediaApi.rateTrack(
            ratingKey = ratingKey,
            rating = if (starred) 10 else 0,
            token = token()
        )
    }
}
