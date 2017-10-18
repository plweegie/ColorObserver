# ColorObserver

This application, based off of **Android Things** [doorbell](https://github.com/androidthings/doorbell) example, uses a Pico-i.MX7D board to take a picture of the surroundings every hour, analyzes the data using the [BoofCV](https://github.com/lessthanoptimal/BoofCV) computer vison library, and sends the [hue-saturation-value](https://en.wikipedia.org/wiki/HSL_and_HSV) readings to **Firebase Realtime Database** for further analysis.
At this stage, it mostly serves as a testing ground for a more powerful version using Raspberry Pi 3.

The next step: Cross-platform companion app written in [Flutter](https://flutter.io) that will read data from Firebase and report the dominant color back to the user.
