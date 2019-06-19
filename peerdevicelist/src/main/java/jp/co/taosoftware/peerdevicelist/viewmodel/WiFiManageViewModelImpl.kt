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
package jp.co.taosoftware.peerdevicelist.viewmodel

import android.content.Context
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import jp.co.taosoftware.peerdevicelist.model.TransferData
import jp.co.taosoftware.peerdevicelist.repository.Listener.WiFiManageRepository
import jp.co.taosoftware.peerdevicelist.viewmodel.listener.WiFiManageViewModel

class WiFiManageViewModelImpl(private val repository: WiFiManageRepository) : ViewModel(), WiFiManageViewModel {

    private val _discover = repository.discoverResult()
    val discover: LiveData<Result<Int>>
        get() = _discover

    private val _wifiP2pEnabled = repository.isWifiP2pEnabled()
    val wifiP2pEnabled: LiveData<Boolean>
        get() = _wifiP2pEnabled

    private val _resetData = repository.resetData()
    val resetData: LiveData<Unit>
        get() = _resetData

    private val _updateThisDevice = repository.updateThisDevice()
    val updateThisDevice: LiveData<WifiP2pDevice>
        get() = _updateThisDevice

    private val _transferDataReceived = repository.onTransferDataReceived()
    val transferDataReceived: LiveData<TransferData>
        get() = _transferDataReceived

    // if edit
    val onPeersAvailable = Transformations.map(repository.onPeersAvailable()) { peerList -> _peersCount.postValue(peerList.deviceList.size)
        peerList}

    private val _onConnectionInfoAvailable = repository.onConnectionInfoAvailable()
    val onConnectionInfoAvailable: LiveData<WifiP2pInfo>
        get() = _onConnectionInfoAvailable

    private val _onChannelDisconnected = MutableLiveData<Unit>()
    val onChannelDisconnected: LiveData<Unit>
        get() = _onChannelDisconnected

    private val _disconnectResult = repository.disconnectResult()
    val disconnectResult: LiveData<Result<Int>>
        get() = _disconnectResult

    private val _peersCount = MutableLiveData<Int>()
    val peersCount: LiveData<Int>
        get() = _peersCount

    private val _recordingStatusChanged = MutableLiveData<Boolean>()
    val recordingStatusChanged: LiveData<Boolean>
        get() = _recordingStatusChanged

    private val _showToast = repository.showToast()
    val showToast: LiveData<String>
        get() = _showToast

    private val _sendFileResult = repository.sendFileResult()
    val sendFileResult: LiveData<Result<Int>>
        get() = _sendFileResult

    init {
    }

    override fun onCleared() {
        super.onCleared()
        // Avoid memory leak
    }

    override fun initialize(c: Context) {
        repository.initialize(c)
    }

    override fun createGroup() {
        repository.createGroup()
    }

    override fun discoverPeers(){
        repository.discoverPeers()
    }

    override fun connect(config: WifiP2pConfig) { repository.connect(config) }

    override fun disconnect() {
        repository.disconnect()
    }

    override fun registerReceiver() { repository.registerReceiver() }

    override fun unregisterReceiver() { repository.unregisterReceiver() }

    override fun resetReceiver() { repository.resetReceiver() }

    override fun sendText(text: String) {
        repository.sendText(text)
    }
    override fun sendText(host: String, text: String) {
        repository.sendText(host, text)
    }
    override fun sendFile(filePath: String) {
        repository.sendFile(filePath)
    }
    override fun sendFile(host: String, filePath: String) {
        repository.sendFile(host, filePath)
    }
    override fun receiveTransferData(filePath: String) {
        repository.receiveTransferData(filePath)
    }
    override fun receiveTransferData(host: String, filePath: String) {
        repository.receiveTransferData(host, filePath)
    }
    override fun recordingStart() { _recordingStatusChanged.postValue(false) }

    override fun recordingStop() { _recordingStatusChanged.postValue(true) }
}
