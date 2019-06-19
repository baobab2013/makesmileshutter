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
package jp.co.taosoftware.peerdevicelist.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import com.airbnb.lottie.LottieAnimationView

class ShutterLottieAnimationView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LottieAnimationView(context, attrs, defStyleAttr) {

    var listener :OnGeastureListener? = null
    interface OnGeastureListener {
        /**
         * Called when a view has been clicked.
         */
        fun onClick()

        /**
         * Called when a view has been long clicked.
         */
        fun onLongClick()

        /**
         * Called when a view has been canceled.
         */
        fun onCancel()
    }

    private lateinit var gestureDetector: GestureDetector
    init {
        // frame 0-4 is disable, 5-8 is normal, 9- is touched
        gestureDetector = GestureDetector(context, ButtonGestureDetector())
        setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event!!.action){
                MotionEvent.ACTION_DOWN -> {
                    frame = 9
                }
                MotionEvent.ACTION_UP -> {
                    if(listener != null){
                        listener!!.onCancel()
                    }
                }
                MotionEvent.ACTION_CANCEL -> {
                    if(listener != null){
                        listener!!.onCancel()
                    }
                }
            }
            true
        }
    }

    inner class ButtonGestureDetector: GestureDetector.OnGestureListener{
        override fun onShowPress(e: MotionEvent?) {}
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            if(listener != null){
                listener!!.onClick()
            }
            return true }
        override fun onDown(e: MotionEvent?): Boolean {return true}
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {return true}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            if(listener != null){
                listener!!.onCancel()
            }
            return true}
        override fun onLongPress(e: MotionEvent?) {
            if(listener != null){
                listener!!.onLongClick()
            }
        }
    }

}