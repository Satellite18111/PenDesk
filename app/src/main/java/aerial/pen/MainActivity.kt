package aerial.pen

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var clock: ClockPanel
    private lateinit var grid: LauncherGrid
    private lateinit var studyButton: TextView
    private var landscape = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(DesktopSettings.PREFS, MODE_PRIVATE)
        landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        applyFullscreen()
        buildUi()
        reload()
    }

    override fun onResume() {
        super.onResume()
        if (::grid.isInitialized) reload()
        if (::studyButton.isInitialized) updateStudyButton()
    }

    private fun applyFullscreen() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    private fun buildUi() {
        val screenW = resources.displayMetrics.widthPixels
        val screenH = resources.displayMetrics.heightPixels

        val root = FrameLayout(this)
        root.addView(BackdropView(this), FrameLayout.LayoutParams(-1, -1))

        systemWallpaper()?.let { drawable ->
            val wallpaper = ImageView(this)
            wallpaper.scaleType = ImageView.ScaleType.CENTER_CROP
            wallpaper.setImageDrawable(drawable)
            root.addView(wallpaper, FrameLayout.LayoutParams(-1, -1))
        }

        val scrim = View(this)
        scrim.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0x30000000, 0x55000000, 0x8A000000.toInt()),
        )
        root.addView(scrim, FrameLayout.LayoutParams(-1, -1))

        clock = ClockPanel(this)
        clock.isClickable = true
        clock.setOnLongClickListener {
            showLauncherMenu()
            true
        }

        val content = LinearLayout(this)
        grid = LauncherGrid(this)

        if (landscape) {
            content.orientation = LinearLayout.HORIZONTAL
            content.setPadding(dp(5), dp(5), dp(5), dp(5))
            content.addView(clock, LinearLayout.LayoutParams(dp(126), -1))

            val scroller = HorizontalScrollView(this)
            scroller.isHorizontalScrollBarEnabled = false
            scroller.overScrollMode = View.OVER_SCROLL_NEVER
            scroller.isFillViewport = false
            scroller.addView(grid, ViewGroup.LayoutParams(-2, -1))
            content.addView(scroller, LinearLayout.LayoutParams(0, -1, 1f))

            grid.configure(screenW - dp(126) - dp(10), screenH - dp(10), true)
        } else {
            content.orientation = LinearLayout.VERTICAL
            content.setPadding(dp(5), dp(5), dp(5), dp(5))
            content.addView(clock, LinearLayout.LayoutParams(-1, dp(106)))

            val scroller = ScrollView(this)
            scroller.isVerticalScrollBarEnabled = false
            scroller.overScrollMode = View.OVER_SCROLL_NEVER
            scroller.isFillViewport = false
            scroller.addView(grid, ViewGroup.LayoutParams(-1, -2))
            content.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))

            grid.configure(screenW - dp(10), 0, false)
        }

        root.addView(content, FrameLayout.LayoutParams(-1, -1))

        studyButton = TextView(this)
        updateStudyButton()
        studyButton.setTextColor(0xFFFFFFFF.toInt())
        studyButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, if (landscape) 10.5f else 10f)
        studyButton.gravity = Gravity.CENTER
        studyButton.isClickable = true
        studyButton.isSingleLine = true
        studyButton.ellipsize = android.text.TextUtils.TruncateAt.END
        studyButton.setPadding(dp(10), 0, dp(10), 0)
        studyButton.setShadowLayer(4f, 0f, 1f, 0xCC000000.toInt())
        studyButton.background = RippleDrawable(
            ColorStateList.valueOf(0x40FFFFFF),
            GradientDrawable().apply {
                cornerRadius = dp(13).toFloat()
                setColor(0x26FFFFFF)
                setStroke(dp(1), 0x40FFFFFF)
            },
            null,
        )
        studyButton.setOnClickListener { openStudyDesktop() }

        val studyParams = if (landscape) {
            FrameLayout.LayoutParams(dp(104), dp(26)).apply {
                gravity = Gravity.BOTTOM or Gravity.START
                leftMargin = dp(11)
                bottomMargin = dp(9)
            }
        } else {
            FrameLayout.LayoutParams(dp(78), dp(24)).apply {
                gravity = Gravity.TOP or Gravity.END
                rightMargin = dp(9)
                topMargin = dp(11)
            }
        }
        root.addView(studyButton, studyParams)
        setContentView(root)
        updateStudyButton()
    }

    private fun updateStudyButton() {
        val target = DesktopSettings.load(this)
        studyButton.text = target.label
        val lp = studyButton.layoutParams as? FrameLayout.LayoutParams ?: return
        val base = if (landscape) dp(104) else dp(78)
        val limit = if (landscape) dp(220) else dp(150)
        val measured = kotlin.math.ceil(studyButton.paint.measureText(target.label)).toInt() + dp(26)
        lp.width = kotlin.math.min(limit, kotlin.math.max(base, measured))
        studyButton.layoutParams = lp
    }

    private fun openStudyDesktop() {
        val target = DesktopSettings.load(this)
        val intent = Intent(Intent.ACTION_MAIN)
        intent.setClassName(target.pkg, target.activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (t: Throwable) {
            val fallback = packageManager.getLaunchIntentForPackage(target.pkg)
            if (fallback == null) {
                Toast.makeText(this, "没有找到「${target.label}」", Toast.LENGTH_SHORT).show()
            } else {
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    startActivity(fallback)
                } catch (t2: Throwable) {
                    Toast.makeText(this, "打不开「${target.label}」", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun reload() {
        val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
        val apps = AppRepository.load(this, hidden).toMutableList()
        apps.add(
            0,
            AppEntry(
                pkg = packageName,
                activity = SettingsActivity::class.java.name,
                label = "桌面设置",
                icon = packageManager.getApplicationIcon(packageName),
                system = false,
            ),
        )
        if (::studyButton.isInitialized) updateStudyButton()
        grid.setApps(apps, ::launchApp, ::showAppMenu)
    }

    private fun launchApp(entry: AppEntry) {
        if (entry.pkg == packageName && entry.activity == SettingsActivity::class.java.name) {
            openSettings()
            return
        }
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.setClassName(entry.pkg, entry.activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            startActivity(intent)
        } catch (t: Throwable) {
            Toast.makeText(this, "打开失败：" + entry.label, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openSettings() {
        startActivity(
            Intent(this, SettingsActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun showAppMenu(entry: AppEntry) {
        if (entry.pkg == packageName) {
            openSettings()
            return
        }
        showSheet(
            entry.label,
            listOf(
                "应用信息" to {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:" + entry.pkg)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        startActivity(intent)
                    } catch (t: Throwable) {
                        Toast.makeText(this, "打不开系统设置", Toast.LENGTH_SHORT).show()
                    }
                },
                "从桌面隐藏" to { hideApp(entry) },
            ),
        )
    }

    private fun hideApp(entry: AppEntry) {
        val hidden = (prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()).toMutableSet()
        hidden.add(entry.pkg)
        prefs.edit().putStringSet(KEY_HIDDEN, hidden).apply()
        reload()
        Toast.makeText(this, "已从桌面移除，长按时钟可恢复", Toast.LENGTH_SHORT).show()
    }

    private fun showLauncherMenu() {
        showSheet(
            "Aerial 桌面",
            listOf(
                "桌面设置" to { openSettings() },
                "显示已隐藏的应用" to { showHiddenApps() },
                "刷新应用列表" to {
                    reload()
                    Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
                },
            ),
        )
    }

    private fun showHiddenApps() {
        val hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()
        if (hidden.isEmpty()) {
            Toast.makeText(this, "没有隐藏的应用", Toast.LENGTH_SHORT).show()
            return
        }
        val pm = packageManager
        val entries = hidden.map { pkg ->
            val label = try {
                pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
            } catch (t: Throwable) {
                pkg
            }
            label to pkg
        }.sortedBy { it.first }

        showSheet(
            "已隐藏的应用",
            entries.map { (label, pkg) ->
                label to {
                    val current = (prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()).toMutableSet()
                    current.remove(pkg)
                    prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
                    reload()
                    Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show()
                }
            },
            scrollable = entries.size > 4,
        )
    }

    private fun showSheet(
        title: String,
        items: List<Pair<String, () -> Unit>>,
        scrollable: Boolean = false,
    ) {
        val content = findViewById(android.R.id.content) as ViewGroup

        val overlay = FrameLayout(this)
        overlay.setBackgroundColor(0xC4000000.toInt())
        overlay.isClickable = true
        overlay.setOnClickListener { content.removeView(overlay) }

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(12), dp(10), dp(12), dp(8))
        card.isClickable = true
        card.background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(0xF0151D26.toInt())
            setStroke(dp(1), 0x33FFFFFF)
        }

        val titleView = TextView(this)
        titleView.text = title
        titleView.setTextColor(0xFFFFFFFF.toInt())
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
        titleView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        titleView.setPadding(dp(6), 0, dp(6), dp(4))
        titleView.setSingleLine(true)
        card.addView(titleView, LinearLayout.LayoutParams(-1, -2))

        val rows = LinearLayout(this)
        rows.orientation = LinearLayout.VERTICAL
        items.forEach { item ->
            val row = TextView(this)
            row.text = item.first
            row.setTextColor(0xFFE8EEF5.toInt())
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            row.gravity = Gravity.CENTER_VERTICAL
            row.setSingleLine(true)
            row.setPadding(dp(6), 0, dp(6), 0)
            row.isClickable = true
            row.background = RippleDrawable(
                ColorStateList.valueOf(0x33FFFFFF),
                GradientDrawable().apply {
                    cornerRadius = dp(8).toFloat()
                    setColor(Color.TRANSPARENT)
                },
                null,
            )
            row.setOnClickListener {
                content.removeView(overlay)
                item.second()
            }
            rows.addView(row, LinearLayout.LayoutParams(-1, dp(33)))
        }

        if (scrollable) {
            val inner = ScrollView(this)
            inner.isVerticalScrollBarEnabled = false
            inner.overScrollMode = View.OVER_SCROLL_NEVER
            inner.addView(rows, ViewGroup.LayoutParams(-1, -2))
            card.addView(inner, LinearLayout.LayoutParams(-1, dp(150)))
        } else {
            card.addView(rows, LinearLayout.LayoutParams(-1, -2))
        }

        overlay.addView(
            card,
            FrameLayout.LayoutParams(dp(230), -2, Gravity.CENTER),
        )
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(140).start()
        content.addView(overlay, ViewGroup.LayoutParams(-1, -1))
    }

    @Deprecated("Launcher keeps home")
    override fun onBackPressed() {
        // 桌面不响应返回键，避免误退出到空白页
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun systemWallpaper(): Drawable? {
        val drawable = try {
            WallpaperManager.getInstance(this).drawable
        } catch (t: Throwable) {
            null
        } ?: return null

        return try {
            val size = 20
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            var total = 0L
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val pixel = bitmap.getPixel(x, y)
                    total += (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                }
            }
            bitmap.recycle()
            val average = total / (size * size)
            if (average > 24) drawable else null
        } catch (t: Throwable) {
            null
        }
    }

    companion object {
        private const val KEY_HIDDEN = "hidden_packages"
    }
}
