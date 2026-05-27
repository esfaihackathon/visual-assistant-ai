// ========== Mock Data ==========
const mockAccount = {
    bankName: "SBI",
    accountLast4: "7845",
    accountType: "Savings",
    balance: 25000
};

const mockTransactions = [
    { id: "TXN001", description: "Reliance Fresh", amount: 1200, date: "Yesterday", type: "DEBIT" },
    { id: "TXN002", description: "Salary Credit", amount: 45000, date: "25 May", type: "CREDIT" },
    { id: "TXN003", description: "Electricity Bill", amount: 2350, date: "24 May", type: "DEBIT" },
    { id: "TXN004", description: "Amazon Purchase", amount: 899, date: "22 May", type: "DEBIT" },
    { id: "TXN005", description: "UPI from Amit", amount: 500, date: "20 May", type: "CREDIT" }
];

const mockContacts = {
    "rahul": "Rahul",
    "amit": "Amit",
    "priya": "Priya",
    "neha": "Neha"
};

// ========== State ==========
let currentScreen = "splash";
let isListening = false;
let awaitingConfirmation = false;
let pendingTransfer = { amount: 0, recipient: "" };
let recentCommands = [];
let speechRate = 0.9;
let selectedLang = "en-IN";
let voiceSupported = false;

// ========== DOM Elements ==========
const screens = {
    splash: document.getElementById("splash-screen"),
    auth: document.getElementById("auth-screen"),
    home: document.getElementById("home-screen"),
    settings: document.getElementById("settings-screen")
};

const els = {
    authBtn: document.getElementById("auth-btn"),
    micBtn: document.getElementById("mic-btn"),
    settingsBtn: document.getElementById("settings-btn"),
    settingsBackBtn: document.getElementById("settings-back-btn"),
    responseCard: document.getElementById("response-card"),
    responseText: document.getElementById("response-text"),
    recognizedCard: document.getElementById("recognized-card"),
    recognizedText: document.getElementById("recognized-text"),
    transactionsCard: document.getElementById("transactions-card"),
    transactionsList: document.getElementById("transactions-list"),
    listeningIndicator: document.getElementById("listening-indicator"),
    processingIndicator: document.getElementById("processing-indicator"),
    recentCommands: document.getElementById("recent-commands"),
    recentList: document.getElementById("recent-list"),
    speechRateSlider: document.getElementById("speech-rate"),
    speedValue: document.getElementById("speed-value"),
    textInput: document.getElementById("text-input"),
    sendBtn: document.getElementById("send-btn"),
    voiceStatus: document.getElementById("voice-status"),
    voiceStatusIcon: document.getElementById("voice-status-icon"),
    voiceStatusText: document.getElementById("voice-status-text")
};

// ========== Screen Navigation ==========
function showScreen(name) {
    Object.values(screens).forEach(s => s.classList.remove("active"));
    screens[name].classList.add("active");
    currentScreen = name;
}

// ========== Voice Status Banner ==========
function showVoiceStatus(message, type) {
    els.voiceStatus.className = "voice-status status-" + type;
    els.voiceStatus.classList.remove("hidden");

    const icons = { error: "mic_off", success: "mic", warning: "warning" };
    els.voiceStatusIcon.textContent = icons[type] || "info";
    els.voiceStatusText.textContent = message;

    if (type === "success") {
        setTimeout(() => els.voiceStatus.classList.add("hidden"), 3000);
    }
}

function hideVoiceStatus() {
    els.voiceStatus.classList.add("hidden");
}

// ========== Text-to-Speech ==========
function speak(text, onEnd) {
    if (!("speechSynthesis" in window)) {
        console.warn("TTS not supported");
        if (onEnd) onEnd();
        return;
    }

    window.speechSynthesis.cancel();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = selectedLang;
    utterance.rate = speechRate;
    utterance.pitch = 1.0;

    const voices = window.speechSynthesis.getVoices();
    const indianVoice = voices.find(v => v.lang === "en-IN") ||
                        voices.find(v => v.lang.startsWith("en"));
    if (indianVoice) utterance.voice = indianVoice;

    if (onEnd) utterance.onend = onEnd;

    window.speechSynthesis.speak(utterance);
}

function stopSpeaking() {
    if ("speechSynthesis" in window) {
        window.speechSynthesis.cancel();
    }
}

// ========== Speech Recognition ==========
let recognition = null;

function initSpeechRecognition() {
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SpeechRecognition) {
        console.warn("Speech recognition API not available in this browser");
        voiceSupported = false;
        return false;
    }

    try {
        recognition = new SpeechRecognition();
        recognition.lang = selectedLang;
        recognition.continuous = false;
        recognition.interimResults = true;
        recognition.maxAlternatives = 1;

        recognition.onstart = () => {
            console.log("Speech recognition started");
            isListening = true;
            updateListeningUI(true);
            hideVoiceStatus();
        };

        recognition.onaudiostart = () => {
            console.log("Audio capture started - microphone is active");
        };

        recognition.onresult = (event) => {
            let finalTranscript = "";
            let interimTranscript = "";

            for (let i = event.resultIndex; i < event.results.length; i++) {
                const transcript = event.results[i][0].transcript;
                if (event.results[i].isFinal) {
                    finalTranscript += transcript;
                } else {
                    interimTranscript += transcript;
                }
            }

            if (interimTranscript) {
                showRecognizedText(interimTranscript);
            }

            if (finalTranscript) {
                console.log("Final transcript:", finalTranscript);
                showRecognizedText(finalTranscript);
                handleVoiceInput(finalTranscript.trim());
            }
        };

        recognition.onerror = (event) => {
            console.error("Speech recognition error:", event.error, event.message);
            isListening = false;
            updateListeningUI(false);

            switch (event.error) {
                case "not-allowed":
                    showVoiceStatus("Microphone access denied. Please allow microphone permission in your browser, or type your command below.", "error");
                    break;
                case "no-speech":
                    showVoiceStatus("No speech detected. Try again or type your command below.", "warning");
                    break;
                case "audio-capture":
                    showVoiceStatus("No microphone found. Please connect a microphone or type your command below.", "error");
                    break;
                case "network":
                    showVoiceStatus("Network error. Speech recognition requires internet. Type your command below.", "error");
                    break;
                case "aborted":
                    break;
                default:
                    showVoiceStatus("Voice error: " + event.error + ". Type your command below.", "error");
            }
        };

        recognition.onend = () => {
            console.log("Speech recognition ended");
            isListening = false;
            updateListeningUI(false);
        };

        voiceSupported = true;
        return true;
    } catch (e) {
        console.error("Failed to initialize speech recognition:", e);
        voiceSupported = false;
        return false;
    }
}

function startListening() {
    if (!recognition) {
        const initialized = initSpeechRecognition();
        if (!initialized) {
            showVoiceStatus("Voice input is not supported in this browser. Please use Chrome, or type your command below.", "error");
            els.textInput.focus();
            return;
        }
    }

    stopSpeaking();
    recognition.lang = selectedLang;

    try {
        recognition.start();
        console.log("Attempting to start speech recognition...");
    } catch (e) {
        console.error("Error starting speech recognition:", e);
        if (e.message && e.message.includes("already started")) {
            recognition.stop();
            setTimeout(() => {
                try { recognition.start(); } catch (e2) {
                    showVoiceStatus("Could not start voice input. Type your command below.", "error");
                }
            }, 200);
        } else {
            showVoiceStatus("Could not start voice input: " + e.message + ". Type your command below.", "error");
        }
    }
    vibrate(50);
}

function stopListening() {
    if (recognition && isListening) {
        recognition.stop();
    }
    vibrate(50);
}

// ========== Text Input Handler ==========
function handleTextInput() {
    const text = els.textInput.value.trim();
    if (!text) return;

    showRecognizedText(text);
    handleVoiceInput(text);
    els.textInput.value = "";
}

// ========== Voice Intent Parser ==========
function parseIntent(text) {
    const lower = text.toLowerCase().trim();

    // Confirmation
    if (/^(yes|yeah|yep|yup|confirm|ok|okay|sure|go ahead|proceed|do it|haan|ha|ji|haa|theek hai|theek)$/i.test(lower)) {
        return { type: "CONFIRM_YES" };
    }
    if (/^(no|nope|cancel|stop|nahi|mat karo|don't|na|ruko|band karo)$/i.test(lower)) {
        return { type: "CONFIRM_NO" };
    }

    // Balance - expanded patterns
    if (/balance|kitna paisa|kitne paise|how much.*money|how much.*have|how much.*bank|how much.*account|mere.*account|mera.*balance|what.*in my.*account|money.*in.*account|tell.*balance|show.*balance|check.*balance|account.*balance|my balance|bank.*balance|savings.*balance|available.*balance|remaining.*balance|what do i have|what's in my account|whats in my account/i.test(lower)) {
        return { type: "BALANCE" };
    }

    // Transfer - expanded patterns
    if (/transfer|send.*money|pay\b|send.*rupees|transfer.*rupees|bhejo|paisa bhejo|payment|send.*to\b|pay.*to\b|rupees.*to\b|rs\.?\s*\d/i.test(lower)) {
        const amountMatch = lower.match(/(\d[\d,]*\.?\d*)/);
        const amount = amountMatch ? parseFloat(amountMatch[1].replace(/,/g, "")) : null;

        let recipient = null;
        const recipientMatch = text.match(/(?:to|for)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)/i);
        if (recipientMatch) {
            recipient = recipientMatch[1];
        }

        return { type: "TRANSFER", amount, recipient };
    }

    // Cheque book
    if (/cheque.*book|check.*book|chequebook|new cheque|issue.*cheque|request.*cheque|order.*cheque|cheque\s*order/i.test(lower)) {
        return { type: "CHEQUE_BOOK" };
    }

    // Recent transactions - expanded patterns
    if (/transaction|recent.*activity|last.*activity|statement|what.*spent|spending|kharcha|history|recent.*purchase|last.*purchase|what.*bought|my.*activity|account.*activity|mini.*statement/i.test(lower)) {
        return { type: "RECENT_TXNS" };
    }

    // Help
    if (/\bhelp\b|assist|support|call.*support|customer.*care|emergency|what.*can.*do|what.*say|kya kar sakte|madad/i.test(lower)) {
        return { type: "HELP" };
    }

    return { type: "UNKNOWN" };
}

// ========== Intent Handlers ==========
function handleVoiceInput(text) {
    addRecentCommand(text);
    showProcessing(true);

    const intent = parseIntent(text);
    console.log("Parsed intent:", intent.type, "from:", text);

    setTimeout(() => {
        showProcessing(false);
        handleIntent(intent);
    }, 500);
}

function handleIntent(intent) {
    switch (intent.type) {
        case "BALANCE":
            handleBalance();
            break;
        case "TRANSFER":
            handleTransfer(intent);
            break;
        case "CHEQUE_BOOK":
            handleChequeBook();
            break;
        case "RECENT_TXNS":
            handleRecentTransactions();
            break;
        case "HELP":
            handleHelp();
            break;
        case "CONFIRM_YES":
            handleConfirmYes();
            break;
        case "CONFIRM_NO":
            handleConfirmNo();
            break;
        default:
            handleUnknown();
    }
}

function handleBalance() {
    const amt = formatAmount(mockAccount.balance);
    const response = `In your ${mockAccount.bankName} savings account ending with ${mockAccount.accountLast4}, you have ${amt} rupees available.`;
    respond(response);
}

function handleTransfer(intent) {
    const { amount, recipient } = intent;

    if (!amount || !recipient) {
        respond("Please say the amount and recipient. For example, transfer 500 rupees to Rahul.");
        return;
    }

    awaitingConfirmation = true;
    pendingTransfer = { amount, recipient };
    const amt = formatAmount(amount);
    respond(`You are transferring ${amt} rupees to ${recipient}. Do you want to continue? Say yes or no.`);
}

function handleConfirmYes() {
    if (awaitingConfirmation) {
        awaitingConfirmation = false;
        mockAccount.balance -= pendingTransfer.amount;
        const txnId = "TXN" + Math.floor(100000 + Math.random() * 900000);
        respond(`Transfer successful. ${formatAmount(pendingTransfer.amount)} rupees sent to ${pendingTransfer.recipient}. Transaction reference number ${txnId}.`);
        vibrateSuccess();
    } else {
        respond("There is nothing to confirm right now.");
    }
}

function handleConfirmNo() {
    if (awaitingConfirmation) {
        awaitingConfirmation = false;
        respond("Transfer cancelled.");
    } else {
        respond("Okay. How can I help you?");
    }
}

function handleChequeBook() {
    respond("Your cheque book request has been registered successfully and will be delivered within 5 working days.");
    vibrateSuccess();
}

function handleRecentTransactions() {
    showTransactions(mockTransactions);
    const latest = mockTransactions[0];
    respond(`Your last transaction was ${formatAmount(latest.amount)} rupees spent at ${latest.description} ${latest.date}.`);
}

function handleHelp() {
    respond("You can say: check my balance, transfer money to someone, request cheque book, show recent transactions, or ask for help.");
}

function handleUnknown() {
    respond("I did not understand that command. Try saying: check my balance, transfer money, request cheque book, show transactions, or help.");
}

// ========== UI Updates ==========
function respond(text) {
    showResponse(text);
    speak(text);
}

function showResponse(text) {
    els.responseText.textContent = text;
    els.responseCard.classList.remove("hidden");
    els.responseCard.style.animation = "none";
    void els.responseCard.offsetHeight;
    els.responseCard.style.animation = "slideIn 0.3s ease";
}

function showRecognizedText(text) {
    els.recognizedText.textContent = '"' + text + '"';
    els.recognizedCard.classList.remove("hidden");
}

function showTransactions(transactions) {
    els.transactionsList.innerHTML = transactions.map(txn => `
        <div class="txn-row">
            <div>
                <div class="txn-desc">${txn.description}</div>
                <div class="txn-date">${txn.date}</div>
            </div>
            <div class="txn-amount ${txn.type.toLowerCase()}">
                ${txn.type === "DEBIT" ? "-" : "+"}&#8377;${txn.amount.toLocaleString("en-IN")}
            </div>
        </div>
    `).join("");
    els.transactionsCard.classList.remove("hidden");
}

function updateListeningUI(listening) {
    if (listening) {
        els.micBtn.classList.add("listening");
        els.listeningIndicator.classList.remove("hidden");
        els.micBtn.setAttribute("aria-label", "Microphone active. Tap to stop listening.");
    } else {
        els.micBtn.classList.remove("listening");
        els.listeningIndicator.classList.add("hidden");
        els.micBtn.setAttribute("aria-label", "Tap to speak a command.");
    }
}

function showProcessing(show) {
    if (show) {
        els.processingIndicator.classList.remove("hidden");
    } else {
        els.processingIndicator.classList.add("hidden");
    }
}

function addRecentCommand(cmd) {
    recentCommands = [cmd, ...recentCommands.filter(c => c !== cmd)].slice(0, 5);
    renderRecentCommands();
}

function renderRecentCommands() {
    if (recentCommands.length === 0) {
        els.recentCommands.classList.add("hidden");
        return;
    }
    els.recentCommands.classList.remove("hidden");
    els.recentList.innerHTML = recentCommands.slice(0, 3).map(cmd =>
        `<span class="recent-chip">${cmd}</span>`
    ).join("");
}

// ========== Haptic Feedback ==========
function vibrate(ms) {
    if ("vibrate" in navigator) navigator.vibrate(ms);
}

function vibrateSuccess() {
    if ("vibrate" in navigator) navigator.vibrate([50, 100, 50]);
}

// ========== Utility ==========
function formatAmount(amount) {
    return amount.toLocaleString("en-IN");
}

// ========== App Flow ==========
function startApp() {
    showScreen("splash");
    speak("Welcome to Saral. Your accessible banking assistant.", () => {
        setTimeout(() => showScreen("auth"), 500);
    });

    setTimeout(() => {
        if (currentScreen === "splash") showScreen("auth");
    }, 4000);
}

// ========== Event Listeners ==========

// Auth
els.authBtn.addEventListener("click", () => {
    vibrate(100);
    speak("Authentication successful. Welcome Jayesh.", () => {
        speak("You can now say commands like check balance, transfer money, or request cheque book. You can also type commands in the text box.");
    });
    setTimeout(() => showScreen("home"), 500);
});

// Mic button
els.micBtn.addEventListener("click", () => {
    if (isListening) {
        stopListening();
    } else {
        startListening();
    }
});

// Text input - send on Enter
els.textInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        handleTextInput();
    }
});

// Send button
els.sendBtn.addEventListener("click", () => {
    handleTextInput();
});

// Quick commands
document.querySelectorAll(".quick-cmd").forEach(btn => {
    btn.addEventListener("click", () => {
        const cmd = btn.dataset.cmd;
        vibrate(50);
        showRecognizedText(cmd);
        handleVoiceInput(cmd);
    });
});

// Settings
els.settingsBtn.addEventListener("click", () => {
    showScreen("settings");
});

els.settingsBackBtn.addEventListener("click", () => {
    showScreen("home");
});

// Language buttons
document.querySelectorAll(".lang-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".lang-btn").forEach(b => b.classList.remove("active"));
        btn.classList.add("active");
        selectedLang = btn.dataset.lang;
        if (recognition) recognition.lang = selectedLang;
        speak(selectedLang === "hi-IN" ? "Language changed to Hindi." : "Language changed to English.");
    });
});

// Speech rate
els.speechRateSlider.addEventListener("input", (e) => {
    speechRate = parseFloat(e.target.value);
    els.speedValue.textContent = speechRate.toFixed(1) + "x";
});

// Load voices
if ("speechSynthesis" in window) {
    window.speechSynthesis.onvoiceschanged = () => {
        window.speechSynthesis.getVoices();
    };
}

// ========== Initialize ==========
window.addEventListener("DOMContentLoaded", () => {
    const speechInited = initSpeechRecognition();
    console.log("Speech recognition supported:", speechInited);

    setTimeout(startApp, 500);
});
