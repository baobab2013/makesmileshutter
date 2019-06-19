/**
 * Copyright 2019 Taosoftware Co.,Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.taosoftware.peerdevicelist.executors

import jp.co.taosoftware.peerdevicelist.network.Server
import java.io.ObjectOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class CommandDataTransferServerRunnable(private val command: String) : Runnable {

    override fun run() {
        val listClients = Server.clients
        for (addr: InetAddress in listClients) {
            val socket = Socket()
            socket.reuseAddress = true
            socket.bind(null)
            socket.connect(InetSocketAddress(addr, Server.PORT), 500)
            socket.getOutputStream().use { os ->
                ObjectOutputStream(os).use{oos ->
                    oos.writeObject(command)
                }
            }
        }
    }

    companion object {
        val TAG = "CommandDataTransfer"
    }
}
