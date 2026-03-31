package com.wearamp.data.repository

import com.wearamp.data.api.PlexAuthApi
import com.wearamp.data.api.model.PlexConnection
import com.wearamp.data.api.model.PlexPin
import com.wearamp.data.api.model.PlexUser
import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val plexAuthApi: PlexAuthApi,
    private val userPreferences: UserPreferences
) {

    companion object {
        private const val PLEX_PRODUCT = "WearAmp"
        private const val PIN_POLL_INTERVAL_MS = 3_000L
        private const val PIN_POLL_MAX_ATTEMPTS = 40
    }

    suspend fun createPin(clientId: String): Result<PlexPin> = runCatching {
        plexAuthApi.createPin(
            clientId = clientId,
            product = PLEX_PRODUCT
        ).pin
    }

    /**
     * Poll the Plex auth endpoint until the user has entered the PIN at plex.tv/link.
     * Returns the auth token on success.
     */
    suspend fun pollForAuthToken(pinId: Long, clientId: String): Result<String> {
        repeat(PIN_POLL_MAX_ATTEMPTS) {
            delay(PIN_POLL_INTERVAL_MS)
            val result = runCatching {
                plexAuthApi.getPin(pinId = pinId, clientId = clientId).pin
            }
            val token = result.getOrNull()?.authToken
            if (token != null) {
                userPreferences.saveAuthToken(token)
                userPreferences.saveClientId(clientId)
                return Result.success(token)
            }
        }
        return Result.failure(Exception("Authentication timed out. Please try again."))
    }

    suspend fun fetchAndSaveUser(authToken: String): Result<PlexUser> = runCatching {
        val user = plexAuthApi.getUser(authToken).user
        userPreferences.saveUserInfo(user.username, user.thumb)
        user
    }

    /**
     * Fetch the user's Plex servers and save the first owned server's connection URI.
     * Prefers a local connection when available.
     */
    suspend fun fetchAndSaveServerUrl(authToken: String): Result<String> = runCatching {
        val resources = plexAuthApi.getResources(authToken)
        val server = resources
            .firstOrNull { it.provides.contains("server") && it.owned == true }
            ?: resources.firstOrNull { it.provides.contains("server") }
            ?: throw Exception("No Plex servers found on this account")

        val connection = server.connections
            ?.sortedWith(
                compareByDescending<PlexConnection> { it.local }
                    .thenByDescending { it.protocol.equals("https", ignoreCase = true) }
            )
            ?.firstOrNull()
            ?: throw Exception("No connections available for server '${server.name}'")

        val normalizedUri = connection.uri.trim().trimEnd('/') + "/"
        val parsedUri = normalizedUri.toHttpUrlOrNull()
            ?: throw Exception("Invalid server URI '${connection.uri}'")

        if (parsedUri.scheme != "http" && parsedUri.scheme != "https") {
            throw Exception("Unsupported server URI scheme '${parsedUri.scheme}'")
        }

        val uri = parsedUri.toString()
        userPreferences.saveServerUrl(uri)
        uri
    }

    suspend fun logout() {
        userPreferences.clearAll()
    }
}
