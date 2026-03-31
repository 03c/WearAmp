package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

data class PlexPin(
    @SerializedName("id") val id: Long,
    @SerializedName("code") val code: String,
    @SerializedName("authToken") val authToken: String?,
    @SerializedName("expiresIn") val expiresIn: Int
)
