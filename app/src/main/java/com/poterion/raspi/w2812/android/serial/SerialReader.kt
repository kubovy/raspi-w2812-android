package com.poterion.raspi.w2812.android.serial

import android.util.Log
import java.io.InputStream

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class SerialReader(private val serialInputStream: InputStream,
				   private var connected: Boolean,
				   private val messageHandler: MessageHandler) : Thread() {
	companion object {
		private val LOG_TAG = SerialReader::class.simpleName
		private const val MAX_BYTES = 8192
	}

	private var buffer = ByteArray(MAX_BYTES)
	private var bufferSize = 0

	override fun run() {
		Log.i("serialReader", "Starting serial loop")
		while (!isInterrupted) {
			try {
				/* check for some bytes, or still bytes still left in buffer */
				if (available() > 0) {
					val newBytes = read(buffer, bufferSize, MAX_BYTES - bufferSize)
					if (newBytes > 0) bufferSize += newBytes
					Log.d(LOG_TAG, "read " + newBytes)
				}

				if (bufferSize > 0) {
					val read = messageHandler.read(bufferSize, buffer)
					// shift unread data to start of buffer
					if (read > 0) {
						var index = 0
						for (i in read until bufferSize) {
							buffer[index++] = buffer[i]
						}
						bufferSize = index
					}
				} else {
					try {
						Thread.sleep(10)
					} catch (ie: InterruptedException) {
						break
					}

				}
			} catch (e: Exception) {
				Log.e(LOG_TAG, "Error reading serial data", e)
			}
		}
		Log.i(LOG_TAG, "Shutting serial loop")
	}

	private fun available(): Int = serialInputStream
			.takeIf { connected }
			?.available()
			?: throw RuntimeException("Connection lost, reconnecting now.")

	private fun read(buffer: ByteArray, byteOffset: Int, byteCount: Int): Int = serialInputStream
			.takeIf { connected }
			?.read(buffer, byteOffset, byteCount)
			?: throw RuntimeException("Connection lost, reconnecting now.")

	fun disconnect() {
		connected = false
		interrupt()
		try {
			join(1000)
		} catch (e: InterruptedException) {
			Log.w(LOG_TAG, e.message, e)
		}
	}
}