package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

data class PlexResource(
    @SerializedName("name") val name: String,
    @SerializedName("provides") val provides: String,
    @SerializedName("owned") val owned: Boolean?,
    @SerializedName("connections") val connections: List<PlexConnection>?
)

data class PlexConnection(
    @SerializedName("protocol") val protocol: String,
    @SerializedName("address") val address: String,
    @SerializedName("port") val port: Int,
    @SerializedName("uri") val uri: String,
    @SerializedName("local") val local: Boolean
)
