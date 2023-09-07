package one.zagura.CeramicLauncher.tools

import android.content.Intent
import android.view.ScaleGestureDetector
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetBehavior
import io.posidon.android.conveniencelib.pullStatusbar
import one.zagura.CeramicLauncher.Home
import one.zagura.CeramicLauncher.LauncherMenu
import one.zagura.CeramicLauncher.items.App
import one.zagura.CeramicLauncher.search.SearchActivity
import one.zagura.CeramicLauncher.storage.Settings

object Gestures {

    const val PULL_DOWN_NOTIFICATIONS = "notif"
    const val OPEN_APP_DRAWER = "drawer"
    const val OPEN_SEARCH = "search"
    const val OPEN_OVERVIEW = "overview"
    const val OPEN_APP = "app"
    const val NOTHING = "nothing"

    fun onLongPress(v: View): Boolean {
        performTrigger(Settings["gesture:long_press", OPEN_OVERVIEW])
        return true
    }

    fun performTrigger(key: String) {
        when (key) {
            PULL_DOWN_NOTIFICATIONS -> Tools.appContext!!.pullStatusbar()
            OPEN_APP_DRAWER -> Home.instance.drawer.state = BottomSheetBehavior.STATE_EXPANDED
            OPEN_SEARCH -> Tools.appContext!!.startActivity(Intent(Tools.appContext, SearchActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            OPEN_OVERVIEW -> LauncherMenu.openOverview(Home.instance)
            else -> {
                if (key.startsWith("$OPEN_APP:")) {
                    val string = key.substring(OPEN_APP.length + 1)
                    kotlin.runCatching {
                        App[string]?.open(Tools.appContext!!, null)
                    }
                }
            }
        }
    }

    fun getIndex(key: String) = when (key) {
        PULL_DOWN_NOTIFICATIONS -> 1
        OPEN_APP_DRAWER -> 2
        OPEN_SEARCH -> 3
        OPEN_OVERVIEW -> 4
        else -> if (key.startsWith(OPEN_APP)) 5 else 0
    }

    fun getKey(i: Int) = when (i) {
        1 -> PULL_DOWN_NOTIFICATIONS
        2 -> OPEN_APP_DRAWER
        3 -> OPEN_SEARCH
        4 -> OPEN_OVERVIEW
        5 -> OPEN_APP
        else -> ""
    }

    object PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector) = true
        override fun onScaleEnd(d: ScaleGestureDetector) = performTrigger(Settings["gesture:pinch", OPEN_OVERVIEW])
    }
}