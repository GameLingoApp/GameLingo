<p align="center">
  <img src="assets/.aistudio/icon.png" width="100" alt="GameLingo Logo">
</p>

<h1 align="center">GameLingo</h1>

<p align="center">
  Профессиональный переводчик текста игр для Android.<br>
  Играй в любую игру на любом языке.
</p>

<p align="center">
  <a href="README.md">🇬🇧 English version</a>
</p>

<p align="center">
  <a href="https://github.com/GameLingoApp/GameLingo/actions/workflows/build.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/GameLingoApp/GameLingo/build.yml?style=flat-square&label=Build" alt="Build Status">
  </a>
  <a href="https://gamelingo.netlify.app"><img src="https://img.shields.io/badge/Сайт-gamelingo.netlify.app-blue?style=flat-square" alt="Сайт"></a>
  <img src="https://img.shields.io/badge/Платформа-Android-green?style=flat-square" alt="Платформа">
  <img src="https://img.shields.io/badge/Язык-Kotlin-purple?style=flat-square" alt="Kotlin">
  <img src="https://img.shields.io/badge/Лицензия-Proprietary-red?style=flat-square" alt="Лицензия">
  <img src="https://img.shields.io/badge/API-26%2B-brightgreen?style=flat-square" alt="Min API">
</p>

<p align="center">
  <a href="https://github.com/GameLingoApp/GameLingo/releases/latest">
    <img src="https://img.shields.io/github/v/release/GameLingoApp/GameLingo?style=flat-square&label=Последний%20релиз&color=blue" alt="Последний релиз">
  </a>
  <a href="https://github.com/GameLingoApp/GameLingo/releases/latest/download/app-debug.apk">
    <img src="https://img.shields.io/badge/⬇️_Скачать_APK-blue?style=for-the-badge&logo=android&logoColor=white" alt="Скачать APK">
  </a>
</p>

---

## О приложении

GameLingo — это Android-приложение для перевода игрового текста в реальном времени.
Использует ML Kit от Google — без API-ключей, без обязательного интернета.

### Возможности

- **Мгновенный перевод** — перевод текста между 50+ языками через Google ML Kit
- **Перевод экрана (OCR)** — нажмите плавающую кнопку, чтобы отсканировать и перевести экран игры
- **Работает офлайн** — скачайте языковые пакеты и переводите без интернета
- **Плавающий оверлей** — переводите текст, не выходя из игры
- **История переводов** — поиск, копирование и управление прошлыми переводами
- **Вход через Google** — синхронизация данных между устройствами через Firebase
- **Тёмная и светлая тема** — автоматическое или ручное переключение

## Скриншоты

> Скоро появятся после финальной полировки интерфейса.

## Стэк технологий

| Компонент | Технология |
|-----------|-----------|
| Язык | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Архитектура | MVVM + Clean Architecture |
| DI | Hilt |
| Перевод | Google ML Kit Translate |
| OCR | Google ML Kit Text Recognition |
| База данных | Room |
| Настройки | DataStore |
| Авторизация | Firebase Auth + Google Sign-In |
| Облако | Firebase Firestore + Cloud Functions |
| Платежи | YooMoney (ЮKassa) |
| Навигация | Navigation Compose |

## Архитектура

```
com.example/
├── data/
│   ├── local/          # Room база данных, DAO, сущности
│   ├── remote/         # Firebase, PaymentManager, AuthManager
│   └── repository/     # Реализации репозиториев
├── domain/
│   ├── model/          # Модели данных
│   └── usecase/        # Бизнес-логика
├── ui/
│   ├── theme/          # Цвета, типографика, тема
│   ├── screens/        # Главный, История, Оверлей, Подписка, Настройки
│   ├── components/     # Переиспользуемые компоненты UI
│   └── viewmodel/      # ViewModel
├── engine/             # Движок перевода, OCR-процессор
├── billing/            # Интеграция платежей
└── di/                 # Модули Hilt
```

## Быстрый старт

### Требования

- Android Studio Hedgehog (2023.1.1) или новее
- JDK 17
- Android SDK 34+
- Проект Firebase (для Auth и Firestore)

### Установка

1. **Клонируйте репозиторий**
   ```bash
   git clone https://github.com/GameLingoApp/GameLingo.git
   ```

2. **Добавьте конфигурацию Firebase**
   - Перейдите в [Firebase Console](https://console.firebase.google.com/)
   - Создайте проект или используйте существующий
   - Скачайте `google-services.json`
   - Поместите файл в папку `app/`

3. **Настройте окружение**
   - Скопируйте `.env.example` в `.env`
   - Добавьте ваши Firebase и YooMoney credentials

4. **Соберите и запустите**
   ```bash
   ./gradlew assembleDebug
   ```

## Тарифы

| План | Цена | Возможности |
|------|-------|----------|
| Free | 0 ₽ | 10 переводов в день, определение игровых терминов, только онлайн |
| Pro Monthly | 150 ₽/мес | Безлимитные переводы, без рекламы, оверлей экрана, приоритетная поддержка |
| Pro Yearly | 999 ₽/год | Всё из Monthly + офлайн-словарь + экспорт истории (скидка 44%) |

*Платежи через YooMoney (ЮKassa).*

## Контакты

- **Email**: support.gamelingo.app@gmail.com
- **Telegram**: [@GameLingoApp](https://t.me/GameLingoApp)
- **Сайт**: [gamelingo.netlify.app](https://gamelingo.netlify.app)

## Лицензия

Copyright (c) 2025 GameLingo. Все права защищены.

Это проприетарное программное обеспечение. Несанкционированное копирование, изменение, распространение или использование данного ПО строго запрещено.
