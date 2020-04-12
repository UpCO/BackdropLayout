package com.upco.util

import android.content.res.Resources
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP

object DimensionUtils {

    fun convertDpToPx(res: Resources, dp: Float) =
        TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, res.displayMetrics)

    fun convertDpToPxInt(res: Resources, dp: Float) = convertDpToPx(res, dp).toInt()
}