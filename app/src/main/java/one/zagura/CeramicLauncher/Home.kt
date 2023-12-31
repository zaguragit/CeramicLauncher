package one.zagura.CeramicLauncher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.content.res.Configuration
import android.graphics.Picture
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.*
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import io.posidon.android.conveniencelib.Device
import one.zagura.CeramicLauncher.provider.notifications.NotificationService
import one.zagura.CeramicLauncher.data.items.Folder
import one.zagura.CeramicLauncher.data.items.LauncherItem
import one.zagura.CeramicLauncher.data.items.PinnedShortcut
import one.zagura.CeramicLauncher.ui.ItemLongPress
import one.zagura.CeramicLauncher.ui.search.SearchActivity
import one.zagura.CeramicLauncher.util.storage.Settings
import one.zagura.CeramicLauncher.external.GestureNavContract
import one.zagura.CeramicLauncher.util.Gestures
import one.zagura.CeramicLauncher.provider.AppLoader
import one.zagura.CeramicLauncher.util.LiveWallpaper
import one.zagura.CeramicLauncher.util.StackTraceActivity
import one.zagura.CeramicLauncher.util.Tools
import one.zagura.CeramicLauncher.util.Tools.updateNavbarHeight
import one.zagura.CeramicLauncher.ui.Setup
import one.zagura.CeramicLauncher.ui.view.ResizableLayout
import one.zagura.CeramicLauncher.ui.view.drawer.DrawerView
import one.zagura.CeramicLauncher.ui.view.feed.Feed
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.system.exitProcess

class Home : AppCompatActivity() {

    val drawer by lazy { findViewById<DrawerView>(R.id.drawer)!! }
    val dock get() = drawer.dock

    val feed by lazy { findViewById<Feed>(R.id.feed)!! }

    val homeView by lazy { findViewById<ViewGroup>(R.id.homeView)!! }

    val appReloader = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            drawer.loadApps()
        }
    }

    private lateinit var powerManager: PowerManager

    init { instance = this }
    companion object { lateinit var instance: Home private set }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StackTraceActivity.init(applicationContext)
        Tools.appContextReference = WeakReference(applicationContext)
        Settings.init(applicationContext)
        if (Settings["init", true]) {
            startActivity(Intent(this, Setup::class.java))
            finish()
            exitProcess(0)
        }
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e("posidonLauncher", "uncaught exception", throwable)
            Settings.applyNow()
            startActivity(Intent(this, StackTraceActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra("throwable", throwable))
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
        Global.accentColor = Settings["accent", 0x04A5AD] or -0x1000000

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        updateNavbarHeight(this@Home)

        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        setContentView(R.layout.main)

        getSystemService(LauncherApps::class.java).registerCallback(AppLoader.Callback(applicationContext, drawer::onAppLoaderEnd))

        registerReceiver(
            appReloader,
            IntentFilter().apply {
                addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
                addAction(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)
            }
        )

        drawer.init(this)
        feed.init(drawer)

        setCustomizations()

        homeView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP && drawer.state == STATE_COLLAPSED)
                LiveWallpaper.tap(v, event.rawX.toInt(), event.rawY.toInt())
            false
        }
        if (Settings["mnmlstatus", false]) {
            window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                SYSTEM_UI_FLAG_LOW_PROFILE
        } else {
            window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        window.decorView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_LOCATION -> {
                    val view = event.localState as View?
                    if (view != null) {
                        val location = IntArray(2)
                        view.getLocationOnScreen(location)
                        val x = abs(event.x - location[0] - view.measuredWidth / 2f)
                        val y = abs(event.y - location[1] - view.measuredHeight / 2f)
                        if (x > view.width / 3.5f || y > view.height / 3.5f) {
                            ItemLongPress.currentPopup?.dismiss()
                            Folder.currentlyOpen?.dismiss()
                            drawer.state = STATE_COLLAPSED
                        }
                    }
                }
                DragEvent.ACTION_DRAG_STARTED -> {
                    (event.localState as View?)?.visibility = INVISIBLE
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    (event.localState as View?)?.visibility = VISIBLE
                    ItemLongPress.currentPopup?.isFocusable = true
                    ItemLongPress.currentPopup?.update()
                }
                DragEvent.ACTION_DROP -> {
                    (event.localState as View?)?.visibility = VISIBLE
                    if (drawer.state == STATE_EXPANDED || !dock.onItemDrop(event)) {
                        val item = LauncherItem(event.clipData.description.label.toString())!!
                        if (item is PinnedShortcut) {
                            PinnedShortcut.unpin(this, item)
                        }
                    }
                }
            }
            true
        }

        System.gc()
    }

    private fun setCustomizations() {

        feed.update(this, drawer)
        drawer.updateTheme(feed)
        drawer.locked = !Settings["drawer:slide_up", true]

        if (Settings["hidestatus", false]) {
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        run {
            val searchHint = Settings["search:text", getString(R.string.searchbarhint)]
            drawer.searchTxt.text = searchHint
            dock.searchTxt.text = searchHint
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Settings["gesture:back", ""] == "") {
                window.decorView.findViewById<View>(android.R.id.content).systemGestureExclusionRects = listOf(Rect(0, 0, Device.screenWidth(this), Device.screenHeight(this)))
            } else {
                window.decorView.findViewById<View>(android.R.id.content).systemGestureExclusionRects = listOf()
            }
        }

        Global.shouldSetApps = false
        Global.customized = false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        onUpdate()
        dock.loadAppsAndUpdateHome(drawer, feed, feed.desktopContent)
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            overridePendingTransition(R.anim.home_enter, R.anim.appexit)
        }
        onUpdate()
        feed.onResume(this)
    }

    private fun onUpdate() {
        val tmp = Tools.navbarHeight
        updateNavbarHeight(this)
        if (Global.customized || tmp != Tools.navbarHeight) {
            setCustomizations()
        }
        if (feed.notifications != null || Settings["notif:badges", true]) {
            NotificationService.onUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        drawer.state = STATE_COLLAPSED
        if (!Settings["feed:keep_pos", false]) {
            feed.scroll.scrollTo(0, if (Settings["feed:rest_at_bottom", false]) feed.desktopContent.height else 0)
        }
        feed.onPause()
    }

    override fun onStart() {
        super.onStart()
        drawer.onAppLoaderEnd()
    }

    override fun onDestroy() {
        runCatching {
            unregisterReceiver(appReloader)
        }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
//            if (feed.notifications != null || Settings["notif:badges", true]) {
//                try { startService(Intent(this, NotificationService::class.java)) }
//                catch (e: Exception) {}
//            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Settings["mnmlstatus", false]) window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                SYSTEM_UI_FLAG_LOW_PROFILE
            drawer.loadAppsIfShould()
        } else {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)  window.decorView.systemUiVisibility =
                SYSTEM_UI_FLAG_LAYOUT_STABLE or
                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        }
    }

    override fun onBackPressed() = when {
        drawer.state == STATE_EXPANDED -> drawer.state = STATE_COLLAPSED
        ResizableLayout.currentlyResizing != null -> ResizableLayout.currentlyResizing?.resizing = false
        else -> Gestures.performTrigger(Settings["gesture:back", ""], this)
    }

    fun openSearch(v: View) = SearchActivity.open(this)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val isActionMain = intent?.action == Intent.ACTION_MAIN
        if (isActionMain) try {
            handleGestureContract(intent!!)
        } catch (e: Exception) { e.printStackTrace() }
    }

    val picture = Picture()

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                //picture.beginRecording(1, 1)
                //picture.endRecording()
                //homeView.removeView(surfaceView)
            }
        } catch (e: Exception) {}
    }

    private val surfaceView by lazy { SurfaceView(this).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        homeView.addView(this, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        holder.addCallback(object : SurfaceHolder.Callback2 {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //drawOnSurface(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                //drawOnSurface(holder)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
            }

            override fun surfaceRedrawNeeded(holder: SurfaceHolder) {
                //drawOnSurface(holder)
            }
        })
    }}

    private fun drawOnSurface(holder: SurfaceHolder) {
        val c = try { holder.lockHardwareCanvas() } catch (e: Exception) { holder.lockCanvas() }
        if (c != null) {
            picture.draw(c)
            holder.unlockCanvasAndPost(c)
        }
    }

    /**
     * Handles gesture nav contract
     */
    private fun handleGestureContract(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val gnc = GestureNavContract.fromIntent(intent)
            if (gnc != null) {
                val r = RectF(0f, 0f, 0f, 0f)
                val i = dock.getLocationForApp(gnc.componentName, gnc.user, r)
                //val app = App[gnc.componentName.packageName, gnc.componentName.className, gnc.user.hashCode()] ?: Dock[i]
                //val icon = app!!.icon!!
                /*picture.run {
                    val c = beginRecording(r.width().toInt(), r.height().toInt())
                    //c.translate(-r.left, -r.top)
                    //dock.container.getChildAt(i).draw(c)
                    icon.draw(c)
                    endRecording()
                }*/
                //drawOnSurface(surfaceView.holder)
                gnc.sendEndPosition(r, surfaceView.surfaceControl)
            }
        }
    }
}