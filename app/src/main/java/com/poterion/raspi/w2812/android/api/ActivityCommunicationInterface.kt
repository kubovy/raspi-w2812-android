package com.poterion.raspi.w2812.android.api

import com.poterion.raspi.w2812.android.data.RealmLightConfiguration

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
interface ActivityCommunicationInterface {
	var set: String?
	var item: RealmLightConfiguration?
	fun editLightConfiguration(lightConfiguration: RealmLightConfiguration?)
	fun showLightConfigurationSet(lightConfigurationSet: String)
	fun sendLightConfigurations(lightConfigurations: List<RealmLightConfiguration>)
}