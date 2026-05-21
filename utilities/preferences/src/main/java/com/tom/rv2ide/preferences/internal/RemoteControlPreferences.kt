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

package com.tom.rv2ide.preferences.internal

object RemoteControlPreferences {
    const val ENABLED = "idepref_remote_enabled"
    const val PORT = "idepref_remote_port"
    const val TOKEN = "idepref_remote_token"
    const val DEFAULT_PORT = 7070

    var isEnabled: Boolean
        get() = prefManager.getBoolean(ENABLED, false)
        set(value) { prefManager.putBoolean(ENABLED, value) }

    var port: Int
        get() = prefManager.getInt(PORT, DEFAULT_PORT)
        set(value) { prefManager.putInt(PORT, value) }
}
