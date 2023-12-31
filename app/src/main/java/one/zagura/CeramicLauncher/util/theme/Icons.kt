package one.zagura.CeramicLauncher.util.theme

import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.luminance
import androidx.palette.graphics.Palette
import io.posidon.android.conveniencelib.drawable.MaskedDrawable
import one.zagura.CeramicLauncher.Global
import one.zagura.CeramicLauncher.util.drawable.ContactDrawable
import one.zagura.CeramicLauncher.util.drawable.NonDrawable
import one.zagura.CeramicLauncher.util.storage.Settings
import one.zagura.CeramicLauncher.util.Tools
import kotlin.math.abs
import kotlin.math.min
import kotlin.random.Random

object Icons {

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun generateAdaptiveIcon(drawable: Drawable, iconShape: IconShape = IconShape(Settings["icshape", 0])): Drawable {
        if (drawable is AdaptiveIconDrawable) {
            val tmp = LayerDrawable(arrayOf(drawable.background ?: NonDrawable(), drawable.foreground ?: NonDrawable()))
            val w = tmp.intrinsicWidth
            val h = tmp.intrinsicHeight
            tmp.setLayerInset(0, -w / 6, -h / 6, -w / 6, -h / 6)
            tmp.setLayerInset(1, -w / 6, -h / 6, -w / 6, -h / 6)
            val width = tmp.intrinsicWidth
            val height = tmp.intrinsicHeight
            tmp.setBounds(0, 0, width, height)
            return BitmapDrawable(Tools.appContext!!.resources, MaskedDrawable(tmp, iconShape.getPath(width, height)).toBitmap())
        }
        return drawable
    }

    private val pics = HashMap<String, ContactDrawable>()
    fun generateContactPicture(name: String, paint: Paint): Drawable? {
        val realName = name.trim()
        if (realName.isEmpty()) return null
        return pics.getOrPut(name) {
            ContactDrawable(
                ColorTools.getColorForText(name),
                realName.substring(0, realName.length.coerceAtMost(2)),
                paint,
            )
        }
    }

    fun generateNotificationBadgeBGnFG(icon: Drawable? = null, onGenerated: (bg: Drawable, fg: Int) -> Unit) {
        val bgType = Settings["notif:badges:bg_type", 0]
        val customBG = Settings["notif:badges:bg_color", 0xffff5555.toInt()]
        if (icon != null && bgType == 0) {
            Palette.from(icon.toBitmap()).generate {
                val bg = it?.getDominantColor(customBG) ?: customBG
                onGenerated(ColorTools.iconBadge(bg), if (bg.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
            }
        } else if (bgType == 1) {
            val bg = Global.accentColor
            onGenerated(ColorTools.iconBadge(bg), if (bg.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
        } else {
            onGenerated(ColorTools.iconBadge(customBG), if (customBG.luminance > .6f) 0xff111213.toInt() else 0xffffffff.toInt())
        }
    }

    inline class IconShape(val int: Int) {
        inline val isSquare get() = int == 3
        inline val isSystem get() = int == 0

        fun getPath(width: Int, height: Int): Path {
            val minSize = min(width, height)
            if (isSystem) {
                val path = AdaptiveIconDrawable(null, null).iconMask
                val rect = RectF()
                path.computeBounds(rect, true)
                val matrix = Matrix()
                matrix.setScale(minSize / rect.right, minSize / rect.bottom)
                path.transform(matrix)
                path.fillType = Path.FillType.INVERSE_EVEN_ODD
                return path
            } else {
                val path = Path()
                when (int) {
                    1 -> path.addCircle(width / 2f, height / 2f, minSize / 2f - 2, Path.Direction.CCW)
                    2 -> path.addRoundRect(2f, 2f, width - 2f, height - 2f, minSize / 4f, minSize / 4f, Path.Direction.CCW)
                    3 -> path.addRoundRect(2f, 2f, width - 2f, height - 2f, minSize / 10f, minSize / 10f, Path.Direction.CCW)
                    4 -> { //Formula: (|x|)^3 + (|y|)^3 = radius^3
                        val xx = 2
                        val yy = 2
                        val radius = minSize / 2 - 2
                        val radiusToPow = radius * radius * radius.toDouble()
                        path.moveTo(-radius.toFloat(), 0f)
                        for (x in -radius..radius) {
                            path.lineTo(x.toFloat(), Math.cbrt(radiusToPow - abs(x * x * x)).toFloat())
                        }
                        for (x in radius downTo -radius) {
                            path.lineTo(x.toFloat(), (-Math.cbrt(radiusToPow - abs(x * x * x))).toFloat())
                        }
                        path.close()
                        val matrix = Matrix()
                        matrix.postTranslate(xx + radius.toFloat(), yy + radius.toFloat())
                        path.transform(matrix)
                    }
                }
                path.fillType = Path.FillType.INVERSE_EVEN_ODD
                return path
            }
        }
    }
}