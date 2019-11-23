package com.poterion.monitor.android.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.*
import com.poterion.monitor.android.R
import com.poterion.monitor.android.adapters.LightConfigurationListAdapter
import com.poterion.monitor.android.api.ActivityCommunicationInterface
import com.poterion.monitor.android.api.Page
import com.poterion.monitor.android.data.RealmLightConfiguration
import com.poterion.monitor.android.serial.Bluetooth
import com.poterion.monitor.android.serial.BluetoothSerial
import io.reactivex.disposables.Disposable
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_light_configuration_list.view.*
import android.view.MenuInflater

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightConfigurationListFragment : Fragment(), Page {
	private var interaction: ActivityCommunicationInterface? = null
	private var realm: Realm? = null
	private var availableSubscription: Disposable? = null
	private val selected = mutableListOf<String>()
	private val bluetoothConnectionBroadcastReceiver= object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			when(intent?.action) {
				BluetoothSerial.BLUETOOTH_CONNECTED -> view?.buttonSend?.isEnabled = true
				else -> view?.buttonSend?.isEnabled = false
			}
		}
	}

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
		context?.also { context ->
			LocalBroadcastManager.getInstance(context).registerReceiver(bluetoothConnectionBroadcastReceiver, IntentFilter().apply {
				addAction(BluetoothSerial.BLUETOOTH_CONNECTED)
				addAction(BluetoothSerial.BLUETOOTH_DISCONNECTED)
				addAction(BluetoothSerial.BLUETOOTH_FAILED)
			})
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
							  savedInstanceState: Bundle?): View? {
		val root = inflater.inflate(R.layout.fragment_light_configuration_list, container, false)
		root.listView.setOnItemClickListener { _, view, _, _ ->
			(root.listView.adapter as? LightConfigurationListAdapter)?.notifyDataSetChanged()
			view?.tag?.let { it as? LightConfigurationListAdapter.ViewHolder }
					?.entryId
					?.let { realm?.where(RealmLightConfiguration::class.java)?.equalTo("id", it)?.findFirst() }
					?.let { realm?.copyFromRealm(it) }
					?.also { interaction?.editLightConfiguration(it) }
		}
		availableSubscription = Bluetooth.available.subscribe { if (it) root.buttonSend.show() else root.buttonSend.hide() }
		root.buttonSend.setOnClickListener { _ ->
			realm?.where(RealmLightConfiguration::class.java)
					?.equalTo("set", interaction?.set ?: "")
					?.sort("order")
					?.findAll()
					?.mapNotNull { realm?.copyFromRealm(it) }
					?.also { interaction?.sendLightConfigurations(it) }
		}

		return root
	}

	override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
		menu?.findItem(R.id.action_new)?.isVisible = selected.isEmpty()
		menu?.findItem(R.id.action_save)?.isVisible = false
		menu?.findItem(R.id.action_clone)?.isVisible = selected.size == 1
		menu?.findItem(R.id.action_delete)?.isVisible = selected.isNotEmpty()
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean = when(item?.itemId) {
		R.id.action_clone -> {
			realm?.where(RealmLightConfiguration::class.java)
					?.equalTo("id", selected.firstOrNull())
					?.findFirst()
					?.let { realm?.copyFromRealm(it) }
					?.apply { id = null }
					?.also { interaction?.editLightConfiguration(it) }
			selected.clear()
			activity?.invalidateOptionsMenu()
			true
		}
		R.id.action_delete -> {
			realm?.executeTransaction {
				it.where(RealmLightConfiguration::class.java)
						.`in`("id", selected.toTypedArray())
						.findAll()
						.deleteAllFromRealm()
			}
			selected.clear()
			view?.listView?.invalidate()
			activity?.invalidateOptionsMenu()
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onShow() {
		Realm.getDefaultInstance().use { realm ->
			realm.where(RealmLightConfiguration::class.java)
					.equalTo("set", interaction?.set ?: "")
					.sort("order")
					.findAllAsync()
					.also { items ->
						view?.listView?.adapter = LightConfigurationListAdapter(items,
								{ interaction?.item },
								{ item-> onSelect(item)},
								{ item, isChecked -> onChecked(item, isChecked)},
								{ item -> interaction?.sendLightConfigurations(listOf(item)) })
					}
		}
	}

	private fun onSelect(item: RealmLightConfiguration) {
		item.let { realm?.copyFromRealm(item) }?.also { interaction?.editLightConfiguration(it) }
	}

	private fun onChecked(item: RealmLightConfiguration, isChecked: Boolean) {
		if (isChecked && !selected.contains(item.id)) item.id?.also { selected.add(it) }
		else while (selected.contains(item.id)) selected.remove(item.id)
		activity?.invalidateOptionsMenu()
	}

	override fun onDestroyView() {
		availableSubscription?.dispose()
		super.onDestroyView()
	}

	override fun onDestroy() {
		context?.also { context ->
			LocalBroadcastManager.getInstance(context).unregisterReceiver(bluetoothConnectionBroadcastReceiver)
		}
		realm?.close()
		realm = null
		super.onDestroy()
	}
}