package com.poterion.monitor.android.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ListAdapter
import android.widget.TextView
import com.poterion.monitor.android.R
import com.poterion.monitor.android.data.RealmLightConfiguration
import com.poterion.monitor.android.serial.Bluetooth
import io.realm.OrderedRealmCollection
import io.realm.RealmBaseAdapter
import kotlinx.android.synthetic.main.row_light_configuration_set.view.*

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightConfigurationSetListAdapter(realmResults: OrderedRealmCollection<RealmLightConfiguration>,
									   private val selected: () -> String,
									   private val onClick: (String) -> Unit) :
		RealmBaseAdapter<RealmLightConfiguration>(realmResults), ListAdapter {
	class ViewHolder {
		internal var title: TextView? = null
		internal var button: ImageButton? = null
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
		val root = convertView ?: LayoutInflater.from(parent?.context)
				.inflate(R.layout.row_light_configuration_set, parent, false)
		val viewHolder: ViewHolder = root.tag
				.takeIf { it is ViewHolder }
				?.let { it as ViewHolder }
				?: ViewHolder().apply {
					title = root.textTitle
					button = root.imageButton.apply {
						isFocusable = false
						isFocusableInTouchMode = false
					}
					Bluetooth.available.subscribe { button?.visibility = if (it) View.VISIBLE else View.INVISIBLE }
					root.tag = this
				}

		if (adapterData != null) {
			val item = adapterData?.get(position)
			viewHolder.title?.text = item?.set
			viewHolder.button?.setOnClickListener { item?.set?.also(onClick) }
			root?.setBackgroundColor(if (selected.invoke() == item?.set) Color.CYAN else Color.TRANSPARENT)
		}
		return root
	}
}