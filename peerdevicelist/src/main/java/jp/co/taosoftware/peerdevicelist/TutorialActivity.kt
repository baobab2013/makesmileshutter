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

import android.content.res.ColorStateList
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewpager.widget.ViewPager
import jp.co.taosoftware.peerdevicelist.prefs.TokenHolder
import jp.co.taosoftware.peerdevicelist.ui.adapters.ViewPagerAdapter
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        val motionLayout = findViewById<MotionLayout>(R.id.motionLayout)

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addPage(R.layout.layout_pager)
        adapter.addPage(R.layout.layout_pager)
        adapter.addPage(R.layout.layout_pager)
        pager.adapter = adapter
        if (motionLayout != null) {
            pager.addOnPageChangeListener(motionLayout as ViewPager.OnPageChangeListener)
            pager.addOnPageChangeListener(object:ViewPager.OnPageChangeListener{
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    btn_get_started.backgroundTintList = ColorStateList.valueOf(getColor(if(2 == position) R.color.colorPrimary else R.color.colorLightGray))
                    btn_get_started.setTextColor(getColor(if(2 == position) R.color.colorBlack else R.color.colorDarkGray))
                }

            })
        }

        btn_get_started.setOnClickListener{
            val currentItem = pager.currentItem
            if(2 == currentItem){
                val tokenHolder = TokenHolder(PreferenceManager.getDefaultSharedPreferences(this))
                tokenHolder.saveForTheFirstTime(false)
                finish()
            }else{
                pager.currentItem = currentItem+1
            }
        }
    }
}