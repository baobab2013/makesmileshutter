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

class ButtonLottieAnimationView @JvmOverloads constructor(
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
    }

    private lateinit var gestureDetector: GestureDetector
    init {
        // frame 0-5 is clicked, 5-15 is long clicked
        gestureDetector = GestureDetector(context, ButtonGestureDetector())
        setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event!!.action){
                MotionEvent.ACTION_DOWN -> {
                    setMinFrame(0)
                    setMaxFrame(4)
                    playAnimation()
                }
                MotionEvent.ACTION_UP -> {
                }
                MotionEvent.ACTION_CANCEL -> {
                    setMinFrame(0)
                    setMaxFrame(4)
                    progress = 0f
                    pauseAnimation()
                }
            }
            true
        }
    }

    inner class ButtonGestureDetector: GestureDetector.OnGestureListener{
        override fun onShowPress(e: MotionEvent?) {}
        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            handler.postDelayed({
                setMinFrame(0)
                setMaxFrame(4)
                progress = 0f
                pauseAnimation()
            },250L)
            if(listener != null){
                listener!!.onClick()
            }
            return true }
        override fun onDown(e: MotionEvent?): Boolean {return true}
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {return true}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            setMinFrame(0)
            setMaxFrame(4)
            progress = 0f
            pauseAnimation()
            return true}
        override fun onLongPress(e: MotionEvent?) {
            setMaxFrame(15)
            setMinFrame(5)
            playAnimation()
            handler.postDelayed({
                setMinFrame(0)
                setMaxFrame(4)
                progress = 0f
                pauseAnimation()
            },800L)
            if(listener != null){
                listener!!.onLongClick()
            }
        }
    }
}