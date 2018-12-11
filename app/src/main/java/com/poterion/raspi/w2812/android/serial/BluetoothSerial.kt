package com.poterion.raspi.w2812.android.serial

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.util.*

/**
 * Provides a high level wrapper around all the lower level bluetooth operations. Handles connecting to,
 * reestablishing connections, and reading from the serialInputStream.
 *
 * @author jpetrocik
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class BluetoothSerial(private var context: Context,
					  private val bluetoothDevice: BluetoothDevice,
					  private var messageHandler: MessageHandler) {

	companion object {
		const val BLUETOOTH_CONNECTED = "bluetooth-connection-started"
		const val BLUETOOTH_DISCONNECTED = "bluetooth-connection-lost"
		const val BLUETOOTH_FAILED = "bluetooth-connection-failed"
		private val LOG_TAG = BluetoothSerial::class.simpleName
	}

	var connected = false
	private var serialOutboundSocket: BluetoothSocket? = null
	private var serialInputStream: InputStream? = null
	private var serialOutputStream: OutputStream? = null
	//private var serialReader: SerialReader? = null
	private var connectionTask: AsyncTask<Void, Void, BluetoothSocket>? = null

	private val bluetoothReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val action = intent.action
			val eventDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

			if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
				if (bluetoothDevice == eventDevice) {
					Log.i(LOG_TAG, "Received bluetooth disconnect notice")
					close()
					LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BLUETOOTH_DISCONNECTED))
					connect()
				}
			}
		}
	}

	fun onResume() {
		context.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED))
		if (!connected) connect() else LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BLUETOOTH_CONNECTED))
	}

	fun onPause() {
		context.unregisterReceiver(bluetoothReceiver)
	}

	private fun connect() {
		if (connected) {
			Log.e(LOG_TAG, "Connection request while already connected")
			return
		}

		if (connectionTask != null && connectionTask?.status == AsyncTask.Status.RUNNING) {
			Log.e(LOG_TAG, "Connection request while attempting connection")
			return
		}

		val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
		if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
			return
		}

		val pairedDevices = ArrayList(bluetoothAdapter.bondedDevices)
		if (pairedDevices.size > 0) {
			bluetoothAdapter.cancelDiscovery()

			/**
			 * AsyncTask to handle the establishing of a bluetooth connection
			 */
			connectionTask = ConnectionTask(context, bluetoothDevice, 7) { socket ->
				serialOutboundSocket = socket
				serialInputStream = socket.inputStream
				serialOutputStream = socket.outputStream
				serialInputStream//?.also { serialInputStream ->
				//serialReader = SerialReader(serialInputStream, true, messageHandler)
				//serialReader?.start()
				//}
				connected = true
				LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BLUETOOTH_CONNECTED))
			}
			connectionTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
		}
	}

	private fun write(buffer: ByteArray) {
		serialOutputStream
				?.takeIf { connected }
				?.also { it.write(buffer) }
				?: throw RuntimeException("Connection lost, reconnecting now.")
	}

	fun writeString(string: String) = write(string.also { Log.d(LOG_TAG, "Writing message: ${it}") }.toByteArray())

	fun close() {
		//onPause()
		connected = false
		//serialReader?.disconnect()

		try {
			serialInputStream?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed releasing input stream connection")
		}

		try {
			serialOutputStream?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed releasing output stream connection")
		}

		try {
			serialOutboundSocket?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed closing socket")
		}

		Log.i(LOG_TAG, "Released bluetooth connections")
	}
}