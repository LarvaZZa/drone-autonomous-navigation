# Drone Autonomous Navigation

## What is This?

This repository contains a copy of a [demo application developed by DJI](https://github.com/dji-sdk/Mobile-UXSDK-Android) with our own custom implementation of autonomous navigation for the DJI Mini 2 drone. The autonomous navigation feature lets the drone fly autonomously from its current location to a predefined new (target) location in the three-dimensional environment. The locations are defined by their latitude, longitude, and altitude.

## How does it work?

This autonomous navigation feature is not officially supported by DJI. For this reason, we developed a custom implementation that supports this feature by utilizing another feature which is supported by DJI. This feature is called Virtual Sticks. The common way to control a drone is via the remote controller which has physical sticks, however, there is a feature that allows to control the drone with virtual sticks that are on the phone's screen. The neat thing about these virtual sticks is that they send information to the drone on how the drone should behave/move by updating some global variables and having a separate thread sending those global variables directly to the drone. Those variables inform how the drone behaves in the three-dimensional environment, and the values are roll, pitch, yaw, and vertical throttle. With these values, we can move the drone however we like. **IMPORTANT:** To better understand what those values mean, please refer to [Figure 2 in this paper](https://essay.utwente.nl/95814/1/serksnas_BA_EEMCS.pdf), they are a way to control the drone's behavior in these directions. Roll and pitch controls the forward, backward, left and right movements. Yaw controls how the drone is rotated along the axes relative to North. Vertical throttle controls the altitude of the drone.

So, in order to have the autonomous navigation, we just have to:

- remove the virtual sticks from the phone's screen,
- have a button that turns on the virtual sticks feature (a.k.a. allowing to send movement data to the drone),
- have an algorithm that somehow calculates the values of those variables for the drone to reach a target location,
- and have a fail-safe feature of turning the virtual sticks off in case something goes wrong.

Of course, creating the algorithm was the tricky part. The algorithm works as follows:

- the current drone location is retrieved (latitude, longitude, altitude),
- the bearing angle between the current location and target location is calculated ([look at Figure 3](https://essay.utwente.nl/95814/1/serksnas_BA_EEMCS.pdf)) which allows to know the yaw angle the drone should turn to face towards the target location,
- the distance between the current location and target location is calculated ([look at Figure 3](https://essay.utwente.nl/95814/1/serksnas_BA_EEMCS.pdf)) which allows to know when the target location is reached,
- the altitude difference between the current location and target location is calculated which allows to know when the target location height is reached,
- and the virtual stick variables are updated depending on the information above, controlling the drone as follows:
  - the drone reaches the height of the target location and rotates its front towards the target location,
  - the drone moves forward until it reaches the target location,
  - the drone stops moving and turns off virtual sticks when it reaches the target location

You can find the autonomous navigation implementation in java/com/dji/ux/sample/CompleteWidgetActivity.java. There you will also find in-line code documentation to help you understand what the code does.

Information on the methods, functions, and everything else was gathered from [Official DJI Mobile SDK Documentation](https://developer.dji.com/api-reference/android-api/Components/SDKManager/DJISDKManager.html). If you want to develop your own custom applications, use the official documentation on the available methods. **NOTE** not all of the methods/classes are supported for starter drones like the DJI Mini 2, thus often a trial and error approach is used to test what works and what doesn't.

## How to use the application?

Make sure to create an API Key via the official [DJI Developer platform](https://developer.dji.com/), and paste the key in the AndroidManifest.xml file.

If you want to execute the autonomous navigation with the drone, you must:

- **_IMPORTANT:_** set the latitude, longitude and altitude of the target location you want the drone to visit before installing the application on the phone! You can find the target location variable in CompleteWidgetActivity.java line 100.
- Install the application on your Android phone;
- Connect the phone to the drone's remote controller;
- Turn on the drone and the remote controller;
- Launch the application;
- Click on "Complete Demo of Default UI Widgets" button;
- Click on "ENABLE VS" button to enable the Virtual Sticks that will control the drone. **Note:** You will NOT be able to control the drone with the remote controller unless you press the "STOP" button. Always stay alert if something goes wrong, you will **ONLY** be able to **TAKE CONTROL** of the drone after pressing the **"STOP" button**;
- Click on "START MISSION" button to start the drone's autonomous navigation towards the target location. **Note:** You will **NOT BE ABLE TO CONTROL** the drone with the remote controller unless you press the **"STOP" button**.

**_ATTENTION ONCE MORE_** - In case of emergencies (if the drone does not behave how you want it to), press the **"STOP" button** to **halt the drone's behavior** and **take over the controls** via the remote controller.
