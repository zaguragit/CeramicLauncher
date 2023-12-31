package one.zagura.CeramicLauncher.ui.view.feed

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import one.zagura.CeramicLauncher.external.widgets.Widget
import one.zagura.CeramicLauncher.util.storage.Settings
import one.zagura.CeramicLauncher.ui.view.ResizableLayout

class WidgetSection(
    context: Context,
    var widget: Widget,
) : ResizableLayout(context, minHeight = 64.dp.toPixels(context)), FeedSection {

    override fun onAdd(feed: Feed, i: Int) {
        layoutParams.height = Settings["widget:${widget.widgetId}:height", ViewGroup.LayoutParams.WRAP_CONTENT]
        onResizeListener = object : OnResizeListener {
            override fun onStop(newHeight: Int) { Settings["widget:${widget.widgetId}:height"] = newHeight }
            override fun onCrossPress() = feed.remove(this@WidgetSection)
            override fun onMajorUpdate(newHeight: Int) = widget.resize(context, newHeight)
            override fun onUpdate(newHeight: Int) {
                layoutParams.height = newHeight
                layoutParams = layoutParams
            }
        }

        if (!widget.fromSettings(this)) {
            feed.remove(this)
            return
        }
        widget.startListening()
    }

    override fun updateTheme(activity: Activity) {}

    override fun onPause() = widget.stopListening()
    override fun onResume(activity: Activity) = widget.startListening()

    override fun toString() = "widget:${widget.widgetId}"

    override fun onDelete(feed: Feed) {
        widget.deleteWidget(this@WidgetSection)
    }
}