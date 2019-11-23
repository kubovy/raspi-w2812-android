package com.poterion.monitor.android

import com.poterion.monitor.android.data.LightPatternConfig
import com.poterion.monitor.android.data.LightPattern

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
val PATTERNS = listOf(
		LightPatternConfig("Light", LightPattern.LIGHT_FULL, hasWidth = false, hasFading = false, hasMin = false),
		LightPatternConfig("Blink", LightPattern.LIGHT_BLINK, hasWidth = false, hasFading = false),
		LightPatternConfig("Fade-in", LightPattern.LIGHT_FADE_IN, hasWidth = false, hasFading = false),
		LightPatternConfig("Fade-out", LightPattern.LIGHT_FADE_OUT, hasWidth = false, hasFading = false),
		LightPatternConfig("Fade-in/out", LightPattern.LIGHT_FADE_INOUT, hasWidth = false, hasFading = false),
		LightPatternConfig("Fade Toggle", LightPattern.LIGHT_FADE_TOGGLE, hasWidth = false, hasFading = false),
		LightPatternConfig("Rotation", LightPattern.LIGHT_ROTATION),
		LightPatternConfig("Wipe", LightPattern.LIGHT_WIPE),
		LightPatternConfig("Lighthouse", LightPattern.LIGHT_LIGHTHOUSE),
		LightPatternConfig("Chaise", LightPattern.LIGHT_CHAISE),
		LightPatternConfig("Theater", LightPattern.LIGHT_THEATER))
		//LightPatternConfig("Spin", LightPattern.LIGHT_SPIN, hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
		//LightPatternConfig("Rainbow", LightPattern.LIGHT_, hasWait = true, hasFading = true, fadingTitle = "Iterations"),
		//LightPatternConfig("Rainbow Cycle", LightPattern.LIGHT_FULL, hasWait = true, hasFading = true, fadingTitle = "Iterations"),
		//LightPatternConfig("Wait", LightPattern.LIGHT_FULL, hasWait = true),
		//LightPatternConfig("Clear", LightPattern.LIGHT_FULL))
