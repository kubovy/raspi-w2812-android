package com.poterion.monitor.android.data

/**
 * WS281x light patterns.
 *
 * @author Jan Kubovy <jan@kubovy.eu>
 */
enum class LightPattern(val code: Int,
							  val delay: Int?,
							  val width: Int?,
							  val fading: Int?,
							  val min: Int?,
							  val max: Int?,
							  val timeout: Int?) {
	LIGHT_OFF(0x00, null, null, null, null, null, null),
	LIGHT_FULL(0x01, 1000, null, 0, null, 255, 50),
	LIGHT_BLINK(0x02, 500, null, 0, 0, 255, 3),
	LIGHT_FADE_IN(0x03, 200, null, 0, 0, 255, 3),
	LIGHT_FADE_OUT(0x04, 200, null, 0, 0, 255, 3),
	LIGHT_FADE_INOUT(0x05, 100, null, 0, 0, 255, 3),
	LIGHT_FADE_TOGGLE(0x06, 100, null, 0, 0, 255, 3),
	LIGHT_ROTATION(0x07, 500, 10, 24, 0, 255, 3),
	LIGHT_WIPE(0x08, 500, null, 0, 0, 255, 1),
	LIGHT_LIGHTHOUSE(0x09, 750, 5, 32, 0, 255, 3),
	LIGHT_CHAISE(0x0A, 300, 10, 24, 0, 255, 3),
	LIGHT_THEATER(0x0B, 1000, 3, 0, 128, 255, 3),
}