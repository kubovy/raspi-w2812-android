package com.poterion.raspi.w2812.android.adapters

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.poterion.raspi.w2812.android.fragments.LightConfigurationFormFragment
import com.poterion.raspi.w2812.android.fragments.LightConfigurationListFragment
import com.poterion.raspi.w2812.android.fragments.LightConfigurationSetListFragment

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 *
 * @author Jan Kubovy &lt;jan@kubovy.eu&gt;
 */
class SectionsPagerAdapter(fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager) {
	private val pages = listOf<Fragment>(
			LightConfigurationSetListFragment(),
			LightConfigurationListFragment(),
			LightConfigurationFormFragment())

	override fun getItem(position: Int): Fragment = pages[position]

	override fun getCount(): Int = pages.size
}