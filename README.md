# Live Mission Recording - Autonomous Navigation Requirement 1

## What is This?

This repository contains a base implementation of the UX SDK [by DJI](https://github.com/dji-sdk/Mobile-UXSDK-Android), and a custom implementation of autonomous navigation for the DJI Mini 2 drone. The drone:
- Lifts up to the target location's height;
- Turns towards the target location;
- Moves forwards till the target location is reached;

## How to use the application?

Make sure to create an API Key via the official [DJI Developer platform](https://developer.dji.com/), and paste the key in the AndroidManifest.xml file.

If you want to execute the autonomous behaviour on the drone, you must:
- ***IMPORTANT:*** set the latitude, longitude and altitude of the target location you want the drone to visit before installing the application on the phone!
- Install the application on your Android phone;
- Connect the phone to the drone remote controller;
- Turn on the drone and the remote cotnroller;
- Launch the applicaiton;
- Click on "Complete Demo of Default UI Widgets" button;
- Click on "ENABLE VS" button to enable the Virtual Sticks that will control teh drone. **Note:** You will NOT be able to control the drone with the remote controller unless you press the "STOP" button.
- Click on "START MISSION" button to start the drone's autonomus behaviour towards the target location. **Note:** You will NOT be able to control the drone with the remote controller unless you press the "STOP" button.

***ATTENTION*** - In case of emergencies (if the drone does not behave how you want it to), press the "STOP" button to halt the drone's behaviour and take over the controls via the remote controller. 