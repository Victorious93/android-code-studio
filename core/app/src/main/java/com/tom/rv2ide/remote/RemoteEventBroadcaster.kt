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

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArraySet

class RemoteEventBroadcaster private constructor() {

    private val log = LoggerFactory.getLogger(RemoteEventBroadcaster::class.java)
    private val clients = CopyOnWriteArraySet<Socket>()
    private val gson = Gson()

    fun addClient(socket: Socket) {
        clients.add(socket)
        log.info("WebSocket client connected, total: ${clients.size}")
    }

    fun removeClient(socket: Socket) {
        clients.remove(socket)
        try { socket.close() } catch (_: Exception) {}
        log.info("WebSocket client disconnected, total: ${clients.size}")
    }

    fun broadcast(eventType: String, data: Any) {
        val payload = gson.toJson(mapOf("type" to eventType, "data" to data))
        val frame = encodeTextFrame(payload)
        val dead = mutableListOf<Socket>()
        for (client in clients) {
            try {
                client.getOutputStream().write(frame)
                client.getOutputStream().flush()
            } catch (_: Exception) {
                dead.add(client)
            }
        }
        dead.forEach { removeClient(it) }
    }

    private fun encodeTextFrame(text: String): ByteArray {
        val payload = text.toByteArray(Charsets.UTF_8)
        val len = payload.size
        val buf = when {
            len <= 125 -> ByteBuffer.allocate(2 + len).apply {
                put(0x81.toByte())
                put(len.toByte())
            }
            len <= 65535 -> ByteBuffer.allocate(4 + len).apply {
                put(0x81.toByte())
                put(126.toByte())
                putShort(len.toShort())
            }
            else -> ByteBuffer.allocate(10 + len).apply {
                put(0x81.toByte())
                put(127.toByte())
                putLong(len.toLong())
            }
        }
        buf.put(payload)
        return buf.array()
    }

    companion object {
        private val INSTANCE = RemoteEventBroadcaster()
        fun getInstance(): RemoteEventBroadcaster = INSTANCE
    }
}
