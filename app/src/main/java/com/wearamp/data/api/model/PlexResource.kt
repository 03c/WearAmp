package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

/**
 * A Plex resource returned by `GET /resources`.
 * Each resource represents a device the user owns (server, player, etc.).
 */
data class PlexResource(
    @SerializedName("name") val name: String,
    @SerializedName("clientIdentifier") val clientIdentifier: String,
    @SerializedName("product") val product: String?,
    @SerializedName("provides") val provides: String?,
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("connections") val connections: List<PlexConnection>?
)

data class PlexConnection(
    @SerializedName("protocol") val protocol: String,
    @SerializedName("address") val address: String,
    @SerializedName("port") val port: Int,
    @SerializedName("uri") val uri: String,
    @SerializedName("local") val local: Boolean
)
