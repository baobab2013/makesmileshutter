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
import android.net.wifi.p2p.WifiP2pManager
import jp.co.taosoftware.peerdevicelist.repository.Listener.WiFiManageRepository
import jp.co.taosoftware.peerdevicelist.repository.WiFiManageRepositoryImpl

object Injection {

    fun provideWiFiManagerRepositoryImpl(context: Context, manager: WifiP2pManager): WiFiManageRepositoryImpl {
        return WiFiManageRepositoryImpl.getInstance(context,manager)
    }
}