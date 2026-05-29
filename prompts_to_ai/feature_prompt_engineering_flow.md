# Saral — Prompt Engineering Flow
## All prompts used to build features, in order of development

---

## 1. Initial App Brief

```
You are building an accessible banking app called "Saral" (Hindi for "simple").
Target users: blind/visually impaired, colour-blind, elderly, low-literacy —
people for whom standard banking apps are effectively unusable.

Core constraint: every single banking action must be completable by voice alone.
No reading required. No typing required. No help from another person required.

Platform: Android (Kotlin / Jetpack Compose) + Web (HTML/CSS/Vanilla JS).
Architecture: MVVM + Hilt DI + StateFlow on Android.

The app must:
- Speak every result, confirmation, and prompt aloud
- Accept voice input for all commands
- Use WCAG AAA contrast (minimum 7:1, target 15:1+)
- Never communicate status through colour alone
- Provide large tap targets (min 88dp Android / 44px web) as a secondary input
- Support font scaling (5 steps) and zoom controls
- Work in English and Hindi

Features to build in v1:
1. Voice-activated balance check
2. Voice-guided money transfer (beneficiary selection → amount → confirm → biometric)
3. Voice cheque book request
4. Transaction query ("Is my salary credited?", "Last 5 transactions")
5. Biometric login
```

---

## 2. Transfer Screen — Voice Interaction Spec

```
Build the money transfer screen as a voice-guided state machine.
The user never needs to see the screen clearly — the app talks them through
every step.

Steps in order:
  Step 1 — Beneficiary selection
    • Display saved beneficiaries as large cards (name + bank, min 88dp height)
    • User can either TAP a card OR say the beneficiary name
    • App speaks: "Who would you like to send money to?
                   You can say the name or tap a card."
    • On selection, app confirms aloud: "You selected [Name] at [Bank]."

  Step 2 — Amount entry
    • App speaks: "How much would you like to send?"
    • User speaks the amount: "five hundred" / "500 rupees"
    • App parses spoken number and confirms:
      "You said ₹500. Is that correct? Say yes to confirm or no to change."

  Step 3 — Confirmation
    • App speaks full summary:
      "You are about to transfer ₹500 to [Name] at [Bank] account ending [XXXX].
       Say confirm to proceed or cancel to go back."
    • On "confirm": proceed to biometric
    • On "cancel": return to Step 1 with spoken confirmation

  Step 4 — Biometric authentication
    • Fingerprint prompt auto-triggers — no button tap
    • App speaks: "Fingerprint authentication started.
                   Please hold your finger on the sensor."
    • On success: app speaks success + returns home
    • On failure: app speaks "Authentication failed. Please try again."
                  and re-presents the prompt

  Step 5 — Completion
    • App speaks: "₹500 successfully transferred to [Name].
                   Transaction reference: [ID].
                   Say Done or Main Menu to return home."

State machine: SelectingBeneficiary → EnteringAmount →
               ConfirmingTransfer → AwaitingBiometric → Completed
```

---

## 3. Accessibility Redesign

```
Redesign the entire app UI to meet WCAG 2.1 Level AAA.

Colour system:
  Background:  #081320  (Navy dark)
  Surface:     #0D1F35  (Navy card)
  Primary:     #FFFFFF  (White text — contrast 18.7:1 on background)
  Accent:      #FFB800  (Amber — contrast 11.5:1 on background)
  Success:     #00C48C  (Teal — never red/green distinction)
  Error:       #FF6B35  (Orange — not red, to not conflict with green/red colour blindness)
  Danger:      #FF6B35  (Orange)

Rules:
  - Every status must be readable without colour: use icons, labels, shapes
  - Minimum font size 18sp (Android) / 18px (web), never smaller
  - Minimum tap target 88dp (Android) / 44px (web)
  - Every interactive element needs contentDescription (Android) / aria-label (web)
  - Font scaling: expose 5-step slider in Settings (0.85x to 1.3x),
    apply via CompositionLocalProvider(LocalDensity) in Compose
  - Zoom controls (A− / A+) visible at all times on home screen

Apply to every screen: Splash, Auth, Home, Transfer, Settings.
```

---

## 4. Post-Transfer Voice Navigation

```
After a money transfer completes, the user needs to get back to the main menu
without reading anything.

Current behaviour: transfer screen just sits there after success.

Required behaviour:
  1. App speaks the success message including transaction reference
  2. App then speaks: "Say Done or Main Menu to return home."
  3. Mic stays active, listening for:
       "done" / "main menu" / "home" / "back" / "okay" / "exit"
  4. On any of those: navigate to HomeScreen and speak
     "Welcome back. How can I help you?"
  5. If the user says something else (a new command), parse it as a fresh intent

Implement as a flag: awaitingPostTransferChoice: Boolean
When flag is true, intercept onVoiceResult() before normal intent parsing.
```

---

## 5. Session Timeout

```
Add a security timeout for inactive sessions.

Specification:
  - Idle timer: 3 minutes (180 seconds) from last user interaction
  - What counts as interaction: any voice input, any tap on the screen
  - At 150 seconds (30 seconds remaining), speak a warning:
      "Your session will expire in 30 seconds.
       Tap the screen or say anything to stay logged in."
  - At 180 seconds: navigate to AuthScreen, speak:
      "Your session has expired for security. Please log in again."
  - Reset the timer on every user interaction

Android: use a coroutine Job in the ViewModel, cancel + restart on interaction.
Web: use clearTimeout / setTimeout, reset on click and voice input.
```

---

## 6. Beneficiary Tap Selection

```
The beneficiary list is shown on screen but the only way to select is by voice.
Add tap selection so users with partial sight can tap instead.

Requirements:
  - Each beneficiary card is already displayed — make it tappable
  - Tap = same outcome as saying the name (moves to EnteringAmount step)
  - Card must have clear visual tap feedback (ripple / highlight)
  - Accessibility: contentDescription = "Transfer to [Name], [Bank]"
  - No modal, no confirmation step — tap immediately selects and advances
  - The voice path must still work exactly as before (no regression)

Both tap and voice lead to the same next state: EnteringAmount.
```

---

## 7. Transaction Follow-up Dialogue

```
After reading transactions, the conversation should not dead-end.

Current: app reads the transactions then goes silent.

Required multi-turn flow:
  Turn 1: User says "Show last 5 transactions"
           → App reads them aloud, one by one
           → App ends with: "Would you like to hear more transactions,
                             or say main menu to go back home?"
           → Flag: awaitingPostTransactionChoice = true

  Turn 2 (option A): User says "more" / "5 more" / "next" / a number
           → Fetch and read the next N transactions
           → Repeat the follow-up prompt

  Turn 2 (option B): User says "main menu" / "home" / "done" / "back"
           → Navigate home, speak "How else can I help you?"
           → Clear flag

  Turn 2 (option C): User says a completely new command ("check balance")
           → Clear flag, parse as fresh intent, handle normally

State: awaitingPostTransactionChoice: Boolean
Intercept in onVoiceResult() before normal intent parsing when flag is true.
```

---

## 8. Balance & Cheque Book Follow-up

```
After checking balance or requesting a cheque book, ask what to do next.
Same pattern as transaction follow-up but simpler (no "more" branch).

Balance flow:
  App: "In your [Bank] savings account ending [XXXX],
        you have ₹[amount] available.
        Say main menu to go home, or tell me what you'd like to do next."
  Flag: awaitingSimpleFollowUp = true

Cheque book flow:
  App: "Your cheque book request has been placed.
        It will be delivered to your registered address in 5–7 working days.
        Say main menu to go home, or tell me what you'd like to do next."
  Flag: awaitingSimpleFollowUp = true

Follow-up handler (shared for both):
  - "main menu" / "home" / "done" / "back" / "okay" / "nothing" / "exit"
      → Respond: "How else can I help you? You can check balance,
                  transfer money, request a cheque book, or ask for help."
      → Clear flag, stay on home screen
  - Any other input
      → Clear flag, parse as a new intent

One flag (awaitingSimpleFollowUp) handles both features
because they have identical follow-up behaviour.
```

---

## 9. Fixed Mic Button (All Screens)

```
The mic button scrolls away when the page content is tall.
Fix it so the mic is always anchored at the bottom of every screen.

Android (Jetpack Compose):
  Replace the single-Column layout with a Box:

  Box(Modifier.fillMaxSize()) {
      // Layer 1: scrollable content
      Column(Modifier.verticalScroll(rememberScrollState())
                     .padding(bottom = 160.dp)) {   // ← room for mic bar
          // ... all screen content here
      }

      // Layer 2: fixed mic bar — always on top, always at bottom
      Column(Modifier
          .align(Alignment.BottomCenter)
          .fillMaxWidth()
          .background(NavyDark)) {
          // mic button, status strip, "Tap to speak" label
      }
  }

Apply this pattern to: HomeScreen, TransferScreen, SettingsScreen.

Web (CSS):
  .screen-container {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow: hidden;        /* container never scrolls */
  }
  .scroll-content {
      flex: 1;                 /* takes all available space */
      overflow-y: auto;        /* THIS part scrolls */
  }
  .mic-bar {
      flex-shrink: 0;          /* never shrinks, always visible */
  }

The mic bar must NEVER scroll. It must be visible on all screens at all times.
```

---

## 10. Auto-Trigger Fingerprint (No Button Tap)

```
In the transfer flow, there is currently a "Tap to authenticate" button
on the biometric screen. Users with motor impairment or visual impairment
cannot reliably tap that button.

Remove the button. The fingerprint scanner must auto-activate.

Android:
  In TransferScreen composable, watch for AwaitingBiometric step:

  LaunchedEffect(step) {
      if (step is TransferStep.AwaitingBiometric) {
          delay(500)           // brief pause so TTS "please hold finger" completes
          onRequestBiometric() // triggers BiometricPrompt
      }
  }

  Replace the "Tap to authenticate" button with a passive indicator:
  CircularProgressIndicator + text "Scanning fingerprint…"
  The user does nothing — just rests their finger on the sensor.

Web:
  In showTransferBiometric():
  - Hide the manual button
  - Show "Please hold your finger on the sensor"
  - After 2200ms (TTS finishes speaking), auto-call executeWebTransfer()
  - No user tap required

  setTimeout(() => {
      speak("Biometric scan successful.", () => executeWebTransfer());
  }, 2200);

The 500ms (Android) / 2200ms (Web) delay exists so the TTS instruction
"Please hold your finger on the sensor" completes before the sensor activates —
otherwise the user won't know what to do.
```

---

## Prompt → Feature → Version Map

| # | Prompt | Feature Delivered | Version | Tests Added |
|---|---|---|---|---|
| 1 | Initial App Brief | Voice banking core, biometric login | v1.0 | — |
| 2 | Transfer Voice State Machine | End-to-end voice transfer flow | v1.0 | 320 |
| 3 | Accessibility Redesign | WCAG AAA colours, fonts, tap targets, ARIA | v6.0 | 18 |
| 4 | Post-Transfer Navigation | "Say Done or Main Menu" after transfer | v6.0 | — |
| 5 | Session Timeout | 3-min idle timer + 30-sec spoken warning | v6.0 | — |
| 6 | Beneficiary Tap | Tap cards as alternative to voice selection | v6.0 | — |
| 7 | Transaction Follow-up | Multi-turn: "more transactions or main menu?" | v6.0 | 34 |
| 8 | Balance / Cheque Follow-up | "Tell me what to do next" after balance/cheque | v7.0 | 15 |
| 9 | Fixed Mic Bar | Mic anchored at bottom — never scrolls away | v8.0 | — |
| 10 | Auto-Trigger Biometric | Fingerprint auto-activates — no button tap needed | v8.0 | 6 |

**Total: 439 tests · 0 failures · 8 releases**
