package com.wearamp.presentation.navigation

object Screen {
    const val LOGIN = "login"
    const val LIBRARY = "library"
    const val BROWSE_ARTISTS = "browse_artists/{sectionId}"
    const val BROWSE_ALL_ALBUMS = "browse_all_albums/{sectionId}"
    const val BROWSE_ALBUMS = "browse_albums/{artistId}"
    const val BROWSE_TRACKS = "browse_tracks/{albumId}"
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"

    fun browseArtists(sectionId: String) = "browse_artists/$sectionId"
    fun browseAllAlbums(sectionId: String) = "browse_all_albums/$sectionId"
    fun browseAlbums(artistId: String) = "browse_albums/$artistId"
    fun browseTracks(albumId: String) = "browse_tracks/$albumId"
}
