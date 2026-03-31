package com.wearamp.di

import com.wearamp.data.local.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp interceptor that rewrites the request's host, port, and scheme with the user's
 * configured Plex server URL stored in [UserPreferences].
 *
 * The server URL is cached in an [AtomicReference] and kept up-to-date by collecting
 * the DataStore flow on a background coroutine, so each request reads the cached value
 * without blocking the network thread.
 *
 * The [scope] is intentionally unbound: as a [Singleton], this interceptor lives for
 * the entire process lifetime (tied to [SingletonComponent]), so the coroutine collecting
 * DataStore updates is valid for the same lifetime as the application process.
 */
@Singleton
class PlexServerUrlInterceptor @Inject constructor(
    userPreferences: UserPreferences
) : Interceptor {

    private val cachedServerUrl = AtomicReference<HttpUrl?>(null)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch {
            userPreferences.serverUrl.collect { urlString ->
                cachedServerUrl.set(urlString?.toHttpUrlOrNull())
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val serverUrl = cachedServerUrl.get()
            ?: throw java.io.IOException(
                "No Plex server configured. Please set your server URL in Settings."
            )

        val original = chain.request()
        val rewritten = original.url.newBuilder()
            .scheme(serverUrl.scheme)
            .host(serverUrl.host)
            .port(serverUrl.port)
            .build()

        return chain.proceed(original.newBuilder().url(rewritten).build())
    }
}
