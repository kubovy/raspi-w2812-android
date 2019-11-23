package com.poterion.monitor.android.serial

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.lang.ref.WeakReference

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class ConnectionTask(context: Context,
					 private val device: BluetoothDevice,
					 private val port: Int,
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
				val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType!!)
				val serialSocket = method.invoke(device, port) as BluetoothSocket
				serialSocket.connect()
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
}