<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="120" height="120" alt="CUEats Logo" style="border-radius: 20px;" />

# CUEats 🍽️

**Your complete campus companion for Chandigarh University**

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://play.google.com/store)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Play Store](https://img.shields.io/badge/Available%20on-Play%20Store-34A853?style=for-the-badge&logo=googleplay&logoColor=white)](https://play.google.com/store)

*Built solo over ~1.5 years of weekends, late nights, and real production experience.*

</div>

---

## 📖 About the App

CUEats started as a simple idea: **why should students have to walk to the mess just to see what's for lunch?**

What began as a meal-menu viewer grew into a full-featured campus companion — with live shop data, event discovery, smart meal notifications, an interactive campus map, budget tracking, and more. Every feature was discovered through real use, not guesswork.

> **Published on Google Play Store** • Version 1.1.2 • Supports Android 7.0+

---

## ✨ Features

### 🍱 Mess / Meal Menu
- Weekly mess menu with **Breakfast, Lunch, Snacks & Dinner**
- Meal detail pages with ingredients and nutritional info
- Smart scheduled **push notifications** at every meal time (powered by Firebase Cloud Functions + FCM)

### 🏪 Shops & Canteens
- Browse all on-campus shops with **live menus and offers**
- Per-item **ratings & reviews** system
- **Favourite** your go-to shops
- Offer banners and highlights

### 📅 Events & Announcements
- Discover college events with **full registration support**
- Swipeable event cards with smooth animations
- **Deep-link support** — share an event link that opens directly in the app

### 🗺️ Campus Map
- Interactive **OpenStreetMap-powered** campus map
- Navigate to any building, shop, or facility

### 👤 Profile & Auth
- **Google Sign-In** via Firebase Authentication
- View your registered events and meal preferences
- Per-user notification settings synced to the cloud

### 💰 Budget Tracker *(beta)*
- Track your daily campus spending across meal types

### 🔔 Smart Notifications
- Scheduled Firebase Cloud Functions send meal-time reminders
- Per-user opt-in/out stored in Firestore
- Invalid FCM tokens automatically cleaned up

### 📦 Home Screen Widget
- Glance-powered **Android widget** showing today's meals right on the home screen

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI Framework | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| Auth | Firebase Authentication (Google Sign-In) |
| Database | Firebase Firestore + Realtime Database |
| Storage | Firebase Storage |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Backend Functions | Firebase Cloud Functions (Node.js) |
| Maps | OSMDroid (OpenStreetMap) |
| Images | Coil |
| Widget | Jetpack Glance |
| Background Work | WorkManager |
| Local Storage | DataStore Preferences |
| Animations | Lottie |
| Minimum SDK | API 24 (Android 7.0) |
| Target SDK | API 35 (Android 15) |

---

## 📁 Project Structure

```
CUEats1/
├── app/src/main/java/com/divyansh/cueats/
│   ├── AnnouncementScreen/    # Events, registration, swipeable cards
│   ├── BudgetScreen/          # Spending tracker
│   ├── HomeScreen/            # Dashboard + ViewModel
│   ├── LoginScreen/           # Auth UI + ViewModel + Repository
│   ├── Maps/                  # Campus map (OSMDroid)
│   ├── MealsJSON/             # Meal data models
│   ├── Mess/                  # Mess menu UI + Firebase setup
│   ├── Notification/          # Notification preferences + screens
│   ├── ProfileScreen/         # User profile
│   ├── SettingScreen/         # App settings
│   ├── ShopsScreen/           # Shops, ratings, offers
│   ├── Widget/                # Glance home screen widget
│   ├── common/                # Shared composables
│   ├── notifirebase/          # Boot receiver for notifications
│   ├── ui/                    # Theming
│   ├── MainActivity.kt        # Entry point + NavHost
│   ├── NavigateRoute.kt       # Type-safe navigation routes
│   └── SplashScreen.kt        # Animated splash
└── functions/
    └── index.js               # Firebase Cloud Functions (scheduled notifications)
```

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 11+
- A Firebase project with Firestore, Auth, Storage, and Cloud Messaging enabled

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/Divyansh5070/CUEats.git
   cd CUEats
   ```

2. **Add Firebase config**
   - Download `google-services.json` from your Firebase project console
   - Place it at `app/google-services.json`

3. **Add signing secrets** *(for release builds only)*
   ```bash
   cp secrets.properties.example secrets.properties
   # Edit secrets.properties and fill in your keystore credentials
   ```

4. **Open in Android Studio** and let Gradle sync

5. **Run on a device or emulator**
   ```
   Run > Run 'app'
   ```

---

## ☁️ Firebase Functions (Backend)

The `functions/` folder contains scheduled Cloud Functions that send meal-time notifications to all opted-in users via FCM.

| Function | Schedule (IST) |
|---|---|
| `sendBreakfastNotifications` | 7:30 AM weekdays |
| `sendBreakfastNotificationsWeekend` | 8:00 AM weekends |
| `sendLunchNotifications` | 12:00 PM weekdays |
| `sendLunchNotificationsWeekend` | 12:30 PM weekends |
| `sendSnackNotifications` | 4:30 PM daily |
| `sendDinnerNotifications` | 7:30 PM daily |

> **Note:** Firebase credentials (`google-services.json`, service account keys) are not included in this repo for security reasons. You'll need your own Firebase project to deploy.

---

## 🗺️ App Navigation Flow

```
Splash Screen
     │
     ▼
 Login Screen (Google Sign-In)
     │
     ▼
 Home Screen ──────────────────────────────────┐
     │                                          │
     ├── Mess Menu → Meal Details               │
     ├── Shops → Shop Menu                      │
     ├── Events → Event Details → Register      │
     ├── Campus Map                             │
     ├── Notifications                          │
     └── Profile ─────────────────────────────-┘
```

---

## 📸 Screenshots

> *Coming soon — screenshots & screen recordings will be added here.*

---

## 🛣️ Roadmap

- [ ] Dark mode toggle (user preference)
- [ ] Meal rating / feedback system
- [ ] Real-time mess stock availability
- [ ] iOS version (SwiftUI)
- [ ] Social sharing for events

---

## 👨‍💻 Developer

**Divyansh Sharma**
- GitHub: [@Divyansh5070](https://github.com/Divyansh5070)
- LinkedIn: [Divyansh Sharma](https://linkedin.com/in/divyansh5070)

---

## ⭐ A Note From the Developer

This app was built completely solo — from the initial idea to Play Store deployment — over about **1.5 years** while studying full-time. It wasn't always smooth. There were evenings spent debugging FCM tokens, nights rewriting the shop UI from scratch, and moments where I almost gave up. But every feature you see here came from real persistence, real learning, and real care for making something useful for my campus community.

It may not have the download numbers I once dreamed of — but building it taught me more than any classroom could.

If you found this project useful or inspiring, a ⭐ on the repo would mean a lot!

---

## 📄 License

This project is open-sourced for educational and portfolio purposes.
You're welcome to study the code and architecture — but please don't publish derivative apps to the Play Store.

---

<div align="center">
Built with ❤️ at <strong>Chandigarh University</strong>
</div>
