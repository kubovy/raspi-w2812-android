package com.poterion.raspi.w2812.android.fragments

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import com.poterion.light.raspiw2812.android.R
import com.poterion.raspi.w2812.android.adapters.LightConfigurationSetListAdapter
import com.poterion.raspi.w2812.android.api.ActivityCommunicationInterface
import com.poterion.raspi.w2812.android.api.Page
import com.poterion.raspi.w2812.android.data.RealmLightConfiguration
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_light_configuration_list.view.*

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightConfigurationSetListFragment : Fragment(), Page {
	companion object {
		private val LOG_TAG = LightConfigurationSetListFragment::class.simpleName
	}

	private var interaction: ActivityCommunicationInterface? = null
	private var realm: Realm? = null

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		try {
			interaction = activity as ActivityCommunicationInterface
		} catch (e: ClassCastException) {
			throw ClassCastException(activity.toString() + " must implement ActivityCommunicationInterface")
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setHasOptionsMenu(true)
		realm = Realm.getDefaultInstance()
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		val root = inflater.inflate(R.layout.fragment_light_configuration_set_list, container, false)
		root.listView.setOnItemClickListener { _, view, position, _ ->
			Log.d(LOG_TAG, "${position} position clicked")
			(root.listView.adapter as? LightConfigurationSetListAdapter)?.notifyDataSetChanged()
			view?.tag?.takeIf { it is LightConfigurationSetListAdapter.ViewHolder }
					?.let { it as LightConfigurationSetListAdapter.ViewHolder }
					?.title
					?.text
					?.also { interaction?.showLightConfigurationSet(it.toString()) }
		}
		realm?.where(RealmLightConfiguration::class.java)
				?.distinct("set")
				?.sort("set")
				?.findAllAsync()
				?.also { sets ->
					root.listView.adapter = LightConfigurationSetListAdapter(sets, { interaction?.set ?: "" }) { set ->
						realm?.where(RealmLightConfiguration::class.java)
								?.equalTo("set", set)
								?.sort("order")
								?.findAll()
								?.mapNotNull { realm?.copyFromRealm(it) }
								?.also { interaction?.sendLightConfigurations(it) }
					}
				}
		return root
	}

	override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
		menu?.findItem(R.id.action_save)?.isVisible = false
	}

	override fun onShow() {
	}

	override fun onDestroy() {
		super.onDestroy()
		realm?.close()
		realm = null
	}
}