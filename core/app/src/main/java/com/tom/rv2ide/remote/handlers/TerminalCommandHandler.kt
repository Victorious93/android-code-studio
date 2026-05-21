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

import android.content.Context
import com.google.gson.JsonObject
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment
import org.slf4j.LoggerFactory
import java.io.File

class TerminalCommandHandler(private val context: Context) {

    private val log = LoggerFactory.getLogger(TerminalCommandHandler::class.java)

    fun exec(body: JsonObject?): Map<String, Any> {
        val cmd = body?.get("cmd")?.asString
            ?: return mapOf("error" to "Missing 'cmd' field")

        return try {
            val env = buildEnv()
            val process = ProcessBuilder("/bin/sh", "-c", cmd)
                .directory(File(System.getenv("HOME") ?: "/data/data/com.tom.rv2ide/files/home"))
                .apply { environment().putAll(env) }
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exit = process.waitFor()
            mapOf("stdout" to output, "exitCode" to exit)
        } catch (e: Exception) {
            log.error("Terminal exec failed for cmd: $cmd", e)
            mapOf("error" to (e.message ?: "Execution failed"))
        }
    }

    private fun buildEnv(): Map<String, String> {
        return try {
            TermuxShellEnvironment().getEnvironment(context, false)
        } catch (_: Exception) {
            System.getenv().toMap()
        }
    }
}
