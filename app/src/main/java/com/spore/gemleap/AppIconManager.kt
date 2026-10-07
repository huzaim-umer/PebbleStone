package com.spore.gemleap

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.spore.gemleap.model.ThemeId

/**
 * Switches the launcher icon to match the selected theme by enabling that theme's
 * `<activity-alias>` (see AndroidManifest.xml) and disabling the others.
 *
 * Call it when the app goes to the background: some launchers refresh or briefly
 * hide the icon when its component changes, so we avoid doing it while the player is
 * looking at the game. It is a no-op when the right alias is already enabled.
 */
object AppIconManager {
    private fun aliasFor(context: Context, id: ThemeId): ComponentName {
        val name = when (id) {
            ThemeId.JEWEL -> "IconJewel"
            ThemeId.CLASSIC -> "IconClassic"
            ThemeId.ZEN -> "IconZen"
        }
        return ComponentName(context, "com.spore.gemleap.$name")
    }

    fun sync(context: Context, theme: ThemeId) {
        val pm = context.packageManager
        val target = aliasFor(context, theme)
        // Enable the new entry first so there is always at least one launcher icon.
        setEnabled(pm, target, true)
        for (id in ThemeId.values()) {
            if (id != theme) setEnabled(pm, aliasFor(context, id), false)
        }
    }

    private fun setEnabled(pm: PackageManager, component: ComponentName, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        if (pm.getComponentEnabledSetting(component) == state) return
        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
    }
}
