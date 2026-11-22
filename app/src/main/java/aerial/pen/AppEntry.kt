package aerial.pen

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.text.Collator
import java.util.Locale

data class AppEntry(
    val pkg: String,
    val activity: String,
    val label: String,
    val icon: Drawable,
    val system: Boolean,
)

object AppRepository {

    private const val STOCK_LAUNCHER = "com.jxw.launcher"

    fun load(context: Context, hidden: Set<String>): List<AppEntry> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)
        val result = LinkedHashMap<String, AppEntry>()

        for (info in resolved) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (pkg == STOCK_LAUNCHER) continue
            if (pkg in hidden) continue
            if (result.containsKey(pkg)) continue

            val appInfo = try {
                pm.getApplicationInfo(pkg, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
            val isSystem = appInfo != null &&
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val icon = try {
                info.loadIcon(pm)
            } catch (t: Throwable) {
                pm.defaultActivityIcon
            }

            result[pkg] = AppEntry(
                pkg = pkg,
                activity = info.activityInfo.name,
                label = try {
                    info.loadLabel(pm).toString()
                } catch (t: Throwable) {
                    pkg
                },
                icon = icon,
                system = isSystem,
            )
        }

        val collator = Collator.getInstance(Locale.CHINA)
        return result.values.sortedWith(Comparator { a, b ->
            if (a.system != b.system) {
                return@Comparator if (a.system) 1 else -1
            }
            val compared = collator.compare(a.label, b.label)
            if (compared != 0) compared else a.pkg.compareTo(b.pkg)
        })
    }
}
