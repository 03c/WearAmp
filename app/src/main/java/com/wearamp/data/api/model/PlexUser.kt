package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

data class PlexUser(
    @SerializedName("id") val id: Long,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("username") val username: String,
    @SerializedName("title") val title: String,
    @SerializedName("email") val email: String,
    @SerializedName("thumb") val thumb: String?
)
