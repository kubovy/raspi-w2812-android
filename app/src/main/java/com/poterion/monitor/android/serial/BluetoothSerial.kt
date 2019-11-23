package com.poterion.monitor.android.serial

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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors

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
		private set
	private var socket: BluetoothSocket? = null
	private var inputStream: InputStream? = null
	private var outputStream: OutputStream? = null
	private var connectionTask: AsyncTask<Void, Void, BluetoothSocket>? = null

	private val messageQueue: ConcurrentLinkedQueue<Pair<ByteArray, Long?>> = ConcurrentLinkedQueue()
	private val checksumQueue: ConcurrentLinkedQueue<Byte> = ConcurrentLinkedQueue()
	private var lastChecksum: Int? = null

	private val inboundExecutor = Executors.newSingleThreadExecutor()
	private val outboundExecutor = Executors.newSingleThreadExecutor()

	private var inboundThread: Thread? = null
	private var outboundThread: Thread? = null

	private val bluetoothReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context, intent: Intent) {
			val action = intent.action
			val eventDevice = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

			if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
				if (bluetoothDevice == eventDevice) {
					Log.i(LOG_TAG, "Received bluetooth disconnect notice")
					disconnect()
					LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BLUETOOTH_DISCONNECTED))
					connect()
				}
			}
		}
	}

	private val inboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted() && connected) try {
				val buffer = ByteArray(256)
				val length = inputStream?.read(buffer) ?: 0
				val message = buffer.copyOfRange(0, length)

				if (length > 0) {
					val chksumReceived = message[0].toInt() and 0xFF
					val chksumCalculated = message.toList().subList(1, message.size).toByteArray().calculateChecksum()
					Log.d(LOG_TAG, "Inbound RAW [${"0x%02X".format(chksumReceived)}/${"0x%02X".format(chksumCalculated)}]:" +
							" ${message.joinToString(" ") { "0x%02X".format(it) }}")

					if (chksumCalculated == chksumReceived) {
						val messageKind = message[1]
								.let { byte -> MessageKind.values().find { it.code.toByte() == byte } }
								?: MessageKind.UNKNOWN

						if (messageKind != MessageKind.CRC) checksumQueue.add(chksumCalculated.toByte())

						when (messageKind) {
							MessageKind.CRC -> {
								lastChecksum = (message[2].toInt() and 0xFF)
								Log.d(LOG_TAG, "Inbound: CRC: ${"0x%02X".format(lastChecksum)}")
							}
							else -> messageHandler.onMessage(message)
						}
					}
				}
			} catch (e: Exception) {
				Log.e(LOG_TAG, e.message, e)
				if (connected) connect() else disconnect()
			}
		} catch (e: Exception) {
			Log.w(LOG_TAG, e.message)
			if (connected) connect() else disconnect()
		}
		Log.d(LOG_TAG, "Inbound thread exited")
	}

	private val outboundRunnable: () -> Unit = {
		try {
			while (!Thread.interrupted() && connected) try {
				if (checksumQueue.isNotEmpty()) {
					val chksum = checksumQueue.poll()
					var data = listOf(MessageKind.CRC.code.toByte(), chksum).toByteArray()
					data = listOf(data.calculateChecksum().toByte(), MessageKind.CRC.code.toByte(), chksum).toByteArray()
					sendMessage(data)
					Log.d(LOG_TAG, "Outbound CRC: ${"0x%02X".format(chksum)} (${checksumQueue.size})")
				} else if (messageQueue.isNotEmpty()) {
					val (message, delay) = messageQueue.peek()
					val kind = MessageKind.values().find { it.code.toByte() == message[0] }
					val checksum = message.calculateChecksum()
					val data = listOf(checksum.toByte(), *message.toTypedArray()).toByteArray()
					lastChecksum = null
					Log.d(LOG_TAG, "Outbound [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]:" +
							" ${data.joinToString(" ") { "0x%02X".format(it) }}" +
							" (try)")
					sendMessage(data)

					var timeout = delay ?: 500 // default delay in ms
					while (lastChecksum != checksum && timeout > 0) {
						Thread.sleep(1)
						timeout--
					}

					val correctlyReceived = checksum == lastChecksum
					if (correctlyReceived) messageQueue.poll()
					when (kind) {
						MessageKind.CRC -> {
						}
						MessageKind.IDD -> {
							//if (correctlyReceived) {
							//	Log.d(LOG_TAG, "Outbound [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]:" +
							//			" ${data.joinToString(" ") { "0x%02X".format(it) }}" +
							//			" (remaining: ${messageQueue.size})")
							//	iddCounter = -5
							//} else {
							//	Log.d(LOG_TAG, "${iddCounter + 1}. ping not returned")
							//	iddCounter++
							//}
							//if (iddCounter > 4) {
							//	if (state == State.CONNECTED) reconnect() else disconnect()
							//}
						}
						else -> {
							Log.d(LOG_TAG, "Outbound [${"0x%02X".format(lastChecksum)}/${"0x%02X".format(checksum)}]:" +
									" ${data.joinToString(" ") { "0x%02X".format(it) }}" +
									" (remaining: ${messageQueue.size})")
							//if (correctlyReceived) listeners
							//		.forEach { Platform.runLater { it.onMessageSent(channel, data.toIntArray(), messageQueue.size) } }
						}
					}
					if (correctlyReceived) lastChecksum = null
				}// else {
				//	if (iddCounter < 0) {
				//		Thread.sleep(100L)
				//		iddCounter++
				//	} else if (iddCounter == 0) {
				//
				//		val message = if (iddState < 2) arrayOf(MessageKind.IDD.code, Random.nextBits(4), iddState)
				//		else if (IDD_PING) arrayOf(MessageKind.IDD.code, Random.nextBits(4))
				//		else null
				//
				//		if (message != null) messageQueue.add(message.map { it.toByte() }.toByteArray() to 500)
				//	}
				//}
			} catch (e: Exception) {
				Log.e(LOG_TAG, e.message, e)
				if (connected) connect() else disconnect()
			}
		} catch (e: Exception) {
			Log.w(LOG_TAG, e.message)
			if (connected) connect() else disconnect()
		}
		Log.d(LOG_TAG, "Outbound thread exited")
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
			Log.e(LOG_TAG, "Connection request while bluetooth disabled")
			return
		}

		val pairedDevices = ArrayList(bluetoothAdapter.bondedDevices)
		if (pairedDevices.size > 0) {
			bluetoothAdapter.cancelDiscovery()

			cleanupConnection()
			inboundThread?.takeIf { !it.isInterrupted }?.interrupt()
			outboundThread?.takeIf { !it.isInterrupted }?.interrupt()

			/**
			 * AsyncTask to handle the establishing of a bluetooth connection
			 */
			connectionTask = ConnectionTask(context, bluetoothDevice, 6) { btSocket -> // TODO port
				socket = btSocket
				inputStream = btSocket.inputStream
				outputStream = btSocket.outputStream

				connected = true

				inboundThread?.takeIf { !it.isInterrupted }?.interrupt()
				outboundThread?.takeIf { !it.isInterrupted }?.interrupt()

				inboundThread = Thread(inboundRunnable)
				inboundThread?.name = "inbound"
				outboundThread = Thread(outboundRunnable)
				outboundThread?.name = "outbound"

				inboundExecutor.execute(inboundThread!!)
				outboundExecutor.execute(outboundThread!!)

				LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(BLUETOOTH_CONNECTED))
			}
			connectionTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
		}
	}

	/** Disconnects from a device. */
	fun disconnect() {
		Log.d(LOG_TAG, "Disconnecting ...")
		messageQueue.clear()
		checksumQueue.clear()

		try {
			if (inboundThread?.isAlive == true) inboundThread?.interrupt()
			if (outboundThread?.isAlive == true) outboundThread?.interrupt()
			cleanupConnection()
		} catch (e: IOException) {
			Log.e(LOG_TAG, e.message, e)
		} finally {
			connected = false
		}
	}

	private fun sendMessage(buffer: ByteArray) {
		outputStream
				?.takeIf { connected }
				?.also { it.write(buffer) }
				?.also { it.flush() }
				?: throw RuntimeException("Connection lost, reconnecting now.")
	}

	/**
	 * Queues a new message to be sent to target device.
	 *
	 * @param kind Message kind.
	 * @param message Message.
	 */
	fun send(message: ByteArray = byteArrayOf()) = message.let { data ->
		ByteArray(data.size) { i -> data[i] }.let { messageQueue.offer(it to null) }
	}

	fun cleanupConnection() {
		try {
			inputStream?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed releasing input stream connection", e)
		}

		try {
			outputStream?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed releasing output stream connection", e)
		}

		try {
			socket?.close()
		} catch (e: Exception) {
			Log.e(LOG_TAG, "Failed closing socket", e)
		}

		Log.i(LOG_TAG, "Released bluetooth connections")
	}

	private fun ByteArray.calculateChecksum() = (map { it.toInt() }.takeIf { it.isNotEmpty() }?.reduce { acc, i -> acc + i }
			?: 0) and 0xFF
}