# Notebook-Watch

Notebook is a smart watch application for Garmin Enduro 3. It makes it available to use the watch as a notebook: storing and viewing text/image items in it. Its companion app [Notebook-Android](../Notebook-Android/README.md) manages and transfers these text/image files via a phone.

The watch application currently supports:

- storing and viewing text items.
- storing and viewing images up to 128 x 128 pixels.
- creating directories for items to add into it.
- creating, renaming and deleting content through the phone-to-watch communication.

## Target and prerequisites

- Garmin Enduro 3
- At least Connect IQ  API 6.0.0.
- Connect IQ SDK 9.2.0.
- A Garmin developer key

This project expects the following parent-directory layout:

```text
Garmin Notebook/
|-- garmin_developer_key
|-- connectiq-sdk-win-9.2.0-2026-06-09-92a1605b2/
|-- Notebook-Watch/
`-- Notebook-Android/
```

In Visual Studio Code, set `monkeyC.developerKeyPath` to the absolute path of `garmin_developer_key` if the Monkey C extension does not find it automatically.

## Running the tests

Run in the project root directory:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\run-watch-tests.ps1 -StartSimulator
```

The script builds `bin\Notebook-tests.prg`, starts the Connect IQ simulator when necessary, and executes all tests against the Enduro 3 profile.

If the simulator is already running, omit `-StartSimulator`:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\run-watch-tests.ps1
```

The storage tests clear Notebook's application storage inside the simulator. Tests do not access or modify storage on a physical watch device. 

## Building the app

Run in the project root directory:

```powershell
$sdk = "..\connectiq-sdk-win-9.2.0-2026-06-09-92a1605b2"
& "$sdk\bin\monkeyc.bat" `
    -f ".\monkey.jungle" `
    -o ".\bin\Notebook.prg" `
    -y "..\garmin_developer_key" `
    -d enduro3 `
    -l 3 `
    -w
```

`BUILD SUCCESSFUL` indicates that the watch app was produced to:

```text
Notebook-Watch\bin\Notebook.prg
```

Test runs can generate also `Notebook-tests.prg` That app is not for running it in the actual Garmin watch.

It is possible to build the app also via Visual Studio Code using the Monkey C extension's **Monkey C: Build for Device** command and selecting Enduro 3.

## Installing on an Enduro 3

Installation instructions below are for local development and for sideloading the app in user's phone. It does not publish the app to the Connect IQ Store.

1. Build `bin\Notebook.prg` using build instructions above.
2. Connect Enduro 3 watch to the computer with a USB data cable.
3. Open the watch's internal storage in File Explorer.
4. Open the `GARMIN\APPS` directory.
5. Copy `bin\Notebook.prg` into `GARMIN\APPS`. If Windows asks whether to replace the existing Notebook app, replace it.
6. Safely eject the watch and disconnect the USB cable.
7. On the watch, open the activity/app list and launch **Notebook**.

Keep your Android phone paired with the watch in Garmin Connect when transferring the content. Install and open the companion Android application separately when managing Notebook directories and items.

## Troubleshooting a watch crash

If the watch displays the Connect IQ `Q!` error, reconnect it over USB and inspect the newest entries in:

```text
GARMIN\APPS\LOGS\CIQ_LOG.YML
```

The simulator tests protect the transfer and rendering behavior, but only a physical Enduro 3 can conclusively verify its watchdog timing and firmware-specific behavior.
