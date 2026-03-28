package com.wearamp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
        private val KEY_CLIENT_ID = stringPreferencesKey("client_id")
        private val KEY_USERNAME = stringPreferencesKey("username")
        private val KEY_USER_THUMB = stringPreferencesKey("user_thumb")
    }

    val authToken: Flow<String?> = context.dataStore.data.map { it[KEY_AUTH_TOKEN] }
    val serverUrl: Flow<String?> = context.dataStore.data.map { it[KEY_SERVER_URL] }
    val clientId: Flow<String?> = context.dataStore.data.map { it[KEY_CLIENT_ID] }
    val username: Flow<String?> = context.dataStore.data.map { it[KEY_USERNAME] }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { it[KEY_SERVER_URL] = url }
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

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
