# Saral — Accessible Banking Assistant
## Project Prompt Document

---

## Problem Statement

Over **80 million people in India** live with visual impairment, low vision, or colour-blindness. An estimated **300 million** are above 60 years of age, many of whom also deal with reduced motor control or digital literacy barriers.

Standard banking apps require:
- Reading small text and dense UI grids
- Precise tapping on tiny buttons
- Navigating multi-level menus
- Interpreting colour-coded statuses (inaccessible to colour-blind users)
- Typing account numbers and amounts accurately

For visually impaired and elderly users these requirements make **independent digital banking effectively impossible**, forcing dependency on family members or physical branch visits for every transaction.

---

## Mission

> **Make every banking action achievable by voice alone — no reading, no typing, no help required.**

---

## Target Users

| Group | Size (India) | Key Barrier |
|---|---|---|
| Blind / total vision loss | 5 million | Cannot read any UI |
| Low vision (partial sight) | 75 million | Struggles with small text and contrast |
| Colour-blind | 300 million | Cannot interpret colour-coded status |
| Senior citizens (60+) | 300 million | Fine motor difficulty, digital unfamiliarity |
| Rural / low-literacy | 400 million | Unable to read app text |

---

## Solution: Saral

**Saral** (Hindi: "simple") is a voice-first accessible banking assistant available as an Android app and a web application. Every feature is operable by voice alone, and the visual UI is designed as a high-contrast, large-font fallback — not the primary interaction channel.

### Core Principles
1. **Voice is primary.** Every action can be completed by speaking.
2. **Touch is secondary.** Large tap targets (minimum 88dp / 44px) for users with partial sight or motor impairment.
3. **Never read-dependent.** The assistant speaks every result, confirmation, and prompt aloud.
4. **Zero colour dependency.** All status is communicated through shape, icon, and text — never colour alone.

---

## Features

### 1. Voice Banking
- Check account balance ("Check my balance")
- Transfer money end-to-end by voice ("Transfer 500 rupees to Rahul")
- Request cheque book ("Request cheque book")
- Query transactions ("Is my salary credited?", "Show last 5 transactions")

### 2. Post-Action Follow-up Prompts
After every action the app asks what the user wants to do next:
- After balance: *"Say main menu to go home, or tell me what you'd like to do next."*
- After cheque book: same
- After transactions: *"Would you like to hear more transactions, or say main menu to go back home?"*
- After transfer: *"Say Done or Main Menu to return home."*

### 3. Beneficiary Selection — Tap or Voice
Beneficiaries are displayed as large tappable cards AND can be selected by saying the name. Both paths lead to the same voice-guided amount + confirmation flow.

### 4. Automatic Fingerprint Authentication
When a transfer is confirmed, the fingerprint scanner activates automatically — no button tap needed. The user simply rests their finger on the sensor. This eliminates motor-precision requirements at the most critical step.

### 5. Session Timeout
3-minute inactivity timer with a 30-second spoken warning: *"Your session will expire in 30 seconds. Tap or speak to continue."* On expiry, the user is returned to the auth screen.

### 6. Accessibility Design
| Requirement | Implementation |
|---|---|
| WCAG AAA contrast | Navy `#081320` + white text (contrast 15.8:1) |
| Colour-blind safe | Teal for success, orange for error, amber for accent |
| Minimum font size | 18sp Android / 18px web base |
| In-app font scaling | 5 steps (Smaller to Largest) via Settings |
| Screen reader | Full `contentDescription` + ARIA labels on every interactive element |
| Zoom | A− / A+ controls accessible at all times on the home screen |
| Fixed mic button | Mic stays anchored at the screen bottom on all screens — never scrolls away |

### 7. Multilingual
English and Hindi voice input/output, switchable in Settings.

---

## Technical Architecture

### Android (Kotlin / Jetpack Compose)
```
MainActivity
├── BiometricPrompt (auto-triggered)
├── SpeechRecognizerManager (voice input)
├── TextToSpeechManager (voice output)
├── HapticManager (vibration feedback)
└── SaralNavGraph
    ├── SplashScreen
    ├── AuthScreen         ← BiometricPrompt
    ├── HomeScreen         ← HomeViewModel (Hilt)
    │   └── Fixed mic bar (always at bottom)
    ├── TransferScreen     ← TransferViewModel (Hilt)
    │   └── Fixed mic bar + auto-biometric LaunchedEffect
    └── SettingsScreen     ← font scale, language, voice, haptics
```

**Key patterns:**
- MVVM with `StateFlow` for UI state
- `SharedFlow` for navigation events
- Synchronous callback pattern (`speakCallback`, `navigateCallback`) for testable side-effects
- `CompositionLocalProvider(LocalDensity)` for runtime font scaling
- `LaunchedEffect(step)` to auto-trigger biometric when step reaches `AwaitingBiometric`

### Web (HTML + CSS + Vanilla JS)
```
index.html
├── Splash Screen
├── Auth Screen      ← auto-speaks welcome, waits for fingerprint
├── Home Screen
│   ├── home-scroll-content (scrollable)
│   └── mic-bar (fixed bottom — never moves)
├── Transfer Screen
│   ├── home-scroll-content (scrollable)
│   └── mic-bar (fixed bottom, hidden during biometric)
└── Settings Screen
```

**Key patterns:**
- Web Speech API for voice input/output
- State machine (`currentScreen`, `currentTransferPhase`, flags) for flow control
- `awaitingPostTransactionChoice`, `awaitingSimpleFollowUp` flags for multi-turn dialogue
- Auto-biometric: `setTimeout(executeWebTransfer, 2200)` on entering biometric phase
- CSS flex column layout: `home-scroll-content` (flex: 1, overflow-y: auto) + `mic-bar` (flex-shrink: 0)

---

## Test Coverage

| Suite | Tests | Failures |
|---|---|---|
| HomeViewModelTest | 55 | 0 |
| TransferViewModelTest | 320 | 0 |
| AuthViewModelTest | 18 | 0 |
| Domain / UseCases | 46 | 0 |
| **Total** | **439** | **0** |

---

## Release History

| Version | Key Changes |
|---|---|
| v1.0 | Initial voice banking app |
| v3.0 | Navigation improvements |
| v5.0 | Biometric auth in transfer flow |
| v6.0 | Full WCAG redesign, session timeout, beneficiary tap, voice navigation post-transfer, transaction follow-up, logo redesign |
| v7.0 | Balance and cheque book post-action follow-up, 15 new tests |
| v8.0 | Fixed mic button (never scrolls), auto-trigger fingerprint (no button tap), 6 new tests |

---

## Prompts Used to Build This App

The following prompt patterns drove each major feature:

### Accessibility Redesign
> *"UI Redesign Instructions for Accessibility: WCAG AAA contrast, 18sp minimum fonts, colour-blind safe palette (no red/green distinction), zoom controls, ARIA labels, screen reader compatibility, clean layout for blind and low-vision users."*

### Voice Navigation
> *"After transfer complete, voice control should return to main menu. Say Done or Main Menu."*

### Session Timeout
> *"Any timeout or session expiry? 3-minute idle timeout with 30-second warning."*

### Beneficiary Tap
> *"Beneficiary list is displayed on screen, but only option to say voice — add tap selection too."*

### Transaction Follow-up
> *"After reading transactions, ask if we want to go back to the main menu or want another transaction's details."*

### Balance / Cheque Follow-up
> *"After check balance, flow should ask if we want to go back to main menu. Same for cheque book."*

### Fixed Mic Button
> *"Mic button is moving in every screen — fix button in all screens at bottom."*

### Auto Biometric
> *"During transfer of money there is a button to authenticate via fingerprint. Instead of tapping on that button, flow should directly go to fingerprint authentication."*

---

## Impact

| Metric | Value |
|---|---|
| Target users in India | 80M+ visually impaired, 300M+ elderly |
| Screens operable by voice alone | 5 / 5 (100%) |
| WCAG compliance | Level AAA |
| Minimum tap target | 88dp (2× Android recommended 44dp) |
| Languages supported | English, Hindi |
| Authentication method | Biometric (fingerprint) — auto-triggered |
| Internet dependency | None (offline-capable mock; API-ready) |
