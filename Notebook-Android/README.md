# Notebook Android companion

`Notebook-Android` is a companion app for the Garmin `Notebook` watch app.

## Connection with Garmin watch

The Android app connects to a Garmin watch through Garmin Connect mobile app:

`Notebook-Android -> Garmin Connect app -> paired Garmin device -> Notebook app`

For that Garmin Connect must be installed and running on the Android phone. 
Garmin watch must be paired in Garmin Connect, connected, and have the Notebook app installed.

The existing watch application UUID is hardcoded as `e774b6d4cd9a4fbb8060f01dbbfdd596` 
(the manifest UUID without separators).

## Implemented functionality

- Connected Garmin device selection.
- Verification that the Notebook Connect IQ app is installed on the selected watch.
- Directory listing and selection.
- Commands to create, rename and delete a directory.
- Commands to create, edit, rename and delete text files.
- Commands to add, rename and delete image files.
- Image selection from image gallery.
- Image downscaling and conversion to a 64-color version for a watch.

## Local development

1. Install Android Studio with Android SDK Platform 36 and Build Tools.
2. Use Android Studio's bundled JDK (JDK 17 or newer) for Gradle.
3. Open this directory as an Android Studio project and allow Gradle to download dependencies.
4. Install Garmin Connect on the test phone and pair with Garmin watch.
5. Install a Notebook app on the watch.

The project uses Garmin IQ Companion App SDK `2.4.0`.

### Building the app

After installing Android Studio and the Android SDK run:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

The APK file will be built to `/app/build/outputs/apk/debug/` directory.