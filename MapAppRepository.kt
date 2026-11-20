package com.example.ridedash.apps

import android.content.Context
import android.content.Intent
import android.net.Uri

/** launchComponent = "pkg/activity" for: am start --display <id> -n <launchComponent> */
data class MapApp(val label: String, val packageName: String, val launchComponent: String)

class MapAppRepository(private val ctx: Context) {
    private val prefs = ctx.getSharedPreferences("ridedash", Context.MODE_PRIVATE)
    private val pm = ctx.packageManager

    // Known map apps (verify package names on your phone) + anything that handles geo: links.
    private val known = listOf(
        "com.autonavi.minimap", "com.google.android.apps.maps", "com.waze",
        "com.here.app.maps", "net.osmand", "app.organicmaps",
    )

    fun installed(): List<MapApp> {
        val pkgs = LinkedHashSet<String>()
        known.filterTo(pkgs) { pm.getLaunchIntentForPackage(it) != null }
        pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=cafe")), 0)
            .forEach { pkgs.add(it.activityInfo.packageName) }
        return pkgs.mapNotNull { pkg ->
            val comp = pm.getLaunchIntentForPackage(pkg)?.component ?: return@mapNotNull null
            MapApp(pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(), pkg, comp.flattenToShortString())
        }
    }

    var selectedPackage: String?
        get() = prefs.getString("map_pkg", null)
        set(v) { prefs.edit().putString("map_pkg", v).apply() }
}
