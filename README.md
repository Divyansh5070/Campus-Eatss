<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="100" height="100" alt="CUEats Logo" style="border-radius: 24px;" />

<br/>

```
  ██████╗██╗   ██╗███████╗ █████╗ ████████╗███████╗
 ██╔════╝██║   ██║██╔════╝██╔══██╗╚══██╔══╝██╔════╝
 ██║     ██║   ██║█████╗  ███████║   ██║   ███████╗
 ██║     ██║   ██║██╔══╝  ██╔══██║   ██║   ╚════██║
 ╚██████╗╚██████╔╝███████╗██║  ██║   ██║   ███████║
  ╚═════╝ ╚═════╝ ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚══════╝
```

### *Your campus. Your food. Your way.*

<br/>

[![Android](https://img.shields.io/badge/Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://play.google.com/store)
[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![Play Store](https://img.shields.io/badge/Play%20Store-v1.1.2-34A853?style=flat-square&logo=googleplay&logoColor=white)](https://play.google.com/store)
[![Android 7.0+](https://img.shields.io/badge/Android%207.0+-API%2024-informational?style=flat-square)](https://developer.android.com/about/versions/nougat)

</div>

---

## The origin story

It started with a single, frustrating question:

> *"Did I just walk 10 minutes to the mess for rajma again?"*

CUEats was born in that moment of campus-life pain. What began as a weekend hack to preview the mess menu turned into a 1.5-year, solo-built, fully-shipped Android app — living on the Play Store — used by real students at Chandigarh University.

No team. No funding. No template. Just late nights, stubborn debugging, and genuine love for the problem.

---

## What's inside

```
┌─────────────────────────────────────────────────────────────┐
│                        C U E A T S                          │
├──────────────┬──────────────┬───────────────┬───────────────┤
│  🍱 Mess     │  🏪 Shops    │  📅 Events    │  🗺️ Map       │
│  Menu        │  & Canteens  │  & Announces  │  (OSM)        │
├──────────────┼──────────────┼───────────────┼───────────────┤
│  🔔 Smart    │  👤 Profile  │  💰 Budget    │  📦 Widget    │
│  Notifs      │  & Auth      │  Tracker      │  (Glance)     │
└──────────────┴──────────────┴───────────────┴───────────────┘
```

### 🍱 Mess & Meal Menu
The feature that started it all. Every meal, every day — **Breakfast, Lunch, Snacks, Dinner** — with ingredients and nutritional info. No more surprise rajma.

### 🏪 Shops & Canteens
A live directory of every on-campus shop — menus, offers, per-item ratings & reviews, and the ability to favourite your regulars. Think Zomato, but for your hostel gate.

### 📅 Events & Announcements
Discover, register for, and share college events — complete with deep-link support so a shared link opens directly inside the app. Swipeable cards, smooth animations, zero friction.

### 🗺️ Campus Map
OpenStreetMap-powered interactive map of the entire campus. Find any building, shop, or facility without asking five seniors for directions.

### 🔔 Smart Meal Notifications
Firebase Cloud Functions fire scheduled FCM notifications for every meal window. Per-user opt-in, synced to Firestore. Invalid tokens are cleaned up automatically.

| Notification | Weekday | Weekend |
|---|---|---|
| Breakfast | 7:30 AM | 8:00 AM |
| Lunch | 12:00 PM | 12:30 PM |
| Snacks | 4:30 PM | 4:30 PM |
| Dinner | 7:30 PM | 7:30 PM |

### 💰 Budget Tracker *(beta)*
Log your daily campus spending. Know where your money goes before it's gone.

### 📦 Home Screen Widget
A **Jetpack Glance** widget that puts today's meal schedule right on your home screen. No app-open needed.

---

## Tech stack

| What | How |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| Auth | Firebase Authentication (Google Sign-In) |
| Database | Cloud Firestore + Realtime Database |
| Storage | Firebase Storage |
| Push Notifications | Firebase Cloud Messaging |
| Backend | Firebase Cloud Functions (Node.js) |
| Maps | OSMDroid (OpenStreetMap) |
| Images | Coil |
| Widget | Jetpack Glance |
| Background | WorkManager |
| Local Storage | DataStore Preferences |
| Animations | Lottie |
| Min / Target SDK | API 24 (Android 7.0) / API 35 (Android 15) |

---

## Project structure

```
CUEats/
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
│   ├── notifirebase/          # Boot receiver
│   ├── ui/                    # Theming
│   ├── MainActivity.kt        # Entry point + NavHost
│   ├── NavigateRoute.kt       # Type-safe routes
│   └── SplashScreen.kt        # Animated splash
└── functions/
    └── index.js               # Scheduled notification Cloud Functions
```

---

## Running it locally

**You'll need:** Android Studio Hedgehog+, JDK 11+, and your own Firebase project (Firestore, Auth, Storage, FCM enabled).

```bash
# 1. Clone
git clone https://github.com/Divyansh5070/CUEats.git
cd CUEats

# 2. Drop in your Firebase config
# Download google-services.json from your Firebase console
# → place at app/google-services.json

# 3. Signing (release builds only)
cp secrets.properties.example secrets.properties
# Fill in your keystore credentials

# 4. Open in Android Studio → let Gradle sync → Run 'app'
```

> Firebase credentials and service account keys are intentionally excluded. You need your own Firebase project to deploy the backend functions.

---

## Navigation map

```
Splash
  └── Login (Google)
        └── Home
              ├── Mess Menu → Meal Detail
              ├── Shops → Shop Menu → Item Reviews
              ├── Events → Event Detail → Register
              ├── Campus Map
              ├── Notifications Settings
              └── Profile
```

---

## What's next

- [ ] Dark mode (user-controlled)
- [ ] Meal rating & feedback
- [ ] Real-time mess stock availability
- [ ] iOS (SwiftUI)
- [ ] Social sharing for events

---

## The developer

**Divyansh Sharma** — built this solo, from idea to Play Store, while studying full-time.

- GitHub → [@Divyansh5070](https://github.com/Divyansh5070)
- LinkedIn → [Divyansh Sharma](https://linkedin.com/in/divyansh5070)

---

## A personal note

There were evenings lost to FCM token bugs. Nights where the shop UI got rewritten from scratch. Moments of *why am I even doing this?*

But every single feature in this app came from real persistence, not a tutorial. The download count was never the point — the learning was. Building CUEats taught me more about Android, Firebase, product thinking, and shipping under pressure than any classroom ever could.

If this project helped you — as a reference, as inspiration, or just as proof that solo-shipped apps are possible — a ⭐ on the repo would genuinely make my day.

---

## License

Open-sourced for learning and portfolio purposes. Study the code, fork for personal projects, learn from the architecture — but please don't republish derivative apps to the Play Store.

---

<div align="center">

Built with stubbornness, coffee, and real care — at **Chandigarh University**

</div>
