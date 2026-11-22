package aerial.pen

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class TileView(context: Context, val entry: AppEntry) : LinearLayout(context) {

    private val iconView = ImageView(context)
    private val labelView = TextView(context)
    private var iconSize = 0

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = false
        setPadding(px(4), px(4), px(4), px(4))
        background = buildCard()

        iconView.scaleType = ImageView.ScaleType.FIT_CENTER
        iconView.setImageDrawable(entry.icon)
        addView(iconView, LayoutParams(px(52), px(52)))

        labelView.apply {
            text = entry.label
            setTextColor(0xFFFFFFFF.toInt())
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setShadowLayer(4f, 0f, 1f, 0xCC000000.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        addView(labelView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        setOnTouchListener { v: View, e: MotionEvent ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> v.animate()
                    .scaleX(0.93f).scaleY(0.93f).setDuration(90).start()

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate()
                    .scaleX(1f).scaleY(1f).setDuration(140).start()
            }
            false
        }
    }

    fun applyMetrics(tileW: Int, tileH: Int) {
        val newIcon = minOf((tileW * 0.46f).toInt(), (tileH * 0.50f).toInt(), px(88))
        if (newIcon != iconSize) {
            iconSize = newIcon
            iconView.layoutParams = LayoutParams(newIcon, newIcon)
            iconView.requestLayout()
        }
        val sp = if (tileW < px(130)) 10.5f else 12f
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    private fun buildCard(): RippleDrawable = RippleDrawable(
        ColorStateList.valueOf(0x40FFFFFF),
        GradientDrawable().apply {
            cornerRadius = px(15).toFloat()
            setColor(0x1FFFFFFF)
            setStroke(px(1), 0x2EFFFFFF)
        },
        null,
    )

    private fun px(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
