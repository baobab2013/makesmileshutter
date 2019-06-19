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
package jp.co.taosoftware.peerdevicelist.utils

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object FileUtility{
    val RECORDER_TAG = "Recorder"
    val PLAYER_TAG = "Player"
    val SHARED_FILENAME = "wifip2pshared"

    /**
     *
     * @param inputStream
     * @param out
     * @return isSucceeded
     */
    fun copyFile(inputStream: InputStream, outputStream: OutputStream): Boolean {
        try {
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            outputStream.close()
            inputStream.close()
        } catch (e: IOException) {
            Log.e("copyFile", e.message)
            return false
        }
        return true
    }

    fun copyFile(byteArrayOutputStream: ByteArrayOutputStream, out: OutputStream): Boolean {
        try {
            byteArrayOutputStream.writeTo(out)
            out.close()
        } catch (e: IOException) {
            Log.e("copyFile", e.message)
            return false
        }

        return true
    }

    fun getInputStreamByteArray(inputStream: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            inputStream.use { input ->
                baos.use { output ->
                    input.copyTo(output)
                }
            }
            baos.flush()
        } catch (ioe: IOException) {
        } finally {
            try {
                inputStream.close()
            } catch (e: IOException) {
            }
        }
        return baos.toByteArray()
    }
}