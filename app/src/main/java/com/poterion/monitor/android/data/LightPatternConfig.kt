package com.poterion.monitor.android.data

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
data class LightPatternConfig(val title: String,
							  val pattern: LightPattern,
							  val hasColor1: Boolean = true,
							  val hasColor2: Boolean = true,
							  val hasColor3: Boolean = true,
							  val hasColor4: Boolean = true,
							  val hasColor5: Boolean = true,
							  val hasColor6: Boolean = true,
							  val hasColor7: Boolean = true,
							  val hasDelay: Boolean = true,
							  val hasWidth: Boolean = true,
							  val hasFading: Boolean = true,
							  val hasMin: Boolean = true,
							  val hasMax: Boolean = true)