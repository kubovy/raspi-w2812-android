package com.poterion.monitor.android

import android.os.AsyncTask
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.monitor.android.data.LightConfig
import com.poterion.monitor.android.serial.Bluetooth
import com.poterion.monitor.android.serial.BluetoothSerial
import com.poterion.monitor.android.serial.MessageKind

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightUpdateTask(private val bluetoothSerial: BluetoothSerial, private val onFinished: (Boolean) -> Unit) :
		AsyncTask<LightConfig, Void, Boolean>() {
	companion object {
		private val LOG_TAG = LightUpdateTask::class.simpleName
	}

	private val objectMapper = ObjectMapper()
	//private var messageSubscription: Disposable? = null
	//private var checksum: Long? = null

	override fun doInBackground(vararg params: LightConfig?): Boolean {
		while (!bluetoothSerial.connected) try {
			Log.i(LOG_TAG, "Waiting for bluetooth connection...")
			Thread.sleep(1000)
		} catch (e: InterruptedException) {
			Log.w(LOG_TAG, e.message, e)
		}

		val lightConfiguration = params.filterNotNull()

		return lightConfiguration.mapIndexed { index, config ->

			/* |===================================================================|
			 * | (C) Configuration for concrete strip NUM                          |
			 * |-------------------------------------------------------------------|
			 * | 0 |  1 | 2 |   3   | 4  ... 24 |25 26|  27 |  28  | 29| 30|   31  |
			 * |CRC|KIND|NUM|PATTERN|RGB0...RGB6|DELAY|WIDTH|FADING|MIN|MAX|TIMEOUT|
			 * |===================================================================| */
			sendMessage(listOf(
					MessageKind.WS281xLIGHT.code,
					0, // Num
					config.pattern.code + (if (index == 0) 0x80 else 0x00),
					config.color1.red, config.color1.green, config.color1.blue,
					config.color2.red, config.color2.green, config.color2.blue,
					config.color3.red, config.color3.green, config.color3.blue,
					config.color4.red, config.color4.green, config.color4.blue,
					config.color5.red, config.color5.green, config.color5.blue,
					config.color6.red, config.color6.green, config.color6.blue,
					config.color7.red, config.color7.green, config.color7.blue,
					config.delay / 256, config.delay % 256,
					config.width,
					config.fading,
					config.min,
					config.max,
					config.timeout)

					.map { it.toByte() }
					.toByteArray())

		}.reduce { acc, b -> acc && b }
	}

	override fun onPreExecute() {
		super.onPreExecute()
		Bluetooth.sending.onNext(true)
	}

	override fun onPostExecute(result: Boolean?) {
		super.onPostExecute(result)
		Bluetooth.sending.onNext(false)
		onFinished.invoke(result ?: false)
	}

	override fun onCancelled() {
		super.onCancelled()
		Bluetooth.sending.onNext(false)
		onFinished.invoke(false)
	}

	private fun sendMessage(message: ByteArray): Boolean {
		var success = true
		try {
			success = success && bluetoothSerial.send(message)
		} catch (e: Exception) {
			Log.e(LOG_TAG, e.message, e)
		}
		try {
			Thread.sleep(2_000L)
		} catch (e: Exception) {
			Log.e(LOG_TAG, e.message, e)
		}
		return success
	}
}