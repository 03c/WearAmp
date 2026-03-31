package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

data class PlexPin(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("auth_token") val authToken: String?,
    @SerializedName("expires_in") val expiresIn: Int
)
