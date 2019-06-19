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
package jp.co.taosoftware.peerdevicelist.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.net.wifi.p2p.WifiP2pDevice
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import jp.co.taosoftware.peerdevicelist.R
import kotlinx.android.synthetic.main.layout_bottom_sheet_dialog.view.*

class ConnectManageBottomSheetDialog : BottomSheetDialogFragment() {

    /** Interface for callback invocation when user clicks connect from dialog  */
    interface ConnectionRequestListener {
        /**
         * The requested to connect to the device
         * @param device Wi-Fi p2p device
         */
        fun onRequestedConnect(device: WifiP2pDevice)
    }

    private lateinit var device: WifiP2pDevice
    private lateinit var listener: ConnectionRequestListener

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile private var INSTANCE: ConnectManageBottomSheetDialog? = null

        fun newInstance(device: WifiP2pDevice, listener:ConnectionRequestListener): ConnectManageBottomSheetDialog {

            if(INSTANCE == null) INSTANCE = ConnectManageBottomSheetDialog()
            INSTANCE!!.device = device
            INSTANCE!!.listener = listener
            return INSTANCE!!
        }
    }

    /**
     * Needed to put RestrictedApi to avoid warning bug as below.
     * setupDialog can only be called from within the same library group (groupId=androidx.appcompat)
     * @param dialog
     * @param style
     */
    @SuppressLint("RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)
        val view = View.inflate(context, R.layout.layout_bottom_sheet_dialog, null)
        view.bsd_title.text = device.deviceName+"("+device.deviceAddress+")"
        view.bsd_item_connect.setOnClickListener{
            dismiss()
            listener.onRequestedConnect(device)
        }
        dialog.setContentView(view)
    }

}