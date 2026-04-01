package com.wearamp.data.repository

import android.os.SystemClock
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

    /** Stores data alongside the monotonic elapsed-realtime at which it was fetched. */
    private data class CacheEntry<T>(val data: T, val fetchedAtMs: Long = SystemClock.elapsedRealtime())

    // Cache keyed by "$serverUrl:$sectionId" to prevent cross-server/account contamination.
    private val artistsCache = ConcurrentHashMap<String, CacheEntry<List<PlexMetadata>>>()
    private val allAlbumsCache = ConcurrentHashMap<String, CacheEntry<List<PlexMetadata>>>()

    private val cacheTtlMs = 60 * 60 * 1000L // 1 hour

    private fun <T> CacheEntry<T>.isValid(): Boolean =
        (SystemClock.elapsedRealtime() - fetchedAtMs) < cacheTtlMs

    /** Returns the composite cache key combining [serverUrl] and [sectionId]. */
    private suspend fun cacheKey(sectionId: String): String = "${serverUrl()}:$sectionId"

    /** Removes all cached artists and albums so the next fetch goes to the network. */
    fun clearLibraryCache() {
        artistsCache.clear()
        allAlbumsCache.clear()
    }

    /**
     * Clears the cache for [sectionId] on the current server, then eagerly re-fetches both
     * artists and all albums from the Plex server and repopulates the cache.
     * Returns [Result.success] on completion or [Result.failure] if either network call fails.
     */
    suspend fun refreshLibraryCache(sectionId: String): Result<Unit> {
        val key = cacheKey(sectionId)
        artistsCache.remove(key)
        allAlbumsCache.remove(key)
        return apiCall {
            // Read token once and fetch both lists before writing to cache;
            // if either call throws, neither cache entry is written.
            val authToken = token()
            val artists = plexMediaApi.getArtists(sectionId, authToken)
                .mediaContainer.items ?: emptyList()
            val albums = plexMediaApi.getAllAlbums(sectionId, authToken)
                .mediaContainer.items ?: emptyList()
            artistsCache[key] = CacheEntry(artists)
            allAlbumsCache[key] = CacheEntry(albums)
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
        val key = cacheKey(sectionId)
        val cached = artistsCache[key]
        if (cached != null) {
            if (cached.isValid()) return Result.success(cached.data)
            // Evict the stale entry before fetching fresh data.
            artistsCache.remove(key)
        }
        return apiCall {
            plexMediaApi.getArtists(sectionId, token())
                .mediaContainer.items ?: emptyList()
        }.also { result ->
            result.onSuccess { artistsCache[key] = CacheEntry(it) }
        }
    }

    suspend fun getAlbums(artistId: String): Result<List<PlexMetadata>> = apiCall {
        plexMediaApi.getAlbums(artistId, token())
            .mediaContainer
            .items
            ?: emptyList()
    }

    suspend fun getAllAlbumsInSection(sectionId: String): Result<List<PlexMetadata>> {
        val key = cacheKey(sectionId)
        val cached = allAlbumsCache[key]
        if (cached != null) {
            if (cached.isValid()) return Result.success(cached.data)
            // Evict the stale entry before fetching fresh data.
            allAlbumsCache.remove(key)
        }
        return apiCall {
            plexMediaApi.getAllAlbums(sectionId, token())
                .mediaContainer.items ?: emptyList()
        }.also { result ->
            result.onSuccess { allAlbumsCache[key] = CacheEntry(it) }
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
