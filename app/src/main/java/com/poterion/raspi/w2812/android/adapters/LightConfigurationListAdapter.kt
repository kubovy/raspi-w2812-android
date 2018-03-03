package com.poterion.raspi.w2812.android.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListAdapter
import android.widget.TextView
import com.poterion.light.raspiw2812.android.R
import com.poterion.raspi.w2812.android.PATTERNS
import com.poterion.raspi.w2812.android.data.RealmLightConfiguration
import com.poterion.raspi.w2812.android.serial.Bluetooth
import io.realm.OrderedRealmCollection
import io.realm.RealmBaseAdapter
import kotlinx.android.synthetic.main.row_light_configuration.view.*

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightConfigurationListAdapter(realmResults: OrderedRealmCollection<RealmLightConfiguration>,
									private val selected: () -> RealmLightConfiguration?,
									private val onClick: (RealmLightConfiguration) -> Unit) :
		RealmBaseAdapter<RealmLightConfiguration>(realmResults), ListAdapter {
	class ViewHolder {
		internal var entryId: String? = null
		internal var title: TextView? = null
		internal var color1: TextView? = null
		internal var color2: TextView? = null
		internal var color3: TextView? = null
		internal var color4: TextView? = null
		internal var color5: TextView? = null
		internal var color6: TextView? = null
		internal var button: ImageButton? = null
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val root = convertView ?: LayoutInflater.from(parent?.context)
				.inflate(R.layout.row_light_configuration, parent, false)
		val viewHolder: ViewHolder = root.tag
				.takeIf { it is ViewHolder }
				?.let { it as ViewHolder }
				?: ViewHolder().apply {
					entryId = adapterData?.get(position)?.id
					title = root.testTitle
					color1 = root.textColor1
					color2 = root.textColor2
					color3 = root.textColor3
					color4 = root.textColor4
					color5 = root.textColor5
					color6 = root.textColor6
					button = root.imageButton.apply {
						isFocusable = false
						isFocusableInTouchMode = false
					}
					Bluetooth.available.subscribe { button?.visibility = if (it) View.VISIBLE else View.INVISIBLE }
					root.tag = this
				}

		if (adapterData != null) {
			val item = adapterData?.get(position)
			val pattern = PATTERNS.firstOrNull { it.id == item?.pattern } ?: PATTERNS.first()

			viewHolder.title?.text = StringBuilder(pattern.title).apply {
				if (pattern.hasWait) item?.wait?.also { append(" ").append(it).append(root?.context?.getString(R.string.units_ms)) }
				if (pattern.hasWidth) item?.width?.also { append(" ").append(it) }
				if (pattern.hasFading) item?.fading?.also { append(" ").append(it) }
				if (pattern.hasMin || pattern.hasMax) append(" (").append(item?.min ?: "").append("-").append(item?.max
						?: "").append(")")
			}

			viewHolder.color1?.setBackgroundColor(item?.color1?.takeIf { pattern.hasColor1 } ?: Color.TRANSPARENT)
			viewHolder.color2?.setBackgroundColor(item?.color2?.takeIf { pattern.hasColor2 } ?: Color.TRANSPARENT)
			viewHolder.color3?.setBackgroundColor(item?.color3?.takeIf { pattern.hasColor3 } ?: Color.TRANSPARENT)
			viewHolder.color4?.setBackgroundColor(item?.color4?.takeIf { pattern.hasColor4 } ?: Color.TRANSPARENT)
			viewHolder.color5?.setBackgroundColor(item?.color5?.takeIf { pattern.hasColor5 } ?: Color.TRANSPARENT)
			viewHolder.color6?.setBackgroundColor(item?.color6?.takeIf { pattern.hasColor6 } ?: Color.TRANSPARENT)
			viewHolder.button?.setOnClickListener { item?.also(onClick) }
			root?.setBackgroundColor(if (selected.invoke()?.id == item?.id) Color.CYAN else Color.TRANSPARENT)
		}
		return root
	}
}