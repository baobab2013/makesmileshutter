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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.TextUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import jp.co.taosoftware.makesmileshutter.phoneapp.ui.CameraSourcePreview
import jp.co.taosoftware.makesmileshutter.phoneapp.ui.FaceGraphic
import jp.co.taosoftware.makesmileshutter.phoneapp.ui.GraphicOverlay
import jp.co.taosoftware.peerdevicelist.MyApplication
import jp.co.taosoftware.peerdevicelist.network.Server
import jp.co.taosoftware.peerdevicelist.ui.ButtonLottieAnimationView
import jp.co.taosoftware.peerdevicelist.ui.ConnectManageBottomSheetDialog
import jp.co.taosoftware.peerdevicelist.ui.ProgressDialog
import jp.co.taosoftware.peerdevicelist.ui.ShutterLottieAnimationView
import jp.co.taosoftware.peerdevicelist.utils.Commands
import jp.co.taosoftware.peerdevicelist.utils.FileUtility
import jp.co.taosoftware.peerdevicelist.utils.VoiceUtility
import jp.co.taosoftware.peerdevicelist.viewmodel.ViewModelFactory
import jp.co.taosoftware.peerdevicelist.viewmodel.WiFiManageViewModelImpl
import kotlinx.android.synthetic.main.audio_device_detail.*
import kotlinx.android.synthetic.main.audio_device_detail.view.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * A fragment that manages a particular peer and allows interaction with device
 * i.e. setting up network connection and transferring data.
 */
class AudioDeviceDetailFragment : Fragment(), ConnectManageBottomSheetDialog.ConnectionRequestListener {


    companion object {
        val TAG  = "ADDFragment phone"
        val RC_HANDLE_GMS = 9001
    }

    private lateinit var SHARED_FILE_URL: String
    private var isRecording = false
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var soundFilePath: String
    private lateinit var contentView: View
    private lateinit var device: WifiP2pDevice
    private lateinit var info: WifiP2pInfo
    private var progressDialog: ProgressDialog? = null
    private lateinit var displayMetrics: DisplayMetrics
    private lateinit var deviceDetailViewModel: WiFiManageViewModelImpl

    // Camera
    private lateinit var cameraSourcePreview: CameraSourcePreview
    private lateinit var cameraSource: CameraSource
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var pictureCallback: CameraSource.PictureCallback

    inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }

    override fun onRequestedConnect(device: WifiP2pDevice) {
        this.device = device
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress
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
        activity?.let {
            val mapp = activity!!.application as MyApplication
            deviceDetailViewModel = ViewModelProviders.of(activity!!, ViewModelFactory.getInstance(activity!!,mapp.getWifiP2pManager())).get(WiFiManageViewModelImpl::class.java)

            deviceDetailViewModel.onConnectionInfoAvailable.observe(this,androidx.lifecycle.Observer {
                setHasOptionsMenu(true)
                onConnectionInfoAvailable(it)
            })

            deviceDetailViewModel.disconnectResult.observe(this,androidx.lifecycle.Observer {result ->
                result.onSuccess { view!!.visibility = View.GONE }
            })
            deviceDetailViewModel.transferDataReceived.observe(this,androidx.lifecycle.Observer {
                if (it != null) {
                    if (it.command != null && !TextUtils.isEmpty(it.command)) {
                        if (it.command == Commands.SHOOT) {
                            takeAPicture()
                        } else if (it.command == Commands.PLAY) {
                            startPlayer(SHARED_FILE_URL, VoiceUtility.NORMAL_PITCH_SPEED.first,  VoiceUtility.NORMAL_PITCH_SPEED.second)
                        } else if (it.command == Commands.PLAY_MONSTER) {
                            startPlayer(SHARED_FILE_URL, VoiceUtility.MONSTER_PITCH_SPEED.first, VoiceUtility.MONSTER_PITCH_SPEED.second)
                        } else if (it.command == Commands.PLAY_ALIEN) {
                            startPlayer(SHARED_FILE_URL, VoiceUtility.ALIEN_PITCH_SPEED.first, VoiceUtility.ALIEN_PITCH_SPEED.second)
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
            deviceDetailViewModel.resetData.observe(this, androidx.lifecycle.Observer {
                setHasOptionsMenu(false)
                resetViews()
            })
            deviceDetailViewModel.sendFileResult.observe(this, androidx.lifecycle.Observer {result ->
                result.onFailure {  }
                result.onSuccess { resetShutterButton() }
            })

        }

        return contentView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        init()
    }

    private fun init() {
        soundFilePath = activity!!.filesDir.toString() + File.separator + "mySound.wav"
        cameraSourcePreview = cameraPreview as CameraSourcePreview
        graphicOverlay = faceOverlay as GraphicOverlay
        normal.listener = object: ButtonLottieAnimationView.OnGeastureListener{
            override fun onClick() { sendText(Commands.PLAY) }
            override fun onLongClick() { startPlayer(soundFilePath, VoiceUtility.NORMAL_PITCH_SPEED.first, VoiceUtility.NORMAL_PITCH_SPEED.second) }
        }
        monster.listener = object: ButtonLottieAnimationView.OnGeastureListener{
            override fun onClick() { sendText(Commands.PLAY_MONSTER) }
            override fun onLongClick() { startPlayer(soundFilePath, VoiceUtility.MONSTER_PITCH_SPEED.first, VoiceUtility.MONSTER_PITCH_SPEED.second) }
        }
        alien.listener = object: ButtonLottieAnimationView.OnGeastureListener{
            override fun onClick() { sendText(Commands.PLAY_ALIEN) }
            override fun onLongClick() { startPlayer(soundFilePath, VoiceUtility.ALIEN_PITCH_SPEED.first, VoiceUtility.ALIEN_PITCH_SPEED.second) }
        }
        lottie_record_btn.setOnTouchListener(object:View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when (event!!.action){
                    MotionEvent.ACTION_DOWN -> {
                        startRecorder(isRecording)
                    }
                    MotionEvent.ACTION_UP -> {
                        stopRecorder(isRecording)
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        stopRecorder(isRecording)
                    }
                }
                return true
            }
        })

        // Unfortunately, LottieView doesn't support wrap_contents for now so we set the height programmatically.
        displayMetrics = resources.displayMetrics
        val params = lottie_mic.layoutParams as ConstraintLayout.LayoutParams
        params.height = displayMetrics.widthPixels/6
        lottie_mic.layoutParams = params

        shutter.listener = object: ShutterLottieAnimationView.OnGeastureListener{
            override fun onClick() { sendText(Commands.SHOOT) }
            override fun onLongClick() {
                resetShutterButton()
                takeAPicture()
            }
            override fun onCancel() { resetShutterButton() }
        }
        resetShutterButton()
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        cameraSourcePreview.stop()
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource.release()
        }
    }

    fun resetShutterButton(){
        // Shutter button shuold be gray out if sound file is not existing.
        shutter.frame = if(isExistingFile(soundFilePath)) 5 else 0
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
        var view = contentView.group_owner
        view.setText(R.string.empty)
        this.view!!.visibility = View.GONE

    }

    private fun startRecorder(isRecording: Boolean) {
        if (!isRecording) {
            this.isRecording = true
            deviceDetailViewModel.recordingStart()
            startRecorder()
        }
    }
    private fun startRecorder() {
        MediaRecorderPrepareTask().execute()
        lottie_mic.playAnimation()
        lottie_record_btn.playAnimation()
        lottie_mic.playAnimation()
        lottie_mic.postDelayed({
            if(isRecording){
                lottie_mic.setMinFrame(60)
            }
        }, 1000L)

    }

    private fun stopRecorder(isRecording: Boolean) {
        if (isRecording) {
            deviceDetailViewModel.recordingStop()
            stopRecorder()
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

        lottie_record_btn.pauseAnimation()
        lottie_record_btn.progress = 0f

        lottie_mic.setMinFrame(0)
        lottie_mic.pauseAnimation()
        lottie_mic.postDelayed({ lottie_mic.frame = 0},100L)

    }

    private fun startPlayer(path: String? = soundFilePath, pitch: Float = 0.7f, speed: Float = 0.5f) {
        if (!isExistingFile(path!!)) {
            return
        }
        val audioManager = activity!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) // 2019/1/21追記
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_PLAY_SOUND) // 2019/1/21追記
        audioManager.isSpeakerphoneOn = true

        val mediaPlayer = MediaPlayer()
        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(AudioManager.STREAM_MUSIC) // 2019/1/21追記
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
            try {
                releaseMediaRecorder()
            } catch (e: Exception) {
                Log.e(FileUtility.RECORDER_TAG, "Exception releasing MediaRecorder: " + e.message)
            }
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

    private fun isExistingFile(path: String): Boolean{
        var file: File = File(soundFilePath!!)
        return (file != null && file.exists())
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

    public fun createCameraSource(){
        val detector = FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build()

        detector.setProcessor(
                MultiProcessor.Builder<Face>(GraphicFaceTrackerFactory())
                        .build())

        if (!detector.isOperational) {
            // Note: The first time that an app using face API is installed on a device, GMS will
            // download a native library to the device in order to do detection.  Usually this
            // completes before the app is run for the first time.  But if that download has not yet
            // completed, then the above call will not detect any faces.
            //
            // isOperational() can be used to check if the required native library is currently
            // available.  The detector will automatically become operational once the library
            // download completes on device.
            Log.w(WiFiDirectActivity.TAG, "Face detector dependencies are not yet available.")
        }

        cameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .build()

        pictureCallback = CameraSource.PictureCallback { bytes ->
            var file_image = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/pics")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                if (!file_image.isDirectory) {
                    file_image.mkdir()
                }
                val df = SimpleDateFormat("_yyyy-MM-dd-hh-mm-ss")
                file_image = File(file_image, "mylastpic" + df.format(Date()) + ".jpg")
                try {
                    val fileOutputStream = FileOutputStream(file_image)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
                    fileOutputStream.flush()
                    fileOutputStream.close()
                } catch (exception: Exception) {
                    Toast.makeText(context, "Error saving: " + exception.toString(), Toast.LENGTH_LONG).show()
                }
                val contentUri = Uri.fromFile(file_image)
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri)
                context!!.sendBroadcast(mediaScanIntent)
            }
            isReady = true
        }
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context!!.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // requesting permission.
        }else{
            // check that the device has play services available.
            val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                    context)
            if (code != ConnectionResult.SUCCESS) {
                val dlg = GoogleApiAvailability.getInstance().getErrorDialog(activity, code, RC_HANDLE_GMS)
                dlg.show()
            }

            if (cameraSource != null) {
                try {
                    cameraSourcePreview.start(cameraSource, graphicOverlay)
                } catch (e: IOException) {
                    Log.e(TAG, "Unable to start camera source.", e)
                    cameraSource.release()
                }
            }
        }

    }
    private var isReady = true
    private fun takeAPicture() {
        if (isReady) {
            isReady = false
            cameraSource!!.takePicture(CameraSource.ShutterCallback { }, pictureCallback)
        }
    }

    //==============================================================================================
    // Graphic Face Tracker
    //==============================================================================================

    /**
     * Factory for creating a face tracker to be associated with a new face.  The multiprocessor
     * uses this factory to create face trackers as needed -- one for each individual.
     */
    private inner class GraphicFaceTrackerFactory : MultiProcessor.Factory<Face> {
        override fun create(face: Face): Tracker<Face> {
            return GraphicFaceTracker(graphicOverlay)
        }
    }

    /**
     * Face tracker for each detected individual. This maintains a face graphic within the app's
     * associated face overlay.
     */
    private inner class GraphicFaceTracker internal constructor(private val overlay: GraphicOverlay) : Tracker<Face>() {
        private val faceGraphic: FaceGraphic

        init {
            faceGraphic = FaceGraphic(overlay)
        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        override fun onNewItem(faceId: Int, item: Face?) {
            faceGraphic.setId(faceId)
        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        override fun onUpdate(detectionResults: Detector.Detections<Face>?, face: Face?) {
            overlay.add(faceGraphic)
            faceGraphic.updateFace(face)
            if (0.99f <= face!!.isSmilingProbability) {
//                takeAPicture()
            }
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        override fun onMissing(detectionResults: Detector.Detections<Face>?) {
            overlay.remove(faceGraphic)
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        override fun onDone() {
            overlay.remove(faceGraphic)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.action_detail_items, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when (item!!.itemId) {
            R.id.atn_disconnect -> {
                resetViews()
                deviceDetailViewModel.disconnect()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }
}