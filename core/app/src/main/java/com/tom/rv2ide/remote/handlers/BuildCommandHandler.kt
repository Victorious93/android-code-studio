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

package com.tom.rv2ide.remote.handlers

import com.google.gson.JsonObject
import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.projects.builder.BuildService
import org.slf4j.LoggerFactory

class BuildCommandHandler {

    private val log = LoggerFactory.getLogger(BuildCommandHandler::class.java)

    fun getStatus(): Map<String, Any> {
        val service = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
        return mapOf(
            "inProgress" to (service?.isBuildInProgress ?: false),
            "serverStarted" to (service?.isToolingServerStarted() ?: false),
        )
    }

    fun runBuild(body: JsonObject?): Map<String, Any> {
        val service = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
            ?: return mapOf("error" to "Build service not available")
        if (service.isBuildInProgress) {
            return mapOf("error" to "Build already in progress")
        }
        val tasksArray = body?.getAsJsonArray("tasks")
        val tasks = if (tasksArray != null && tasksArray.size() > 0) {
            (0 until tasksArray.size()).map { tasksArray[it].asString }
        } else {
            listOf("assembleDebug")
        }
        return try {
            service.executeTasks(*tasks.toTypedArray())
            mapOf("status" to "started", "tasks" to tasks)
        } catch (e: Exception) {
            log.error("Failed to start build", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }

    fun cancelBuild(): Map<String, Any> {
        val service = Lookup.getDefault().lookup(BuildService.KEY_BUILD_SERVICE)
            ?: return mapOf("error" to "Build service not available")
        return try {
            service.cancelCurrentBuild()
            mapOf("status" to "cancellation requested")
        } catch (e: Exception) {
            log.error("Failed to cancel build", e)
            mapOf("error" to (e.message ?: "Unknown error"))
        }
    }
}
