package com.poterion.monitor.android

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.poterion.monitor.android.adapters.SectionsPagerAdapter
import com.poterion.monitor.android.api.ActivityCommunicationInterface
import com.poterion.monitor.android.api.Page
import com.poterion.monitor.android.data.RealmLightConfiguration
import com.poterion.monitor.android.serial.Bluetooth
import com.poterion.monitor.android.serial.BluetoothSerial
import io.reactivex.disposables.Disposable
import io.realm.Realm
import io.realm.RealmConfiguration
import kotlinx.android.synthetic.main.activity_main.*
import android.view.KeyEvent


/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class MainActivity : AppCompatActivity(), ActivityCommunicationInterface {
	companion object {
		val LOG_TAG = MainActivity::class.simpleName
		const val REQUEST_ENABLE_BT = 1000
		const val NO_DEVICE = "#######"
	}

	override var set: String? = null
	override var item: RealmLightConfiguration? = null

	private var deviceName: String = NO_DEVICE
	private var availableSubscription: Disposable? = null
	private val title: String
		get() = deviceName.takeIf { it != NO_DEVICE } ?: getString(R.string.app_name)
	private var backPressed = 0;

	private var bluetoothAdapter: BluetoothAdapter? = null
	private var bluetoothSerial: BluetoothSerial? = null
	private val bluetoothConnectionBroadcastReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			Bluetooth.connected.onNext(intent?.action == BluetoothSerial.BLUETOOTH_CONNECTED)
			when (intent?.action) {
				BluetoothSerial.BLUETOOTH_CONNECTED -> toolbar.title = getString(R.string.status_connected, title)
				BluetoothSerial.BLUETOOTH_DISCONNECTED -> toolbar.title = getString(R.string.status_disconnected, title)
				BluetoothSerial.BLUETOOTH_FAILED -> toolbar.title = getString(R.string.status_failed, title)
				else -> toolbar.title = title
			}
		}
	}

	private var sectionsPagerAdapter: SectionsPagerAdapter? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Realm.init(this)
		Realm.setDefaultConfiguration(RealmConfiguration.Builder()
				.name("db.realm")
				.schemaVersion(1)
				//.migration(MyMigration())
				.build())
		setContentView(R.layout.activity_main)

		setSupportActionBar(toolbar)
		progressBar.visibility = View.GONE

		tabs.addTab(tabs.newTab().setText(getString(R.string.tab_sets)))
		tabs.addTab(tabs.newTab().setText(getString(R.string.tab_list)))
		tabs.addTab(tabs.newTab().setText(getString(R.string.tab_form)))
		tabs.tabGravity = TabLayout.GRAVITY_FILL
		tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
			override fun onTabSelected(tab: TabLayout.Tab?) {
				container.currentItem = tab?.position ?: 0
				backPressed = 0
			}

			override fun onTabReselected(tab: TabLayout.Tab?) {
			}

			override fun onTabUnselected(tab: TabLayout.Tab?) {
			}
		})

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
		sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

		// Set up the ViewPager with the sections adapter.
		container.adapter = sectionsPagerAdapter
		container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
		container.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
			override fun onPageSelected(position: Int) {
				(sectionsPagerAdapter?.getItem(position) as? Page)?.onShow()
				invalidateOptionsMenu()
			}

			override fun onPageScrollStateChanged(state: Int) {
			}

			override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
			}
		})

		LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothConnectionBroadcastReceiver, IntentFilter().apply {
			addAction(BluetoothSerial.BLUETOOTH_CONNECTED)
			addAction(BluetoothSerial.BLUETOOTH_DISCONNECTED)
			addAction(BluetoothSerial.BLUETOOTH_FAILED)
		})

		availableSubscription = Bluetooth.available.subscribe { progressBar.visibility = if (it) View.GONE else View.VISIBLE }
		setup()
	}

	override fun onResume() {
		super.onResume()
		bluetoothSerial?.onResume()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu_main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
		R.id.action_new -> {
			editLightConfiguration(null)
			true
		}
		else -> super.onOptionsItemSelected(item)
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean = when (keyCode) {
		KeyEvent.KEYCODE_BACK -> {
			if (container.currentItem > 0) {
				goto(container.currentItem - 1)
				backPressed = 0
				true
			} else if (backPressed == 0) {
				Toast.makeText(this, R.string.back_to_exit, Toast.LENGTH_LONG).show()
				backPressed++
				true
			} else super.onKeyDown(keyCode, event)
		}
		else -> super.onKeyDown(keyCode, event)
	}


	override fun onPause() {
		super.onPause()
		bluetoothSerial?.onPause()
	}

	override fun onDestroy() {
		availableSubscription?.dispose()
		bluetoothSerial?.disconnect()
		LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothConnectionBroadcastReceiver)
		super.onDestroy()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		super.onActivityResult(requestCode, resultCode, data)
		if (requestCode == REQUEST_ENABLE_BT) {
			setup()
		}
	}

	override fun showLightConfigurationSet(lightConfigurationSet: String) {
		set = lightConfigurationSet
		goto(1)
	}

	override fun editLightConfiguration(lightConfiguration: RealmLightConfiguration?) {
		item = lightConfiguration
		goto(2)
	}

	override fun sendLightConfigurations(lightConfigurations: List<RealmLightConfiguration>) {
		bluetoothSerial?.also { bt ->
			LightUpdateTask(bt) {success ->
				if (!success) Toast.makeText(this, getString(R.string.error_changing_light_failed), Toast.LENGTH_LONG).show()
			}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					*lightConfigurations.map { it.toLightConfiguration() }.toTypedArray())
		}
	}

	override fun goto(position: Int) = if (container.currentItem != position) {
		container.currentItem = position
	} else {
		(sectionsPagerAdapter?.getItem(position) as? Page)?.onShow()
	}

	private fun setup() {
		val adapter = BluetoothAdapter.getDefaultAdapter()
				?.also { bluetoothAdapter = it }
		if (adapter == null) {
			Log.e(LOG_TAG, "Bluetooth is not supported by this device")
			finish()
		} else if (!adapter.isEnabled) {
			val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
		} else {
			if (deviceName == NO_DEVICE) {
				AlertDialog.Builder(this).apply {
					setTitle(getString(R.string.select_device))
					setCancelable(false)

					val arrayAdapter = ArrayAdapter<String>(this@MainActivity, android.R.layout.select_dialog_singlechoice)
					BluetoothAdapter.getDefaultAdapter()
							?.takeIf { it.isEnabled }
							?.bondedDevices
							?.sortedBy { it.name }?.forEach { arrayAdapter.add(it.name) }

					setAdapter(arrayAdapter) { _, which ->
						deviceName = arrayAdapter.getItem(which) ?: "Unknown Device"
						initialize()
					}
				}.show()
			} else {
				initialize()
			}
		}
	}

	private fun initialize() {
		if (bluetoothSerial == null || bluetoothSerial?.connected == false) {
			bluetoothAdapter
					?.bondedDevices
					?.firstOrNull { it.name.toUpperCase().startsWith(deviceName.toUpperCase()) }
					?.also {
						bluetoothSerial = BluetoothSerial(this@MainActivity, it, Bluetooth)
						bluetoothSerial?.onResume()
					}
		}
	}
}
