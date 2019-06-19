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
package jp.co.taosoftware.peerdevicelist.viewmodel.listener

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig

interface WiFiManageViewModel {
    fun initialize(c: Context)
    fun createGroup()
    fun discoverPeers()
    fun connect(config: WifiP2pConfig)
    fun disconnect()
    fun registerReceiver()
    fun unregisterReceiver()
    fun resetReceiver()
    fun sendText(text: String)
    fun sendText(host: String, text: String)
    fun sendFile(filePath: String)
    fun sendFile(host: String, filePath: String)
    fun receiveTransferData(filePath: String)
    fun receiveTransferData(host: String, filePath: String)
    fun recordingStart()
    fun recordingStop()
}
