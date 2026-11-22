package aerial.pen

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.max

class BackdropView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var base: Shader? = null
    private var glowTop: Shader? = null
    private var glowBottom: Shader? = null
    private var ready = false

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val widthF = w.toFloat()
        val heightF = h.toFloat()
        val span = max(w, h).toFloat()

        base = LinearGradient(
            0f, 0f, 0f, heightF,
            intArrayOf(0xFF1B2E42.toInt(), 0xFF0B141C.toInt(), 0xFF04070A.toInt()),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        glowTop = RadialGradient(
            widthF * 0.74f, heightF * 0.06f, span * 0.72f,
            intArrayOf(0x664C86B0, 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        glowBottom = RadialGradient(
            widthF * 0.04f, heightF * 0.95f, span * 0.62f,
            intArrayOf(0x3D13566D, 0x00000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        ready = true
    }

    override fun onDraw(canvas: Canvas) {
        if (!ready) return
        val w = width.toFloat()
        val h = height.toFloat()

        paint.shader = base
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = glowTop
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = glowBottom
        canvas.drawRect(0f, 0f, w, h, paint)

        paint.shader = LinearGradient(
            0f, h * 0.55f, 0f, h,
            intArrayOf(0x00000000, 0x70000000),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, h * 0.55f, w, h, paint)
        paint.shader = null
    }
}
