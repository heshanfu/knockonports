/*
 * Copyright (c) 2018 Alexander Yaburov
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package me.impa.knockonports.service

import android.content.Context
import me.impa.knockonports.R
import me.impa.knockonports.database.entity.Port
import me.impa.knockonports.database.entity.Sequence
import org.jetbrains.anko.*
import java.net.*

class Knocker(val context: Context, private val sequence: Sequence, private val ports: List<Port>): AnkoLogger {

    private val _defaultTimeout = 1000
    private val _maxSleep = 15000

    fun execute() {
        if (sequence.host.isNullOrBlank()) {
            warn { "Empty host '${sequence.name}'" }
            context.runOnUiThread {
                context.toast(R.string.host_not_set)
            }
            return
        }
        val p = ports.filter { it.number in 1..65535 }

        if (p.isEmpty()) {
            warn { "Empty sequence '${sequence.name}'" }
            context.runOnUiThread {
                context.toast(context.getString(R.string.empty_sequence_warning, sequence.name))
            }
            return
        }
        info { "Knocking to '${sequence.name}'" }


        val address = try {
            InetAddress.getByName(sequence.host)
        } catch (e: UnknownHostException) {
            context.runOnUiThread {
                context.toast(context.getString(R.string.error_resolve, sequence.host))
            }
            return
        } catch (e: Exception) {
            warn("Resolve error", e)
            return
        }
        context.runOnUiThread {
            context.toast(context.getString(R.string.start_knocking, sequence.name))
        }
        debug { "Remote address $address" }
        val udpSocket = if (ports.any { portModel -> portModel.type == Port.PORT_TYPE_UDP }) {
            DatagramSocket()
        } else {
            null
        }
        val contentString = sequence.udpContent
        val udpContent = if (!contentString.isNullOrEmpty()) {
            if (sequence.base64 == 1) {
                // decode base64
                try {
                    android.util.Base64.decode(contentString, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    warn { "Unable to decode udp content string (invalid base64?)" }
                    context.runOnUiThread {
                        context.toast(R.string.error_base64)
                    }
                    ByteArray(0)
                }
            } else {
                contentString?.toByteArray() ?: ByteArray(0)
            }
        } else {
            ByteArray(0)
        }

        try {
            var cnt = 0
            p.forEach {
                info { "Knock #${++cnt}..." }
                if (it.type == Port.PORT_TYPE_UDP) {
                    debug { "Knock UDP ${it.number}" }
                    udpSocket?.send(DatagramPacket(udpContent, udpContent.size, address, it.number!!))
                } else {
                    debug { "Knock TCP ${it.number}" }
                    val socket = Socket()
                    try {
                        socket.tcpNoDelay = true
                        socket.keepAlive = false
                        var timeout = sequence.timeout ?: _defaultTimeout
                        if (timeout <= 0)
                            timeout = _defaultTimeout
                        socket.soTimeout = timeout
                        socket.setSoLinger(true, 0)
                        socket.connect(InetSocketAddress(sequence.host, it.number!!), timeout)
                    } catch (e: SocketTimeoutException) {
                        // it's ok
                    } catch (e: Exception) {
                        warn("Error while sending TCP knock", e)
                    } finally {
                        socket.close()
                    }
                }
                val delay = sequence.delay ?: 0
                if (delay > 0) {
                    val sleepMs = Math.min(delay, _maxSleep).toLong()
                    debug { "Sleep for ${sleepMs}ms" }
                    Thread.sleep(sleepMs)
                }
            }
        } catch (e: Exception) {
            warn("Knocking error", e)
        } finally {
            udpSocket?.close()
        }
        val app = sequence.application
        if (!app.isNullOrEmpty()) {
            info { "Starting $app..." }
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app ?: "")
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else {
                warn { "Could not find launch intent" }
            }
        }
        info("Knocking complete")
        context.runOnUiThread {
            context.toast(R.string.end_knocking)
        }

    }

}