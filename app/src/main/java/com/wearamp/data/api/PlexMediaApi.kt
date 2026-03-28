package com.wearamp.data.api

import com.wearamp.data.api.model.PlexMediaContainer
import com.wearamp.data.api.model.PlexLibraryContainer
import com.wearamp.data.api.model.PlexMusicContainer
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Plex media server API for browsing and streaming music.
 * Base URL is the user's Plex server address (e.g. http://192.168.1.x:32400).
 */
interface PlexMediaApi {

    @GET("library/sections")
    suspend fun getLibrarySections(
        @Header("X-Plex-Token") token: String
    ): PlexMediaContainer<PlexLibraryContainer>

    @GET("library/sections/{sectionId}/all")
    suspend fun getArtists(
        @Path("sectionId") sectionId: String,
        @Header("X-Plex-Token") token: String,
        @Query("type") type: Int = 8
    ): PlexMediaContainer<PlexMusicContainer>

    @GET("library/metadata/{artistId}/children")
    suspend fun getAlbums(
        @Path("artistId") artistId: String,
        @Header("X-Plex-Token") token: String
    ): PlexMediaContainer<PlexMusicContainer>

    @GET("library/metadata/{albumId}/children")
    suspend fun getTracks(
        @Path("albumId") albumId: String,
        @Header("X-Plex-Token") token: String
    ): PlexMediaContainer<PlexMusicContainer>

    @GET("library/sections/{sectionId}/all")
    suspend fun getRecentlyAdded(
        @Path("sectionId") sectionId: String,
        @Header("X-Plex-Token") token: String,
        @Query("type") type: Int = 10,
        @Query("sort") sort: String = "addedAt:desc",
        @Query("limit") limit: Int = 20
    ): PlexMediaContainer<PlexMusicContainer>

    /**
     * Rate a track. Set rating to 10 to star/like, 0 to unrate.
     */
    @GET(":/rate")
    suspend fun rateTrack(
        @Query("key") ratingKey: String,
        @Query("identifier") identifier: String = "com.plexapp.plugins.library",
        @Query("rating") rating: Int,
        @Header("X-Plex-Token") token: String
    )
}
