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
package jp.co.taosoftware.peerdevicelist.executors

import android.text.TextUtils
import android.util.Log
import jp.co.taosoftware.peerdevicelist.model.TransferData
import jp.co.taosoftware.peerdevicelist.network.Server
import jp.co.taosoftware.peerdevicelist.utils.FileUtility
import java.io.*
import java.net.ServerSocket

class TransferDataReceiverServerRunnable(private val filepath: String, private var listener:TransferDataReceiveListener?) : Runnable {

    companion object {
        val TAG = "TransferDataReceiver"
    }

    interface TransferDataReceiveListener {
        fun onPostExecute(data: TransferData)
    }

    private fun clearListener(){
        this.listener = null
    }

    override fun run() {
        val data = TransferData()
        try {
            ServerSocket(Server.PORT).use {serverSocket->
                serverSocket.accept().use {socket->
                    socket.reuseAddress = true
                    if (!Server.clients.contains(socket.inetAddress)) {
                        Server.clients.add(socket.inetAddress)
                    }
                    socket.getInputStream().use {inputstream ->
                        val input = FileUtility.getInputStreamByteArray(inputstream)
                        var text: String? = null
                        try {
                            ObjectInputStream(ByteArrayInputStream(input) as InputStream).use {oin -> text = oin.readObject() as String }
                        } catch (ioe: IOException) {
                        }
                        data.command = text
                        if (data.command == null || TextUtils.isEmpty(data.command)) {
                            val f = File(filepath)
                            val dirs = File(f.parent)
                            if (!dirs.exists())
                                dirs.mkdirs()
                            f.createNewFile()
                            FileOutputStream(f).use { fos -> fos.write(input) }
                            data.url = f.absolutePath
                        }
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, e.message)
        }
        if(listener != null){
            listener!!.onPostExecute(data)
        }
        clearListener()
    }


}
