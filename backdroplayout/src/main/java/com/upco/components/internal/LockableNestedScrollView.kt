package com.upco.components.internal

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.widget.NestedScrollView

class LockableNestedScrollView(ctx: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    NestedScrollView(ctx, attrs, defStyleAttr) {

    var locked: Boolean = false

    constructor(ctx: Context, attrs: AttributeSet) : this(ctx, attrs, 0)
    constructor(ctx: Context) : this(ctx, null, 0)

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return !locked && super.onTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return !locked && super.onInterceptTouchEvent(ev)
    }
}