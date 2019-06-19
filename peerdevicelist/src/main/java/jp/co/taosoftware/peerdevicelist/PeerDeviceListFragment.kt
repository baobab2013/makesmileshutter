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


import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.ListFragment
import androidx.lifecycle.ViewModelProviders
import jp.co.taosoftware.peerdevicelist.ui.ProgressDialog
import jp.co.taosoftware.peerdevicelist.viewmodel.ViewModelFactory
import jp.co.taosoftware.peerdevicelist.viewmodel.WiFiManageViewModelImpl
import kotlinx.android.synthetic.main.device_list.*
import kotlinx.android.synthetic.main.device_list.view.*
import kotlinx.android.synthetic.main.row_devices.view.*
import java.util.*

/**
 * A ListFragment that displays available peers on discovery and requests the
 * parent activity to handle user interaction events
 */
class PeerDeviceListFragment : ListFragment() {
    val TAG = "PeerDeviceListFragment"

    private lateinit var peerDeviceViewModel: WiFiManageViewModelImpl
    private val peers = ArrayList<WifiP2pDevice>()
    internal var progressDialog: ProgressDialog? = null
    internal lateinit var contentView: View

    /**
     * @return this device
     */
    var device: WifiP2pDevice? = null
        private set

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        this.listAdapter = WiFiPeerListAdapter(activity!!.applicationContext, R.layout.row_devices, peers)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        activity?.let {
            val mapp = activity!!.application as MyApplication
            setHasOptionsMenu(true)
            peerDeviceViewModel = ViewModelProviders.of(activity!!, ViewModelFactory.getInstance(activity!!,mapp.getWifiP2pManager())).get(WiFiManageViewModelImpl::class.java)
            peerDeviceViewModel.onPeersAvailable.observe(this,androidx.lifecycle.Observer {
                onPeersAvailable(it)
            })
            peerDeviceViewModel.updateThisDevice.observe(this,androidx.lifecycle.Observer {
                updateThisDevice(it)
            })
            peerDeviceViewModel.resetData.observe(this,androidx.lifecycle.Observer {
                clearPeers()
                setHasOptionsMenu(true)
            })
            peerDeviceViewModel.peersCount.observe(this,androidx.lifecycle.Observer {
                if(it == 0){
                    btn_start_scan.visibility = View.VISIBLE
                }else{
                    btn_start_scan.visibility = View.GONE
                }
                Toast.makeText(context,""+it+ if(1<it) " devices" else " device"+" found", Toast.LENGTH_LONG).show()
            })
            peerDeviceViewModel.onConnectionInfoAvailable.observe(this,androidx.lifecycle.Observer {
                setHasOptionsMenu(false)
            })
        }

        contentView = inflater.inflate(R.layout.device_list, null)
        contentView.findViewById<View>(R.id.btn_start_scan).setOnClickListener{
            onInitiateDiscovery()
            peerDeviceViewModel.discoverPeers()
        }
        return contentView
    }

    private fun getDeviceStatus(deviceStatus: Int): String {
        when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> return "Available"
            WifiP2pDevice.INVITED -> return "Invited"
            WifiP2pDevice.CONNECTED -> return "Connected"
            WifiP2pDevice.FAILED -> return "Failed"
            WifiP2pDevice.UNAVAILABLE -> return "Unavailable"
            else -> return "Unknown"
        }
    }

    private fun getDeviceStatusColor(deviceStatus: Int): Int {
        when (deviceStatus) {
            WifiP2pDevice.AVAILABLE -> return R.color.colorAvailable
            WifiP2pDevice.INVITED -> return R.color.colorAvailable
            WifiP2pDevice.CONNECTED -> return R.color.colorConnected
            WifiP2pDevice.FAILED -> return R.color.colorAvailable
            WifiP2pDevice.UNAVAILABLE -> return R.color.colorAvailable
            else -> return R.color.colorAvailable
        }
    }

    /**
     * Initiate a connection with the peer.
     */
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val device = listAdapter.getItem(position) as WifiP2pDevice
        (activity as DeviceActionListener).showDetails(device)
    }

    /**
     * Array adapter for ListFragment that maintains WifiP2pDevice list.
     */
    private inner class WiFiPeerListAdapter

    /**
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    (context: Context, textViewResourceId: Int,
     private val items: List<WifiP2pDevice>) : ArrayAdapter<WifiP2pDevice>(context, textViewResourceId, items) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var v = convertView
            if (v == null) {
                val vi = context.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
                v = vi.inflate(R.layout.row_devices, null)
            }
            val device = items[position]
            if (device != null) {
                v!!.ic_device_status.imageTintList = ColorStateList.valueOf(context!!.getColor(getDeviceStatusColor(device.status)))
                v!!.device_name.text = device.deviceName
                v!!.device_details.text = getDeviceStatus(device.status)
            }
            return v!!
        }
    }

    /**
     * Update UI for this device.
     *
     * @param device WifiP2pDevice object
     */
    private fun updateThisDevice(device: WifiP2pDevice) {
        this.device = device
        var view = contentView.my_name
        view.text = device.deviceName
        view = contentView.my_status
        view.text = getDeviceStatus(device.status)
        val imageview = contentView.ic_my_status
        imageview.imageTintList = ColorStateList.valueOf(context!!.getColor(getDeviceStatusColor(device.status)))
        wifi_setting.setOnClickListener{
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    private fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        onPeersAvailable(peerList.deviceList)
    }

    private fun onPeersAvailable(deviceList: Collection<WifiP2pDevice>) {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        peers.clear()
        peers.addAll(deviceList)
        (listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()
    }

    fun clearPeers() {
        peers.clear()
        (listAdapter as WiFiPeerListAdapter).notifyDataSetChanged()
        btn_start_scan.visibility = View.VISIBLE
    }

    /**
     *
     */
    fun onInitiateDiscovery() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        progressDialog = ProgressDialog.newInstance("finding peers")
        progressDialog!!.show(activity!!.supportFragmentManager, "tag")

    }

    fun startScan(){
        onInitiateDiscovery()
        peerDeviceViewModel.discoverPeers()
    }

    /**
     * An interface-callback for the activity to listen to fragment interaction
     * events.
     */
    interface DeviceActionListener {
        fun showDetails(device: WifiP2pDevice)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.action_discover_items, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.atn_direct_discover -> {
                startScan()
                return true
            }
            R.id.atn_tutorial -> {
                startActivity(Intent(activity,TutorialActivity::class.java))
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}