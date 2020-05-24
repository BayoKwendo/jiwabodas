package com.bados.jiwa.helpers

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import com.bodas.jiwa.R
import io.codetail.animation.ViewAnimationUtils
import io.codetail.widget.RevealFrameLayout

@Suppress("SENSELESS_COMPARISON", "NAME_SHADOWING")
class PromptPopUpView @JvmOverloads
constructor(context: Context?, attrs: AttributeSet? = null,
            defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {
    //ActionListener listener;
    private var stack: RevealFrameLayout? = null
    var nextView: View? = null
    var root: View? = null
    var response: TextView? = null
    private var currentViewIndex = 0
    fun init() {
        root = View.inflate(context, R.layout.confirm_popup, this)
        setupSubviews()
    }

    private fun setupSubviews() {
        stack = root?.findViewById(R.id.view_stack)
        response = root?.findViewById(R.id.result)
//        val revealManager = ViewRevealManager()

        root?.addOnLayoutChangeListener { _: View?, _: Int, _: Int,
                                          _: Int, _: Int, _: Int,
                                          _: Int, _: Int, _: Int ->
            if (nextView != null) { // get the center for the clipping circle
                val cx = (nextView!!.left + nextView!!.right) / 2
                val cy = (nextView!!.top + nextView!!.bottom) / 2
                // get the final radius for the clipping circle
                val dx = Math.max(cx, nextView!!.width - cx)
                val dy = Math.max(cy, nextView!!.height - cy)
                val finalRadius = Math.hypot(dx.toDouble(), dy.toDouble()).toFloat()
                val revealAnimator = ViewAnimationUtils.createCircularReveal(nextView, cx, cy, 0f,
                        finalRadius, View.LAYER_TYPE_HARDWARE)
                revealAnimator.duration = SLOW_DURATION.toLong()
                revealAnimator.interpolator = FastOutLinearInInterpolator()
                revealAnimator.start()
            }
        }
    }

    fun changeStatus(color: Int, text: String) {
        val v = View(context)
        v.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT)
        response?.text = text.toString()
        if (color == 2) {
            v.setBackgroundColor(ContextCompat.getColor(context, R.color.success))
        }
        else if (color == 1) {
            v.setBackgroundColor(ContextCompat.getColor(context, R.color.error))
        }
//        else {
//            v.setBackgroundColor(ContextCompat.getColor(context, R.color.lender_colorAccent))
//        }
//        else {
//            v.setBackgroundColor(ContextCompat.getColor(context, R.color.lender_colorAccent))
//        }
        stack?.addView(v)
        nextView = next
        nextView!!.bringToFront()
        nextView!!.visibility = View.VISIBLE
    }

    private val currentView: View
        get() = stack!!.getChildAt(currentViewIndex)

    private val next: View
        get() = getViewAt(++currentViewIndex)

    private fun getViewAt(index: Int): View {
        var index = index
        if (index >= stack!!.childCount) {
            index = 0
        } else if (index < 0) {
            index = stack!!.childCount - 1
        }
        return stack!!.getChildAt(index)
    } //    public interface ActionListener {

    //        String[] dataSelection();
//    }
    companion object {
        const val SLOW_DURATION = 400
    }
    init {
        //        if (context instanceof ActionListener) {
//            listener = (ActionListener) context;
//        } else {
//            throw new IllegalStateException(context + " must implement" + ActionListener.class.getSimpleName());
//        }
        init()
    }


}