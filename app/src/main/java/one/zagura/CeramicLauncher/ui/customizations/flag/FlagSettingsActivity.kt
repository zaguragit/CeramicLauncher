package one.zagura.CeramicLauncher.ui.customizations.flag

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.posidon.android.conveniencelib.getStatusBarHeight
import io.posidon.android.slablauncher.ui.settings.flag.ColorsAdapter
import one.zagura.CeramicLauncher.Global
import one.zagura.CeramicLauncher.R
import one.zagura.CeramicLauncher.ui.view.setting.configureWindowForSettings
import one.zagura.CeramicLauncher.util.Tools
import one.zagura.CeramicLauncher.util.storage.Settings

class FlagSettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Settings.init(applicationContext)
        configureWindowForSettings()
        setContentView(R.layout.flag_settings_activity)

        val colorsAdapter = ColorsAdapter()
        val colorsRecycler = findViewById<RecyclerView>(R.id.colors_recycler).apply {
            setPadding(0, getStatusBarHeight(), 0, Tools.navbarHeight)
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            this.adapter = colorsAdapter
        }

        with(findViewById<FloatingActionButton>(R.id.add)) {
            backgroundTintList = ColorStateList.valueOf(Global.getPastelAccent())
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.ui_background))
            (layoutParams as MarginLayoutParams).bottomMargin = Tools.navbarHeight
            setOnClickListener {
                colorsAdapter.addColor(this@FlagSettingsActivity)
            }
        }

        with(findViewById<FloatingActionButton>(R.id.templates)) {
            backgroundTintList = ColorStateList.valueOf(Global.getPastelAccent())
            imageTintList = ColorStateList.valueOf(context.getColor(R.color.ui_background))
            (layoutParams as MarginLayoutParams).bottomMargin = Tools.navbarHeight
            setOnClickListener {
                AlertDialog.Builder(this@FlagSettingsActivity)
                    .setItems(
                        resources.getStringArray(R.array.flag_presets)
                    ) { d, i ->
                        colorsAdapter.setColors(context, FLAG_PRESETS[i].mapTo(ArrayList()) { it.toString(16) })
                        d.dismiss()
                    }
                    .show()
            }
        }

        val th = ItemTouchHelper(TouchCallback(colorsAdapter))
        th.attachToRecyclerView(colorsRecycler)
    }
    companion object {
        val FLAG_PRESET_ACE = intArrayOf(0xff000000.toInt(), 0xff7f7f7f.toInt(), 0xffffffff.toInt(), 0xff660066.toInt())
        val FLAG_PRESET_NB = intArrayOf(0xfffcf431.toInt(), 0xfffcfcfc.toInt(), 0xff9d59d2.toInt(), 0xff2a2a2a.toInt())
        val FLAG_PRESET_RAINBOW = intArrayOf(0xffe50000.toInt(), 0xffff8d00.toInt(), 0xffffee00.toInt(), 0xff008121.toInt(), 0xff3a62bf.toInt(), 0xff760188.toInt())
        val FLAG_PRESET_TRANS = intArrayOf(0xff5bcffa.toInt(), 0xfff5abb9.toInt(), 0xffffffff.toInt(), 0xfff5abb9.toInt(), 0xff5bcffa.toInt())
        val FLAG_PRESET_BI = intArrayOf(0xffD60270.toInt(), 0xffD60270.toInt(), 0xff9B4F96.toInt(), 0xff0038A8.toInt(), 0xff0038A8.toInt())
        val FLAG_PRESET_WLW = intArrayOf(0xffD52D00.toInt(), 0xffFF9A56.toInt(), 0xffFFFFFF.toInt(), 0xffD162A4.toInt(), 0xffA30262.toInt())
        val FLAG_PRESET_MLM = intArrayOf(0xff078D70.toInt(), 0xff98E8C1.toInt(), 0xffFFFFFF.toInt(), 0xff7BADE2.toInt(), 0xff3D1A78.toInt())
        val FLAG_PRESETS = arrayOf(FLAG_PRESET_ACE, FLAG_PRESET_NB, FLAG_PRESET_RAINBOW, FLAG_PRESET_TRANS, FLAG_PRESET_BI, FLAG_PRESET_WLW, FLAG_PRESET_MLM)
    }

    class TouchCallback(val adapter: ColorsAdapter) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {
            adapter.removeColor(v.itemView.context, v.adapterPosition)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.adapterPosition
            val toPosition = target.adapterPosition
            adapter.onMove(recyclerView.context, fromPosition, toPosition)
            return true
        }
    }
}