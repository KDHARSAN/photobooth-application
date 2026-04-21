# Photo Booth Application

A classic, retro-style photo booth application for Android built entirely with Kotlin and Jetpack Compose. Capture, apply filters, and store your favorite memories with a modern app architecture!

## Features

- **Modern UI/UX**: Fully built using Jetpack Compose with Material 3 components.
- **Camera Capture**: Live camera preview and image capture using Android's CameraX API.
- **Retro Filters**: Apply customized visual filters to photos using GPUImage.
- **Local Storage**: Caches photos locally using a Room Database, ensuring offline accessibility.
- **Cloud Synchronization**: Secure cloud backup and fetching using Firebase (Auth, Firestore, and Storage).
- **Google Authentication**: Seamless and secure sign-in via Google Play Services.

## Tech Stack Overview

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Camera:** CameraX API
- **Image Processing:** GPUImage (jp.co.cyberagent.android) & Coil for asynchronous image loading
- **Local DB:** Room Database
- **Cloud & Backend:** Firebase Authentication, Firestore, and Firebase Cloud Storage
- **Concurrency:** Kotlin Coroutines

## API and Backend Integration

This application relies on multiple platform APIs and cloud services. Access and security are managed as follows:

### 1. CameraX API
The app natively interfaces with the device hardware through the Android framework's CameraX API to power live previews and photo capturing features.

### 2. Google Play Services Authentication
The app uses Google Play Services to let users securely authenticate with their Google accounts. Access is managed through an OAuth 2.0 Client credential on the Google Cloud platform.

### 3. Firebase Suite (Firestore, Storage, Auth)
- **Firebase Auth**: Verifies user identity via Google Sign-In and controls session access.
- **Cloud Storage**: Authenticated users can upload and retrieve their captured photos.
- **Firestore**: Maintains a structured ledger of a user's uploaded images and metadata.
**Access Management:**
To connect to these services, the project requires a valid `google-services.json` config file generated from your Firebase console. Client-side requests are securely managed by Firebase Security Rules, resolving authorized user contexts to restrict data modification and access purely to the resource owner.

## Installation & Setup

1. **Clone the Repository**
   ```bash
   git clone https://github.com/KDHARSAN/photobooth-application
   cd photobooth-application
   ```
2. **Add Firebase Configuration**
   - Create a project on the Firebase Console.
   - Register your Android app with the package name: `com.example.photobooth`
   - Enable **Google Sign-In** inside the Firebase Authentication settings.
   - Set up Firestore and Firebase Cloud Storage.
   - Download the `google-services.json` file and place it in the `app/` directory of this project.

3. **Build & Run**
   - Open the project in Android Studio.
   - Sync Gradle.
   - Build and run the app on an emulator (API 26+) or a physical device.

## Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you would like to change.

## License

MIT License. See the `LICENSE` file for more details.
