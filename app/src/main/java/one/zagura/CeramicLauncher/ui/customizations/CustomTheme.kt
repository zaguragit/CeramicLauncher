package one.zagura.CeramicLauncher.ui.customizations

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import io.posidon.android.conveniencelib.drawable.MaskedDrawable
import io.posidon.android.conveniencelib.drawable.toBitmap
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import one.zagura.CeramicLauncher.Global
import one.zagura.CeramicLauncher.R
import one.zagura.CeramicLauncher.util.drawable.FastColorDrawable
import one.zagura.CeramicLauncher.util.storage.Settings
import one.zagura.CeramicLauncher.util.theme.Icons
import one.zagura.CeramicLauncher.ui.view.setting.*

class CustomTheme : AppCompatActivity() {

    private val icShapeViews by lazy { arrayOf<View>(findViewById(R.id.icshapedef), findViewById(R.id.icshaperound), findViewById(R.id.icshaperoundrect), findViewById(R.id.icshaperect), findViewById(R.id.icshapesquircle)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
        configureWindowForSettings()

        setSettingsContentView(R.string.settings_title_theme) {
            color(
                R.string.accent_color,
                R.drawable.ic_droplet,
                "accent",
                0xff04A5AD.toInt(),
                hasAlpha = false,
            ).onSelected = {
                Global.accentColor = it
            }
            title(R.string.icon_shape)
            custom {
                layoutInflater.inflate(R.layout.custom_icon_shapes, viewGroup, false)
            }
        }
        val size = 48.dp.toPixels(this)
        val bg = BitmapDrawable(resources, FastColorDrawable(Global.accentColor and 0x00ffffff or 0x55000000).toBitmap(size, size))
        bg.setBounds(0, 0, size, size)
        icShapeViews.onEachIndexed { i, v ->
            (v as ViewGroup).addView(ImageView(this).apply {
                val s = Icons.IconShape(i)
                setImageDrawable(MaskedDrawable(bg, s.getPath(size, size)))
            }, 0, ViewGroup.LayoutParams(size, size))
        }
        icShapeViews[Settings["icshape", 0]].setBackgroundResource(R.drawable.selection)
        Global.shouldSetApps = true
    }

    fun icshapedef(v: View) {
        icShapeViews[Settings["icshape", 0]].setBackgroundColor(0x0)
        Settings["icshape"] = 0
        v.setBackgroundResource(R.drawable.selection)
    }

    fun icshaperound(v: View) {
        icShapeViews[Settings["icshape", 0]].setBackgroundColor(0x0)
        Settings["icshape"] = 1
        v.setBackgroundResource(R.drawable.selection)
    }

    fun icshaperoundrect(v: View) {
        icShapeViews[Settings["icshape", 0]].setBackgroundColor(0x0)
        Settings["icshape"] = 2
        v.setBackgroundResource(R.drawable.selection)
    }

    fun icshaperect(v: View) {
        icShapeViews[Settings["icshape", 0]].setBackgroundColor(0x0)
        Settings["icshape"] = 3
        v.setBackgroundResource(R.drawable.selection)
    }

    fun icshapesquircle(v: View) {
        icShapeViews[Settings["icshape", 0]].setBackgroundColor(0x0)
        Settings["icshape"] = 4
        v.setBackgroundResource(R.drawable.selection)
    }
}