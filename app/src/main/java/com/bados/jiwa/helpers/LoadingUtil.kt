package com.bados.jiwa.helpers

import android.view.View
import android.view.ViewGroup

object LoadingUtil {
    @JvmStatic
    fun enableDisableView(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) {
            val group = view
            for (idx in 0 until group.childCount) {
                enableDisableView(group.getChildAt(idx), enabled)
            }
        }
    }
}

