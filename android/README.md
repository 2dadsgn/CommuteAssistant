# 🚦 Commute Assistant — Android App

A smart commute app built with **Kotlin + Jetpack Compose** that learns your daily traffic patterns and recommends the ideal departure time — even the day before.

---

## ✨ Features

| Feature | Status |
|---|---|
| Save commute routines per day of week | ✅ Ready |
| Smart departure recommendations | ✅ Ready |
| Background traffic checks every 30 min | ✅ Ready |
| Push notifications for traffic alerts | ✅ Ready |
| Historical pattern learning (Room DB) | ✅ Ready |
| Confidence score for recommendations | ✅ Ready |
| Alternative departure time suggestions | ✅ Ready |
| Google Maps integration (plug-in point) | 🔌 Configured |

---

## 🗂 Project Structure

```
CommuteAssistant/
├── app/src/main/kotlin/com/commuteassistant/
│   ├── MainActivity.kt                  # Entry point + navigation
│   ├── CommuteApp.kt                    # Application class (Hilt)
│   ├── di/
│   │   └── AppModule.kt                 # Hilt dependency injection
│   ├── domain/
│   │   ├── model/Models.kt              # CommuteRoutine, TrafficSnapshot, etc.
│   │   └── usecase/
│   │       └── GetDepartureRecommendationUseCase.kt
│   ├── data/
│   │   ├── db/Database.kt               # Room DB, DAOs, Entities
│   │   └── repository/CommuteRepository.kt
│   ├── notifications/
│   │   └── TrafficCheckWorker.kt        # WorkManager + notifications
│   ├── viewmodel/ViewModels.kt          # HomeViewModel, RoutineFormViewModel
│   └── ui/
│       ├── theme/Theme.kt               # Material 3 theme
│       └── screens/
│           ├── HomeScreen.kt            # Main dashboard
│           └── AddRoutineScreen.kt      # Add/edit routine form
└── app/src/main/res/
    ├── values/strings.xml
    └── drawable/ic_traffic.xml
```

---

## 🚀 Getting Started

### 1. Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35
- A Google Cloud account (for Maps + Directions API)

### 2. Clone & Open

```bash
git clone <your-repo-url>
cd CommuteAssistant
```

Open in Android Studio → **File → Open** → select the `CommuteAssistant` folder.

### 3. Add your Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Enable these APIs:
   - **Maps SDK for Android**
   - **Directions API** (for live traffic data)
   - **Places API** (for address search — optional)
3. Create an API key and restrict it to your app's package name
4. Open (or create) `local.properties` in the project root and add:

```properties
MAPS_API_KEY=YOUR_KEY_HERE
```

> ⚠️ Never commit `local.properties` to Git — it's already in `.gitignore`.

### 4. Run the App

Click ▶ **Run** in Android Studio, or:

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔌 Connecting a Real Traffic API

The recommendation engine is ready to accept live traffic data. To connect Google Directions API:

Open `GetDepartureRecommendationUseCase.kt` and replace the placeholder with a real API call:

```kotlin
// Replace this block:
val baseMinutes = avgHistorical?.toInt() ?: 30

// With a real Directions API call, e.g.:
val response = directionsApiService.getRoute(
    origin = "${routine.originLat},${routine.originLng}",
    destination = "${routine.destinationLat},${routine.destinationLng}",
    departureTime = "now",
    apiKey = BuildConfig.MAPS_API_KEY
)
val baseMinutes = response.routes.first().legs.first().durationInTraffic.value / 60
```

You'll need to add a Retrofit service for the Directions API — the project structure is ready for it in the `data/` layer.

---

## 📱 How It Works

```
User sets routine (day + time + route)
        ↓
WorkManager checks traffic every 30 min
        ↓
TrafficSnapshot saved to Room DB
        ↓
Recommendation engine reads:
  • Historical average duration
  • Latest congestion level
  • Time until departure
        ↓
If leaving earlier saves > 5 min → push notification
        ↓
Home screen shows recommended time + confidence score
```

---

## 🛣 Roadmap / Next Steps

- [ ] **Google Directions API integration** — live travel time with traffic
- [ ] **Places Autocomplete** — address search instead of manual coordinates
- [ ] **Map picker screen** — tap to set origin/destination on a map
- [ ] **Day-before predictions** — use historical data to warn the night before
- [ ] **Widgets** — glanceable home screen widget
- [ ] **Multiple destinations** — support work, gym, school, etc.
- [ ] **Wearable notifications** — Wear OS support
- [ ] **ML-based prediction** — replace rule-based engine with on-device ML model

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Database | Room |
| Background | WorkManager |
| Navigation | Navigation Compose |
| Maps | Google Maps SDK (configured) |
| Architecture | MVVM + Clean Architecture |

---

## 📄 License

MIT — use freely, build something great.
