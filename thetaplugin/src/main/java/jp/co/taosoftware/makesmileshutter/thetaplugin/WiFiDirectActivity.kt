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
package jp.co.taosoftware.makesmileshutter.thetaplugin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.theta360.pluginapplication.task.TakePictureTask
import com.theta360.pluginlibrary.activity.PluginActivity
import com.theta360.pluginlibrary.callback.KeyCallback
import jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel.WebServerViewModelFactory
import jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel.WebServerViewModelImpl
import jp.co.taosoftware.peerdevicelist.MyApplication
import jp.co.taosoftware.peerdevicelist.PeerDeviceListFragment
import jp.co.taosoftware.peerdevicelist.ui.ConnectManageBottomSheetDialog
import jp.co.taosoftware.peerdevicelist.viewmodel.ViewModelFactory
import jp.co.taosoftware.peerdevicelist.viewmodel.WiFiManageViewModelImpl
import kotlinx.android.synthetic.main.main.*


/**
 * An activity that uses WiFi Direct APIs to discover and connect with available
 * devices. WiFi Direct APIs are asynchronous and rely on callback mechanism
 * using interfaces to notify the application of operation success or failure.
 * The application should also register a BroadcastReceiver for notification of
 * WiFi state related events.
 */
class WiFiDirectActivity : PluginActivity(), PeerDeviceListFragment.DeviceActionListener{
    private var isWifiP2pEnabled = false
    private val shouldBeOwner = true
    private var behavior: BottomSheetBehavior<*>? = null

    private var myApplication: MyApplication? = null
    private lateinit var peerDeviceViewModel: WiFiManageViewModelImpl
    private lateinit var webServerViewModel: WebServerViewModelImpl

    private val mTakePictureTaskCallback = TakePictureTask.Callback { }

    private lateinit var bottomSheetDialog: ConnectManageBottomSheetDialog
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Coarse location permission is not granted!")
                finish()
            }
        }
    }

    fun setBotomSheetState(state: Int) {
        behavior!!.state = state
    }

    fun stateBottomSheetColappsed(){
        toolbar.setBackgroundColor(getColor(R.color.tool_bar_color))
    }

    fun stateBottomSheetExpanded(){
        toolbar.setBackgroundColor(getColor(R.color.tool_bar_color_yellow))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        setSupportActionBar(toolbar)

        behavior = BottomSheetBehavior.from(findViewById<View>(R.id.frag_detail))
        setBotomSheetState(BottomSheetBehavior.STATE_HIDDEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            behavior!!.setBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback(){
                override fun onSlide(p0: View, rate: Float) {
                    val scrollrate = if(0 > rate) 0.0f else rate
                    toolbar.setBackgroundColor(Color.rgb(239+((16*(1.0-scrollrate)).toInt()),234+((21*(1.0-scrollrate)).toInt()),7+((248*(1.0-scrollrate)).toInt())))
                }
                override fun onStateChanged(p0: View, state: Int) {
                    when (state) {
                        BottomSheetBehavior.STATE_COLLAPSED -> stateBottomSheetColappsed()
                        BottomSheetBehavior.STATE_EXPANDED -> stateBottomSheetExpanded()
                        BottomSheetBehavior.STATE_HIDDEN -> stateBottomSheetColappsed()
                    }
                }
            })
        }

        val wifi = getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifi.isWifiEnabled = true // true or false to activate/deactivate wifi

        myApplication = application as MyApplication

        // Set enable to close by pluginlibrary, If you set false, please call close() after finishing your end processing.
        setAutoClose(true)
        // Set a callback when a button operation event is acquired.
        setKeyCallback(object : KeyCallback {
            override fun onKeyDown(keyCode: Int, event: KeyEvent) {}

            override fun onKeyUp(keyCode: Int, event: KeyEvent) {}

            override fun onKeyLongPress(keyCode: Int, event: KeyEvent) {}
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WiFiDirectActivity.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
        }


        this?.let {
            //            peerDeviceViewModel = ViewModelProviders.of(it).get(WiFiManageViewModelImpl::class.java)
            peerDeviceViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance(this!!,myApplication!!.getWifiP2pManager())).get(WiFiManageViewModelImpl::class.java)
            webServerViewModel = ViewModelProviders.of(this, WebServerViewModelFactory.getInstance(this!!)).get(WebServerViewModelImpl::class.java)
            if (shouldBeOwner) {
                peerDeviceViewModel.createGroup()
            }
            peerDeviceViewModel.wifiP2pEnabled.observe(this,androidx.lifecycle.Observer {
                this.isWifiP2pEnabled = it
            })
            peerDeviceViewModel.onConnectionInfoAvailable.observe(this,androidx.lifecycle.Observer {
                behavior!!.isHideable = false
                stateBottomSheetExpanded()
                setBotomSheetState(BottomSheetBehavior.STATE_EXPANDED)
            })
            peerDeviceViewModel.resetData.observe(this,androidx.lifecycle.Observer {
                behavior!!.isHideable = true
                stateBottomSheetColappsed()
                setBotomSheetState(BottomSheetBehavior.STATE_HIDDEN)
            })
            peerDeviceViewModel.showToast.observe(this,androidx.lifecycle.Observer {
                Toast.makeText(applicationContext,it,Toast.LENGTH_LONG).show()
            })
            peerDeviceViewModel.onPeersAvailable.observe(this,androidx.lifecycle.Observer {
                webServerViewModel.updateP2PDeviceList(it)
            })
            peerDeviceViewModel.updateThisDevice.observe(this,androidx.lifecycle.Observer {
                webServerViewModel.updateThisDevice(it)
            })
            webServerViewModel.startWebServer()

            webServerViewModel.startScan.observe(this,androidx.lifecycle.Observer {
                val fragment = supportFragmentManager
                        .findFragmentById(R.id.frag_list) as PeerDeviceListFragment?
                fragment!!.startScan()
            })
        }
    }

    fun makeBottomSheetDialog(device: WifiP2pDevice, fragment: AudioDeviceDetailFragment) {
        bottomSheetDialog = ConnectManageBottomSheetDialog.newInstance(device, fragment)
        bottomSheetDialog.show(supportFragmentManager, bottomSheetDialog.tag)
    }

    /** register the BroadcastReceiver with the intent values to be matched  */
    public override fun onResume() {
        super.onResume()
        peerDeviceViewModel.resetReceiver()
    }

    public override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        webServerViewModel.stopWebServer()
        peerDeviceViewModel.unregisterReceiver()
    }

    fun shoot() {
        TakePictureTask(mTakePictureTaskCallback).execute()
    }

    override fun showDetails(device: WifiP2pDevice) {
        val fragment = supportFragmentManager
                .findFragmentById(R.id.frag_detail) as AudioDeviceDetailFragment?
        fragment!!.showDetails(device)
        makeBottomSheetDialog(device,fragment)
    }

    companion object {
        val TAG = "WiFiDirectActivity"
        private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001
    }
}