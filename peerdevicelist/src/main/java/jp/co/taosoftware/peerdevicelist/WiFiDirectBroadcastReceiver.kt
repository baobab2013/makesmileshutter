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
package jp.co.taosoftware.peerdevicelist

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Parcelable

/**
 * A BroadcastReceiver that receives and callbacks wifi p2p events through WiFiDirectControllListener.
 */
class WiFiDirectBroadcastReceiver
/**
 * @param listener bridge between receiver and repository
 */
(private val listener:WiFiDirectControllListener) : BroadcastReceiver() {

    interface WiFiDirectControllListener {
        fun changeWifiP2pEnabled(enable: Boolean)
        fun requestPeers()
        fun requestConnectionInfo()
        fun updateThisDevice(device: WifiP2pDevice)
        fun resetBroadcastReceiver()
        fun isManagerExist():Boolean
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION == action) {
            // Wifi p2p status will be used in ui screen to show availability of WiFi Direct.
            val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi Direct mode is enabled
                listener.changeWifiP2pEnabled(true)
            } else {
                // Wifi Direct mode is disabled
                // ex. It's called when Wifi is turned off
                listener.changeWifiP2pEnabled(false)
                listener.resetBroadcastReceiver()
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION == action) {
            // Request available peers list from the wifi p2p manager to show peer device list.
            listener.requestPeers()
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == action) {
            if (!listener.isManagerExist()) {
                return
            }
            val networkInfo = intent
                    .getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as NetworkInfo
            if (networkInfo.isConnected) {
                // Connected with the other device so this app needs this request to find group owner IP
                listener.requestConnectionInfo()
            } else {
                // It's a disconnect
                listener.resetBroadcastReceiver()
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION == action) {
            listener.updateThisDevice(intent.getParcelableExtra<Parcelable>(
                    WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as WifiP2pDevice)
        }
    }

    fun getIntentFilter() : IntentFilter{
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        return intentFilter
    }
}