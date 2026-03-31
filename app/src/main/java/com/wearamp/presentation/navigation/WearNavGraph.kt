package com.wearamp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.wearamp.presentation.screens.browse.BrowseAlbumsScreen
import com.wearamp.presentation.screens.browse.BrowseAllAlbumsScreen
import com.wearamp.presentation.screens.browse.BrowseArtistsScreen
import com.wearamp.presentation.screens.browse.BrowseTracksScreen
import com.wearamp.presentation.screens.library.LibraryScreen
import com.wearamp.presentation.screens.login.LoginScreen
import com.wearamp.presentation.screens.player.NowPlayingScreen
import com.wearamp.presentation.screens.settings.SettingsScreen

@Composable
fun WearNavGraph(
    navViewModel: NavViewModel = hiltViewModel()
) {
    val authToken by navViewModel.userPreferences.authToken.collectAsState(initial = null)
    val startDestination = if (authToken != null) Screen.LIBRARY else Screen.LOGIN

    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.LIBRARY) {
                        popUpTo(Screen.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LIBRARY) {
            LibraryScreen(
                onArtistsClick = { sectionId ->
                    navController.navigate(Screen.browseArtists(sectionId))
                },
                onAlbumsClick = { sectionId ->
                    navController.navigate(Screen.browseAllAlbums(sectionId))
                },
                onNowPlayingClick = {
                    navController.navigate(Screen.NOW_PLAYING)
                },
                onSettingsClick = {
                    navController.navigate(Screen.SETTINGS)
                }
            )
        }

        composable(Screen.BROWSE_ARTISTS) { _ ->
            BrowseArtistsScreen(
                onArtistSelected = { artistId ->
                    navController.navigate(Screen.browseAlbums(artistId))
                },
                onNowPlayingClick = { navController.navigate(Screen.NOW_PLAYING) }
            )
        }

        composable(Screen.BROWSE_ALL_ALBUMS) { _ ->
            BrowseAllAlbumsScreen(
                onAlbumSelected = { albumId ->
                    navController.navigate(Screen.browseTracks(albumId))
                },
                onNowPlayingClick = { navController.navigate(Screen.NOW_PLAYING) }
            )
        }

        composable(Screen.BROWSE_ALBUMS) { _ ->
            BrowseAlbumsScreen(
                onAlbumSelected = { albumId ->
                    navController.navigate(Screen.browseTracks(albumId))
                },
                onNowPlayingClick = { navController.navigate(Screen.NOW_PLAYING) }
            )
        }

        composable(Screen.BROWSE_TRACKS) { _ ->
            BrowseTracksScreen(
                onNowPlayingClick = { navController.navigate(Screen.NOW_PLAYING) }
            )
        }

        composable(Screen.NOW_PLAYING) {
            NowPlayingScreen()
        }

        composable(Screen.SETTINGS) {
            SettingsScreen(
                onLogout = {
                    navController.navigate(Screen.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
