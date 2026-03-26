/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.music.db.entities.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber

class MatrixRPC(
    private val homeserver: String,
    private val userId: String,
    private val accessToken: String,
    private val client: OkHttpClient = OkHttpClient()
) {
    private var lastStatusMsg: String? = null
    private var lastPresence: String? = null

    suspend fun updateSong(
        song: Song?,
        statusFormat: String = "Listening to: {song_name} by {artist_name} | Metrolist Android",
        presence: String = "online"
    ) = runCatching {
        if (homeserver.isEmpty() || userId.isEmpty() || accessToken.isEmpty()) {
            return@runCatching
        }

        val resolvedText = if (song != null) {
            resolveVariables(statusFormat.ifEmpty { "Listening to: {song_name} by {artist_name} | Metrolist Android" }, song)
        } else {
            ""
        }

        if (resolvedText == lastStatusMsg && presence == lastPresence) {
            return@runCatching
        }

        val jsonBody = JSONObject().apply {
            put("presence", presence)
            put("status_msg", resolvedText)
        }.toString()

        val url = homeserver.toHttpUrlOrNull()?.newBuilder()
            ?.addPathSegment("_matrix")
            ?.addPathSegment("client")
            ?.addPathSegment("v3")
            ?.addPathSegment("presence")
            ?.addPathSegment(userId)
            ?.addPathSegment("status")
            ?.build()
            ?.toString() ?: return@runCatching

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .put(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()
            
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.tag("MatrixRPC").e("Failed to update status: ${response.code} ${response.message}")
                } else {
                    lastStatusMsg = resolvedText
                    lastPresence = presence
                }
            }
        }
    }.onFailure { throwable ->
        Timber.tag("MatrixRPC").e(throwable, "Failed to update Matrix presence via updateSong")
    }

    suspend fun close() {
        // You could set presence to offline, or clear the status msg here
        updateSong(song = null, statusFormat = "", presence = "offline")
    }

    companion object {
        fun resolveVariables(text: String, song: Song): String {
            return text
                .replace("{song_name}", song.song.title)
                .replace("{artist_name}", song.artists.joinToString { it.name })
                .replace("{album_name}", song.album?.title ?: "")
        }
    }
}
