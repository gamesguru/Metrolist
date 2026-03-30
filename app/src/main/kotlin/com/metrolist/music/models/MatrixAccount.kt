/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import kotlinx.serialization.Serializable

/**
 * Represents a Matrix account's basic metadata required for identification.
 * Access tokens are stored separately in [com.metrolist.music.utils.MatrixTokenStore].
 *
 * @property homeserver The Matrix homeserver URL (e.g., https://matrix.org).
 * @property userId The Matrix user ID (e.g., @user:matrix.org).
 */
@Serializable
data class MatrixAccount(
    val homeserver: String,
    val userId: String,
) {
    override fun toString(): String {
        return "MatrixAccount(homeserver='$homeserver', userId='$userId')"
    }
}
