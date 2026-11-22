package aerial.pen

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.view.View
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClockPanel(context: Context) : View(context) {

    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        setShadowLayer(px(8f), 0f, px(2f), 0xCC000000.toInt())
    }

    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFFFFF.toInt()
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        setShadowLayer(px(6f), 0f, px(1f), 0xCC000000.toInt())
    }

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.CHINA)
    private val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)

    private var batteryLevel = -1
    private var charging = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 20_000L)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (level >= 0 && scale > 0) batteryLevel = level * 100 / scale
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            invalidate()
        }
    }

    init {
        try {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (t: Throwable) {
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(ticker)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (t: Throwable) {
        }
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val land = w > h * 1.6f
        val now = Date()
        val time = timeFormat.format(now)
        val date = dateFormat.format(now)

        if (land) {
            val timeSize = minOf(w * 0.40f, h * 0.22f)
            val dateSize = minOf(w * 0.115f, h * 0.062f).coerceAtLeast(px(11f))
            val batSize = minOf(w * 0.10f, h * 0.055f).coerceAtLeast(px(10f))

            timePaint.textSize = timeSize
            timePaint.textAlign = Paint.Align.CENTER
            subPaint.textSize = dateSize
            subPaint.textAlign = Paint.Align.CENTER

            val blockH = timeSize + dateSize * 1.5f + batSize * 2.2f
            var y = (h - blockH) / 2f + timeSize * 0.84f
            canvas.drawText(time, w / 2f, y, timePaint)
            y += dateSize * 1.65f
            canvas.drawText(date, w / 2f, y, subPaint)
            y += batSize * 1.9f
            drawBattery(canvas, w / 2f, y, batSize, centered = true)
        } else {
            val timeSize = minOf(w * 0.20f, h * 0.38f)
            val dateSize = minOf(w * 0.055f, h * 0.13f).coerceAtLeast(px(11f))
            val batSize = minOf(w * 0.048f, h * 0.11f).coerceAtLeast(px(10f))

            timePaint.textSize = timeSize
            timePaint.textAlign = Paint.Align.LEFT
            subPaint.textSize = dateSize
            subPaint.textAlign = Paint.Align.LEFT

            val startY = h * 0.30f + timeSize * 0.78f
            canvas.drawText(time, px(12f), startY, timePaint)
            canvas.drawText(date, px(12f), startY + dateSize * 1.75f, subPaint)
            drawBattery(canvas, px(12f), startY + dateSize * 3.2f + batSize, batSize, centered = false)
        }
    }

    private fun drawBattery(
        canvas: Canvas,
        x: Float,
        baselineY: Float,
        textSize: Float,
        centered: Boolean,
    ) {
        val bodyW = textSize * 2.4f
        val bodyH = textSize * 1.05f
        val left = if (centered) x - bodyW / 2f - textSize * 0.35f else x

        subPaint.textSize = textSize
        subPaint.textAlign = Paint.Align.LEFT
        subPaint.style = Paint.Style.STROKE
        subPaint.strokeWidth = px(1.2f)
        subPaint.color = 0xCCFFFFFF.toInt()

        val body = RectF(left, baselineY - bodyH, left + bodyW, baselineY)
        canvas.drawRoundRect(body, px(2f), px(2f), subPaint)
        val nub = RectF(
            left + bodyW,
            baselineY - bodyH * 0.78f,
            left + bodyW + textSize * 0.28f,
            baselineY - bodyH * 0.24f,
        )
        canvas.drawRect(nub, subPaint)

        val level = if (batteryLevel in 0..100) batteryLevel else 0
        val fillW = bodyW * (level / 100f)
        if (fillW > 0f) {
            subPaint.style = Paint.Style.FILL
            subPaint.color = if (charging) 0xFF6BE39B.toInt() else 0xCCFFFFFF.toInt()
            canvas.drawRoundRect(
                RectF(
                    left + px(1.5f),
                    baselineY - bodyH + px(1.5f),
                    left + px(1.5f) + fillW * 0.96f,
                    baselineY - px(1.5f),
                ),
                px(1.5f),
                px(1.5f),
                subPaint,
            )
        }

        subPaint.style = Paint.Style.FILL
        subPaint.color = 0xE6FFFFFF.toInt()
        val label = if (batteryLevel in 0..100) "$batteryLevel%" else "--"
        canvas.drawText(label, left + bodyW + textSize * 0.95f, baselineY, subPaint)
    }

    private fun px(v: Float): Float = v * resources.displayMetrics.density
}
