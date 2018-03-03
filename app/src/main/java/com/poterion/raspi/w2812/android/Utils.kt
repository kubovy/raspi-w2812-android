package com.poterion.raspi.w2812.android

import com.poterion.raspi.w2812.android.data.LightColor
import com.poterion.raspi.w2812.android.data.LightConfig
import com.poterion.raspi.w2812.android.data.RealmLightConfiguration

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
fun RealmLightConfiguration.toLightConfiguration() = LightConfig(
		pattern = pattern,
		color1 = color1.toLightColor(),
		color2 = color2.toLightColor(),
		color3 = color3.toLightColor(),
		color4 = color4.toLightColor(),
		color5 = color5.toLightColor(),
		color6 = color6.toLightColor(),
		wait = wait,
		width = width,
		fading = fading,
		min = min,
		max = max)

fun Int.toLightColor(): LightColor {
	val r = this shr 16 and 0xff
	val g = this shr 8 and 0xff
	val b = this and 0xff
	return LightColor(r, g, b)
}