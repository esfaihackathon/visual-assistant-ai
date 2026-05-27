# Test Coverage Documentation

## Overview

Comprehensive unit test suite for Saral Voice-First Banking Accessibility Layer. Total: **12 test files** with **55+ test cases** across all layers.

---

## Test Suite Structure

### 1. Voice Intent Parsing Layer

**File:** `app/src/test/java/com/saral/app/voice/VoiceIntentParserTest.kt`

| Test Case                                | Description                                         | Input                                     | Expected Output                                               |
| ---------------------------------------- | --------------------------------------------------- | ----------------------------------------- | ------------------------------------------------------------- |
| `parsesCheckBalance()`                   | Recognizes balance inquiry patterns                 | "How much money do I have in my account?" | `VoiceIntent.CheckBalance`                                    |
| `parsesTransferWithAmountAndRecipient()` | Extracts amount and recipient from transfer command | "Transfer 500 rupees to Rahul"            | `VoiceIntent.TransferMoney(amount=500.0, recipient="Rahul")`  |
| `parsesTransferWithCommasAndDecimals()`  | Handles formatted amounts with decimals             | "Send 1,250.50 rupees to Amit"            | `VoiceIntent.TransferMoney(amount=1250.50, recipient="Amit")` |
| `parsesConfirmYesAndNo()`                | Recognizes affirmative and negative responses       | "Yes" / "No"                              | `VoiceIntent.ConfirmYes` / `VoiceIntent.ConfirmNo`            |
| `parsesChequeBookRequest()`              | Recognizes cheque book request patterns             | "Request cheque book"                     | `VoiceIntent.RequestChequeBook`                               |
| `parsesRecentTransactions()`             | Recognizes transaction inquiry patterns             | "Show recent transactions"                | `VoiceIntent.RecentTransactions`                              |
| `parsesHelp()`                           | Recognizes help command patterns                    | "Help"                                    | `VoiceIntent.Help`                                            |
| `unknownWhenNoPatternMatches()`          | Returns unknown for unrecognized input              | "Blah blah gibberish"                     | `VoiceIntent.Unknown`                                         |

---

### 2. Use Case Layer

#### 2.1 GetBalanceUseCase

**File:** `app/src/test/java/com/saral/app/domain/usecases/GetBalanceUseCaseTest.kt`

| Test Case                        | Description                                         | Verification                                                                |
| -------------------------------- | --------------------------------------------------- | --------------------------------------------------------------------------- |
| `invoke_returnsAccountBalance()` | Delegates to repository and returns account balance | Account not null, bank name is "SBI", account last 4 is "7845", balance > 0 |
| `invoke_returnsSavingsAccount()` | Verifies account type is Savings                    | Account type equals "Savings"                                               |

#### 2.2 TransferMoneyUseCase

**File:** `app/src/test/java/com/saral/app/domain/usecases/TransferMoneyUseCaseTest.kt`

| Test Case                             | Description                                 | Verification                                                 |
| ------------------------------------- | ------------------------------------------- | ------------------------------------------------------------ |
| `invoke_transfersMoneySuccessfully()` | Executes transfer with amount and recipient | Success = true, amount matches, recipient name matches       |
| `invoke_generatesTransactionId()`     | Generates unique transaction ID             | TXN ID starts with "TXN" and has length > 3                  |
| `invoke_handlesVariousAmounts()`      | Supports multiple transfer amounts          | Multiple transfers succeed with correct amounts (100, 50000) |

#### 2.3 RequestChequeBookUseCase

**File:** `app/src/test/java/com/saral/app/domain/usecases/RequestChequeBookUseCaseTest.kt`

| Test Case                            | Description                   | Verification                |
| ------------------------------------ | ----------------------------- | --------------------------- |
| `invoke_requestsChequeBook()`        | Initiates cheque book request | Status = "REQUESTED"        |
| `invoke_providesEstimatedDelivery()` | Returns delivery estimate     | Estimated delivery days = 5 |

#### 2.4 GetRecentTransactionsUseCase

**File:** `app/src/test/java/com/saral/app/domain/usecases/GetRecentTransactionsUseCaseTest.kt`

| Test Case                              | Description                                | Verification                                    |
| -------------------------------------- | ------------------------------------------ | ----------------------------------------------- |
| `invoke_returnsList()`                 | Retrieves transaction list                 | Returns non-empty list                          |
| `invoke_returnsMultipleTransactions()` | Ensures sufficient transaction history     | List size >= 5                                  |
| `invoke_includesTransactionDetails()`  | Verifies all required fields               | ID, description, amount, date, type all present |
| `invoke_hasMixedCreditAndDebit()`      | Ensures both credit and debit transactions | Both CREDIT and DEBIT types present in list     |

---

### 3. Domain Model Layer

#### 3.1 BankAccount

**File:** `app/src/test/java/com/saral/app/domain/models/BankAccountTest.kt`

| Test Case                      | Description                                 | Verification                                     |
| ------------------------------ | ------------------------------------------- | ------------------------------------------------ |
| `creation_withValidData()`     | Creates account with valid parameters       | All fields correctly set                         |
| `equality_sameValues()`        | Data class equality with same values        | Two accounts with same data are equal            |
| `inequality_differentValues()` | Data class inequality with different values | Two accounts with different data are not equal   |
| `copy_preservesValues()`       | Copy with balance modification              | Non-modified fields preserved, balance updated   |
| `toString_containsValues()`    | String representation includes key data     | toString() contains bank name and account last 4 |

#### 3.2 Transaction

**File:** `app/src/test/java/com/saral/app/domain/models/TransactionTest.kt`

| Test Case                      | Description                   | Verification                                  |
| ------------------------------ | ----------------------------- | --------------------------------------------- |
| `creation_creditTransaction()` | Creates credit transaction    | All fields set correctly, type = CREDIT       |
| `creation_debitTransaction()`  | Creates debit transaction     | All fields set correctly, type = DEBIT        |
| `equality_sameValues()`        | Data class equality           | Two transactions with same values are equal   |
| `inequality_differentIds()`    | Data class inequality by ID   | Transactions with different IDs are not equal |
| `copy_changesAmount()`         | Copy with amount modification | ID and type preserved, amount updated         |

#### 3.3 TransferResult

**File:** `app/src/test/java/com/saral/app/domain/models/TransferResultTest.kt`

| Test Case                       | Description                             | Verification                                             |
| ------------------------------- | --------------------------------------- | -------------------------------------------------------- |
| `creation_successfulTransfer()` | Creates successful transfer result      | Success = true, TXN ID set, amount and recipient present |
| `creation_failedTransfer()`     | Creates failed transfer result          | Success = false                                          |
| `equality_sameValues()`         | Data class equality                     | Two results with same values are equal                   |
| `inequality_differentTxnId()`   | Data class inequality by transaction ID | Results with different TXN IDs are not equal             |
| `copy_preservesFields()`        | Copy with amount modification           | TXN ID and recipient preserved, amount updated           |

#### 3.4 ChequeBookRequest

**File:** `app/src/test/java/com/saral/app/domain/models/ChequeBookRequestTest.kt`

| Test Case                            | Description                            | Verification                                 |
| ------------------------------------ | -------------------------------------- | -------------------------------------------- |
| `creation_withValidData()`           | Creates cheque request with valid data | Status and delivery days set correctly       |
| `equality_sameValues()`              | Data class equality                    | Two requests with same values are equal      |
| `inequality_differentStatus()`       | Data class inequality by status        | Requests with different status are not equal |
| `inequality_differentDeliveryDays()` | Data class inequality by delivery days | Requests with different days are not equal   |
| `copy_changesDeliveryDays()`         | Copy with delivery days modification   | Status preserved, delivery days updated      |
| `toString_containsValues()`          | String representation includes data    | toString() contains status                   |

---

### 4. Data Repository Layer

**File:** `app/src/test/java/com/saral/app/data/mock/MockBankingRepositoryTest.kt`

| Test Case                                         | Description                                    | Verification                                                       |
| ------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------ |
| `getBalance_returnsAccount()`                     | Retrieves current account balance              | Bank = "SBI", last 4 = "7845", balance > 0                         |
| `transferMoney_reducesBalance_and_returnsTxnId()` | Executes transfer and deducts from balance     | Success = true, TXN ID present, balance reduced by transfer amount |
| `requestChequeBook_returnsEstimate()`             | Requests cheque book and returns delivery info | Status = "REQUESTED", estimated days > 0                           |
| `getRecentTransactions_returnsList()`             | Retrieves transaction list                     | Returns non-empty list of transactions                             |

---

### 5. Presentation Layer (ViewModel)

**File:** `app/src/test/java/com/saral/app/presentation/home/HomeViewModelTest.kt`

| Test Case                                 | Scenario                             | Input                                      | Expected Behavior                                                            |
| ----------------------------------------- | ------------------------------------ | ------------------------------------------ | ---------------------------------------------------------------------------- |
| `checkBalance_speaksBalance()`            | User asks for balance                | "Check my balance"                         | ViewModel speaks balance info, stops processing                              |
| `transferFlow_confirmationAndExecution()` | User initiates and confirms transfer | 1. "Transfer 500 rupees to Rahul" 2. "Yes" | Sets confirmation state, speaks prompt, executes transfer after confirmation |
| `transferFlow_cancellation()`             | User initiates and cancels transfer  | 1. "Transfer 200 rupees to Priya" 2. "No"  | Sets confirmation state, speaks prompt, cancels transfer on "No"             |
| `unknownIntent_speaksHelp()`              | User speaks unrecognized command     | "asldkfjasldkfj"                           | ViewModel speaks help message with available commands                        |

---

### 6. Dependency Injection Layer

**File:** `app/src/test/java/com/saral/app/di/AppModuleTest.kt`

| Test Case                                             | Description                          | Verification                                                                             |
| ----------------------------------------------------- | ------------------------------------ | ---------------------------------------------------------------------------------------- |
| `mockBankingRepository_implementsBankingRepository()` | Mock repository implements interface | `MockBankingRepository instanceof BankingRepository`                                     |
| `mockBankingRepository_canBeUsedAsInterface()`        | Mock can be used as interface type   | Mock instantiates as BankingRepository                                                   |
| `bindingStrategy_mockRepoHasAllMethods()`             | Mock implements all required methods | All methods present: getBalance, transferMoney, requestChequeBook, getRecentTransactions |

---

### 7. Android Instrumentation Layer

#### 7.1 TextToSpeechManager

**File:** `app/src/androidTest/java/com/saral/app/voice/TextToSpeechManagerTest.kt`

| Test Case                         | Description                                         | Verification                                          |
| --------------------------------- | --------------------------------------------------- | ---------------------------------------------------- |
| `initialize_setsUpTextToSpeech()` | Initializes TTS engine and calls callback           | Callback invoked on ready                             |
| `isSpeaking_flowEmitsCorrectly()` | Verifies speaking state flow                        | Initially reports not speaking                        |
| `speak_withText()`                | Speaks text without exception                       | speak() callable after initialization                 |
| `speak_withCallback()`            | Speaks with completion callback                     | callback accepted and no failure                      |
| `setLanguage_updatesLocale()`     | Updates TTS locale                                  | Locale can be changed safely                          |
| `shutdown_cleansUp()`             | Shuts down TTS resources correctly                  | No exception on shutdown                              |

#### 7.2 SpeechRecognizerManager

**File:** `app/src/androidTest/java/com/saral/app/voice/SpeechRecognizerManagerTest.kt`

| Test Case                            | Description                                          | Verification                                                         |
| ------------------------------------ | ---------------------------------------------------- | -------------------------------------------------------------------- |
| `initialize_setUpsSpeechRecognizer()` | Initializes speech recognizer                         | Initialization succeeds without exceptions                          |
| `isListening_flowInitiallyFalse()`   | Verifies initial listening state                      | Flow emits false initially                                           |
| `recognizedText_flowInitiallyEmpty()`| Verifies initial recognized text                      | Flow emits empty string initially                                    |
| `startListening_withCallback()`      | Starts listening with callback                        | Callback accepted and startListening() call succeeds                |
| `stopListening_stopsRecognition()`   | Stops speech recognition                               | stopListening() succeeds without exceptions                         |
| `setLanguage_updatesLocale()`        | Sets recognition language                              | Language can be switched without exception                         |
| `destroy_cleansUp()`                 | Destroys speech recognizer                             | destroy() succeeds without exceptions                               |
| `startListening_multipleCalls()`     | Handles repeated start/stop cycles                     | Multiple start/stop cycles execute without exception                 |

#### 7.3 HapticManager

**File:** `app/src/androidTest/java/com/saral/app/accessibility/HapticManagerTest.kt`

| Test Case                             | Description                                         | Verification                                                        |
| ------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------ |
| `vibrateShort_executesWithoutException()` | Executes short vibration pattern                   | No exception thrown                                                 |
| `vibrateMedium_executesWithoutException()`| Executes medium vibration pattern                  | No exception thrown                                                 |
| `vibrateSuccess_executesDoublePattern()` | Executes success vibration pattern                | No exception thrown                                                 |
| `vibrateError_executesLongPattern()`   | Executes error vibration pattern                    | No exception thrown                                                 |
| `vibrateShort_beforeVibrateSuccess()`  | Runs short then success vibration sequence          | Multiple patterns execute safely                                   |
| `allVibrationPatterns_sequential()`    | Runs all patterns sequentially                      | All patterns execute in order without errors                        |
| `hapticManager_worksOnMultipleApiLevels()` | Works across supported API levels                 | Adapts to API level and uses appropriate vibrator API              |
| `vibrateSuccess_calledMultipleTimes()` | Repeats success pattern multiple times              | No conflicts when repeated                                          |
| `vibrateError_calledMultipleTimes()`   | Repeats error pattern multiple times                | No conflicts when repeated                                          |

---

## Test Metrics

### By Layer

| Layer                | Test Files | Test Cases | Coverage Type  |
| -------------------- | ---------- | ---------- | -------------- |
| Voice Intent Parsing | 1          | 8          | Unit           |
| Use Cases            | 4          | 9          | Unit           |
| Domain Models        | 4          | 16         | Unit           |
| Data Repository      | 1          | 4          | Unit           |
| View Model           | 1          | 4          | Unit           |
| Dependency Injection | 1          | 3          | Unit           |
| Android Instrumentation | 3 | 23 | Instrumentation |
| **Total**            | **15**     | **78+**    | **Mixed** |

### Dependencies

- **JUnit 4** — Test framework
- **kotlinx-coroutines-test** — Coroutine testing support for async use cases
- **Robolectric 4.11.1** — Android framework mocking for unit tests
- **androidx.test:runner:1.5.2** — Instrumentation test runner
- **androidx.test.ext:junit:1.1.5** — Android JUnit extension

---

## How to Run Tests

### Prerequisites

- Java 11 or newer (JDK 17 recommended)
- Gradle wrapper included in repository

### Run All Unit Tests

```bash
cd visual-assistant-ai
./gradlew :app:testDebugUnitTest
```

### Run Specific Test Class

```bash
./gradlew :app:testDebugUnitTest --tests com.saral.app.voice.VoiceIntentParserTest
```

### Run Specific Test Case

```bash
./gradlew :app:testDebugUnitTest --tests com.saral.app.voice.VoiceIntentParserTest.parsesCheckBalance
```

### Run Instrumentation Tests

```bash
./gradlew :app:connectedAndroidTest
```

### Run Specific Instrumentation Test

```bash
./gradlew :app:connectedAndroidTest --tests com.saral.app.voice.TextToSpeechManagerTest
```

### Generate Test Report

```bash
./gradlew :app:testDebugUnitTest
# Unit test report: app/build/reports/tests/testDebugUnitTest/index.html

./gradlew :app:connectedAndroidTest
# Instrumentation report: app/build/reports/androidTests/connected/index.html
```

---

## Test Scenarios Covered

### Banking Operations

- ✅ Check account balance
- ✅ Transfer money with amount and recipient extraction
- ✅ Transfer confirmation before execution
- ✅ Transfer cancellation
- ✅ Request cheque book
- ✅ View recent transactions with mixed credit/debit

### Intent Parsing

- ✅ Balance inquiry patterns (multiple phrasings)
- ✅ Transfer patterns with amount and recipient extraction
- ✅ Decimal and comma-formatted amounts
- ✅ Confirmation responses (yes/no/affirmative/negative variants)
- ✅ Cheque book request patterns
- ✅ Transaction history inquiry
- ✅ Help command
- ✅ Unknown/unrecognized input handling

### Data Integrity

- ✅ Account information retrieval
- ✅ Balance deduction on transfer
- ✅ Transaction ID generation
- ✅ Delivery time estimation
- ✅ Transaction type classification (CREDIT/DEBIT)
- ✅ Data class equality and copying
- ✅ State management (confirmation state, listening state)

### Use Case Delegation

- ✅ Use cases properly delegate to repository
- ✅ Use cases return expected data structures
- ✅ Use cases handle multiple parameter combinations

### Dependency Injection

- ✅ Mock repository binds to interface
- ✅ All repository methods available through interface
- ✅ Singleton scope maintained

---

## Not Yet Tested (Requires Instrumentation/Robolectric)

- ❌ Android TextToSpeechManager — requires Android framework
- ❌ Android SpeechRecognizerManager — requires Android framework
- ❌ HapticManager (Vibrator API) — requires Android framework
- ❌ Compose UI screens (AuthScreen, HomeScreen, SettingsScreen, SplashScreen)
- ❌ Navigation routing (SaralNavGraph)
- ❌ MainActivity lifecycle and biometric integration

---

## Future Test Enhancements

### Instrumentation Tests

- Add Robolectric tests for Android managers
- Test TTS initialization and speech callbacks
- Test speech recognition lifecycle
- Test haptic vibration patterns

### UI Tests

- Compose UI component tests
- Navigation flow testing
- User interaction scenarios (button clicks, text input)
- Accessibility feature validation (TalkBack labels, contrast ratios)

### Integration Tests

- End-to-end banking operation flows
- Biometric authentication + balance retrieval
- Voice input → intent parsing → response generation

---

## Test Maintenance Guidelines

### Adding New Tests

1. Identify the layer (voice, domain, data, presentation)
2. Create test class in corresponding `src/test/java/com/saral/app/` directory
3. Use meaningful test names: `[method]_[scenario]_[expected]`
4. Include setup and teardown with `@Before` / `@After`
5. Use assertions from `org.junit.Assert`
6. Document complex test logic with comments

### Test Naming Convention

```kotlin
// Pattern: [Function/Feature]_[Scenario/Input]_[ExpectedResult]
fun parseBalance_multiplePatterns_recognizesAll()
fun transfer_withAmountAndRecipient_extractsBothCorrectly()
fun viewModel_balanceQuery_speaksResponse()
```

### Best Practices

- ✅ One assertion per test (or related assertions)
- ✅ Use descriptive variable names
- ✅ Mock external dependencies
- ✅ Test edge cases (null values, empty lists, invalid input)
- ✅ Keep tests independent (no cross-test state)
- ✅ Use `runBlocking { }` for coroutine tests
- ✅ Set up test data in `@Before` methods

---

## Quick Reference: Test Class Locations

```
app/src/test/java/com/saral/app/
├── voice/
│   └── VoiceIntentParserTest.kt (8 cases)
├── domain/
│   ├── usecases/
│   │   ├── GetBalanceUseCaseTest.kt (2 cases)
│   │   ├── TransferMoneyUseCaseTest.kt (3 cases)
│   │   ├── RequestChequeBookUseCaseTest.kt (2 cases)
│   │   └── GetRecentTransactionsUseCaseTest.kt (4 cases)
│   └── models/
│       ├── BankAccountTest.kt (5 cases)
│       ├── TransactionTest.kt (5 cases)
│       ├── TransferResultTest.kt (5 cases)
│       └── ChequeBookRequestTest.kt (6 cases)
├── data/
│   └── mock/
│       └── MockBankingRepositoryTest.kt (4 cases)
├── presentation/
│   └── home/
│       └── HomeViewModelTest.kt (4 cases)
└── di/
    └── AppModuleTest.kt (3 cases)
```

---

**Last Updated:** May 27, 2026  
**Test Suite Version:** 1.0  
**Total Test Cases:** 55+  
**Coverage:** Core business logic (voice parsing, banking operations, data integrity, state management)
