package com.poterion.raspi.w2812.android.data

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
data class LightPattern(val id: String,
						val title: String,
						val hasColor1: Boolean = false,
						val hasColor2: Boolean = false,
						val hasColor3: Boolean = false,
						val hasColor4: Boolean = false,
						val hasColor5: Boolean = false,
						val hasColor6: Boolean = false,
						val hasWait: Boolean = false,
						val hasWidth: Boolean = false,
						val hasFading: Boolean = false,
						val fadingTitle: String = "Fading",
						val hasMin: Boolean = false,
						val hasMax: Boolean = false)