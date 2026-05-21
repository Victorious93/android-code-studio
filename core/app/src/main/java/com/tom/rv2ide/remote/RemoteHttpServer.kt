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
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tom.rv2ide.preferences.internal.RemoteControlPreferences
import com.tom.rv2ide.remote.handlers.BuildCommandHandler
import com.tom.rv2ide.remote.handlers.DeviceCommandHandler
import com.tom.rv2ide.remote.handlers.EditorCommandHandler
import com.tom.rv2ide.remote.handlers.TerminalCommandHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.Base64

class RemoteHttpServer(private val context: Context, private val scope: CoroutineScope) {

    private val log = LoggerFactory.getLogger(RemoteHttpServer::class.java)
    private val gson = Gson()
    private val auth = RemoteAuthManager(context)
    private val broadcaster = RemoteEventBroadcaster.getInstance()

    private val buildHandler = BuildCommandHandler()
    private val terminalHandler = TerminalCommandHandler(context)
    private val editorHandler = EditorCommandHandler(context)
    private val deviceHandler = DeviceCommandHandler()

    private var serverSocket: ServerSocket? = null

    fun start(port: Int) {
        serverSocket = ServerSocket(port)
        log.info("Remote control server started on port $port")
        scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClient(client) }
                } catch (e: Exception) {
                    if (isActive) log.error("Server accept error", e)
                }
            }
        }
    }

    fun stop() {
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }

    private fun handleClient(socket: Socket) {
        try {
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return
            val method = parts[0]
            val path = parts[1].split("?")[0]

            val headers = mutableMapOf<String, String>()
            var line = input.readLine()
            while (!line.isNullOrEmpty()) {
                val idx = line.indexOf(':')
                if (idx > 0) {
                    headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                }
                line = input.readLine()
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val bodyChars = CharArray(contentLength)
            if (contentLength > 0) input.read(bodyChars)
            val body = String(bodyChars)

            if (path == "/ws/events" && headers["upgrade"]?.lowercase() == "websocket") {
                handleWebSocketUpgrade(socket, headers)
                return
            }

            val out = PrintWriter(socket.getOutputStream(), true)

            if (path == "/api/status" && method == "GET") {
                sendJson(out, 200, mapOf(
                    "server" to "Android Code Studio Remote Control",
                    "port" to RemoteControlPreferences.port,
                    "auth" to "Bearer token required for all other endpoints",
                ))
                return
            }

            val authHeader = headers["authorization"] ?: ""
            if (!authHeader.startsWith("Bearer ") || !auth.validateToken(authHeader.removePrefix("Bearer ").trim())) {
                sendJson(out, 401, mapOf("error" to "Unauthorized"))
                return
            }

            val bodyJson: JsonObject? = if (body.isNotEmpty()) {
                runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull()
            } else null

            val response: Any = when {
                path == "/api/devices" && method == "GET" -> deviceHandler.listDevices()
                path == "/api/devices/connect" && method == "POST" -> deviceHandler.connect(bodyJson)
                path == "/api/devices/disconnect" && method == "POST" -> deviceHandler.disconnect(bodyJson)
                path == "/api/build/status" && method == "GET" -> buildHandler.getStatus()
                path == "/api/build/run" && method == "POST" -> buildHandler.runBuild(bodyJson)
                path == "/api/build/cancel" && method == "POST" -> buildHandler.cancelBuild()
                path == "/api/files" && method == "GET" -> editorHandler.getFileTree()
                path == "/api/editor/open" && method == "POST" -> editorHandler.openFile(bodyJson)
                path == "/api/terminal/exec" && method == "POST" -> terminalHandler.exec(bodyJson)
                else -> {
                    sendJson(out, 404, mapOf("error" to "Not found"))
                    return
                }
            }
            sendJson(out, 200, response)
        } catch (e: Exception) {
            log.error("Error handling client", e)
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun handleWebSocketUpgrade(socket: Socket, headers: Map<String, String>) {
        val key = headers["sec-websocket-key"] ?: run {
            socket.close()
            return
        }
        val acceptKey = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1")
                .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").toByteArray(Charsets.UTF_8))
        )
        val out = socket.getOutputStream()
        val response = "HTTP/1.1 101 Switching Protocols\r\n" +
            "Upgrade: websocket\r\n" +
            "Connection: Upgrade\r\n" +
            "Sec-WebSocket-Accept: $acceptKey\r\n\r\n"
        out.write(response.toByteArray(Charsets.UTF_8))
        out.flush()
        broadcaster.addClient(socket)
    }

    private fun sendJson(out: PrintWriter, status: Int, body: Any) {
        val json = gson.toJson(body)
        val statusText = when (status) {
            200 -> "OK"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val bytes = json.toByteArray(Charsets.UTF_8)
        out.print("HTTP/1.1 $status $statusText\r\n")
        out.print("Content-Type: application/json; charset=utf-8\r\n")
        out.print("Content-Length: ${bytes.size}\r\n")
        out.print("Connection: close\r\n")
        out.print("\r\n")
        out.print(json)
        out.flush()
    }
}
