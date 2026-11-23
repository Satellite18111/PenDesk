package aerial.pen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.ComponentName
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.Collator
import java.util.Locale

object DesktopSettings {
    const val PREFS = "aerial_pen"
    const val KEY_JUMP_PKG = "jump_pkg"
    const val KEY_JUMP_ACTIVITY = "jump_activity"
    const val KEY_JUMP_LABEL = "jump_label"

    const val DEFAULT_PKG = "com.jxw.launcher"
    const val DEFAULT_ACTIVITY = "com.jxw.scanpen.a19.ui.ScanPenActivity"
    const val DEFAULT_LABEL = "学习桌面"

    data class Target(val pkg: String, val activity: String, val label: String)

    fun load(context: Context): Target {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return Target(
            prefs.getString(KEY_JUMP_PKG, DEFAULT_PKG) ?: DEFAULT_PKG,
            prefs.getString(KEY_JUMP_ACTIVITY, DEFAULT_ACTIVITY) ?: DEFAULT_ACTIVITY,
            prefs.getString(KEY_JUMP_LABEL, DEFAULT_LABEL) ?: DEFAULT_LABEL,
        )
    }

    fun save(context: Context, target: Target) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_JUMP_PKG, target.pkg)
            .putString(KEY_JUMP_ACTIVITY, target.activity)
            .putString(KEY_JUMP_LABEL, target.label)
            .apply()
    }

    fun reset(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_JUMP_PKG)
            .remove(KEY_JUMP_ACTIVITY)
            .remove(KEY_JUMP_LABEL)
            .apply()
    }
}

class SettingsActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var currentLabel: TextView
    private lateinit var currentPackage: TextView
    private lateinit var currentHome: TextView
    private val rows = ArrayList<Pair<Choice, RowView>>()

    private data class RowView(
        val root: LinearLayout,
        val title: TextView,
        val subtitle: TextView,
    )

    private data class Choice(
        val pkg: String,
        val activity: String,
        val label: String,
        val icon: Drawable,
    ) {
        val key: String get() = "$pkg/$activity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(DesktopSettings.PREFS, MODE_PRIVATE)
        applyFullscreen()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        syncHomeAlias()
        refreshCurrent()
    }

    private fun homeAlias(): ComponentName =
        ComponentName(packageName, packageName + ".HomeAlias")

    private fun isHomeAliasEnabled(): Boolean = try {
        packageManager.getComponentEnabledSetting(homeAlias()) ==
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    } catch (t: Throwable) {
        false
    }

    private fun syncHomeAlias() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolvedPkg = try {
            packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (t: Throwable) {
            null
        }
        // 只有在 Aerial 已是默认桌面时才保留 HOME 候选，避免无端抢占系统主屏幕。
        if (resolvedPkg != packageName && isHomeAliasEnabled()) {
            try {
                packageManager.setComponentEnabledSetting(
                    homeAlias(),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            } catch (t: Throwable) {
            }
        }
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
        val scroller = ScrollView(this)
        scroller.setBackgroundColor(0xFF0B141C.toInt())
        scroller.isVerticalScrollBarEnabled = false

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(14), dp(14), dp(14), dp(24))
        scroller.addView(column, ViewGroup.LayoutParams(-1, -2))

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val title = text("桌面设置", 26f, 0xFFFFFFFF.toInt(), true)
        top.addView(title, LinearLayout.LayoutParams(0, dp(44), 1f))
        val back = pill("返回", 0xFF16212C.toInt(), dp(44), 16f)
        back.setOnClickListener { finish() }
        top.addView(back, LinearLayout.LayoutParams(dp(76), dp(44)))
        column.addView(top)

        column.addView(text("Aerial 桌面 · 快捷跳转与入口", 14f, 0xFF93A6B8.toInt(), false))

        column.addView(section("当前胶囊按钮跳转目标"))
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(14), dp(12), dp(14), dp(12))
        card.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(0xFF16212C.toInt())
            setStroke(dp(1), 0x33FFFFFF)
        }
        currentLabel = text("", 18f, 0xFFFFFFFF.toInt(), true)
        card.addView(currentLabel)
        currentPackage = text("", 12.5f, 0xFF93A6B8.toInt(), false)
        currentPackage.setPadding(0, dp(4), 0, dp(8))
        card.addView(currentPackage)

        val reset = pill("恢复默认（学王桌面）", 0xFF2F6FE0.toInt(), dp(48), 16f)
        reset.setOnClickListener {
            DesktopSettings.reset(this)
            refreshCurrent()
            updateRows()
            Toast.makeText(this, "已恢复默认学王桌面", Toast.LENGTH_SHORT).show()
        }
        card.addView(reset, LinearLayout.LayoutParams(-1, dp(48)))
        column.addView(card, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        column.addView(section("默认桌面（系统主屏幕）"))
        val homeCard = LinearLayout(this)
        homeCard.orientation = LinearLayout.VERTICAL
        homeCard.setPadding(dp(14), dp(12), dp(14), dp(12))
        homeCard.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(0xFF16212C.toInt())
            setStroke(dp(1), 0x33FFFFFF)
        }
        currentHome = text("", 18f, 0xFFFFFFFF.toInt(), true)
        homeCard.addView(currentHome)
        homeCard.addView(
            text("点下面按钮进入系统「主屏幕」选择页，选中要用的桌面即可。", 13f, 0xFF93A6B8.toInt(), false)
                .apply { setPadding(0, dp(4), 0, dp(8)) },
        )
        val chooseHome = pill("选择默认桌面", 0xFF2F6FE0.toInt(), dp(48), 16f)
        chooseHome.setOnClickListener { openHomeSettings() }
        homeCard.addView(chooseHome, LinearLayout.LayoutParams(-1, dp(48)))
        column.addView(homeCard, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(4) })

        column.addView(section("选择要跳转的应用"))
        column.addView(
            text(
                "点一下列表中的应用即可设为跳转目标；桌面左下角按钮会显示对应名称。",
                13f,
                0xFF8FA2B4.toInt(),
                false,
            ).apply { setPadding(0, 0, 0, dp(8)) },
        )

        val choices = loadChoices()
        if (choices.isEmpty()) {
            column.addView(text("没有找到可跳转的应用", 15f, 0xFF93A6B8.toInt(), false))
        }
        choices.forEach { choice ->
            val row = buildRow(choice)
            column.addView(row.root, LinearLayout.LayoutParams(-1, dp(62)).apply {
                topMargin = dp(6)
            })
            rows.add(choice to row)
        }

        val home = pill("返回桌面", 0xFF16212C.toInt(), dp(54), 17f)
        home.setOnClickListener { finish() }
        column.addView(home, LinearLayout.LayoutParams(-1, dp(54)).apply {
            topMargin = dp(18)
        })

        setContentView(scroller)
        refreshCurrent()
        updateRows()
    }

    private fun buildRow(choice: Choice): RowView {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(dp(14), dp(6), dp(14), dp(6))
        row.isClickable = true
        row.background = rowBackground(false)

        val icon = ImageView(this)
        icon.scaleType = ImageView.ScaleType.FIT_CENTER
        icon.setImageDrawable(choice.icon)
        row.addView(icon, LinearLayout.LayoutParams(dp(34), dp(34)).apply {
            rightMargin = dp(12)
        })

        val texts = LinearLayout(this)
        texts.orientation = LinearLayout.VERTICAL
        val title = text(choice.label, 16f, 0xFFE8EEF5.toInt(), false)
        title.isSingleLine = true
        title.ellipsize = android.text.TextUtils.TruncateAt.END
        texts.addView(title)
        val subtitle = text(choice.pkg, 11.5f, 0xFF93A6B8.toInt(), false)
        subtitle.isSingleLine = true
        subtitle.ellipsize = android.text.TextUtils.TruncateAt.END
        texts.addView(subtitle)
        row.addView(texts, LinearLayout.LayoutParams(0, -2, 1f))

        row.setOnClickListener {
            DesktopSettings.save(
                this,
                DesktopSettings.Target(choice.pkg, choice.activity, choice.label),
            )
            refreshCurrent()
            updateRows()
            Toast.makeText(this, "已设为「${choice.label}」", Toast.LENGTH_SHORT).show()
        }
        return RowView(row, title, subtitle)
    }

    private fun updateRows() {
        val current = DesktopSettings.load(this)
        rows.forEach { (choice, row) ->
            val selected = choice.pkg == current.pkg && choice.activity == current.activity
            row.root.background = rowBackground(selected)
            row.title.setTextColor(if (selected) 0xFFFFFFFF.toInt() else 0xFFE8EEF5.toInt())
            row.subtitle.setTextColor(
                if (selected) 0xFFBBD4FF.toInt() else 0xFF93A6B8.toInt(),
            )
        }
    }

    private fun rowBackground(selected: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(14).toFloat()
        setColor(if (selected) 0xFF203A5E.toInt() else 0xFF141F29.toInt())
        setStroke(dp(1), if (selected) 0xFF5E9BFF.toInt() else 0x26FFFFFF)
    }

    private fun refreshCurrent() {
        val target = DesktopSettings.load(this)
        currentLabel.text = target.label
        currentPackage.text = target.pkg + "/" + target.activity

        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        currentHome.text = try {
            resolved?.loadLabel(packageManager)?.toString() ?: "未设置"
        } catch (t: Throwable) {
            "未设置"
        }
    }

    private fun openHomeSettings() {
        try {
            packageManager.setComponentEnabledSetting(
                homeAlias(),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
        } catch (t: Throwable) {
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            return
        } catch (t: Throwable) {
        }
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            startActivity(Intent.createChooser(homeIntent, "选择默认桌面"))
        } catch (t: Throwable) {
            Toast.makeText(this, "系统没有提供默认桌面设置页", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadChoices(): List<Choice> {
        val pm = packageManager
        val result = LinkedHashMap<String, Choice>()

        fun add(infos: List<android.content.pm.ResolveInfo>) {
            for (info in infos) {
                val pkg = info.activityInfo.packageName
                if (pkg == packageName) continue
                val label = try {
                    info.loadLabel(pm).toString()
                } catch (t: Throwable) {
                    pkg
                }
                val icon = try {
                    info.loadIcon(pm)
                } catch (t: Throwable) {
                    pm.defaultActivityIcon
                }
                if (!result.containsKey(pkg)) {
                    result[pkg] = Choice(pkg, info.activityInfo.name, label, icon)
                }
            }
        }

        add(pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0))
        add(pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0))

        // 保证默认学王入口始终在列表里
        if (!result.containsKey(DesktopSettings.DEFAULT_PKG)) {
            try {
                val info = pm.getActivityInfo(
                    android.content.ComponentName(
                        DesktopSettings.DEFAULT_PKG,
                        DesktopSettings.DEFAULT_ACTIVITY,
                    ),
                    0,
                )
                val label = info.loadLabel(pm).toString()
                result[DesktopSettings.DEFAULT_PKG] = Choice(
                    DesktopSettings.DEFAULT_PKG,
                    DesktopSettings.DEFAULT_ACTIVITY,
                    label,
                    info.loadIcon(pm),
                )
            } catch (t: Throwable) {
            }
        }

        val collator = Collator.getInstance(Locale.CHINA)
        return result.values.sortedWith(Comparator { a, b ->
            val compared = collator.compare(a.label, b.label)
            if (compared != 0) compared else a.pkg.compareTo(b.pkg)
        })
    }

    private fun section(title: String): TextView {
        val view = text(title, 16f, 0xFFFFFFFF.toInt(), true)
        view.setPadding(0, dp(18), 0, dp(7))
        return view
    }

    private fun text(text: String, size: Float, color: Int, bold: Boolean): TextView {
        val view = TextView(this)
        view.text = text
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        view.setTextColor(color)
        if (bold) view.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        return view
    }

    private fun pill(label: String, color: Int, height: Int, textSize: Float = 16f): TextView {
        val view = TextView(this)
        view.text = label
        view.setTextColor(Color.WHITE)
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        view.gravity = Gravity.CENTER
        view.isClickable = true
        view.background = GradientDrawable().apply {
            cornerRadius = dp(14).toFloat()
            setColor(color)
        }
        view.layoutParams = LinearLayout.LayoutParams(-1, height)
        return view
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
