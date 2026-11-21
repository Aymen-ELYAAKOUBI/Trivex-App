# Trivex Delivery App 🚀📦

A modern, high-performance Android delivery application built with **Kotlin**, **Jetpack Compose**, **Material 3**, and **MapLibre / OpenStreetMap**. Trivex provides dedicated workflows for Clients, Couriers (Drivers), and Companies, backed by Firebase Authentication and Cloud Firestore.

---

## 🌟 Key Features

### 👤 Client & Company
- **Authentication**: Secure email/password login and registration with Firebase Auth and profile management.
- **Order Creation**: Request deliveries specifying pickup and drop-off locations, recipient details, and package descriptions.
- **Live Tracking**: Real-time order status updates synchronized seamlessly via Cloud Firestore.

### 🛵 Courier (Driver)
- **Order Marketplace**: Browse shared pending delivery requests in real-time.
- **Secure Reservation**: Claim orders with atomic Firestore transactions to prevent double assignment.
- **Interactive Routing**: Built-in interactive **OpenStreetMap & MapLibre** navigation with real-time route rendering and camera controls.
- **Order Lifecycle**: Advance delivery states (Pending ➔ Accepted ➔ Picked Up ➔ Delivered).

---

## 🛠️ Tech Stack & Architecture

- **UI**: Jetpack Compose, Material 3, Adaptive UIs, Custom App Icon (Metallic T design).
- **Navigation**: Jetpack Navigation Compose with role-based routing (`AppNav.kt`).
- **Backend & Database**: Firebase Authentication & Cloud Firestore (named database `fast-delivery-app`).
- **Maps & Routing**: MapLibre GL Native for Android (`org.maplibre.android`), OpenStreetMap tiles, and OSRM route calculation.
- **Concurrency**: Kotlin Coroutines & Flow.

---

## 📱 Project Structure

```text
app/src/main/java/com/example/suiverliverson/
├── AppNav.kt                    # Role-based navigation and routing
├── AccountStore.kt              # Firebase Authentication & user profiles
├── OrderRepository.kt           # Firestore order CRUD and real-time listeners
├── RoleHomeScreen.kt            # Client and Company dashboards
├── HomeScreen.kt                # Driver dashboard and order queue
├── DeliveryManager.kt           # Delivery lifecycle state management
├── MapScreen.kt                 # MapLibre OpenStreetMap integration & route view
└── RouteService.kt              # OSM / OSRM road route fetching
firestore.rules                  # Secure Cloud Firestore security rules
firebase.json                    # Firestore database association configuration
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala / Ladybug or newer recommended).
- **Android SDK Platform 37** (`minSdk 29`, `targetSdk 37`).
- Android device or emulator running **API 29+**.

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/Aymen-ELYAAKOUBI/Trivex-App.git
   ```

2. Open the project in Android Studio.

3. **Firebase Setup**:
   - Download your `google-services.json` from your Firebase Console for package `com.example.suiverliverson`.
   - Place it in `app/google-services.json` (this file is git-ignored for security).

4. **Build & Run**:
   - Sync project with Gradle files.
   - Run the `app` configuration on your device or emulator.

To build the debug APK via terminal:
- **Windows**: `gradlew.bat :app:assembleDebug`
- **macOS/Linux**: `./gradlew :app:assembleDebug`

---

## 🔒 Security & Privacy

- Sensitive credentials (`google-services.json`, `local.properties`) are excluded via `.gitignore`.
- Firestore security rules (`firestore.rules`) enforce strict user data isolation and role-based access control.

---

## 👨‍💻 Author

**Aymen Elyaakoubi**

- **Portfolio**: [my-portfolio-beta-eight-27.vercel.app](https://my-portfolio-beta-eight-27.vercel.app/)
- **GitHub**: [Aymen-ELYAAKOUBI](https://github.com/Aymen-ELYAAKOUBI)

---

## 📄 License

All rights reserved. Please contact the author before reusing or redistributing code.
