package com.wearamp.data.repository

import com.wearamp.data.api.PlexAuthApi
import com.wearamp.data.api.model.PlexConnection
import com.wearamp.data.api.model.PlexPin
import com.wearamp.data.api.model.PlexResource
import com.wearamp.data.api.model.PlexUser
import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.delay
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
        )
    }

    /**
     * Poll the Plex auth endpoint until the user has entered the PIN at plex.tv/link.
     * Returns the auth token on success but does NOT persist it yet —
     * call [finaliseLogin] after server discovery to save credentials.
     */
    suspend fun pollForAuthToken(pinId: Long, clientId: String): Result<String> {
        repeat(PIN_POLL_MAX_ATTEMPTS) {
            delay(PIN_POLL_INTERVAL_MS)
            val result = runCatching {
                plexAuthApi.getPin(pinId = pinId, clientId = clientId)
            }
            val token = result.getOrNull()?.authToken
            if (!token.isNullOrBlank()) {
                return Result.success(token)
            }
        }
        return Result.failure(Exception("Authentication timed out. Please try again."))
    }

    /**
     * Persist credentials once the full login flow (auth + user + server discovery)
     * has completed. This triggers the NavGraph to redirect to the Library.
     */
    suspend fun finaliseLogin(authToken: String, clientId: String) {
        userPreferences.saveAuthToken(authToken)
        userPreferences.saveClientId(clientId)
    }

    suspend fun fetchAndSaveUser(authToken: String, clientId: String): Result<PlexUser> = runCatching {
        val user = plexAuthApi.getUser(authToken, clientId)
        userPreferences.saveUserInfo(user.username, user.thumb)
        user
    }

    /**
     * Fetch all Plex Media Servers visible to the authenticated user.
     * Returns only resources whose [PlexResource.provides] field contains "server".
     */
    suspend fun getServers(authToken: String, clientId: String): Result<List<PlexResource>> = runCatching {
        plexAuthApi.getResources(authToken, clientId)
            .filter { it.provides?.contains("server") == true }
    }

    /**
     * Discover the user's Plex Media Server via the plex.tv resources endpoint
     * and save the best connection URL.
     *
     * Prefers local (LAN) connections, then falls back to relay/remote.
     */
    suspend fun discoverAndSaveServer(authToken: String, clientId: String): Result<String> = runCatching {
        val resources = plexAuthApi.getResources(authToken, clientId)

        // Find the first resource that provides "server"
        val server = resources.firstOrNull { it.provides?.contains("server") == true }
            ?: throw IllegalStateException("No Plex Media Server found on your account")

        val connections = server.connections
            ?: throw IllegalStateException("Server '${server.name}' has no connections")

        // Prefer local connections, then any available
        val best = connections.firstOrNull { it.local }
            ?: connections.first()

        val serverUrl = best.uri.trimEnd('/')
        userPreferences.saveServerUrl(serverUrl)

        // Save the server-specific access token for direct media access
        server.accessToken?.let { userPreferences.saveServerToken(it) }

        serverUrl
    }

    /**
     * Save a manually-selected server connection URL and its resource access token.
     */
    suspend fun saveServerConnection(resource: PlexResource, connection: PlexConnection) {
        userPreferences.saveServerUrl(connection.uri.trimEnd('/'))
        val serverToken = resource.accessToken ?: ""
        userPreferences.saveServerToken(serverToken)
    }

    suspend fun logout() {
        userPreferences.clearAll()
    }
}
