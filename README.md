# ColorObserver

This application, based off of **Android Things** [doorbell](https://github.com/androidthings/doorbell) example, uses a Pico-i.MX7D board to take a picture of the surroundings every hour, analyzes the data using the [BoofCV](https://github.com/lessthanoptimal/BoofCV) computer vison library, and sends the [hue-saturation-value](https://en.wikipedia.org/wiki/HSL_and_HSV) readings to **Firebase Realtime Database** for further analysis.
At this stage, it mostly serves as a testing ground for a more powerful version using Raspberry Pi 3.

**UPDATE** The companion [Flutter](https://flutter.io) app is now up and running, reading HSV values from the database and converting them to actual colors shown in UI. Tested on Android so far, iOS optimization forthcoming.
