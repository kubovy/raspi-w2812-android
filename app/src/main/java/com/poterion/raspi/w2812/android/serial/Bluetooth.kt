package com.poterion.raspi.w2812.android.serial

import android.util.Log
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
object Bluetooth : MessageHandler {

	private val LOG_TAG = Bluetooth::class.simpleName
	val messages: PublishSubject<String> = PublishSubject.create<String>()
	val connected: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
	val sending: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
	val available: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

	private var rest: String = ""

	init {
		connected.subscribe { available.onNext(it && !sending.value) }
		sending.subscribe { available.onNext(!it && connected.value) }
	}

	override fun read(bufferSize: Int, buffer: ByteArray): Int {
		Log.i(LOG_TAG, "Message: ${buffer.copyOfRange(0, bufferSize).toString(Charsets.UTF_8)}")
		val incoming = buffer.copyOfRange(0, bufferSize).toString(Charsets.UTF_8).split("[\n\r]".toRegex())
		incoming.forEachIndexed { index, line ->
			val result : String? = when {
				index == 0 -> "${rest}${line}".also { rest = "" }
				index != incoming.lastIndex -> line
				index == incoming.lastIndex -> {
					rest = line
					null
				}
				else -> null
			}
			result?.takeIf { it.isNotEmpty() }?.also { messages.onNext(it) }
		}
		return bufferSize
	}
}