package com.poterion.monitor.android.api

import com.poterion.monitor.android.data.RealmLightConfiguration

/**
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
interface ActivityCommunicationInterface {
	var set: String?
	var item: RealmLightConfiguration?
	fun editLightConfiguration(lightConfiguration: RealmLightConfiguration?)
	fun showLightConfigurationSet(lightConfigurationSet: String)
	fun sendLightConfigurations(lightConfigurations: List<RealmLightConfiguration>)
	fun goto(number: Int): Unit?
}