package com.wearamp.di

import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that rewrites the request's host, port, and scheme with the user's
 * configured Plex server URL stored in [UserPreferences].
 *
 * This avoids the need to re-create the Retrofit instance whenever the server URL changes
 * and prevents blocking the Hilt dependency graph at startup.
 */
@Singleton
class PlexServerUrlInterceptor @Inject constructor(
    private val userPreferences: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val serverUrl = runBlocking { userPreferences.serverUrl.firstOrNull() }
            ?.toHttpUrlOrNull()
            ?: return chain.proceed(chain.request())

        val original = chain.request()
        val rewritten = original.url.newBuilder()
            .scheme(serverUrl.scheme)
            .host(serverUrl.host)
            .port(serverUrl.port)
            .build()

        return chain.proceed(original.newBuilder().url(rewritten).build())
    }
}
