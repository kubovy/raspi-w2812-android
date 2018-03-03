package com.poterion.raspi.w2812.android

import android.os.AsyncTask
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.poterion.raspi.w2812.android.data.LightConfig
import com.poterion.raspi.w2812.android.serial.BluetoothSerial
import com.poterion.raspi.w2812.android.serial.Bluetooth
import com.poterion.raspi.w2812.android.serial.ACK
import com.poterion.raspi.w2812.android.serial.ETX
import com.poterion.raspi.w2812.android.serial.STX
import io.reactivex.disposables.Disposable
import java.util.zip.CRC32

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class LightUpdateTask(private val bluetoothSerial: BluetoothSerial, private val onFinished: (Boolean) -> Unit) :
		AsyncTask<LightConfig, Void, Boolean>() {
	companion object {
		private val LOG_TAG = LightUpdateTask::class.simpleName
	}

	private val objectMapper = ObjectMapper()
	private var messageSubscription: Disposable? = null
	private var checksum: Long? = null

	override fun doInBackground(vararg params: LightConfig?): Boolean {
		while (!bluetoothSerial.connected) try {
			Log.i(LOG_TAG, "Waiting for bluetooth connection...")
			Thread.sleep(1000)
		} catch (e: InterruptedException) {
			Log.w(LOG_TAG, e.message, e)
		}

		val lightConfiguration = params.filterNotNull()
		return lightConfiguration
				.map { objectMapper.writeValueAsString(it) }
				.takeIf { it.isNotEmpty() }
				?.joinToString(",", "[", "]")
				?.let { sendMessage(it) } ?: false
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

	private fun sendMessage(message: String): Boolean {
		var iterations = 0
		var success: Boolean
		do {
			Log.d(LOG_TAG, "Sending tryout ${iterations + 1}")
			val checksum = CRC32().apply {
				update(message.replace("[\\n\\r]".toRegex(), "").toByteArray())
			}.value
			setModeWrite()
			try {
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
				bluetoothSerial.writeString("${STX}\n\r")
				bluetoothSerial.writeString("${message}\n\r")
				bluetoothSerial.writeString("${ETX}\n\r")
			} catch (e: Exception) {
				Log.e(LOG_TAG, e.message, e)
			}

			setModeRead()
			val sent = System.currentTimeMillis()
			while (this.checksum == null && System.currentTimeMillis() - sent < 5_000L) {
				Thread.sleep(500L)
			}
			setModeWrite()

			Log.i(LOG_TAG, "Iteration ${iterations}: Received: ${this.checksum}, Calculated: ${checksum}")
			success = checksum == this.checksum
			this.checksum = null
			iterations++
		} while (!success && iterations < 5)
		setModeWrite()
		if (success) bluetoothSerial.writeString("${ACK}\n\r${ACK}\n\r${ACK}\n\r")
		Log.i(LOG_TAG, "Sending ${if (success) "SUCCESSFUL" else "FAILED"} after ${iterations} iterations")
		return success
	}

	private fun setModeWrite() {
		messageSubscription?.dispose()
	}

	private fun setModeRead() {
		messageSubscription = Bluetooth.messages.subscribe { message ->
			if (message.startsWith("${ACK}:")) {
				checksum = message.substring(ACK.length + 1).toLongOrNull()
				Log.d(LOG_TAG, "Checksum: \"${checksum}\"")
			}
		}
	}
}