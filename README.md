# Saral - Voice-First Banking Accessibility Layer

## The Problem

Over 8 million visually impaired people in India depend on banking apps that were designed for sighted users. Existing apps rely on small text, complex navigation, and visual confirmations that are inaccessible without screen readers. Even with Android TalkBack, most banking apps have unlabeled buttons, inaccessible menus, and no voice-driven workflows. The result: visually impaired users must depend on family members or branch visits for basic banking tasks like checking a balance or sending money.

## What Saral Does

Saral (Hindi for "simple") is a **voice-first accessibility layer** that can be integrated into any Indian banking app. It is **not** a standalone bank. It is an **SDK / feature module** that wraps around existing banking functionality (SBI, HDFC, ICICI, etc.) and makes it fully operable through voice commands, spoken feedback, and haptic cues.

A user opens the app, authenticates with a fingerprint, and then speaks naturally: "How much money do I have?", "Send 500 rupees to Rahul", or "Request cheque book." Saral understands the intent, executes the action, and confirms everything aloud. No screen reading required.

### Who It Helps

- Visually impaired and blind users who need independent access to banking
- Elderly users who struggle with small text and complex app navigation
- Low-literacy users who can speak commands but cannot read UI text
- Any user who prefers hands-free, voice-driven banking

## Features

### Voice Interaction
- **Voice input** via microphone with natural language understanding
- **Voice output** that speaks every response, confirmation, and error aloud
- **Text input fallback** for situations where voice is unavailable or impractical
- **Bilingual support** for English and Hindi (`en-IN` / `hi-IN`)
- **Adjustable speech speed** from 0.5x to 1.5x

### Banking Operations
- **Check Balance** — "Check my balance", "How much money in my account", "Mera balance batao"
- **Transfer Money** — "Transfer 500 rupees to Rahul", "Send 1000 to Priya", "Pay Amit 200"
- **Transfer Confirmation** — Speaks the amount and recipient, waits for "Yes" / "No" before executing
- **Cheque Book Request** — "Request cheque book", "Order cheque book"
- **Recent Transactions** — "Show recent transactions", "Mini statement", "My spending"
- **Help** — "Help", "What can you do", "Madad"

### Accessibility
- **High-contrast dark UI** with navy background (#0A1628) and bright accent blue (#4DA8FF), meeting WCAG AA contrast ratios
- **Large fonts** with a minimum of 18sp across all text
- **Haptic feedback** with distinct vibration patterns for tap (50ms), success (double pulse), and error (long double pulse)
- **TalkBack-compatible** with `contentDescription` on every interactive element
- **Semantic labels** describing button states (e.g., "Microphone active. Tap to stop listening.")

### Security
- **Biometric authentication** using Android BiometricPrompt (fingerprint)
- **Demo bypass** when biometric hardware is unavailable (emulators, devices without fingerprint sensor)
- **Spoken confirmation required** before any money transfer is executed
- **No passwords, API keys, tokens, or secrets** anywhere in the codebase
- **No network calls** to external servers (all data is mock / in-memory)
- **No persistent storage** of sensitive data
- **Account numbers are never spoken in full** — only last 4 digits

## How the Core Logic Works

### Voice Intent Engine

When a user speaks (or types) a command, it flows through this pipeline:

```
User Speech  -->  SpeechRecognizer  -->  Raw Text  -->  VoiceIntentParser  -->  Intent  -->  ViewModel  -->  UseCase  -->  Repository
                                                                                                              |
                                                                                                        Spoken Response
```

1. **Speech capture**: Android `SpeechRecognizer` (or Web Speech API in the browser) converts audio to text
2. **Intent parsing**: `VoiceIntentParser` uses regex pattern matching against the text to determine the user's intent. Each intent category has multiple patterns to handle varied phrasing:
   - Balance patterns: "check balance", "how much money", "my balance", "tell me balance", etc.
   - Transfer patterns: "transfer", "send money", "pay", "send rupees", etc. The parser also extracts the **amount** (any number in the text) and **recipient** (word after "to" or "for")
   - Confirmation patterns: exact matches for "yes", "ok", "haan", "ji" (affirmative) or "no", "cancel", "nahi" (negative)
3. **Intent handling**: `HomeViewModel` routes each intent to the appropriate use case
4. **Response**: The result is both displayed on screen and spoken aloud via `TextToSpeechManager`

### Transfer Workflow (the most complex flow)

```
User: "Transfer 500 rupees to Rahul"
  |
  v
Parser extracts: amount=500, recipient="Rahul"
  |
  v
ViewModel sets awaitingTransferConfirmation=true, stores pending amount and recipient
  |
  v
TTS speaks: "You are transferring 500 rupees to Rahul. Do you want to continue?"
  |
  v
User: "Yes"
  |
  v
Parser returns: ConfirmYes
  |
  v
ViewModel checks awaitingTransferConfirmation==true, calls TransferMoneyUseCase
  |
  v
Repository processes transfer, deducts from balance, returns TransferResult with txnId
  |
  v
TTS speaks: "Transfer successful. Transaction reference number TXN847291."
```

If the user says "No" or "Cancel" instead, the pending transfer is cleared and the app responds "Transfer cancelled."

### Navigation Flow

```
SplashScreen (3s, speaks "Welcome to Saral")
    |
    v
AuthScreen (fingerprint prompt, or demo bypass on Cancel)
    |
    v
HomeScreen (voice assistant — main interaction screen)
    |
    v (settings gear icon)
SettingsScreen (language, speech speed, haptic toggle)
```

## Project Structure

```
saral/
├── app/                                        # Android application (primary)
│   ├── build.gradle.kts                        # App-level Gradle config
│   └── src/main/
│       ├── AndroidManifest.xml                 # Permissions & activity registration
│       ├── java/com/saral/app/
│       │   ├── MainActivity.kt                 # Entry point: biometric, voice init, Compose host
│       │   ├── SaralApplication.kt             # @HiltAndroidApp application class
│       │   ├── accessibility/
│       │   │   └── HapticManager.kt            # Vibration patterns (short, success, error)
│       │   ├── data/
│       │   │   ├── mock/
│       │   │   │   └── MockBankingRepository.kt  # Fake banking data (balance, transactions, transfers)
│       │   │   └── repository/
│       │   │       └── BankingRepository.kt      # Interface — swap mock for real API here
│       │   ├── di/
│       │   │   └── AppModule.kt                # Hilt module binding MockRepo -> BankingRepository
│       │   ├── domain/
│       │   │   ├── models/
│       │   │   │   ├── BankAccount.kt          # bankName, accountLast4, balance
│       │   │   │   ├── ChequeBookRequest.kt    # status, estimatedDeliveryDays
│       │   │   │   ├── Transaction.kt          # id, description, amount, date, CREDIT/DEBIT
│       │   │   │   ├── TransferResult.kt       # success, txnId, amount, recipientName
│       │   │   │   └── VoiceIntent.kt          # Sealed class: CheckBalance, TransferMoney, etc.
│       │   │   └── usecases/
│       │   │       ├── GetBalanceUseCase.kt
│       │   │       ├── GetRecentTransactionsUseCase.kt
│       │   │       ├── RequestChequeBookUseCase.kt
│       │   │       └── TransferMoneyUseCase.kt
│       │   ├── navigation/
│       │   │   └── SaralNavGraph.kt            # SPLASH -> AUTH -> HOME -> SETTINGS
│       │   ├── presentation/
│       │   │   ├── auth/
│       │   │   │   └── AuthScreen.kt           # Pulsing fingerprint icon, auto-triggers biometric
│       │   │   ├── home/
│       │   │   │   ├── HomeScreen.kt           # Mic button, response card, transactions, recent cmds
│       │   │   │   └── HomeViewModel.kt        # Intent routing, transfer confirmation state machine
│       │   │   ├── settings/
│       │   │   │   └── SettingsScreen.kt       # Language, speech speed slider, haptic toggle
│       │   │   └── splash/
│       │   │       └── SplashScreen.kt         # Animated fade-in, speaks welcome message
│       │   ├── ui/theme/
│       │   │   ├── Color.kt                    # Navy/blue/green/red palette
│       │   │   ├── Theme.kt                    # SaralTheme composable (dark Material3)
│       │   │   └── Type.kt                     # Typography (display, headline, body, label)
│       │   └── voice/
│       │       ├── SpeechRecognizerManager.kt  # Android SpeechRecognizer wrapper
│       │       ├── TextToSpeechManager.kt      # Android TTS wrapper with completion callbacks
│       │       └── VoiceIntentParser.kt        # Regex-based NLU: text -> VoiceIntent
│       └── res/
│           ├── drawable/
│           │   └── ic_launcher_foreground.xml   # Blue circle vector icon
│           ├── mipmap-anydpi-v26/
│           │   └── ic_launcher.xml              # Adaptive icon definition
│           ├── mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/
│           │   └── ic_launcher.png              # Launcher icons at each density
│           └── values/
│               ├── colors.xml                   # XML color resources
│               ├── ic_launcher_background.xml   # Launcher icon background color
│               ├── strings.xml                  # App strings (welcome, auth, errors)
│               └── themes.xml                   # Material NoActionBar theme with navy status bar
│
├── web/                                        # Web prototype (for quick testing)
│   ├── index.html                              # 4 screens: splash, auth, home, settings
│   ├── styles.css                              # Dark navy theme, responsive, accessibility CSS
│   └── app.js                                  # Voice recognition, intent parser, UI logic
│
├── releases/                                   # Pre-built APK releases
│   └── apk_saral_1.0/
│       ├── saral-v1.0-debug.apk               # Ready-to-install debug APK (18 MB)
│       └── VERSION_INFO.txt                    # Build metadata and install instructions
│
├── build.gradle.kts                            # Root Gradle: AGP 8.7.3, Kotlin 2.1.0, Hilt 2.53.1
├── settings.gradle.kts                         # Gradle settings, Google/Maven repos
├── gradle.properties                           # JVM args, AndroidX, compileSdk suppression
├── gradle/wrapper/
│   ├── gradle-wrapper.jar                      # Gradle wrapper binary
│   └── gradle-wrapper.properties               # Gradle 8.11.1 distribution URL
├── gradlew                                     # Unix Gradle wrapper script
├── gradlew.bat                                 # Windows Gradle wrapper script
├── local.properties                            # Android SDK path (machine-specific, not committed)
└── README.md
```

## Tech Stack

### Android App

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.1.0 |
| UI framework | Jetpack Compose + Material 3 | BOM 2024.12.01 |
| Architecture | MVVM + Clean Architecture | — |
| Dependency injection | Hilt (Dagger) | 2.53.1 |
| Annotation processing | KSP | 2.1.0-1.0.29 |
| Navigation | Jetpack Navigation Compose | 2.8.5 |
| Voice input | Android SpeechRecognizer | Platform API |
| Voice output | Android TextToSpeech | Platform API |
| Authentication | AndroidX BiometricPrompt | 1.1.0 |
| State management | ViewModel + StateFlow | Lifecycle 2.8.7 |
| Build system | Gradle | 8.11.1 |
| Android Gradle Plugin | AGP | 8.7.3 |
| Min SDK | Android 8.0 (Oreo) | API 26 |
| Target SDK | Android 15 | API 35 |
| Compile SDK | — | API 36 |
| JDK | JBR / OpenJDK | 17 |

### Web Prototype

| Layer | Technology |
|-------|-----------|
| Voice input | Web Speech API (`webkitSpeechRecognition`) |
| Voice output | Web Speech Synthesis API |
| Intent parsing | Regex-based NLU (mirrors Android logic) |
| UI | Vanilla HTML + CSS + JavaScript |
| Fonts | Google Fonts (Inter) + Material Icons |
| Hosting | Any static server (`npx serve`, Python `http.server`, etc.) |

## Getting Started

### Option 1: Install the Pre-Built APK (Fastest)

A ready-to-install debug APK is included in the repository.

**Location:** `releases/apk_saral_1.0/saral-v1.0-debug.apk` (18 MB)

**Steps:**

1. Transfer `saral-v1.0-debug.apk` to your Android phone (via USB, email, Google Drive, etc.)
2. On the phone, open **Settings > Security** (or **Settings > Apps > Special app access**) and enable **Install from Unknown Sources** for your file manager
3. Open the APK file and tap **Install**
4. Open **Saral** from the app drawer
5. The splash screen will speak "Welcome to Saral"
6. The fingerprint prompt appears:
   - **If your device has a fingerprint sensor**: authenticate with your fingerprint
   - **If not** (or on an emulator): tap **Cancel** — the app enters demo mode automatically
7. Grant **microphone permission** when prompted
8. Tap the blue mic button and speak a command, e.g., "Check my balance"

**Requirements:**
- Android 8.0 (Oreo) or later — API 26+
- A microphone (built-in works)
- Internet is NOT required (all data is local)

### Option 2: Run the Web Prototype (No Android Needed)

The web prototype mirrors all voice and UI features of the Android app and runs in any modern browser.

**Prerequisites:**
- **Google Chrome** (recommended — best Web Speech API support; also works in Edge)
- **Node.js** installed — [download here](https://nodejs.org/) (for `npx serve`)
- A **microphone** (optional — text input fallback is always available)

**Steps:**

```bash
# 1. Open a terminal and navigate to the project root
cd saral

# 2. Start a local static server on port 3000
npx serve web -l 3000
```

3. Open **http://localhost:3000** in Google Chrome
4. The splash screen speaks "Welcome to Saral", then moves to the auth screen
5. Tap **"Tap to Authenticate (Demo)"** — the app greets you as Jayesh
6. You are now on the home screen. You can interact three ways:
   - **Voice**: Tap the blue mic button and speak naturally
   - **Text**: Type a command in the text box at the bottom and press Enter
   - **Quick buttons**: Tap any preset button (Check Balance, Transfer Money, etc.)

**Alternative server (no Node.js required):**

```bash
# Using Python 3
cd saral/web
python -m http.server 3000

# Then open http://localhost:3000 in Chrome
```

**Granting microphone permission in Chrome:**

When you tap the mic button for the first time, Chrome asks for microphone permission — click **Allow**.

If you accidentally denied it:
1. Click the **lock icon** (or tune icon) in the address bar, left of the URL
2. Find **Microphone** and set it to **Allow**
3. Reload the page (Ctrl+R / Cmd+R)

Alternatively, go to `chrome://settings/content/microphone` and add `http://localhost:3000` to the Allow list.

### Option 3: Build from Source in Android Studio

**Prerequisites:**
- **Android Studio** Hedgehog (2023.1) or newer
- **JDK 17** (bundled with Android Studio as JBR)
- **Android SDK** with platform API 35 or higher installed (via SDK Manager)
- A **physical Android device** (recommended for fingerprint + microphone) or an emulator

**Steps:**

1. Clone or download the project:
   ```bash
   git clone <repository-url>
   cd saral
   ```

2. Open the project in Android Studio:
   - **File > Open** and select the `saral` root folder
   - Android Studio will detect the Gradle project and begin syncing

3. Wait for Gradle sync to complete:
   - The Gradle wrapper (`gradlew`) is included — no manual Gradle installation needed
   - If SDK 35/36 is missing, Android Studio will prompt you to install it via SDK Manager
   - If `local.properties` is missing, Android Studio generates it automatically with your SDK path

4. Connect a device or start an emulator:
   - **Physical device** (recommended): Enable **USB Debugging** in Developer Options, connect via USB
   - **Emulator**: Create an AVD with API 26+ in **Tools > Device Manager**

5. Run the app:
   - Select the **app** run configuration in the toolbar
   - Click the green **Run** button (or press Shift+F10)
   - The app will build, install, and launch on your device

6. Grant permissions when prompted:
   - **Microphone** (RECORD_AUDIO) — required for voice commands
   - Biometric and vibration permissions are granted automatically

**Building the APK from the command line:**

```bash
# Unix/macOS
cd saral
./gradlew assembleDebug

# Windows
cd saral
gradlew.bat assembleDebug
```

The output APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

**Android permissions declared in the manifest:**

| Permission | Purpose |
|-----------|---------|
| `RECORD_AUDIO` | Voice command input via SpeechRecognizer |
| `VIBRATE` | Haptic feedback on actions and confirmations |
| `USE_BIOMETRIC` | Fingerprint authentication |
| `INTERNET` | Required by Gradle/dependencies; the app makes no network calls at runtime |

## Voice Commands Reference

The intent parser understands natural conversational phrasing in English and Hindi. You do not need to say exact keywords.

| Intent | Example Phrases | What Happens |
|--------|----------------|-------------|
| **Check Balance** | "Check my balance", "How much money in my account", "Tell me my balance", "Mera balance batao", "What's in my account" | Speaks your account balance: bank name, last 4 digits, and amount |
| **Transfer Money** | "Transfer 500 rupees to Rahul", "Send 1000 to Priya", "Pay Amit 200" | Asks for spoken confirmation before executing. Says amount and recipient. |
| **Confirm Transfer** | "Yes", "Confirm", "Go ahead", "Haan", "Ji", "Ok" | Executes the pending transfer and speaks a transaction reference number |
| **Cancel Transfer** | "No", "Cancel", "Nahi", "Stop", "Ruko" | Cancels the pending transfer |
| **Cheque Book** | "Request cheque book", "Order cheque book", "New cheque" | Registers the request and speaks estimated delivery (5 working days) |
| **Transactions** | "Show recent transactions", "Mini statement", "My spending", "Transaction history" | Displays a color-coded list and narrates the most recent transaction |
| **Help** | "Help", "What can you do", "Madad", "Customer care" | Speaks the list of available commands |

## Mock Data

All banking data is hardcoded in-memory. No real banking integration exists. No sensitive data is stored, transmitted, or used.

| Data | Value |
|------|-------|
| **Account** | SBI Savings, ending **7845**, balance **Rs. 25,000** |
| **Transfer contacts** | Rahul, Amit, Priya, Neha |
| **Transactions** | Reliance Fresh (-1,200), Salary Credit (+45,000), Electricity Bill (-2,350), Amazon (-899), UPI from Amit (+500) |
| **Cheque book delivery** | 5 working days (simulated) |

The mock repository (`MockBankingRepository.kt`) includes simulated delays (500ms-1000ms) to mimic real API latency.

## Demo Script for Judges

### Path A: Using the Pre-Built APK (recommended for Android demo)

1. Install `releases/apk_saral_1.0/saral-v1.0-debug.apk` on a phone
2. **Open the app** — Splash screen speaks: "Welcome to Saral"
3. **Fingerprint auth** — Use fingerprint, or tap Cancel for demo mode. Hear: "Welcome Jayesh"
4. **Tap the mic** and say **"Check my balance"** — Hear: "In your SBI savings account ending with 7845, you have 25,000 rupees"
5. **Tap the mic** and say **"Transfer 500 rupees to Rahul"** — Hear: "You are transferring 500 rupees to Rahul. Do you want to continue?"
6. **Tap the mic** and say **"Yes"** — Hear: "Transfer successful. Transaction reference number TXN..."
7. **Tap the mic** and say **"Request cheque book"** — Hear: "Cheque book request registered... 5 working days"
8. **Tap the mic** and say **"Recent transactions"** — Transaction list appears with color coding (red=debit, green=credit) and narration
9. **Tap the settings gear** — Show language toggle (English/Hindi), speech speed slider, haptic feedback toggle

### Path B: Using the Web Prototype (quickest setup)

1. Run `npx serve web -l 3000` and open http://localhost:3000 in Chrome
2. **Splash screen** — Speaks "Welcome to Saral", transitions to auth
3. **Tap "Authenticate"** — Speaks "Welcome Jayesh"
4. **Type "How much money in my account"** in the text box — Hears balance
5. **Type "Send 1000 rupees to Priya"** — Confirmation prompt
6. **Type "Yes"** — Transfer success with reference number
7. **Tap "Transactions" quick button** — Color-coded list + voice narration
8. **Open Settings** — Toggle language, adjust speech speed

## Architecture Decisions

| Decision | Rationale |
|----------|-----------|
| **MVVM + Clean Architecture** | Separates voice logic from UI and data. Use cases can be tested independently. |
| **Hilt dependency injection** | `BankingRepository` is an interface — swap `MockBankingRepository` for a real API implementation by changing one line in `AppModule.kt` |
| **Regex-based intent parsing** | No LLM dependency, no internet required, instant response, works offline. Sufficient for the fixed set of banking commands. |
| **StateFlow for UI state** | Single source of truth for the home screen. The `awaitingTransferConfirmation` flag prevents accidental double-transfers. |
| **FragmentActivity base class** | Required by `BiometricPrompt`. Extends `ComponentActivity` so Compose `setContent {}` works normally. |
| **Compose-first UI** | All screens use Jetpack Compose with semantic accessibility modifiers (`contentDescription`, `Role.Button`). |
| **Dark navy theme** | High contrast for low-vision users. White/blue text on dark background. All text >= 18sp. |
| **Web prototype mirrors Android** | Same intent patterns, same mock data, same UI flow. Allows testing without Android Studio. |
| **No persistent storage** | Demo app stores nothing to disk. No SharedPreferences, no Room database, no files. |

## Troubleshooting

### Android App / APK

| Issue | Solution |
|-------|---------|
| **"App not installed"** when installing APK | Enable **Install from Unknown Sources** in Settings > Security for your file manager. On Android 8+, this is per-app. |
| **Fingerprint prompt does not appear** | The app auto-detects if biometric hardware is unavailable and enters demo mode. On an emulator, tap Cancel or just wait. |
| **No voice output** | Ensure your device has a TTS engine installed. Most devices ship with Google TTS. Check **Settings > Accessibility > Text-to-Speech**. |
| **Voice recognition says "No speech detected"** | Speak clearly within 1-2 seconds of tapping the mic. Ensure RECORD_AUDIO permission is granted (check **Settings > Apps > Saral > Permissions**). |
| **Gradle sync fails with "SDK not found"** | Open **Tools > SDK Manager** in Android Studio and install **Android SDK Platform 35** (or higher). |
| **Gradle sync fails with "local.properties missing"** | Android Studio generates this automatically on first sync. If it doesn't, create `local.properties` in the project root with: `sdk.dir=/path/to/Android/Sdk` |
| **Build error about compileSdk** | The project uses `compileSdk = 36`. Install the latest SDK platform via SDK Manager. The `gradle.properties` file includes `android.suppressUnsupportedCompileSdk=36` to handle AGP compatibility. |

### Web Prototype

| Issue | Solution |
|-------|---------|
| **"Microphone access denied"** | Click the lock icon in Chrome's address bar > set Microphone to Allow > reload the page. Or visit `chrome://settings/content/microphone`. |
| **Voice button does nothing** | Use **Google Chrome** or **Microsoft Edge**. Firefox and Safari have limited or no Web Speech API support. |
| **"No microphone found"** | Connect a microphone. Laptop built-in mics work. Check your system audio input settings. |
| **"Network error" on voice** | Chrome's Web Speech API sends audio to Google servers for processing. Ensure you have an internet connection. |
| **No audio output** | Check your system volume. Ensure your browser tab is not muted (right-click the tab > Unmute). |
| **`npx serve` not found** | Install Node.js from [nodejs.org](https://nodejs.org/). Then run `npx serve web -l 3000`. |
| **Port 3000 already in use** | Use a different port: `npx serve web -l 3001` and open `http://localhost:3001`. |

## License

MIT — Built for ESF AI Hackathon 2025
