package com.wearamp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "wearamp_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_SERVER_TOKEN = stringPreferencesKey("server_token")
        private val KEY_CLIENT_ID = stringPreferencesKey("client_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_USER_THUMB = stringPreferencesKey("user_thumb")
        private val KEY_SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
        /** Stored as an Int matching Player.REPEAT_MODE_* constants (0=off, 1=one, 2=all). */
        private val KEY_REPEAT_MODE = intPreferencesKey("repeat_mode")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }
    val serverUrl: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER_URL] }
    /** The resource-specific access token for the Plex server (used for streaming). */
    val serverToken: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER_TOKEN] }
    val clientId: Flow<String?> = context.dataStore.data.map { it[KEY_CLIENT_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[KEY_USERNAME] }
    val shuffleModeEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SHUFFLE_MODE] ?: false }
    /** Persisted repeat mode; defaults to 2 (Player.REPEAT_MODE_ALL) to match original behaviour. */
    val repeatMode: Flow<Int> = context.dataStore.data.map { it[KEY_REPEAT_MODE] ?: 2 }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
    }

    suspend fun saveServerToken(token: String) {
        context.dataStore.edit { it[KEY_SERVER_TOKEN] = token }
    }

    suspend fun saveClientId(id: String) {
        context.dataStore.edit { it[KEY_CLIENT_ID] = id }
    }

    suspend fun saveUserInfo(username: String, thumb: String?) {
        context.dataStore.edit {
            it[KEY_USERNAME] = username
            if (thumb != null) it[KEY_USER_THUMB] = thumb
        }
    }

    suspend fun saveShuffleMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHUFFLE_MODE] = enabled }
    }

    suspend fun saveRepeatMode(mode: Int) {
        context.dataStore.edit { it[KEY_REPEAT_MODE] = mode }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
