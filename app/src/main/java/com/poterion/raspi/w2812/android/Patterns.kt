package com.poterion.raspi.w2812.android

import com.poterion.raspi.w2812.android.data.LightPattern

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
val PATTERNS = listOf(
		LightPattern("light", "Light", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
		LightPattern("blink", "Blink", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
		LightPattern("rotation", "Rotation", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
		LightPattern("wipe", "Wipe", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasFading = true),
		//LightPattern("spin" , "Spin", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true),
		LightPattern("chaise", "Chaise", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
		LightPattern("lighthouse", "Lighthouse", hasColor1 = true, hasColor2 = true, hasColor3 = true, hasColor4 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasWidth = true, hasFading = true),
		LightPattern("fade", "Fade", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasMin = true, hasMax = true),
		LightPattern("fadeToggle", "Fade Toggle", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasMin = true, hasMax = true),
		LightPattern("theater", "Theater", hasColor1 = true, hasColor2 = true, hasColor5 = true, hasColor6 = true, hasWait = true, hasFading = true, fadingTitle = "Iterations"),
		LightPattern("rainbow", "Rainbow", hasWait = true, hasFading = true, fadingTitle = "Iterations"),
		LightPattern("rainbowCycle", "Rainbow Cycle", hasWait = true, hasFading = true, fadingTitle = "Iterations"),
		LightPattern("wait", "Wait", hasWait = true),
		LightPattern("clear", "Clear"))