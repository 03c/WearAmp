package com.wearamp.domain.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamKey: String,
    val thumbUrl: String?,
    val isStarred: Boolean = false
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val thumbUrl: String?,
    val trackCount: Int = 0
)

data class Artist(
    val id: String,
    val name: String,
    val thumbUrl: String?
)

data class Library(
    val id: String,
    val title: String,
    val thumbUrl: String?
)
