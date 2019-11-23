package com.poterion.monitor.android.data

/**
 * @author Jan Kubovy <jan@kubovy.eu>
 */
data class LightConfig(var pattern: LightPattern = LightPattern.LIGHT_FULL,
					   var color1: LightColor = LightColor(),
					   var color2: LightColor = LightColor(),
					   var color3: LightColor = LightColor(),
					   var color4: LightColor = LightColor(),
					   var color5: LightColor = LightColor(),
					   var color6: LightColor = LightColor(),
					   var color7: LightColor = LightColor(),
					   var delay: Int = 50,
					   var width: Int = 3,
					   var fading: Int = 0,
					   var min: Int = 0,
					   var max: Int = 255,
					   var timeout: Int = 1)