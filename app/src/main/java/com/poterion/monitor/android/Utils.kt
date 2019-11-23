package com.poterion.monitor.android

import com.poterion.monitor.android.data.LightColor
import com.poterion.monitor.android.data.LightConfig
import com.poterion.monitor.android.data.RealmLightConfiguration
import com.poterion.monitor.android.data.LightPattern

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
fun RealmLightConfiguration.toLightConfiguration() = LightConfig(
		pattern = LightPattern.values().firstOrNull { it.name == pattern } ?: LightPattern.LIGHT_FULL,
		color1 = color1.toLightColor(),
		color2 = color2.toLightColor(),
		color3 = color3.toLightColor(),
		color4 = color4.toLightColor(),
		color5 = color5.toLightColor(),
		color6 = color6.toLightColor(),
		color7 = color7.toLightColor(),
		delay = delay,
		width = width,
		fading = fading,
		min = min,
		max = max,
		timeout = timeout)

fun Int.toLightColor(): LightColor {
	val r = this shr 16 and 0xff
	val g = this shr 8 and 0xff
	val b = this and 0xff
	return LightColor(r, g, b)
}