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

package com.tom.rv2ide.preferences

import android.content.Context
import android.content.Intent
import androidx.preference.Preference
import com.tom.rv2ide.preferences.internal.RemoteControlPreferences
import com.tom.rv2ide.remote.RemoteControlService
import com.tom.rv2ide.resources.R.string
import kotlinx.parcelize.Parcelize

@Parcelize
class RemoteControlPreferencesScreen(
    override val key: String = "idepref_remote_control",
    override val title: Int = string.title_remote_control,
    override val summary: Int? = string.idepref_remote_summary,
    override val children: List<IPreference> = mutableListOf(),
) : IPreferenceScreen() {

    init {
        addPreference(RemoteControlEnabled())
        addPreference(RemoteControlPort())
        addPreference(RemoteControlToken())
    }
}

@Parcelize
class RemoteControlEnabled(
    override val key: String = RemoteControlPreferences.ENABLED,
    override val title: Int = string.title_remote_control,
    override val summary: Int? = string.idepref_remote_summary,
) : SwitchPreference(
    getValue = { RemoteControlPreferences.isEnabled },
    setValue = { RemoteControlPreferences.isEnabled = it },
) {
    override fun onPreferenceChanged(preference: Preference, newValue: Any?): Boolean {
        val enabled = newValue as Boolean? ?: false
        RemoteControlPreferences.isEnabled = enabled
        val context = preference.context
        val intent = Intent(context, RemoteControlService::class.java)
        if (enabled) {
            context.startForegroundService(intent)
        } else {
            context.stopService(intent)
        }
        return true
    }
}

@Parcelize
class RemoteControlPort(
    override val key: String = RemoteControlPreferences.PORT,
    override val title: Int = string.title_remote_port,
    override val summary: Int? = string.msg_remote_port,
) : NumberInputEditTextPreference(
    hint = string.title_remote_port,
    getValue = { RemoteControlPreferences.port },
    setValue = { RemoteControlPreferences.port = it },
)

@Parcelize
class RemoteControlToken(
    override val key: String = RemoteControlPreferences.TOKEN,
    override val title: Int = string.msg_remote_token,
) : BasePreference() {

    override fun onCreatePreference(context: Context): Preference {
        val pref = Preference(context)
        val prefs = context.getSharedPreferences("remote_control_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString(RemoteControlPreferences.TOKEN, null)
        pref.summary = if (token.isNullOrBlank()) context.getString(string.msg_remote_token_hint) else token
        return pref
    }

    override fun onPreferenceChanged(preference: Preference, newValue: Any?): Boolean = false
}
