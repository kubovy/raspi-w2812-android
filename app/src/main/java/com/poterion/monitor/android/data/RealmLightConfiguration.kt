package com.poterion.monitor.android.data

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import io.realm.annotations.Required

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
open class RealmLightConfiguration(
		@PrimaryKey var id: String? = null,
		@Required var set: String = "",
		@Required var order: Int? = null,
		@Required var pattern: String = LightPattern.LIGHT_FULL.name,
		var color1: Int = 0,
		var color2: Int = 0,
		var color3: Int = 0,
		var color4: Int = 0,
		var color5: Int = 0,
		var color6: Int = 0,
		var color7: Int = 0,
		var delay: Int = 1000,
		var width: Int = 5,
		var fading: Int = 0,
		var min: Int = 0,
		var max: Int = 100,
		var timeout: Int = 1) : RealmObject()