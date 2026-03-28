package com.wearamp.data.api

import com.wearamp.data.api.model.PlexPin
import com.wearamp.data.api.model.PlexPinResponse
import com.wearamp.data.api.model.PlexUserResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Plex authentication API at plex.tv.
 * Uses PIN-based OAuth flow:
 * 1. POST /pins to generate a PIN code
 * 2. Direct user to https://plex.tv/link to enter the code
 * 3. Poll GET /pins/{id} until auth_token is present
 * 4. Use the auth_token for all subsequent media requests
 */
interface PlexAuthApi {

    @FormUrlEncoded
    @POST("pins")
    suspend fun createPin(
        @Header("X-Plex-Client-Identifier") clientId: String,
        @Header("X-Plex-Product") product: String,
        @Field("strong") strong: Boolean = true
    ): PlexPinResponse

    @GET("pins/{id}")
    suspend fun getPin(
        @Path("id") pinId: Long,
        @Header("X-Plex-Client-Identifier") clientId: String
    ): PlexPinResponse

    @GET("user")
    suspend fun getUser(
        @Header("X-Plex-Token") authToken: String
    ): PlexUserResponse
}
