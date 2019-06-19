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
package jp.co.taosoftware.makesmileshutter.thetaplugin.repository

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fi.iki.elonen.NanoHTTPD
import jp.co.taosoftware.makesmileshutter.thetaplugin.repository.listener.WebServerRepository
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.StringBuilder
import java.nio.charset.StandardCharsets

class WebServerRepositoryImpl(private val context: Context): WebServerRepository {



    private var webServer: WebServer? = null
    private var device: WifiP2pDevice? = null
    private var devices: WifiP2pDeviceList? = null

    private var _isConnected:Boolean = false
    override fun isConnected(value: Boolean) {
        _isConnected = value
    }

    private val _startScan= MutableLiveData<Unit>()
    override fun startScan():LiveData<Unit> {return _startScan}
    private val _connect= MutableLiveData<Unit>()
    override fun connect():LiveData<Unit> {return _connect}
    private val _connectDevice = MutableLiveData<WifiP2pDevice>()
    override fun connectDevice(): LiveData<WifiP2pDevice> {return _connectDevice}
    private val _disconnect= MutableLiveData<Unit>()
    override fun disconnect():LiveData<Unit> {return _disconnect}

    init {
        webServer = WebServer(8888, context, object : WebServerListener {
            override fun startScan() {
                _startScan.postValue(Unit)
            }
            override fun connect() {
                _connect.postValue(Unit)
            }
            override fun connect(position:Int) {
                Log.w("WebServer","connect position:"+position)
                if(devices != null && position < devices!!.deviceList.size){
                    _connectDevice.postValue(devices!!.deviceList.toTypedArray()[position])
                }
            }
            override fun disconnect() {
                _disconnect.postValue(Unit)
            }
        })
    }

    override fun startWebServer() {
        try {
            if(webServer != null) {
                webServer!!.start()
            }
        } catch (e: IOException) {
        }
    }

    override fun updateThisDevice(device: WifiP2pDevice?) {
        this.device = device
    }

    override fun updateP2PDeviceList(devices: WifiP2pDeviceList?) {
        this.devices = devices
    }

    override fun stopWebServer() {
        if (webServer != null) {
            webServer!!.stop()
        }
    }
    interface WebServerListener {
        fun startScan()
        fun connect()
        fun connect(position:Int)
        fun disconnect()
    }
    inner class WebServer(port: Int, val context: Context, val listener:WebServerListener) : NanoHTTPD(port) {

        override fun serve(session: IHTTPSession?): Response {
            val method = session!!.method
            val uri = session.uri
            Log.w("WebServer","method:"+method+" uri:"+uri)
            when(uri){
                "/" -> return serveFile()
                "/startscan" ->{
                    listener.startScan()
                    return serveFile()
                }
                "/update" ->{
                    return serveFile()
                }
                "/connect" -> {
                    listener.connect()
                    return serveFile()
                }
                "/disconnect" -> {
                    listener.disconnect()
                    return serveFile()
                }
                else -> {
                    for (i in 1..30) {
                        if(("/device"+i).equals(uri)){
                            listener.connect(i-1)
                            return serveFile()
                        }
                    }
                }
            }
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain",
                    "Method [" + method + "] is not allowed.")
        }

        private fun serveFile(): NanoHTTPD.Response {
            var inputStream : InputStream? = null
            try {
//                inputStream = assetManager.open("index.html")  // NanoHTTPD will close this inputStream.
                inputStream = ByteArrayInputStream(generateHtml().toByteArray(StandardCharsets.UTF_8))
                return newChunkedResponse(Response.Status.OK, "text/html", inputStream)
            } catch (e: IOException) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.message)
            }
        }

        private fun generateHtml():String{
            var sb = StringBuilder()
            sb.append("<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "<meta name=\"viewport\" content=\"width=device-width, user scalable=no\" />\n" +
                    "<meta charset=\"utf-8\">\n" +
                    "<title>Make Smile Shutter</title>\n" +
                    "<script src='//ajax.googleapis.com/ajax/libs/jquery/1.8.2/jquery.min.js'></script>\n" +
                    "<script>\n" +
                    "\$(function(){\n" +
                    "\$(\"#startscan\").click(function(){\n" +
                    "\$.get(\"/startscan\");\n" +
                    "});\n" +
                    "\$(\"#disconnect\").click(function(){\n" +
                    "\$.get(\"/disconnect\");\n" +
                    "});\n" +
                    "for (var i = 1; i < 31; i++) {\n" +
                    "with ({i:i}) {\n" +
                    "\$(\"#device\"+i).click(function(){\n" +
                    "\$.get(\"/device\"+i);\n" +
                    "});\n" +
                    "}\n" +
                    "}\n" +
                    "setTimeout(\"location.reload()\", 3000);\n" +
                    "});\n" +
                    "</script>\n" +
                    "<style>\n" +
                    "* {\n" +
                    "    margin: 0;\n" +
                    "    padding: 0;\n" +
                    "}\n" +
                    "body {\n" +
                    "    color: #211103;\n" +
                    "    line-height: 1.5;\n" +
                    "    background-color: #fff;\n" +
                    "    padding: 1rem;\n" +
                    "}\n" +
                    "h1 {\n" +
                    "    font-size: 1.5rem;\n" +
                    "    margin-bottom: 1rem;\n" +
                    "}\n" +
                    "h2 {\n" +
                    "    font-size: 0.8rem;\n" +
                    "    margin-bottom: 0.5rem;\n" +
                    "}\n" +
                    "section {\n" +
                    "    margin-bottom: 2rem;\n" +
                    "}\n" +
                    ".list {\n" +
                    "    display: flex;\n" +
                    "    align-items: center;\n" +
                    "}\n" +
                    ".list p {\n" +
                    "    word-break: break-all;\n" +
                    "    flex-basis: calc(100% - 40px);\n" +
                    "}\n" +
                    ".list-multi {\n" +
                    "    justify-content: space-between;\n" +
                    "    border-bottom: solid 1px rgba(112, 112, 112, .24);\n" +
                    "    padding: 0.5rem 0;\n" +
                    "}\n" +
                    ".list-multi:active {\n" +
                    "    background-color: rgba(33, 17, 3, .24);\n" +
                    "}\n" +
                    ".list-multi p {\n" +
                    "    flex-basis: calc(100% - 40px);\n" +
                    "}\n" +
                    ".icon {\n" +
                    "    margin-right: 0.5rem;\n" +
                    "    flex-basis: 40px;\n" +
                    "}\n" +
                    ".pure-material-button-contained {\n" +
                    "    position: relative;\n" +
                    "    display: inline-block;\n" +
                    "    box-sizing: border-box;\n" +
                    "    border: none;\n" +
                    "    border-radius: 4px;\n" +
                    "    padding: 0 16px 0 40px;\n" +
                    "    min-width: 64px;\n" +
                    "    height: 36px;\n" +
                    "    vertical-align: middle;\n" +
                    "    text-align: center;\n" +
                    "    text-overflow: ellipsis;\n" +
                    "    text-transform: uppercase;\n" +
                    "    color: rgb(var(--pure-material-onprimary-rgb, 33, 17, 3));\n" +
                    "    background-color: rgb(var(--pure-material-primary-rgb, 239, 234, 7));\n" +
                    "    box-shadow: 0 3px 1px -2px rgba(0, 0, 0, 0.2), 0 2px 2px 0 rgba(0, 0, 0, 0.14), 0 1px 5px 0 rgba(0, 0, 0, 0.12);\n" +
                    "    font-family: var(--pure-material-font, \"Roboto\", \"Segoe UI\", BlinkMacSystemFont, system-ui, -apple-system);\n" +
                    "    font-size: 14px;\n" +
                    "    font-weight: 500;\n" +
                    "    line-height: 36px;\n" +
                    "    overflow: hidden;\n" +
                    "    outline: none;\n" +
                    "    cursor: pointer;\n" +
                    "    transition: box-shadow 0.2s;\n" +
                    "}\n" +
                    " .pure-material-button-contained::-moz-focus-inner {\n" +
                    " border: none;\n" +
                    "}\n" +
                    "/* Overlay */\n" +
                    ".pure-material-button-contained::before {\n" +
                    "    content: \"\";\n" +
                    "    position: absolute;\n" +
                    "    top: 0;\n" +
                    "    bottom: 0;\n" +
                    "    left: 0;\n" +
                    "    right: 0;\n" +
                    "    background-color: rgb(var(--pure-material-onprimary-rgb, 255, 255, 255));\n" +
                    "    opacity: 0;\n" +
                    "    transition: opacity 0.2s;\n" +
                    "}\n" +
                    "/* Ripple */\n" +
                    ".pure-material-button-contained::after {\n" +
                    "    content: \"\";\n" +
                    "    position: absolute;\n" +
                    "    left: 50%;\n" +
                    "    top: 50%;\n" +
                    "    border-radius: 50%;\n" +
                    "    padding: 50%;\n" +
                    "    width: 32px; /* Safari */\n" +
                    "    height: 32px; /* Safari */\n" +
                    "    background-color: rgb(var(--pure-material-onprimary-rgb, 255, 255, 255));\n" +
                    "    opacity: 0;\n" +
                    "    transform: translate(-50%, -50%) scale(1);\n" +
                    "    transition: opacity 1s, transform 0.5s;\n" +
                    "}\n" +
                    "/* Hover, Focus */\n" +
                    ".pure-material-button-contained:hover, .pure-material-button-contained:focus {\n" +
                    "    box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.2), 0 4px 5px 0 rgba(0, 0, 0, 0.14), 0 1px 10px 0 rgba(0, 0, 0, 0.12);\n" +
                    "}\n" +
                    ".pure-material-button-contained:hover::before {\n" +
                    "    opacity: 0.08;\n" +
                    "}\n" +
                    ".pure-material-button-contained:focus::before {\n" +
                    "    opacity: 0.24;\n" +
                    "}\n" +
                    ".pure-material-button-contained:hover:focus::before {\n" +
                    "    opacity: 0.3;\n" +
                    "}\n" +
                    "/* Active */\n" +
                    ".pure-material-button-contained:active {\n" +
                    "    box-shadow: 0 5px 5px -3px rgba(0, 0, 0, 0.2), 0 8px 10px 1px rgba(0, 0, 0, 0.14), 0 3px 14px 2px rgba(0, 0, 0, 0.12);\n" +
                    "}\n" +
                    ".pure-material-button-contained:active::after {\n" +
                    "    opacity: 0.32;\n" +
                    "    transform: translate(-50%, -50%) scale(0);\n" +
                    "    transition: transform 0s;\n" +
                    "}\n" +
                    "/* Disabled */\n" +
                    ".pure-material-button-contained:disabled {\n" +
                    "    color: rgba(var(--pure-material-onsurface-rgb, 0, 0, 0), 0.38);\n" +
                    "    background-color: rgba(var(--pure-material-onsurface-rgb, 0, 0, 0), 0.12);\n" +
                    "    box-shadow: none;\n" +
                    "    cursor: initial;\n" +
                    "}\n" +
                    ".pure-material-button-contained:disabled::before {\n" +
                    "    opacity: 0;\n" +
                    "}\n" +
                    ".pure-material-button-contained:disabled::after {\n" +
                    "    opacity: 0;\n" +
                    "}\n" +
                    ".icn_theta {\n" +
                    "        background-image: url('data:image/svg+xml;charset=utf8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2024%2024%22%20height%3D%2224%22%20width%3D%2224%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23211103%3B%7D.cls-2%7Bfill%3Anone%3B%7D%3C%2Fstyle%3E%3C%2Fdefs%3E%3Ctitle%3Eic_theta%3C%2Ftitle%3E%3Cg%20id%3D%22layer%22%20data-name%3D%22layer%22%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M12%2C3.92c-.12%2C0-.22%2C0-.33%2C0a1%2C1%2C0%2C1%2C1-1.5%2C1.12%2C2.05%2C2.05%2C0%2C0%2C0-.2.86%2C2%2C2%2C0%2C0%2C0%2C2%2C2%2C2%2C2%2C0%2C0%2C0%2C2-2A2%2C2%2C0%2C0%2C0%2C12%2C3.92Z%22%2F%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M15.45%2C1.5A11.86%2C11.86%2C0%2C0%2C0%2C12%2C1a10.88%2C10.88%2C0%2C0%2C0-3.4.5C7.94%2C1.78%2C7%2C2.24%2C7%2C3.08V21.77C7%2C22.61%2C7.65%2C23%2C8.56%2C23h6.89A1.4%2C1.4%2C0%2C0%2C0%2C17%2C21.77V3.08C17%2C2.3%2C16.29%2C1.78%2C15.45%2C1.5ZM12%2C15.35a2%2C2%2C0%2C1%2C1%2C2-2A2%2C2%2C0%2C0%2C1%2C12%2C15.35Zm0-6.43a3%2C3%2C0%2C0%2C1-3-3%2C3%2C3%2C0%2C0%2C1%2C3-3%2C3%2C3%2C0%2C0%2C1%2C3%2C3A3%2C3%2C0%2C0%2C1%2C12%2C8.92Z%22%2F%3E%3Crect%20class%3D%22cls-2%22%20width%3D%2224%22%20height%3D%2224%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E');\n" +
                    "        background-repeat: no-repeat;\n" +
                    "    flex-basis: 40px;\n" +
                    "    width: 40px;\n" +
                    "    height: 24px;\n" +
                    "}\n" +
                    ".icn_connect {\n" +
                    "    background-image: url('data:image/svg+xml;charset=utf8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2024%2024%22%20x%3D%220%22%20y%3D%220%22%20width%3D%2224%22%20height%3D%2224%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3Anone%3B%7D.cls-2%7Bfill%3A%23211103%3B%7D%3C%2Fstyle%3E%3C%2Fdefs%3E%3Ctitle%3Eic_connect%3C%2Ftitle%3E%3Cg%20id%3D%22layer%22%20data-name%3D%22layer%22%3E%3Crect%20class%3D%22cls-1%22%20width%3D%2224%22%20height%3D%2224%22%2F%3E%3Crect%20class%3D%22cls-1%22%20width%3D%2224%22%20height%3D%2224%22%2F%3E%3Cpath%20id%3D%22ic_link_24px%22%20data-name%3D%22ic%20link%2024px%22%20class%3D%22cls-2%22%20d%3D%22M3.9%2C12A3.1%2C3.1%2C0%2C0%2C1%2C7%2C8.9h4V7H7A5%2C5%2C0%2C0%2C0%2C7%2C17h4V15.1H7A3.1%2C3.1%2C0%2C0%2C1%2C3.9%2C12ZM8%2C13h8V11H8Zm9-6H13V8.9h4a3.1%2C3.1%2C0%2C1%2C1%2C0%2C6.2H13V17h4A5%2C5%2C0%2C0%2C0%2C17%2C7Z%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E');\n" +
                    "    background-repeat: no-repeat;\n" +
                    "    flex-basis: 60px;\n" +
                    "    width: 60px;\n" +
                    "    height: 24px;\n" +
                    "    background-position: right;\n" +
                    "}\n" +
                    ".icn_scan {\n" +
                    "    background-image: url('data:image/svg+xml;charset=utf8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2024%2024%22%20x%3D%220%22%20y%3D%220%22%20width%3D%2224%22%20height%3D%2224%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23211103%3B%7D.cls-2%7Bfill%3Anone%3B%7D%3C%2Fstyle%3E%3C%2Fdefs%3E%3Ctitle%3Eic_scan%3C%2Ftitle%3E%3Cg%20id%3D%22layer%22%20data-name%3D%22layer%22%3E%3Cpath%20id%3D%22ic_search_24px%22%20data-name%3D%22ic%20search%2024px%22%20class%3D%22cls-1%22%20d%3D%22M15.77%2C14.27H15L14.7%2C14a6.51%2C6.51%2C0%2C1%2C0-.7.7l.27.28v.79l5%2C5%2C1.49-1.49Zm-6%2C0a4.5%2C4.5%2C0%2C1%2C1%2C4.5-4.5%2C4.5%2C4.5%2C0%2C0%2C1-4.5%2C4.5Z%22%2F%3E%3Crect%20class%3D%22cls-2%22%20width%3D%2224%22%20height%3D%2224%22%2F%3E%3C%2Fg%3E%3C%2Fsvg%3E');\n" +
                    "    background-repeat: no-repeat;\n" +
                    "    background-position: 8px;\n" +
                    "}\n" +
                    ".icn_disconnect {\n" +
                    "    background-image: url('data:image/svg+xml;charset=utf8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20width%3D%2224%22%20height%3D%2224%22%20viewBox%3D%220%200%2024%2024%22%3E%3Cpath%20fill%3D%22none%22%20d%3D%22M0%200h24v24H0V0z%22%2F%3E%3Cpath%20d%3D%22M17%207h-4v1.9h4c1.71%200%203.1%201.39%203.1%203.1%200%201.43-.98%202.63-2.31%202.98l1.46%201.46C20.88%2015.61%2022%2013.95%2022%2012c0-2.76-2.24-5-5-5zm-1%204h-2.19l2%202H16zM2%204.27l3.11%203.11C3.29%208.12%202%209.91%202%2012c0%202.76%202.24%205%205%205h4v-1.9H7c-1.71%200-3.1-1.39-3.1-3.1%200-1.59%201.21-2.9%202.76-3.07L8.73%2011H8v2h2.73L13%2015.27V17h1.73l4.01%204L20%2019.74%203.27%203%202%204.27z%22%2F%3E%3Cpath%20fill%3D%22none%22%20d%3D%22M0%2024V0%22%2F%3E%3C%2Fsvg%3E');\n" +
                    "    background-repeat: no-repeat;\n" +
                    "    background-position: 8px;\n" +
                    "}\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "<section>\n" +
                    "<h1>Make Smile Shutter</h1>")

            if(!_isConnected){
                sb.append("  <input type=\"button\" value=\"Start Scan\" id=\"startscan\" class=\"pure-material-button-contained icn_scan\">")

            }else{
                sb.append( "  <input type=\"button\" value=\"Disconnect\" id=\"disconnect\" class=\"pure-material-button-contained icn_disconnect\">")
            }
            sb.append("</section>\n" +
                    "<section>\n" +
                    "  <h2>Me</h2>\n" +
                    "  <div class=\"list\">\n" +
                    "  <span class=\"icn_theta\"></span>")
            sb.append("  <p>")
            if(device == null){
                sb.append("My device name")
            }else{
                sb.append(device!!.deviceName+"("+device!!.deviceAddress+")")
            }
            sb.append("</p></div>\n" +
                    "</section>\n" +
                    "<section>\n" +
                    "  <h2>Peers</h2>")

            if(!_isConnected){
                if(devices != null && devices!!.deviceList != null){
                    var num = 0
                    for (device: WifiP2pDevice in devices!!.deviceList) {
                        num++
                        val deviceId = "device"+num
                        sb.append("  <div class=\"list list-multi\" id=\""+deviceId+"\">")
                        sb.append("    <p>"+device.deviceName+"("+device.deviceAddress+")"+"</p>")
                        sb.append("    <span class=\"icn_connect\"></span>\n" +
                                "  </div>")
                    }
                }
            }
            sb.append("</section>\n" +
                    "</body>\n" +
                    "</html>")

            return sb.toString()
        }
    }
}