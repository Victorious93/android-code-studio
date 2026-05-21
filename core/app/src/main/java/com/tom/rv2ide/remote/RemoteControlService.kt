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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.preferences.internal.RemoteControlPreferences
import com.tom.rv2ide.resources.R
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.slf4j.LoggerFactory

class RemoteControlService : Service() {

    private val log = LoggerFactory.getLogger(RemoteControlService::class.java)
    private val serviceScope = CoroutineScope(Dispatchers.IO + CoroutineName("RemoteControlService"))
    private var server: RemoteHttpServer? = null

    override fun onCreate() {
        val port = RemoteControlPreferences.port
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(port))
        server = RemoteHttpServer(this, serviceScope).also { it.start(port) }
        log.info("RemoteControlService started on port $port")
    }

    override fun onDestroy() {
        server?.stop()
        serviceScope.cancel()
        log.info("RemoteControlService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            BaseApplication.NOTIFICATION_REMOTE_CONTROL,
            getString(R.string.title_remote_control_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
    }

    private fun buildNotification(port: Int): Notification {
        return Notification.Builder(this, BaseApplication.NOTIFICATION_REMOTE_CONTROL)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentTitle(getString(R.string.title_remote_control))
            .setContentText(getString(R.string.msg_remote_enabled, port))
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 17572
    }
}
