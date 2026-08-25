<p align="center">
  <img src="assets/.aistudio/icon.png" width="100" alt="GameLingo Logo">
</p>

<h1 align="center">GameLingo</h1>

<p align="center">
  Professional game text translator for Android.<br>
  Play any game in any language.
</p>

<p align="center">
  <a href="README.ru.md">🇷🇺 Русская версия</a>
</p>

<p align="center">
  <a href="https://github.com/GameLingoApp/GameLingo/actions/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/GameLingoApp/GameLingo/build.yml?style=flat-square&label=Build" alt="Build Status">
  </a>
  <a href="https://gamelingo.netlify.app"><img src="https://img.shields.io/badge/Website-gamelingo.netlify.app-blue?style=flat-square" alt="Website"></a>
  <img src="https://img.shields.io/badge/Platform-Android-green?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square" alt="Kotlin">
  <img src="https://img.shields.io/badge/License-Proprietary-red?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen?style=flat-square" alt="Min API">
</p>

<p align="center">
  <a href="https://github.com/GameLingoApp/GameLingo/releases/latest">
    <img src="https://img.shields.io/github/v/release/GameLingoApp/GameLingo?style=flat-square&label=Latest%20Release&color=blue" alt="Latest Release">
  </a>
  <a href="https://github.com/GameLingoApp/GameLingo/releases/latest/download/app-debug.apk">
    <img src="https://img.shields.io/badge/⬇️_Download_APK-blue?style=for-the-badge&logo=android&logoColor=white" alt="Download APK">
  </a>
</p>

---

## About

GameLingo is an Android application that translates game text in real time.
It uses on-device ML Kit translation — no API keys, no internet required for basic translation.

### Key Features

- **Instant Translation** — Translate game text between 50+ languages using Google ML Kit
- **Screen Translation (OCR)** — Tap the floating button to scan and translate your game screen in real time
- **Works Offline** — Download language packs and translate without internet
- **Floating Overlay** — Translate text without leaving your game
- **Translation History** — Search, copy, and manage past translations
- **Google Sign-In** — Sync your data across devices via Firebase
- **Dark & Light Themes** — Automatic or manual theme switching

## Screenshots

> Coming soon after final UI polish.

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Translation | Google ML Kit Translate |
| OCR | Google ML Kit Text Recognition |
| Database | Room |
| Preferences | DataStore |
| Auth | Firebase Auth + Google Sign-In |
| Cloud | Firebase Firestore + Cloud Functions |
| Payments | YooMoney (ЮKassa) |
| Navigation | Navigation Compose |

## Architecture

```
com.example/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Firebase, PaymentManager, AuthManager
│   └── repository/     # Repository implementations
├── domain/
│   ├── model/          # Data models
│   └── usecase/        # Business logic
├── ui/
│   ├── theme/          # Colors, typography, theme
│   ├── screens/        # Home, History, Overlay, Premium, Settings
│   ├── components/     # Reusable UI components
│   └── viewmodel/      # ViewModels
├── engine/             # Translation engine, OCR processor
├── billing/            # Payment integration
└── di/                 # Hilt modules
```

## Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34+
- Firebase project (for Auth and Firestore)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/GameLingoApp/GameLingo.git
   ```

2. **Add Firebase configuration**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a project or use existing one
   - Download `google-services.json`
   - Place it in the `app/` directory

3. **Configure environment**
   - Copy `.env.example` to `.env`
   - Add your Firebase and YooMoney credentials

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```

## Pricing

| Plan | Price | Features |
|------|-------|----------|
| Free | 0 ₽ | 10 translations/day, game term detection, online only |
| Pro Monthly | 150 ₽/month | Unlimited translations, no ads, screen overlay, priority support |
| Pro Yearly | 999 ₽/year | Everything in Monthly + offline dictionary + export history (Save 44%) |

*Payments processed via YooMoney (ЮKassa).*

## Contact

- **Email**: support.gamelingo.app@gmail.com
- **Telegram**: [@GameLingoApp](https://t.me/GameLingoApp)
- **Website**: [gamelingo.netlify.app](https://gamelingo.netlify.app)

## License

Copyright (c) 2025 GameLingo. All rights reserved.

This is proprietary software. Unauthorized copying, modification, distribution, or use of this software, via any medium, is strictly prohibited.
