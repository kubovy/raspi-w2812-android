package com.poterion.raspi.w2812.android.serial

/**
 * Reads from the serial buffer, processing any available messages.  Must return the number of bytes
 * consumer from the buffer
 *
 * @author jpetrocik
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
interface MessageHandler {
	fun read(bufferSize: Int, buffer: ByteArray): Int
}