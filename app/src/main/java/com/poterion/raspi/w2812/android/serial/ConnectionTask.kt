package com.poterion.raspi.w2812.android.serial

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.lang.ref.WeakReference
import java.util.*

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class ConnectionTask(context: Context,
					 private val device: BluetoothDevice,
					 private val socketProvider: (BluetoothSocket) -> Unit) :
		AsyncTask<Void, Void, BluetoothSocket>() {

	companion object {
		private val LOG_TAG = ConnectionTask::class.simpleName
		private const val MAX_ATTEMPTS = 30
	}

	private var attemptCounter = 0
	private val context = WeakReference(context)

	override fun doInBackground(vararg params: Void): BluetoothSocket? {
		while (!isCancelled) {
			Log.i(LOG_TAG, attemptCounter.toString() + ": Attempting connection to " + device.name)

			try {
				val serialSocket = try {
					// Standard SerialPortService ID
					val uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
					device.createRfcommSocketToServiceRecord(uuid)
				} catch (ce: Exception) {
					connectViaReflection(device)
				}

				serialSocket?.connect()
				Log.i(LOG_TAG, "Connected to " + device.name)
				return serialSocket
			} catch (e: Exception) {
				Log.i(LOG_TAG, e.message, e)
			}

			try {
				attemptCounter++
				if (attemptCounter > MAX_ATTEMPTS)
					this.cancel(false)
				else
					Thread.sleep(1000)
			} catch (e: InterruptedException) {
				break
			}

		}

		Log.i(LOG_TAG, "Stopping connection attempts")
		context.get()?.also { context ->
			LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BluetoothSerial.BLUETOOTH_FAILED))
		}
		return null
	}

	override fun onPostExecute(result: BluetoothSocket) {
		super.onPostExecute(result)
		socketProvider.invoke(result)
		context.get()?.also { context ->
			LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BluetoothSerial.BLUETOOTH_CONNECTED))
		}
	}

	// see: http://stackoverflow.com/questions/3397071/service-discovery-failed-exception-using-bluetooth-on-android
	private fun connectViaReflection(device: BluetoothDevice): BluetoothSocket {
		val m = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType!!)
		return m.invoke(device, 1) as BluetoothSocket
	}
}