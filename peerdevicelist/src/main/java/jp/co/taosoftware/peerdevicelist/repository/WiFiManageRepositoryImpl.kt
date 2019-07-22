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
package jp.co.taosoftware.peerdevicelist.repository

import android.content.Context
import android.net.wifi.p2p.*
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import jp.co.taosoftware.peerdevicelist.WiFiDirectBroadcastReceiver
import jp.co.taosoftware.peerdevicelist.executors.*
import jp.co.taosoftware.peerdevicelist.model.TransferData
import jp.co.taosoftware.peerdevicelist.repository.Listener.WiFiManageRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WiFiManageRepositoryImpl(private val context: Context, private val manager:WifiP2pManager) : WiFiManageRepository {

    private lateinit var channel : WifiP2pManager.Channel
    private var deviceStatus = WifiP2pDevice.UNAVAILABLE
    private lateinit var receiver: WiFiDirectBroadcastReceiver
    private var executorService: ExecutorService

    private val _isWifiP2pEnabled = MutableLiveData<Boolean>()
    override fun isWifiP2pEnabled(): LiveData<Boolean> { return _isWifiP2pEnabled }
    private val _createGroupResult = MutableLiveData<Result<Int>>()
    override fun createGroupResult() : LiveData<Result<Int>> {return _createGroupResult}
    private val _discoverResult = MutableLiveData<Result<Int>>()
    override fun discoverResult() : LiveData<Result<Int>> {return _discoverResult}
    private val _connectResult = MutableLiveData<Result<Int>>()
    override fun connectResult() : LiveData<Result<Int>> {return _connectResult}
    private val _resetData = MutableLiveData<Unit>()
    override fun resetData(): LiveData<Unit> { return _resetData }
    private val _showDetail = MutableLiveData<WifiP2pDevice>()
    override fun showDetail(): LiveData<WifiP2pDevice> { return _showDetail }
    private val _updateThisDevice = MutableLiveData<WifiP2pDevice>()
    override fun updateThisDevice():LiveData<WifiP2pDevice> { return _updateThisDevice }
    private val _onPeersAveilable = MutableLiveData<WifiP2pDeviceList>()
    override fun onPeersAvailable():LiveData<WifiP2pDeviceList> { return _onPeersAveilable }
    private val _onConnectionInfoAvailable = MutableLiveData<WifiP2pInfo>()
    override fun onConnectionInfoAvailable(): LiveData<WifiP2pInfo> { return _onConnectionInfoAvailable }
    private val _onChannelDisconnected = MutableLiveData<Unit>()
    override fun onChannelDisconnected():LiveData<Unit> { return _onChannelDisconnected }
    private val _disconnectResult = MutableLiveData<Result<Int>>()
    override fun disconnectResult():LiveData<Result<Int>> { return _disconnectResult }
    private val _onTransferDataReceived = MutableLiveData<TransferData>()
    override fun onTransferDataReceived():LiveData<TransferData> { return _onTransferDataReceived }
    private val _showToast = MutableLiveData<String>()
    override fun showToast(): LiveData<String> { return _showToast }

    private val _sendFileResult = MutableLiveData<Result<Int>>()
    override fun sendFileResult(): LiveData<Result<Int>> { return _sendFileResult }

    init {
        deviceStatus = WifiP2pDevice.UNAVAILABLE
        executorService = Executors.newCachedThreadPool()
        initialize(context)
    }

    private var retryChannel = false
    override fun initialize(c: Context) {
        channel = manager.initialize(c, c.mainLooper, object: WifiP2pManager.ChannelListener {
            override fun onChannelDisconnected() {
                if (manager != null && !retryChannel) {
                    _showToast.postValue("Channel lost. Trying again")
                    _resetData.postValue(Unit)
                    retryChannel = true
                    manager!!.initialize(c, c.mainLooper, this)
                } else {
                    _showToast.postValue("Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.")
                }
            }
        })
        registerReceiver()
    }

    override fun createGroup() {
        manager!!.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {  _createGroupResult.postValue(Result.success(0))}
            override fun onFailure(reason: Int) {_createGroupResult.postValue(Result.failure(Exception(getReasonInString(reason))))}
        })
    }

    override fun discoverPeers() {
        if(receiver == null){
            registerReceiver()
        }
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() { _discoverResult.postValue(Result.success(0)) }
            override fun onFailure(reason: Int) { _discoverResult.postValue(Result.failure(Exception(getReasonInString(reason)))) }
        })
    }


    override fun requestPeers(listener: WifiP2pManager.PeerListListener) {
        manager.requestPeers(channel, listener)
    }

    override fun sendText(text: String) {
        executorService.submit(CommandDataTransferServerRunnable(text))
    }

    override fun sendText(host: String, text: String) {
        executorService.submit(CommandDataTransferClientRunnable(host, text))
    }

    override fun sendFile(filePath: String) {
        executorService.submit(AudioFileTransferServerRunnable(filePath!!,object:AudioFileTransferServerRunnable.TransferListener{
            override fun onSucceeded() {
                _sendFileResult.postValue(Result.success(0))
            }
            override fun onFailed() {
                _sendFileResult.postValue(Result.failure(Exception()))
            }
        }))
    }

    override fun sendFile(host: String, filePath: String) {
        executorService.submit(AudioFileTransferClientRunnable(host, filePath!!, object:AudioFileTransferClientRunnable.TransferListener{
            override fun onSucceeded() {
                _sendFileResult.postValue(Result.success(0))
            }
            override fun onFailed() {
                _sendFileResult.postValue(Result.failure(Exception()))
            }
        }))
    }

    override fun receiveTransferData(filePath: String) {
        executorService.submit(TransferDataReceiverServerRunnable(filePath, object: TransferDataReceiverServerRunnable.TransferDataReceiveListener{
            override fun onPostExecute(data: TransferData) {
                _onTransferDataReceived.postValue(data)
            }
        }))
    }

    override fun receiveTransferData(host: String, filePath: String) {
        executorService.submit(TransferDataReceiverClientRunnable(host, filePath, object: TransferDataReceiverClientRunnable.TransferDataReceiveListener{
            override fun onPostExecute(data: TransferData) {
                _onTransferDataReceived.postValue(data)
            }
        }))
    }

    override fun connect(config: WifiP2pConfig) {
        manager!!.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                _connectResult.postValue(Result.success(0))
            }

            override fun onFailure(reason: Int) {
                _connectResult.postValue(Result.failure(Exception(getReasonInString(reason))))
                _showToast.postValue("Connect failed. Retry.")
            }
        })
    }

    override fun disconnect() {
        if(WifiP2pDevice.CONNECTED == deviceStatus){
            manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
                override fun onFailure(reason: Int) {
                    _disconnectResult.postValue(Result.failure(Exception(getReasonInString(reason))))
                }
                override fun onSuccess() {
                    _disconnectResult.postValue(Result.success(0))
                }
            })
        }else if(WifiP2pDevice.INVITED == deviceStatus){
            manager.cancelConnect(channel, object : WifiP2pManager.ActionListener {
                override fun onFailure(reason: Int) {
                    _disconnectResult.postValue(Result.failure(Exception(getReasonInString(reason))))
                }
                override fun onSuccess() {
                    _disconnectResult.postValue(Result.success(0))
                }
            })
        }
    }

    override fun cancelDisconnect() {
    }

    fun getReasonInString(reason: Int): String{
        when(reason){
            WifiP2pManager.P2P_UNSUPPORTED -> return "P2P_UNSUPPORTED"
            WifiP2pManager.ERROR -> return "ERROR"
            WifiP2pManager.BUSY -> return "BUSY"
        }
        return "UNKNOWN ERROR"
    }

    override fun resetReceiver() {
        unregisterReceiver()
        registerReceiver()
    }

    override fun registerReceiver(){
        receiver = WiFiDirectBroadcastReceiver(object: WiFiDirectBroadcastReceiver.WiFiDirectControllListener{
            override fun isManagerExist(): Boolean {
                return (manager != null)
            }
            override fun changeWifiP2pEnabled(enable: Boolean) {
                _isWifiP2pEnabled.postValue(enable)
            }
            override fun requestPeers() {
                val listener : (WifiP2pDeviceList) -> Unit = {list ->  _onPeersAveilable.postValue(list)}
                manager.requestPeers(channel, listener )
            }
            override fun requestConnectionInfo() {
                Log.w("repository","requestConnectionInfo")
                val listener : (WifiP2pInfo) -> Unit = {info -> _onConnectionInfoAvailable.postValue(info)}
                manager.requestConnectionInfo(channel, listener)
            }
            override fun updateThisDevice(device: WifiP2pDevice) {
                deviceStatus = device.status
                _updateThisDevice.postValue(device)
            }
            override fun resetBroadcastReceiver() {
                _resetData.postValue(Unit)
            }
        })

        context.registerReceiver(receiver, receiver.getIntentFilter())
    }

    override fun unregisterReceiver(){
        try{
            context.unregisterReceiver(receiver)
        }catch (e:IllegalArgumentException){
            // It called if BroadcastReceiver is not registered.
        }

    }

    companion object {

        private var INSTANCE: WiFiManageRepositoryImpl? = null

        /**
         * Returns the single instance of this class, creating it if necessary.
         * @param ForActivityCallback
         * @param ForPeerListCallback
         * @param ForDeviceDetailCallback
         * *
         * @return the [WiFiManageRepositoryImpl] instance
         */
        @JvmStatic fun getInstance(context: Context,  manager:WifiP2pManager) =
                INSTANCE ?: synchronized(WiFiManageRepositoryImpl::class.java) {
                    INSTANCE ?: WiFiManageRepositoryImpl(context, manager)
                            .also { INSTANCE = it }
                }

        @JvmStatic fun destroyInstance() {
            INSTANCE = null
        }
    }
}
