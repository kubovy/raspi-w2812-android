package com.poterion.monitor.android.serial

/**
 * Reads from the serial buffer, processing any available messages.  Must return the number of bytes
 * consumer from the buffer
 *
 * @author jpetrocik
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
interface MessageHandler {
	fun onMessage(data: ByteArray)
}