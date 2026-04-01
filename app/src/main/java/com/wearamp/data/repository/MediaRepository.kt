package com.wearamp.data.repository

import com.wearamp.data.api.PlexMediaApi
import com.wearamp.data.api.model.PlexLibrarySection
import com.wearamp.data.api.model.PlexMetadata
import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val plexMediaApi: PlexMediaApi,
    private val userPreferences: UserPreferences
) {

    private data class CacheEntry<T>(val data: T, val fetchedAt: Long = System.currentTimeMillis())

    private val artistsCache = ConcurrentHashMap<String, CacheEntry<List<PlexMetadata>>>()
    private val allAlbumsCache = ConcurrentHashMap<String, CacheEntry<List<PlexMetadata>>>()

    private val cacheTtlMs = 60 * 60 * 1000L // 1 hour

    private fun <T> CacheEntry<T>.isValid(): Boolean =
        (System.currentTimeMillis() - fetchedAt) < cacheTtlMs

    /** Removes all cached artists and albums so the next fetch goes to the network. */
    fun clearLibraryCache() {
        artistsCache.clear()
        allAlbumsCache.clear()
    }

    /**
     * Clears the cache for [sectionId], then eagerly re-fetches both artists and all albums from
     * the Plex server and repopulates the cache.  Returns [Result.success] on completion or
     * [Result.failure] if either network call fails.
     */
    suspend fun refreshLibraryCache(sectionId: String): Result<Unit> {
        artistsCache.remove(sectionId)
        allAlbumsCache.remove(sectionId)
        return apiCall {
            // Fetch both before updating cache; if either call throws, neither cache entry is written.
            val artists = plexMediaApi.getArtists(sectionId, token())
                .mediaContainer.items ?: emptyList()
            val albums = plexMediaApi.getAllAlbums(sectionId, token())
                .mediaContainer.items ?: emptyList()
            artistsCache[sectionId] = CacheEntry(artists)
            allAlbumsCache[sectionId] = CacheEntry(albums)
        }
    }

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

    suspend fun getArtists(sectionId: String): Result<List<PlexMetadata>> {
        artistsCache[sectionId]?.let { cached ->
            if (cached.isValid()) return Result.success(cached.data)
        }
        return apiCall {
            plexMediaApi.getArtists(sectionId, token())
                .mediaContainer.items ?: emptyList()
        }.also { result ->
            result.onSuccess { artistsCache[sectionId] = CacheEntry(it) }
        }
    }

    suspend fun getAlbums(artistId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getAlbums(artistId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun getAllAlbumsInSection(sectionId: String): Result<List<PlexMetadata>> {
        allAlbumsCache[sectionId]?.let { cached ->
            if (cached.isValid()) return Result.success(cached.data)
        }
        return apiCall {
            plexMediaApi.getAllAlbums(sectionId, token())
                .mediaContainer.items ?: emptyList()
        }.also { result ->
            result.onSuccess { allAlbumsCache[sectionId] = CacheEntry(it) }
        }
    }

    suspend fun getArtistTracks(artistId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getArtistTracks(artistId, token())
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
