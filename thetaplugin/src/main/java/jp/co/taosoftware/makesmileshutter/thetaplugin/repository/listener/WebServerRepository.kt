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
package jp.co.taosoftware.makesmileshutter.thetaplugin.repository.listener

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import androidx.lifecycle.LiveData

interface WebServerRepository {
    fun startWebServer()
    fun stopWebServer()
    fun updateThisDevice(device: WifiP2pDevice?)
    fun updateP2PDeviceList(devices: WifiP2pDeviceList?)
    fun startScan(): LiveData<Unit>
    fun connect(): LiveData<Unit>
    fun connectDevice(): LiveData<WifiP2pDevice>
    fun disconnect(): LiveData<Unit>
    fun isConnected(value:Boolean)
}