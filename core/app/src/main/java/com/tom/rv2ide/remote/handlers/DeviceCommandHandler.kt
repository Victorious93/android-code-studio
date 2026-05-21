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
import org.slf4j.LoggerFactory

class DeviceCommandHandler {

    private val log = LoggerFactory.getLogger(DeviceCommandHandler::class.java)

    fun listDevices(): Map<String, Any> {
        return try {
            val output = runAdb("devices")
            val devices = output.lines()
                .drop(1)
                .filter { it.isNotBlank() }
                .map { line ->
                    val parts = line.trim().split("\\s+".toRegex())
                    mapOf("serial" to (parts.getOrNull(0) ?: ""), "state" to (parts.getOrNull(1) ?: ""))
                }
            mapOf("devices" to devices)
        } catch (e: Exception) {
            log.error("Failed to list devices", e)
            mapOf("error" to (e.message ?: "adb failed"))
        }
    }

    fun connect(body: JsonObject?): Map<String, Any> {
        val ip = body?.get("ip")?.asString ?: return mapOf("error" to "Missing 'ip'")
        val port = body.get("port")?.asInt ?: 5555
        return try {
            val output = runAdb("connect", "$ip:$port")
            mapOf("output" to output.trim())
        } catch (e: Exception) {
            log.error("adb connect failed", e)
            mapOf("error" to (e.message ?: "adb connect failed"))
        }
    }

    fun disconnect(body: JsonObject?): Map<String, Any> {
        val ip = body?.get("ip")?.asString ?: return mapOf("error" to "Missing 'ip'")
        val port = body.get("port")?.asInt ?: 5555
        return try {
            val output = runAdb("disconnect", "$ip:$port")
            mapOf("output" to output.trim())
        } catch (e: Exception) {
            log.error("adb disconnect failed", e)
            mapOf("error" to (e.message ?: "adb disconnect failed"))
        }
    }

    private fun runAdb(vararg args: String): String {
        val adb = findAdb()
        val process = ProcessBuilder(adb, *args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        return output
    }

    private fun findAdb(): String {
        val candidates = listOf(
            "/data/data/com.tom.rv2ide/files/usr/bin/adb",
            "/data/data/com.tom.rv2ide/files/home/.android/platform-tools/adb",
            "adb",
        )
        return candidates.firstOrNull { java.io.File(it).canExecute() } ?: "adb"
    }
}
