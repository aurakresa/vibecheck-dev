# VibeCheck

**VibeCheck** is a retro-inspired, remote camera control Android application. It allows you to transform your Android device into a host camera that can be controlled remotely via a local network connection, complete with a "manly but bright" Y2K aesthetic and pixel-art UI.

## 🚀 Features

* **Remote Camera Control:** Turn your phone into a remote camera host using a built-in local server.
* **P2P Video Streaming:** Real-time video transmission powered by WebRTC.
* **Smart Gesture Shutter:** Hand detection using Google ML Kit to trigger the camera shutter effortlessly.
* **Retro Y2K Aesthetic:** A unique, custom UI built with Jetpack Compose, featuring neon colors, pixel fonts, and DOS-inspired elements.
* **Real-time Communication:** Low-latency commands (like shutter and zoom) handled via WebSockets.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (`androidx.compose`)
* **Camera API:** CameraX
* **Local Server & WebSockets:** Ktor Server (`ktor-server-netty`, `ktor-server-websockets`)
* **Machine Learning:** Google ML Kit (Pose/Hand Detection)
* **P2P Streaming:** WebRTC (`stream-webrtc-android`)

## 📂 Project Structure

* `app/src/main/java/com/example/vibecheck_dev/`: Contains the main source code.
    * `MainActivity.kt`: Entry point and permissions handling.
    * `server/VibeServer.kt`: Ktor WebSocket server implementation for remote commands.
    * `ui/screens/CameraHostScreens.kt`: The retro UI for the camera host.
    * `ui/theme/`: Custom Y2K typography and color palette.
    * `camera/`: CameraX and analysis utilities.

## ⚙️ Getting Started

### Prerequisites
* Android Studio (Latest version recommended)
* Android device running SDK 31 (Android 12) or higher.

### Installation
1.  Clone this repository.
2.  Open the project in Android Studio.
3.  Sync the project with Gradle files.
4.  Build and run the app on a physical device (Emulators may not support all camera features).

### Usage
1.  Launch the app and grant the necessary Camera and Audio permissions.
2.  Tap **START HOST SERVER** to initialize the Ktor server and WebRTC stream.
3.  Connect to the displayed IP address from a remote client to view the stream and send commands.

## 🧑‍💻 Developer

- **Fikal Alif**
- **Aura Kresa**

## 📄 License

This project is open-source and available for development and learning purposes.
