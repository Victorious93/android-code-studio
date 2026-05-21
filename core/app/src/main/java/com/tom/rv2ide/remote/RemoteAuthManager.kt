/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.remote

import android.content.Context
import com.tom.rv2ide.preferences.internal.RemoteControlPreferences
import java.security.SecureRandom

class RemoteAuthManager(private val context: Context) {

    private val prefs by lazy {
        context.getSharedPreferences("remote_control_prefs", Context.MODE_PRIVATE)
    }

    fun getOrCreateToken(): String {
        val existing = prefs.getString(RemoteControlPreferences.TOKEN, null)
        if (!existing.isNullOrBlank()) return existing
        val token = generateToken()
        prefs.edit().putString(RemoteControlPreferences.TOKEN, token).apply()
        return token
    }

    fun validateToken(provided: String): Boolean {
        val expected = prefs.getString(RemoteControlPreferences.TOKEN, null) ?: return false
        return expected == provided
    }

    fun regenerateToken(): String {
        val token = generateToken()
        prefs.edit().putString(RemoteControlPreferences.TOKEN, token).apply()
        return token
    }

    private fun generateToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val rng = SecureRandom()
        return (1..32).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }
}
