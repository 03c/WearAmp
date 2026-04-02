package com.wearamp.data.api.model

import com.google.gson.annotations.SerializedName

data class PlexMediaContainer<T>(
    @SerializedName("MediaContainer") val mediaContainer: T
)

data class PlexLibraryContainer(
    @SerializedName("size") val size: Int,
    @SerializedName("Directory") val sections: List<PlexLibrarySection>?
)

data class PlexLibrarySection(
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String,
    @SerializedName("art") val art: String?,
    @SerializedName("thumb") val thumb: String?
)

data class PlexMusicContainer(
    @SerializedName("size") val size: Int,
    @SerializedName("Metadata") val items: List<PlexMetadata>?
)

data class PlexMetadata(
    @SerializedName("ratingKey") val ratingKey: String,
    @SerializedName("key") val key: String,
    @SerializedName("title") val title: String,
    @SerializedName("type") val type: String,
    @SerializedName("thumb") val thumb: String?,
    @SerializedName("art") val art: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("year") val year: Int?,
    @SerializedName("duration") val duration: Long?,
    @SerializedName("userRating") val userRating: Float?,
    @SerializedName("parentRatingKey") val parentRatingKey: String?,
    @SerializedName("parentTitle") val parentTitle: String?,
    @SerializedName("grandparentRatingKey") val grandparentRatingKey: String?,
    @SerializedName("grandparentTitle") val grandparentTitle: String?,
    @SerializedName("Media") val media: List<PlexMedia>?
)

data class PlexMedia(
    @SerializedName("id") val id: Long,
    @SerializedName("duration") val duration: Long,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("Part") val parts: List<PlexPart>
)

data class PlexPart(
    @SerializedName("id") val id: Long,
    @SerializedName("key") val key: String,
    @SerializedName("duration") val duration: Long,
    @SerializedName("file") val file: String,
    @SerializedName("size") val size: Long
)
