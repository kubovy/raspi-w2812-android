package com.poterion.monitor.android.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.support.v4.content.LocalBroadcastManager
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import com.poterion.monitor.android.PATTERNS
import com.poterion.monitor.android.R
import com.poterion.monitor.android.api.ActivityCommunicationInterface
import com.poterion.monitor.android.api.Page
import com.poterion.monitor.android.data.LightPattern
import com.poterion.monitor.android.data.RealmLightConfiguration
import com.poterion.monitor.android.serial.Bluetooth
import com.poterion.monitor.android.serial.BluetoothSerial
import com.poterion.monitor.android.data.LightPatternConfig
import io.reactivex.disposables.Disposable
import io.realm.Realm
import kotlinx.android.synthetic.main.fragment_light_configuration_form.view.*
import java.util.*

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightConfigurationFormFragment : Fragment(), Page {
	private var interaction: ActivityCommunicationInterface? = null
	private var realm: Realm? = null
	private var availableSubscription: Disposable? = null
	private val bluetoothConnectionBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			when (intent?.action) {
				BluetoothSerial.BLUETOOTH_CONNECTED -> view?.buttonSend?.isEnabled = true
				else -> view?.buttonSend?.isEnabled = false
			}
		}
	}

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		// This makes sure that the container activity has implemented
		// the interaction interface. If not, it throws an exception
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
		val root = inflater.inflate(R.layout.fragment_light_configuration_form, container, false)

		val sets = Realm.getDefaultInstance().use { realm ->
			realm.where(RealmLightConfiguration::class.java)
					.distinct("set")
					.sort("set")
					.findAll()
					.map { it.set }
		}

		root.autoCompleteSet.setAdapter(ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, sets))
		root.spinnerPattern.adapter = object : ArrayAdapter<LightPatternConfig>(requireContext(), android.R.layout.simple_dropdown_item_1line, PATTERNS) {
			override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
				var row = convertView
				if (row == null) { //inflate your custom layout for the textview
					row = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
				}
				//put the data in it
				val item = getItem(position)
				if (item != null) {
					(row as? TextView)?.text = item.title
				}
				return row
			}

			override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
				var row = convertView
				if (row == null) {
					row = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
				}
				//put the data in it
				val item = getItem(position)
				if (item != null) {
					(row as? TextView)?.text = item.title
				}

				return row
			}
		}
		root.spinnerPattern.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onNothingSelected(parent: AdapterView<*>?) {
			}

			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				PATTERNS[position].also { pattern ->
					if (root.spinnerColor.selectedItemPosition == 0) {
						root.buttonColor1.isEnabled = pattern.hasColor1
						root.buttonColor2.isEnabled = pattern.hasColor2
						root.buttonColor3.isEnabled = pattern.hasColor3
						root.buttonColor4.isEnabled = pattern.hasColor4
						root.buttonColor5.isEnabled = pattern.hasColor5
						root.buttonColor6.isEnabled = pattern.hasColor6
						// TODO root.buttonColor7.isEnabled = pattern.hasColor7
						root.editFading.isEnabled = pattern.hasFading
					} else {
						root.buttonColor1.isEnabled = false
						root.buttonColor2.isEnabled = false
						root.buttonColor3.isEnabled = false
						root.buttonColor4.isEnabled = false
						root.buttonColor5.isEnabled = false
						root.buttonColor6.isEnabled = false
						// TODO root.buttonColor7.isEnabled = false
						root.editFading.isEnabled = true
					}
					root.editDelay.isEnabled = pattern.hasDelay
					root.editWidth.isEnabled = pattern.hasWidth
					root.editFading.isEnabled = pattern.hasFading
					root.editTimeout.isEnabled = true
					root.seekMinimum.isEnabled = pattern.hasMin
					root.seekMaximum.isEnabled = pattern.hasMax

					root.editDelay.setText(pattern.pattern.delay?.toString() ?: "1000")
					root.editWidth.setText(pattern.pattern.width?.toString() ?: "3")
					root.editFading.setText(pattern.pattern.fading?.toString() ?: "0")
					root.editTimeout.setText(pattern.pattern.timeout?.toString() ?: "1")
					root.seekMinimum.progress = pattern.pattern.min?.let { it / 255 * 100 } ?: 0
					root.seekMaximum.progress = pattern.pattern.max?.let { it / 255 * 100 } ?: 100

				}
			}
		}

		root.spinnerColor.adapter = object : ArrayAdapter<String>(requireContext(), android.R.layout.simple_dropdown_item_1line, resources.getStringArray(R.array.colors)) {
			override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
				var row = convertView
				if (row == null) { //inflate your custom layout for the textview
					row = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
				}
				//put the data in it
				val item = getItem(position)
				if (item != null) (row as? TextView)?.text = item
				return row
			}

			override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View? {
				var row = convertView
				if (row == null) {
					row = LayoutInflater.from(context).inflate(android.R.layout.simple_dropdown_item_1line, parent, false)
				}
				//put the data in it
				val item = getItem(position)
				if (item != null) (row as? TextView)?.text = item

				return row
			}
		}
		root.spinnerColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
			override fun onNothingSelected(parent: AdapterView<*>?) {
			}

			override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
				if (position == 0) {
					val pattern = (root.spinnerPattern.selectedItem as? LightPatternConfig)
					root.buttonColor1.isEnabled = pattern?.hasColor1 ?: true
					root.buttonColor2.isEnabled = pattern?.hasColor2 ?: true
					root.buttonColor3.isEnabled = pattern?.hasColor3 ?: true
					root.buttonColor4.isEnabled = pattern?.hasColor4 ?: true
					root.buttonColor5.isEnabled = pattern?.hasColor5 ?: true
					root.buttonColor6.isEnabled = pattern?.hasColor6 ?: true
					// TODO root.buttonColor7.isEnabled = pattern?.hasColor7 ?: true
					root.editFading.isEnabled = pattern?.hasFading ?: true
				} else {
					root.buttonColor1.isEnabled = false
					root.buttonColor2.isEnabled = false
					root.buttonColor3.isEnabled = false
					root.buttonColor4.isEnabled = false
					root.buttonColor5.isEnabled = false
					root.buttonColor6.isEnabled = false
					// TODO root.buttonColor7.isEnabled = false
					root.editFading.isEnabled = true
				}
			}
		}


		root.buttonColor1.setBackgroundColor(Color.WHITE)
		root.buttonColor1.setOnClickListener {
			createColorPicker((root.buttonColor1.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor1.setBackgroundColor(it)
			}
		}
		root.buttonColor2.setBackgroundColor(Color.WHITE)
		root.buttonColor2.setOnClickListener {
			createColorPicker((root.buttonColor2.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor2.setBackgroundColor(it)
			}
		}
		root.buttonColor3.setBackgroundColor(Color.WHITE)
		root.buttonColor3.setOnClickListener {
			createColorPicker((root.buttonColor3.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor3.setBackgroundColor(it)
			}
		}
		root.buttonColor4.setBackgroundColor(Color.WHITE)
		root.buttonColor4.setOnClickListener {
			createColorPicker((root.buttonColor4.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor4.setBackgroundColor(it)
			}
		}
		root.buttonColor5.setBackgroundColor(Color.WHITE)
		root.buttonColor5.setOnClickListener {
			createColorPicker((root.buttonColor5.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor5.setBackgroundColor(it)
			}
		}
		root.buttonColor6.setBackgroundColor(Color.WHITE)
		root.buttonColor6.setOnClickListener {
			createColorPicker((root.buttonColor6.background as? ColorDrawable)?.color
					?: Color.WHITE) {
				root.buttonColor6.setBackgroundColor(it)
			}
		}

		availableSubscription = Bluetooth.available.subscribe { if (it) root.buttonSend.show() else root.buttonSend.hide() }
		root.buttonSend.setOnClickListener { interaction?.sendLightConfigurations(listOf(createLightConfig())) }
		return root
	}

	override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
		menu?.findItem(R.id.action_new)?.isVisible = true
		menu?.findItem(R.id.action_save)?.isVisible = true
		menu?.findItem(R.id.action_clone)?.isVisible = true
		menu?.findItem(R.id.action_delete)?.isVisible = interaction?.item?.id != null
	}

	override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
		R.id.action_new -> {
			onShow()
			true
		}
		R.id.action_save -> Realm.getDefaultInstance().use { realm ->
			realm.executeTransaction { transaction ->
				createLightConfig().also { item ->
					transaction.insertOrUpdate(item)
					interaction?.item = item
				}
			}
			true
		}
		R.id.action_clone -> {
			interaction?.editLightConfiguration(
					createLightConfig().apply { id = UUID.randomUUID().toString() })
			true
		}
		R.id.action_delete -> {
			Realm.getDefaultInstance().use { realm ->
				realm.executeTransaction { transaction ->
					interaction?.item?.id?.also { id ->
						realm.executeTransaction {
							it.where(RealmLightConfiguration::class.java)
									.equalTo("id", id)
									.findFirst()
									?.deleteFromRealm()
							interaction?.goto(1)
						}
					}
				}
			}
			true
		}
		else -> super.onOptionsItemSelected(item)
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

	override fun onShow() {
		val item = interaction?.item
		view?.autoCompleteSet?.setText(item?.set ?: interaction?.set ?: "")
		view?.spinnerPattern?.setSelection(PATTERNS
				.indexOfFirst { it.pattern.name == item?.pattern }
				.takeIf { it >= 0 }
				?: 0)
		view?.spinnerColor?.setSelection(item?.getRainbow() ?: 0)

		view?.buttonColor1?.setBackgroundColor(item?.takeIf { it.color1.getRainbow() == 0 }?.color1 ?: Color.WHITE)
		view?.buttonColor2?.setBackgroundColor(item?.takeIf { it.color2.getRainbow() == 0 }?.color2 ?: Color.WHITE)
		view?.buttonColor3?.setBackgroundColor(item?.takeIf { it.color3.getRainbow() == 0 }?.color3 ?: Color.WHITE)
		view?.buttonColor4?.setBackgroundColor(item?.takeIf { it.color4.getRainbow() == 0 }?.color4 ?: Color.WHITE)
		view?.buttonColor5?.setBackgroundColor(item?.takeIf { it.color5.getRainbow() == 0 }?.color5 ?: Color.WHITE)
		view?.buttonColor6?.setBackgroundColor(item?.takeIf { it.color6.getRainbow() == 0 }?.color6 ?: Color.WHITE)
		//view?.buttonColor7?.setBackgroundColor(item?.takeIf { it.color7.getRainbow() == 0 }?.color7 ?: Color.WHITE)
		view?.editDelay?.setText(item?.delay?.toString())
		view?.editWidth?.setText(item?.width?.toString())
		view?.editFading?.setText(item?.fading?.toString())
		view?.editTimeout?.setText(item?.timeout?.toString())
		view?.seekMinimum?.progress = item?.min ?: 0
		view?.seekMaximum?.progress = item?.max ?: 100
	}

	private fun createColorPicker(@ColorInt initialColor: Int = Color.WHITE, onSelect: (Int) -> Unit): Unit = ColorPickerDialogBuilder
			.with(context)
			.setTitle("Choose color")
			.initialColor(initialColor)
			.wheelType(ColorPickerView.WHEEL_TYPE.CIRCLE)
			.density(12)
			.setOnColorSelectedListener { }
			.setPositiveButton("Ok") { _, selectedColor, _ -> onSelect.invoke(selectedColor) }
			.setNegativeButton("Cancel") { _, _ -> }
			.build()
			.show()

	private fun createLightConfig(): RealmLightConfiguration {
		val set = view?.autoCompleteSet?.text.toString()
		val nextOrder = interaction?.item?.id?.let { realm?.where(RealmLightConfiguration::class.java)?.equalTo("id", it)?.findFirst()?.order }
				?: realm?.where(RealmLightConfiguration::class.java)?.equalTo("set", set)?.max("order")?.toInt()?.let { it + 1 }
				?: 0


		return RealmLightConfiguration(
				id = interaction?.item?.id ?: UUID.randomUUID().toString(),
				set = set,
				order = nextOrder,
				pattern = view?.spinnerPattern?.selectedItemPosition
						.takeIf { it != AdapterView.INVALID_POSITION }
						?.let { PATTERNS[it] }?.pattern?.name
						?: LightPattern.LIGHT_FULL.name,
				color1 = view?.buttonColor1?.getColor() ?: Color.BLACK,
				color2 = view?.buttonColor2?.getColor() ?: Color.BLACK,
				color3 = view?.buttonColor3?.getColor() ?: Color.BLACK,
				color4 = view?.buttonColor4?.getColor() ?: Color.BLACK,
				color5 = view?.buttonColor5?.getColor() ?: Color.BLACK,
				color6 = view?.buttonColor6?.getColor() ?: Color.BLACK,
				color7 = Color.BLACK, //TODO view?.buttonColor7?.getColor() ?: Color.BLACK,
				delay = view?.editDelay?.text?.toString()?.toIntOrNull() ?: 50,
				width = view?.editWidth?.text?.toString()?.toIntOrNull() ?: 3,
				fading = view?.editFading?.text?.toString()?.toIntOrNull() ?: 0,
				min = 255 * (view?.seekMinimum?.progress ?: 0) / 100,
				max = 255 * (view?.seekMaximum?.progress ?: 100) / 100,
				timeout = view?.editTimeout?.text?.toString()?.toIntOrNull() ?: 1)
	}

	private fun Button.getColor() = view?.spinnerColor?.selectedItemPosition
			?.takeIf { it > 0 }
			?.let { Color.rgb(1, 2, it) }
			?: (this.background as? ColorDrawable)?.color ?: Color.BLACK

	private fun Int.getRainbow() = this.takeIf { Color.red(it) == 1 && Color.green(it) == 2 }
			?.let { Color.blue(it) }
			?: 0

	private fun RealmLightConfiguration.getRainbow() = if (color1.getRainbow() > 0
			&& color2.getRainbow() > 0
			&& color3.getRainbow() > 0
			&& color4.getRainbow() > 0
			&& color5.getRainbow() > 0
			&& color6.getRainbow() > 0)
		color1.getRainbow() else 0
}