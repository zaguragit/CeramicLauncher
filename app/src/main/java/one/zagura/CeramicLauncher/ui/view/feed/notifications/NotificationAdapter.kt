package one.zagura.CeramicLauncher.ui.view.feed.notifications

import android.app.RemoteInput
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.graphics.luminance
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import one.zagura.CeramicLauncher.Global
import one.zagura.CeramicLauncher.R
import one.zagura.CeramicLauncher.data.NotificationItem
import one.zagura.CeramicLauncher.provider.notifications.NotificationService
import one.zagura.CeramicLauncher.util.storage.Settings
import one.zagura.CeramicLauncher.util.Gestures
import one.zagura.CeramicLauncher.ui.view.SwipeableLayout

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(
        val card: SwipeableLayout,
    ) : RecyclerView.ViewHolder(card)

    private var notifications = ArrayList<NotificationItem>()

    override fun getItemCount() = notifications.size

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): ViewHolder {
        val context = parent.context

        val view = LayoutInflater.from(context).inflate(R.layout.notification, null).apply {
            findViewById<TextView>(R.id.txt).maxLines = Settings["notif:text:max_lines", 3]
            setOnLongClickListener(Gestures::onLongPress)
        }
        val card = SwipeableLayout(view).apply {
            val bg = Settings["notif:card_swipe_bg_color", 0x880d0e0f.toInt()]
            setIconColor(if (bg.luminance > .6f) 0xff000000.toInt() else 0xffffffff.toInt())
            setSwipeColor(bg)
        }

        return ViewHolder(card).apply {
            card.onSwipeAway = {
                val n = notifications[adapterPosition]
                if (n.isCancellable) {
                    try { n.cancel() }
                    catch (e: Exception) { e.printStackTrace() }
                    NotificationService.update()
                } else card.reset()
            }
            view.setOnClickListener { notifications[adapterPosition].open() }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, i: Int) {
        val context = holder.card.context
        val view = holder.card
        val notification = notifications[i]

        view.reset()

        val progressBar = view.findViewById<ProgressBar>(R.id.progress)
        if (notification.max != -1 && notification.progress != -1 && notification.max != notification.progress) {
            progressBar.isVisible = true
            if (notification.progress == -2) {
                progressBar.isIndeterminate = true
            } else {
                progressBar.isIndeterminate = false
                progressBar.progressTintList = ColorStateList.valueOf(Global.accentColor)
                progressBar.progressBackgroundTintList = ColorStateList.valueOf(Global.accentColor)
                progressBar.progress = notification.progress
                progressBar.max = notification.max
            }
        } else {
            progressBar.isVisible = false
        }
        val actionList = view.findViewById<LinearLayout>(R.id.action_list)
        actionList.removeAllViews()
        if (notification.actions != null && Settings["notif:actions:enabled", true]) {
            actionList.isVisible = true
            view.findViewById<View>(R.id.top_separator).visibility = View.VISIBLE
            view.findViewById<View>(R.id.top_separator).setBackgroundColor(Settings["notif:text_color", -0xdad9d9] and 0xffffff or 0x33000000)
            with(view.findViewById<View>(R.id.action_area)) {
                setBackgroundColor(Settings["notif:actions:background_color", 0x88e0e0e0.toInt()])
                isVisible = notification.actions.isNotEmpty()
            }
            for (action in notification.actions) {
                val a = TextView(context)
                a.text = action.title
                a.textSize = 14f
                a.isAllCaps = true
                a.typeface = Typeface.DEFAULT_BOLD
                a.setTextColor(Settings["notif:actions:text_color", -0xdad9d9])
                val vPadding = 10.dp.toPixels(context)
                val hPadding = 12.dp.toPixels(context)
                a.setPadding(hPadding, vPadding, hPadding, vPadding)
                actionList.addView(a)
                a.setOnClickListener {
                    try {
                        val oldInputs = action.remoteInputs
                        if (oldInputs != null) {
                            view.findViewById<View>(R.id.bottom_separator).visibility = View.VISIBLE
                            view.findViewById<View>(R.id.bottom_separator).setBackgroundColor(Settings["notif:text_color", -0xdad9d9] and 0xffffff or 0x33000000)
                            view.findViewById<View>(R.id.reply).apply lin@ {
                                visibility = View.VISIBLE
                                val imm = getSystemService(context, InputMethodManager::class.java)!!
                                val textArea = findViewById<EditText>(R.id.replyText).apply {
                                    setTextColor(Settings["notif:text_color", -0xdad9d9])
                                    setHintTextColor(Settings["notif:text_color", -0xdad9d9] and 0xffffff or 0x88000000.toInt())
                                    requestFocus()
                                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                                    setOnFocusChangeListener { _, hasFocus ->
                                        if (!hasFocus) {
                                            text.clear()
                                            this@lin.visibility = View.GONE
                                            view.findViewById<View>(R.id.bottom_separator).visibility = View.GONE
                                            imm.hideSoftInputFromWindow(windowToken, 0)
                                        }
                                    }
                                }
                                findViewById<ImageView>(R.id.cancel).apply {
                                    imageTintList = ColorStateList.valueOf(Settings["notif:text_color", -0xdad9d9])
                                    setOnClickListener {
                                        textArea.text.clear()
                                        this@lin.visibility = View.GONE
                                        view.findViewById<View>(R.id.bottom_separator).visibility = View.GONE
                                    }
                                }
                                findViewById<ImageView>(R.id.replySend).apply {
                                    imageTintList = ColorStateList.valueOf(Settings["notif:text_color", -0xdad9d9])
                                    setOnClickListener {
                                        val intent = Intent()
                                        val bundle = Bundle()
                                        val actualInputs: ArrayList<RemoteInput> = ArrayList()
                                        for (input in oldInputs) {
                                            bundle.putCharSequence(input.resultKey, textArea.text)
                                            val builder = RemoteInput.Builder(input.resultKey)
                                            builder.setLabel(input.label)
                                            builder.setChoices(input.choices)
                                            builder.setAllowFreeFormInput(input.allowFreeFormInput)
                                            builder.addExtras(input.extras)
                                            actualInputs.add(builder.build())
                                        }
                                        val inputs = actualInputs.toArray(arrayOfNulls<RemoteInput>(actualInputs.size))
                                        RemoteInput.addResultsToIntent(inputs, intent, bundle)
                                        action.actionIntent.send(context, 0, intent)
                                        textArea.text.clear()
                                        this@lin.visibility = View.GONE
                                        view.findViewById<View>(R.id.bottom_separator).visibility = View.GONE
                                    }
                                }
                            }
                        } else {
                            action.actionIntent.send()
                        }
                    }
                    catch (e: Exception) { e.printStackTrace() }
                }
            }
        }

        view.findViewById<TextView>(R.id.source).apply {
            text = notification.source
            setTextColor(notification.color)
        }
        view.findViewById<ImageView>(R.id.source_icon).setImageDrawable(notification.sourceIcon)
        view.findViewById<TextView>(R.id.title).run {
            text = notification.title
            setTextColor(Settings["notif:title_color", -0xeeeded])
        }
        view.findViewById<TextView>(R.id.txt).run {
            text = notification.text
            setTextColor(Settings["notif:text_color", -0xdad9d9])
        }

        if (notification.image != null) with(view.findViewById<ImageView>(R.id.iconimg)) {
            isVisible = true
            setImageDrawable(notification.image)
        } else {
            view.findViewById<ImageView>(R.id.iconimg).isVisible = false
        }

        view.findViewById<View>(R.id.notif_separator).setBackgroundColor(Settings["notif:text_color", -0xdad9d9] and 0xffffff or 0x33000000)
    }

    fun update(notifications: ArrayList<NotificationItem>) {
        this.notifications = notifications
        notifyDataSetChanged()
    }
}
