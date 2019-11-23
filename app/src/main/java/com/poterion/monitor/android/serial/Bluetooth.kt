package com.poterion.monitor.android.serial

import android.util.Log
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
object Bluetooth : MessageHandler {

	private val LOG_TAG = Bluetooth::class.simpleName
	val messages: PublishSubject<ByteArray> = PublishSubject.create<ByteArray>()
	val connected: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
	val sending: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)
	val available: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

	init {
		connected.subscribe { available.onNext(it && !(sending.value ?: false)) }
		sending.subscribe { available.onNext((connected.value ?: false) && !it) }
	}

	override fun onMessage(data: ByteArray) {
		Log.i(LOG_TAG, "Message: ${data.joinToString(" ") { "%02X".format(it) }}")
		messages.onNext(data)
	}
}