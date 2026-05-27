# Saral - Voice-First Banking Accessibility Layer

An Android application that demonstrates how Indian banking apps can integrate an accessibility-first module to provide inclusive banking experiences for visually impaired users. A web prototype is also included for quick UI and voice testing without Android Studio.

## What is Saral?

Saral is **not** a standalone bank. It is an **accessibility SDK / feature layer** that can be integrated into existing Indian banking apps (SBI, HDFC, ICICI, etc.) to enable voice-guided, accessibility-compliant banking.

### Key Features

- **Voice-First Interaction** — Every action speaks aloud, accepts voice input, and gives audio confirmations
- **Natural Language Understanding** — Understands varied phrasing like "how much money in my bank account", "tell me my balance", "check my balance for SBI account"
- **Biometric Authentication** — Fingerprint-based login using Android BiometricPrompt API
- **Check Balance** — Voice-activated account balance inquiry
- **Transfer Money** — Voice-guided fund transfer with spoken confirmation before execution
- **Cheque Book Request** — Voice-activated cheque book ordering
- **Recent Transactions** — Voice narration of transaction history
- **Emergency Help** — Quick access to customer support
- **Haptic Feedback** — Vibration cues for listening state changes and confirmations
- **High Contrast UI** — Dark navy theme with large fonts (minimum 18sp) and WCAG-compliant contrast ratios
- **TalkBack Compatible** — Proper semantic labels on all interactive elements
- **Multi-language Support** — English and Hindi voice input/output

## Tech Stack

### Android App (Primary)

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Navigation | Jetpack Navigation Compose |
| Voice Input | Android SpeechRecognizer |
| Voice Output | Android TextToSpeech (TTS) |
| Auth | Android BiometricPrompt |
| State | ViewModel + StateFlow |

### Web Prototype (for quick testing)

| Layer | Technology |
|-------|-----------|
| Voice Input | Web Speech API (SpeechRecognition) |
| Voice Output | Web Speech Synthesis API |
| Intent Parsing | Regex-based NLU (same logic as Android) |
| UI | Vanilla HTML/CSS/JS |
| Hosting | Local static server (npx serve) |

## Project Structure

```
saral/
├── app/                          # Android application
│   └── src/main/java/com/saral/app/
│       ├── data/
│       │   ├── mock/             # MockBankingRepository with fake data
│       │   └── repository/       # BankingRepository interface
│       ├── di/                   # Hilt dependency injection modules
│       ├── domain/
│       │   ├── models/           # BankAccount, Transaction, VoiceIntent, etc.
│       │   └── usecases/         # GetBalance, TransferMoney, RequestChequeBook, etc.
│       ├── presentation/
│       │   ├── auth/             # Biometric authentication screen
│       │   ├── home/             # Voice assistant home (main screen)
│       │   ├── settings/         # Accessibility settings
│       │   └── splash/           # Splash screen with welcome message
│       ├── navigation/           # NavGraph + route definitions
│       ├── voice/                # TTS manager, speech recognizer, intent parser
│       ├── accessibility/        # Haptic feedback manager
│       └── ui/theme/             # Colors, typography, Material theme
├── web/                          # Web prototype for quick testing
│   ├── index.html                # Main HTML structure
│   ├── styles.css                # Dark theme CSS with accessibility styles
│   └── app.js                    # Voice recognition, intent parser, UI logic
├── build.gradle.kts              # Root Gradle config
├── settings.gradle.kts           # Gradle settings
└── README.md
```

## Quick Start — Web Prototype

The web prototype lets you test all voice and UI features instantly in a browser, without needing Android Studio.

### Prerequisites
- **Google Chrome** (recommended — best Web Speech API support)
- **Node.js** (for `npx serve`) — [download](https://nodejs.org/)
- A **microphone** (optional — text input fallback is available)

### Launch

```bash
# Navigate to the project root
cd saral

# Start a local static server
npx serve web -l 3000
```

Then open **http://localhost:3000** in Google Chrome.

### Microphone Permission

When you tap the mic button for the first time, Chrome will ask for microphone permission — click **Allow**.

If you accidentally denied it:
1. Click the **lock/tune icon** in the address bar (left of the URL)
2. Set **Microphone** to **Allow**
3. Reload the page

### Three Ways to Give Commands

1. **Voice** — Tap the blue mic button and speak naturally
2. **Text** — Type in the text box at the bottom and press Enter
3. **Quick buttons** — Tap any preset command button (Check Balance, Transfer Money, etc.)

All three methods feed into the same intent parser and produce identical results.

## Setup — Android App (Primary Target)

> The Android app is the primary deliverable. The web prototype is for rapid testing.

### Prerequisites
- Android Studio Hedgehog (2023.1) or newer
- JDK 17
- Android SDK 35
- A physical Android device (recommended for biometric + microphone testing)

### Build & Run

1. Clone or copy the project
2. Open in Android Studio
3. Sync Gradle (Android Studio will auto-generate the Gradle wrapper if missing)
4. Connect a physical device or start an emulator
5. Run the `app` configuration

### Permissions Required
- `RECORD_AUDIO` — Voice commands
- `VIBRATE` — Haptic feedback
- `USE_BIOMETRIC` — Fingerprint authentication

## Voice Commands

The intent parser understands natural conversational phrasing, not just exact keywords:

| Say Something Like... | What Happens |
|----------------------|-------------|
| "Check my balance" / "How much money in my bank account" / "Tell me my balance" | Reads account balance aloud |
| "Transfer 500 rupees to Rahul" / "Send 1000 to Priya" / "Pay Amit 200" | Initiates transfer with confirmation |
| "Yes" / "Confirm" / "Go ahead" / "Haan" | Confirms pending transfer |
| "No" / "Cancel" / "Nahi" / "Ruko" | Cancels pending transfer |
| "Request cheque book" / "Order cheque book" | Registers cheque book request |
| "Show recent transactions" / "My spending" / "Mini statement" | Lists and narrates recent transactions |
| "Help" / "What can you do" / "Madad" | Shows available commands |

## Demo Script for Judges

### Using Web Prototype (quickest)

1. Run `npx serve web -l 3000` and open http://localhost:3000 in Chrome
2. **Splash screen** — Voice: "Welcome to Saral"
3. **Tap "Authenticate"** → "Welcome Jayesh"
4. **Type or say "How much money in my account"** → Hears balance: "25,000 rupees"
5. **Type or say "Send 1000 rupees to Priya"** → Confirmation prompt
6. **Type or say "Yes"** → Transfer success with reference number
7. **Tap "Transactions" button** → Color-coded transaction list + voice narration
8. **Open Settings** → Language, speech speed, haptic toggles

### Using Android App

1. **Open app** — Splash screen with voice: "Welcome to Saral"
2. **Fingerprint auth** — Authenticate → "Welcome Jayesh"
3. **Tap mic** → Say **"Check my balance"** → Hears balance response
4. **Tap mic** → Say **"Transfer 500 rupees to Rahul"** → Confirmation prompt
5. **Tap mic** → Say **"Yes"** → Transfer success with reference number
6. **Tap mic** → Say **"Request cheque book"** → Cheque book confirmation
7. **Tap mic** → Say **"Recent transactions"** → Transaction list displayed + narrated
8. **Open Settings** → Show language, speech speed, haptic toggles

## Mock Data

All banking data is mocked locally. No real banking integration exists. No sensitive data is stored, transmitted, or hardcoded.

- **Account**: SBI Savings, ending 7845, balance Rs. 25,000
- **Contacts**: Rahul, Amit, Priya, Neha (for transfers)
- **Transactions**: Reliance Fresh, Salary Credit, Electricity Bill, Amazon, UPI

## Security Practices

- **No passwords, API keys, or secrets** in the codebase
- **No network calls** to external APIs (all data is mock/in-memory)
- **No persistent storage** of sensitive data
- Never speaks full account numbers (only last 4 digits)
- Requires biometric authentication (auto-bypass for demo when hardware unavailable)
- Requires spoken confirmation before any transfer
- External resources limited to Google Fonts CDN (fonts only)

## Architecture Decisions

- **Stateless**: No persistent storage of sensitive data — all mock, in-memory
- **Voice Intent Engine**: Regex + keyword matching (no LLM dependency, works offline)
- **Hilt DI**: Clean separation allows swapping mock repo for real API integration
- **Compose-first**: Fully declarative UI with accessibility semantics baked in
- **Web prototype mirrors Android**: Same intent patterns, same mock data, same UI flow

## License

MIT — Built for ESF AI Hackathon 2025
