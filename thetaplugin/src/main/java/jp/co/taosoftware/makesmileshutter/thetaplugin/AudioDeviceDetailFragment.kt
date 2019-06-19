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

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel.WebServerViewModelFactory
import jp.co.taosoftware.makesmileshutter.thetaplugin.viewmodel.WebServerViewModelImpl
import jp.co.taosoftware.peerdevicelist.MyApplication
import jp.co.taosoftware.peerdevicelist.ui.ConnectManageBottomSheetDialog
import jp.co.taosoftware.peerdevicelist.ui.ProgressDialog
import jp.co.taosoftware.peerdevicelist.utils.Commands
import jp.co.taosoftware.peerdevicelist.utils.FileUtility
import jp.co.taosoftware.peerdevicelist.viewmodel.ViewModelFactory
import jp.co.taosoftware.peerdevicelist.viewmodel.WiFiManageViewModelImpl
import kotlinx.android.synthetic.main.audio_device_detail.*
import java.io.File

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
class AudioDeviceDetailFragment : Fragment(), ConnectManageBottomSheetDialog.ConnectionRequestListener {

    companion object {
        val TAG  = "AudioDeviceDetailFragment thetaplugin"
    }

    private lateinit var SHARED_FILE_URL: String
    private var isRecording = false
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var soundFilePath: String
    private lateinit var contentView: View
    private lateinit var device: WifiP2pDevice
    private lateinit var info: WifiP2pInfo
    internal var progressDialog: ProgressDialog? = null

    private lateinit var deviceDetailViewModel: WiFiManageViewModelImpl
    private lateinit var webServerViewModel: WebServerViewModelImpl

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onRequestedConnect(device: WifiP2pDevice) {
        this.device = device
        val config = WifiP2pConfig()
        config.deviceAddress = device!!.deviceAddress
        config.wps.setup = WpsInfo.PBC
        config.groupOwnerIntent = 15
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        progressDialog = ProgressDialog.newInstance("Connecting to :" + device.deviceAddress)
        progressDialog!!.show(activity!!.supportFragmentManager, "tag")
        deviceDetailViewModel.connect(config)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        SHARED_FILE_URL = File(context!!.getExternalFilesDir("received"),
                "$FileUtility.SHARED_FILENAME.wav").absolutePath
        contentView = inflater.inflate(R.layout.audio_device_detail, null)
        init(contentView)

        activity?.let {
            val mapp = activity!!.application as MyApplication
            deviceDetailViewModel = ViewModelProviders.of(activity!!, ViewModelFactory.getInstance(activity!!,mapp.getWifiP2pManager())).get(WiFiManageViewModelImpl::class.java)

            deviceDetailViewModel.onConnectionInfoAvailable.observe(this,androidx.lifecycle.Observer {
                setHasOptionsMenu(true)
                onConnectionInfoAvailable(it)
                webServerViewModel.isConnected(true)
            })

            deviceDetailViewModel.disconnectResult.observe(this,androidx.lifecycle.Observer {result ->
                result.onSuccess { view!!.visibility = View.GONE }
            })

            deviceDetailViewModel.transferDataReceived.observe(this,androidx.lifecycle.Observer {
                if (it != null) {
                    if (it.command != null && !TextUtils.isEmpty(it.command)) {
                        if (it.command == Commands.SHOOT) {
                            (activity as WiFiDirectActivity).shoot()
                        } else if (it.command == Commands.PLAY) {
                            startPlayer(SHARED_FILE_URL, 1.0f, 1.0f)
                        } else if (it.command == Commands.PLAY_MONSTER) {
                            startPlayer(SHARED_FILE_URL, 2.0f, 0.5f)
                        } else if (it.command == Commands.PLAY_ALIEN) {
                            startPlayer(SHARED_FILE_URL, 4.0f, 1.0f)
                        }
                        // This method needs to receive next command.
                        deviceDetailViewModel.resetReceiver()
                    } else {
                        if (it.url != null) {
                            startPlayer(it.url, 1.0f, 1.0f)
                            // This method needs to receive next audio.
                            deviceDetailViewModel.resetReceiver()
                        }
                    }
                }
            })
            deviceDetailViewModel.resetData.observe(this,androidx.lifecycle.Observer {
                setHasOptionsMenu(false)
                resetViews()
                webServerViewModel.updateP2PDeviceList(null)
                webServerViewModel.isConnected(false)
            })
            webServerViewModel = ViewModelProviders.of(activity!!, WebServerViewModelFactory.getInstance(activity!!)).get(WebServerViewModelImpl::class.java)
            webServerViewModel.connect.observe(this,androidx.lifecycle.Observer {
                onRequestedConnect(device)
            })
            webServerViewModel.connectDevice.observe(this,androidx.lifecycle.Observer {
                onRequestedConnect(it)
            })
            webServerViewModel.disconnect.observe(this,androidx.lifecycle.Observer {
                disconnect()
            })
        }
        return contentView
    }

    private fun init(root:View) {
        soundFilePath = activity!!.filesDir.toString() + File.separator + "mySound.wav"
        root.findViewById<View>(R.id.start_record).alpha = 1.0f
        root.findViewById<View>(R.id.stop_record).alpha = 0.5f
        root.findViewById<View>(R.id.start_record).setOnClickListener {
            if (!isRecording) {
                startRecorder()
                root.findViewById<View>(R.id.start_record).alpha = 0.5f
                root.findViewById<View>(R.id.stop_record).alpha = 1.0f
            }
        }
        root!!.findViewById<View>(R.id.stop_record).setOnClickListener {
            if (isRecording) {
                stopRecorder()
                root.findViewById<View>(R.id.start_record).alpha = 1.0f
                root.findViewById<View>(R.id.stop_record).alpha = 0.5f
            } else if (!isRecording && mediaRecorder == null) {
                root.findViewById<View>(R.id.start_record).alpha = 1.0f
                root.findViewById<View>(R.id.stop_record).alpha = 0.5f
            }
        }
        root.findViewById<View>(R.id.send).setOnClickListener { sendFile() }
        root.findViewById<View>(R.id.play).setOnClickListener { startPlayer() }
        root.findViewById<View>(R.id.shot).setOnClickListener { sendText(Commands.SHOOT) }
        root.findViewById<View>(R.id.normal).setOnClickListener { sendText(Commands.PLAY) }
        root.findViewById<View>(R.id.monster).setOnClickListener { sendText(Commands.PLAY_MONSTER) }
        root.findViewById<View>(R.id.alien).setOnClickListener { sendText(Commands.PLAY_ALIEN) }
    }

    private fun sendText(text: String) {
        if (info.groupFormed && info.isGroupOwner) {
            deviceDetailViewModel.sendText(text)
        } else if (info.groupFormed && !info.isGroupOwner) {
            deviceDetailViewModel.sendText(info.groupOwnerAddress.hostAddress,text)
        }
    }

    private fun sendFile() {
        if (info.groupFormed && info.isGroupOwner) {
            deviceDetailViewModel.sendFile(soundFilePath)
        } else if (info.groupFormed && !info.isGroupOwner) {
            deviceDetailViewModel.sendFile(info.groupOwnerAddress.hostAddress, soundFilePath)
        }
    }

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
        }
        this.info = info

        Log.w("onConnectionInfoAvailable", "" + info)
        this.view!!.visibility = View.VISIBLE
        // The owner IP is now known.
        var view = contentView.findViewById<View>(R.id.group_owner) as TextView
        view.text = resources.getString(R.string.group_owner_text) + if (info.isGroupOwner == true)
            resources.getString(R.string.yes)
        else
            resources.getString(R.string.no)
        // After the group negotiation, we assign the group owner as the file
        // server. The file server is single threaded, single connection server
        // socket.
        if (info.groupFormed && info.isGroupOwner) {
            deviceDetailViewModel.receiveTransferData(File(context!!.getExternalFilesDir("received"),
                    "$FileUtility.SHARED_FILENAME.wav").absolutePath)
        } else if (info.groupFormed && !info.isGroupOwner) {
            // This device will acts as client.
            deviceDetailViewModel.receiveTransferData(info.groupOwnerAddress.hostAddress, File(context!!.getExternalFilesDir("received"),
                    "$FileUtility.SHARED_FILENAME.wav").absolutePath)
        }
    }

    /**
     * Updates the UI with device data
     *
     * @param device the device to be displayed
     */
    fun showDetails(device: WifiP2pDevice) {
        this.device = device
        this.view!!.visibility = View.VISIBLE
    }

    /**
     * Clears the UI fields after a disconnect or direct mode disable operation.
     */
    fun resetViews() {
        var view = contentView.findViewById<View>(R.id.group_owner) as TextView
        view.setText(R.string.empty)
        this.view!!.visibility = View.GONE
    }

    private fun startRecorder(isRecording: Boolean) {
        if (!isRecording) {
            this.isRecording = true
            startRecorder()
        }
    }
    private fun startRecorder() {
        MediaRecorderPrepareTask().execute()
        start_record.alpha = 0.5f
        stop_record.alpha = 1.0f
    }

    private fun stopRecorder(isRecording: Boolean) {
        if (isRecording) {
            stopRecorder()
            contentView.findViewById<View>(R.id.start_record).alpha = 1.0f
            contentView.findViewById<View>(R.id.stop_record).alpha = 0.5f
        } else if (!isRecording && mediaRecorder == null) {
            contentView.findViewById<View>(R.id.start_record).alpha = 1.0f
            contentView.findViewById<View>(R.id.stop_record).alpha = 0.5f
        }
        sendFile()
    }

    private fun stopRecorder() {
        try {
            if(mediaRecorder != null){
                mediaRecorder.stop()
            }
        } catch (e: RuntimeException) {
            Log.e(FileUtility.RECORDER_TAG, "RuntimeException: stop() is called immediately after start()")
            deleteSoundFile()
        } finally {
            isRecording = false
            releaseMediaRecorder()
        }
    }

    private fun startPlayer(path: String? = soundFilePath, pitch: Float = 0.7f, speed: Float = 0.5f) {
        var file: File = File(path)
        if (!file.exists()) {
            return
        }
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM) // 2019/1/21追記
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, AudioManager.FLAG_PLAY_SOUND) // 2019/1/21追記
        audioManager.isSpeakerphoneOn = true

        val mediaPlayer = MediaPlayer()
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_ALARM) // 2019/1/21追記
                .build()
        try {
            mediaPlayer.setAudioAttributes(attributes)
            mediaPlayer.setDataSource(path)
            mediaPlayer.setVolume(1.0f, 1.0f)
            mediaPlayer.setOnCompletionListener { mp -> mp.release() }
            mediaPlayer.setOnPreparedListener { mp ->
                val params = mp.playbackParams
                params.pitch = pitch
                params.speed = speed
                mp.playbackParams = params
                mp.start()
            }
            mediaPlayer.prepare()
        } catch (e: Exception) {
            mediaPlayer.release()
        }

    }

    private fun prepareMediaRecorder(): Boolean {

        deleteSoundFile()
        // Switch theta mic from stero to Mono
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setParameters("RicUseBFormat=false")

        mediaRecorder = MediaRecorder()
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setAudioSamplingRate(44100) // 2019/1/21追記
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        mediaRecorder.setOutputFile(soundFilePath)

        try {
            mediaRecorder.prepare()
        } catch (e: Exception) {
            Log.e(FileUtility.RECORDER_TAG, "Exception preparing MediaRecorder: " + e.message)
            releaseMediaRecorder()
            return false
        }

        return true
    }

    private fun releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset()
            mediaRecorder.release()
        }
    }

    private fun deleteSoundFile() {
        var file: File = File(soundFilePath!!)
        if (file != null && file.exists()) {
            file.delete()
        }
    }

    /**
     * Asynchronous task for preparing the [MediaRecorder] since it's a long
     * blocking operation.
     */
    private inner class MediaRecorderPrepareTask : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg voids: Void): Boolean? {
            if (prepareMediaRecorder()) {
                if (mediaRecorder != null) {
                    mediaRecorder.start()
                    isRecording = true
                }
                return true
            }
            return false
        }

        override fun onPostExecute(result: Boolean?) {
            if (!result!!) {
                Log.e(FileUtility.RECORDER_TAG, "MediaRecorder prepare failed")
                return
            }
        }
    }

    private fun disconnect(){
        resetViews()
        deviceDetailViewModel.disconnect()
        webServerViewModel.updateP2PDeviceList(null)
        webServerViewModel.isConnected(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.action_detail_items, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.atn_disconnect -> {
                disconnect()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

}