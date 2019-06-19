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
package jp.co.taosoftware.makesmileshutter.phoneapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pDevice
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import jp.co.taosoftware.peerdevicelist.MyApplication
import jp.co.taosoftware.peerdevicelist.PeerDeviceListFragment
import jp.co.taosoftware.peerdevicelist.TutorialActivity
import jp.co.taosoftware.peerdevicelist.behavior.UserLockBottomSheetBehavior
import jp.co.taosoftware.peerdevicelist.prefs.TokenHolder
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
class WiFiDirectActivity : AppCompatActivity(), PeerDeviceListFragment.DeviceActionListener  {
    private var isWifiP2pEnabled = false
    private val shouldBeOwner = false
    private var behavior: UserLockBottomSheetBehavior<*>? = null

    private var toolbarTitleMargin: Int = 0
    private var myApplication: MyApplication? = null
    private lateinit var peerDeviceViewModel: WiFiManageViewModelImpl
    private lateinit var bottomSheetDialog: ConnectManageBottomSheetDialog


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION -> {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "Coarse location permission is not granted!")
                    finish()
                }else{
                    createCameraSource()
                }
            }
        }
    }

    fun setBotomSheetState(state: Int) {
        behavior!!.state = state
    }

    fun stateBottomSheetColappsed(){
        toolbar.setBackgroundColor(getColor(R.color.tool_bar_color))
        toolbar.contentInsetStartWithNavigation = 0
        supportActionBar!!.setDisplayHomeAsUpEnabled(false)
    }

    fun stateBottomSheetExpanded(){
        toolbar.setBackgroundColor(getColor(R.color.tool_bar_color_yellow))
        toolbar.contentInsetStartWithNavigation = toolbarTitleMargin!!
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        setContentView(R.layout.main)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }
        val dm = resources.displayMetrics
        toolbarTitleMargin = ((dm.widthPixels-((48.0f*dm.density)+(48.0f*dm.density)+(80.0f*dm.density)))/2.0f).toInt()
        behavior = BottomSheetBehavior.from(findViewById<View>(R.id.frag_detail)) as UserLockBottomSheetBehavior
        setBotomSheetState(BottomSheetBehavior.STATE_HIDDEN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            behavior!!.setBottomSheetCallback(object: BottomSheetCallback(){
                override fun onSlide(p0: View, rate: Float) {
                    Log.w("BottomSheetCallback","onSlide rate:"+rate+" translationY:"+p0.translationY)
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

        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifi.isWifiEnabled = true // true or false to activate/deactivate wifi

        myApplication = application as MyApplication

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // After this point you wait for callback in
            // onRequestPermissionsResult(int, String[], int[]) overridden method
            requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.RECORD_AUDIO
                    , Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                    WiFiDirectActivity.PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION)

        }else{
            createCameraSource()
        }

        this?.let {
            // peerDeviceViewModel = ViewModelProviders.of(it).get(WiFiManageViewModelImpl::class.java)
            peerDeviceViewModel = ViewModelProviders.of(this, ViewModelFactory.getInstance(this!!,myApplication!!.getWifiP2pManager())).get(WiFiManageViewModelImpl::class.java)
            if (shouldBeOwner) {
                peerDeviceViewModel.createGroup()
            }
            peerDeviceViewModel.wifiP2pEnabled.observe(this,androidx.lifecycle.Observer {
                this.isWifiP2pEnabled = it
            })
            peerDeviceViewModel.peersCount.observe(this, Observer {
                it?.let {
                    Toast.makeText(applicationContext,""+it+ "devices found", Toast.LENGTH_LONG).show()
                }
            })
            peerDeviceViewModel.recordingStatusChanged.observe(this, Observer {
                behavior!!.allowUserTouch = it
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
        }

        val tokenHolder = TokenHolder(PreferenceManager.getDefaultSharedPreferences(this))
        if( tokenHolder.forTheFirstTime){
            startActivity(Intent(this,TutorialActivity::class.java))
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
        peerDeviceViewModel.unregisterReceiver()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    fun shoot() {

    }

    override fun onBackPressed() {
        if(BottomSheetBehavior.STATE_EXPANDED == behavior!!.state){
            behavior!!.state = BottomSheetBehavior.STATE_COLLAPSED
        }else{
            super.onBackPressed()
        }
    }

    override fun showDetails(device: WifiP2pDevice) {
        val fragment = supportFragmentManager
                .findFragmentById(R.id.frag_detail) as AudioDeviceDetailFragment?
        fragment!!.showDetails(device)
        makeBottomSheetDialog(device,fragment)
    }

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     */
    private fun createCameraSource() {
        val fragment = supportFragmentManager
                .findFragmentById(R.id.frag_detail) as AudioDeviceDetailFragment?
        fragment!!.createCameraSource()
    }

    companion object {
        val TAG = "WiFiDirectActivity"
        private val PERMISSIONS_REQUEST_CODE_ACCESS_COARSE_LOCATION = 1001
    }
}