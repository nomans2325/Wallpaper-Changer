# Auto Wallpaper Changer — Android project

An offline Android app (Java, min Android 8.0) that imports multiple images into private app storage, rotates **home-screen** wallpaper sequentially and schedules changes at selectable approximate intervals (15 minutes, 30 minutes, 1/3/6/12/24 hours).

## Build APK with Android Studio
1. Install Android Studio and Android SDK Platform 35.
2. Open **AutoWallpaperChanger** (the folder containing `settings.gradle`).
3. Allow Gradle to sync and install the requested SDK/build tools.
4. From the **Build** menu select **Build Bundle(s) / APK(s) > Build APK(s)** (menu wording may vary by Android Studio version).
5. Find the debug APK at `app/build/outputs/apk/debug/app-debug.apk` and install it on an Android phone.

## Use
1. Tap **Add wallpapers**, select one or more photos.
2. Select how often to change wallpapers.
3. Tap **Start automatic changing**. The next automatic change is scheduled one interval later.
4. Tap **Change now** to advance immediately, or **Stop** to disable the timer.
5. Photos can be removed from the in-app gallery.

## Details and limitations
- **Home screen only**. The lock screen is not altered.
- Wallpapers rotate in import order (file name sorts by import timestamp).
- Android battery saver, Doze and manufacturer restrictions can delay alarms, particularly on newer Android versions. This project deliberately uses inexact alarms, does not request special exact-alarm access, and cannot guarantee precise change times.
- The schedule resumes after a device reboot (once the user has unlocked the device) or an app update.
- Photos are copied to internal app storage; uninstalling the app removes imported photos.
- No internet permission, no storage-wide permission, no trackers, and no external library dependencies in the app.
- Tested by static review only; an Android SDK and Gradle build environment are not installed in this workspace, so the APK has **not** been compiled or device-tested here.
