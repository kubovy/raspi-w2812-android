# Raspi W2812 Light App

A quite simple android app for controlling the [Raspi W2812 Light](https://blog.kubovy.eu/2018/02/11/status-light-with-raspberry-pi-zero-and-w2812-led-strip/) 

In the beginning it shows you the list of paired devices where you need to pick the one representing the 
[Raspi W2812 Light](https://blog.kubovy.eu/2018/02/11/status-light-with-raspberry-pi-zero-and-w2812-led-strip/). Note 
that you need to pair your phone with the light before you can use this app.

The app stores a list of light configurations as described in [Raspi W2812 Light](https://blog.kubovy.eu/2018/02/11/status-light-with-raspberry-pi-zero-and-w2812-led-strip/)
post additionally with a “set name” as free text. The light configurations are grouped by those sets. We can run a whole
set or just one configuration.

<img src="/devel/images/screenshots/Screenshot_20180303-102931.png" width="280">

There are three tabs on the bottom. The first is showing the list of “sets”. In this tab if we press on the “send” 
button next to a set the whole set will be sent to the light. If we press somewhere else on the set item we will be 
navigated to the “list” tab where the list of the light configurations will be shown.

In the “list” tab pressing on the floating action button on the bottom right will send the whole set to the light. 
Pressing on the “send” button next to a light configuration will send only this one configuration to the light. 
Pressing somewhere else on the light configuration will navigate us to the “light” tab where the light configuration 
details will be shown.

<img src="/devel/images/screenshots/Screenshot_20180303-102925.png" width="280">

In the “light” tab we can change the colors and other parameters of the light configuration and also the set on the top.
By pressing the floating action button on the bottom right the configuration will be sent to the light. If we want to 
save our configuration for later we need to press the “save” button in the action menu.

You can find the app in the store:

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="180">](https://play.google.com/store/apps/details?id=com.poterion.raspi.w2812.android)
