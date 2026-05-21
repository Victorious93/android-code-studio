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
import android.content.Intent
import com.google.gson.JsonObject
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import org.slf4j.LoggerFactory
import java.io.File

class EditorCommandHandler(private val context: Context) {

    private val log = LoggerFactory.getLogger(EditorCommandHandler::class.java)

    fun getFileTree(): Map<String, Any> {
        val projectDir = try {
            ProjectManagerImpl.getInstance().projectDir
        } catch (_: Exception) {
            null
        } ?: return mapOf("error" to "No project open")

        val entries = projectDir.listFiles()?.map { f ->
            mapOf("name" to f.name, "path" to f.absolutePath, "isDir" to f.isDirectory)
        } ?: emptyList()

        return mapOf("root" to projectDir.absolutePath, "entries" to entries)
    }

    fun openFile(body: JsonObject?): Map<String, Any> {
        val path = body?.get("path")?.asString
            ?: return mapOf("error" to "Missing 'path' field")
        val file = File(path)
        if (!file.exists()) return mapOf("error" to "File not found: $path")

        return try {
            val intent = Intent("com.tom.rv2ide.editor.action.OPEN_FILE")
            intent.putExtra("com.tom.rv2ide.editor.extra.FILE_PATH", path)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            mapOf("status" to "opened", "path" to path)
        } catch (e: Exception) {
            log.error("Failed to open file: $path", e)
            mapOf("error" to (e.message ?: "Failed to open file"))
        }
    }
}
