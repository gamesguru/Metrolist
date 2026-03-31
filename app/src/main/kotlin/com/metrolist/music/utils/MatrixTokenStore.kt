/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber

/**
 * Secure store for Matrix access tokens using [EncryptedSharedPreferences].
 * This ensures that sensitive credentials are not stored in plaintext on the device.
 */
object MatrixTokenStore {
    private const val PREFS_FILE = "matrix_tokens"
    private const val TAG = "MatrixTokenStore"

    @Volatile
    private var prefs: SharedPreferences? = null

    /**
     * Retrieves or initializes the [EncryptedSharedPreferences] instance.
     *
     * @param context The context used to access shared preferences.
     * @return The [SharedPreferences] instance, or null if initialization fails.
     */
    private fun getPrefs(context: Context): SharedPreferences? {
        val cached = prefs
        if (cached != null) {
            return cached
        }

        return synchronized(this) {
            val recheck = prefs
            if (recheck != null) {
                recheck
            } else {
                try {
                    val appContext = context.applicationContext
                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    val created = EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_FILE,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                    prefs = created
                    created
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to initialize EncryptedSharedPreferences")
                    null
                }
            }
        }
    }

    /**
     * Generates a unique key for a Matrix account based on its homeserver and user ID.
     *
     * @param homeserver The Matrix homeserver URL.
     * @param userId The Matrix user ID.
     * @return A unique string key for storage.
     */
    private fun createKey(homeserver: String, userId: String): String {
        return "${homeserver.length}:$homeserver|${userId.length}:$userId"
    }

    /**
     * Securely saves a Matrix access token for the specified account.
     *
     * @param context The context used to access shared preferences.
     * @param homeserver The Matrix homeserver URL.
     * @param userId The Matrix user ID.
     * @param token The access token to save.
     */
    fun saveToken(context: Context, homeserver: String, userId: String, token: String) {
        val sharedPrefs = getPrefs(context)
        if (sharedPrefs == null) {
            Timber.tag(TAG).w("saveToken: encrypted storage unavailable, token not persisted")
            return
        }
        sharedPrefs.edit().putString(createKey(homeserver, userId), token).apply()
    }

    /**
     * Retrieves the saved Matrix access token for the specified account.
     *
     * @param context The context used to access shared preferences.
     * @param homeserver The Matrix homeserver URL.
     * @param userId The Matrix user ID.
     * @return The access token if found, or null otherwise.
     */
    fun getToken(context: Context, homeserver: String, userId: String): String? {
        val sharedPrefs = getPrefs(context)
        if (sharedPrefs == null) {
            Timber.tag(TAG).w("getToken: encrypted storage unavailable, returning null")
            return null
        }
        return sharedPrefs.getString(createKey(homeserver, userId), null)
    }

    /**
     * Removes the saved Matrix access token for the specified account.
     *
     * @param context The context used to access shared preferences.
     * @param homeserver The Matrix homeserver URL.
     * @param userId The Matrix user ID.
     */
    fun removeToken(context: Context, homeserver: String, userId: String) {
        val sharedPrefs = getPrefs(context)
        if (sharedPrefs == null) {
            Timber.tag(TAG).w("removeToken: encrypted storage unavailable, nothing to remove")
            return
        }
        sharedPrefs.edit().remove(createKey(homeserver, userId)).apply()
    }
}
