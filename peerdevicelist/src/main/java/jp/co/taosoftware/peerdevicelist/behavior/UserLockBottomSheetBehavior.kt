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
package jp.co.taosoftware.peerdevicelist.behavior

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

/**
 * This custom behavior can lock user touch during allowuserTouch == false.
 * This custom behavior is used to lock while user is recording voice through recording button.
 */
class UserLockBottomSheetBehavior<V : View> : BottomSheetBehavior<V> {

    var allowUserTouch = true

    constructor() : super() {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    override fun onInterceptTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if(allowUserTouch){
            return super.onInterceptTouchEvent(parent, child, event)
        }else{
            return allowUserTouch
        }
    }


    override fun onTouchEvent(parent: CoordinatorLayout, child: V, event: MotionEvent): Boolean {
        if(allowUserTouch){
            return super.onTouchEvent(parent, child, event)
        }else{
            return allowUserTouch
        }
    }

    override fun onStartNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, directTargetChild: View, target: View, nestedScrollAxes: Int): Boolean {
        if(allowUserTouch){
            return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes)
        }else{
            return allowUserTouch
        }
    }

    override fun onNestedPreScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View, dx: Int, dy: Int, consumed: IntArray) {}

    override fun onStopNestedScroll(coordinatorLayout: CoordinatorLayout, child: V, target: View) {}

    override fun onNestedPreFling(coordinatorLayout: CoordinatorLayout, child: V, target: View, velocityX: Float, velocityY: Float): Boolean {
        if(allowUserTouch){
            return super.onNestedPreFling(coordinatorLayout, child, target, velocityX, velocityY)
        }else{
            return allowUserTouch
        }
    }
}