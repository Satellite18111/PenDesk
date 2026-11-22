package aerial.pen

import android.content.Context
import android.view.View
import android.view.ViewGroup
import kotlin.math.ceil
import kotlin.math.floor

class LauncherGrid(context: Context) : ViewGroup(context) {

    private var apps: List<AppEntry> = emptyList()
    private var landscape = false
    private var areaW = 0
    private var areaH = 0
    private var columns = 1
    private var rows = 1
    private var cellW = 0f
    private var cellH = 0f
    private var gap = 0f
    private var pad = 0f
    private var onClick: ((AppEntry) -> Unit)? = null
    private var onLongClick: ((AppEntry) -> Unit)? = null

    fun configure(areaWidth: Int, areaHeight: Int, isLandscape: Boolean) {
        areaW = areaWidth
        areaH = areaHeight
        landscape = isLandscape
        requestLayout()
    }

    fun setApps(items: List<AppEntry>, onTap: (AppEntry) -> Unit, onHold: (AppEntry) -> Unit) {
        apps = items
        onClick = onTap
        onLongClick = onHold
        removeAllViews()
        items.forEach { entry ->
            val tile = TileView(context, entry)
            tile.setOnClickListener { onClick?.invoke(entry) }
            tile.setOnLongClickListener {
                onLongClick?.invoke(entry)
                true
            }
            addView(tile)
        }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentW = MeasureSpec.getSize(widthMeasureSpec)
        val parentH = MeasureSpec.getSize(heightMeasureSpec)
        computeMetrics(parentW, parentH)

        val tileW = (cellW - gap).toInt().coerceAtLeast(1)
        val tileH = (cellH - gap).toInt().coerceAtLeast(1)

        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            if (child is TileView) child.applyMetrics(tileW, tileH)
            child.measure(
                MeasureSpec.makeMeasureSpec(tileW, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(tileH, MeasureSpec.EXACTLY),
            )
        }

        if (landscape) {
            setMeasuredDimension(totalWidth(), parentH)
        } else {
            setMeasuredDimension(parentW, totalHeight())
        }
    }

    private fun computeMetrics(parentW: Int, parentH: Int) {
        pad = px(2)
        gap = px(7)

        if (landscape) {
            val availH = (areaH - pad * 2).coerceAtLeast(px(60))
            val minCellH = px(104)
            rows = floor((availH + gap) / (minCellH + gap)).toInt().coerceAtLeast(1)
            cellH = (availH - gap * (rows - 1)) / rows

            columns = ceil(apps.size.toFloat() / rows).toInt().coerceAtLeast(1)
            val availW = (areaW - pad * 2).coerceAtLeast(px(200))
            val natural = (availW - gap * (columns - 1)) / columns
            cellW = natural.coerceIn(px(104), px(200))
        } else {
            val availW = (areaW - pad * 2).coerceAtLeast(px(120))
            columns = if (availW >= px(430)) 3 else 2
            cellW = (availW - gap * (columns - 1)) / columns
            cellH = px(124)
            rows = ceil(apps.size.toFloat() / columns).toInt().coerceAtLeast(1)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val col: Int
            val row: Int
            if (landscape) {
                col = i / rows
                row = i % rows
            } else {
                col = i % columns
                row = i / columns
            }
            val x = (pad + col * (cellW + gap)).toInt()
            val y = (pad + row * (cellH + gap)).toInt()
            child.layout(
                x,
                y,
                x + (cellW - gap).toInt(),
                y + (cellH - gap).toInt(),
            )
        }
    }

    private fun totalWidth(): Int =
        (pad * 2 + cellW * columns + gap * (columns - 1)).toInt()

    private fun totalHeight(): Int =
        (pad * 2 + cellH * rows + gap * (rows - 1)).toInt()

    private fun px(v: Int): Float = v * resources.displayMetrics.density
}
