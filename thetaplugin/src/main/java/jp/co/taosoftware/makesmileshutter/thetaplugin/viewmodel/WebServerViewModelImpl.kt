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
package jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import jp.co.taosoftware.makesmileshutter.thetaplugin.repository.listener.WebServerRepository
import jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel.listener.WebServerViewModel

class WebServerViewModelImpl(private val repository: WebServerRepository): ViewModel(), WebServerViewModel{

    private val _startScan = repository.startScan()
    val startScan: LiveData<Unit>
        get() = _startScan
    private val _connect = repository.connect()
    val connect: LiveData<Unit>
        get() = _connect
    private val _connectDevice = repository.connectDevice()
    val connectDevice: LiveData<WifiP2pDevice>
        get() = _connectDevice
    private val _disconnect = repository.disconnect()
    val disconnect: LiveData<Unit>
        get() = _disconnect

    override fun startWebServer() {
        repository.startWebServer()
    }

    override fun stopWebServer() {
        repository.stopWebServer()
    }

    override fun updateThisDevice(device: WifiP2pDevice?) {
        repository.updateThisDevice(device)
    }

    override fun updateP2PDeviceList(devices: WifiP2pDeviceList?) {
        repository.updateP2PDeviceList(devices)
    }

    override fun isConnected(value: Boolean) {
        repository.isConnected(value)
    }
}