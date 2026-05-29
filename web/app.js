// ========== Mock Data ==========
const mockAccount = {
    bankName: "SBI",
    accountLast4: "7845",
    accountType: "Savings",
    balance: 25000
};

const mockTransactions = [
    { id: "TXN001", description: "Reliance Fresh",      amount: 1200,  date: "27 May", type: "DEBIT"  },
    { id: "TXN002", description: "Salary Credit",       amount: 45000, date: "25 May", type: "CREDIT" },
    { id: "TXN003", description: "Electricity Bill",    amount: 2350,  date: "24 May", type: "DEBIT"  },
    { id: "TXN004", description: "Amazon Purchase",     amount: 899,   date: "22 May", type: "DEBIT"  },
    { id: "TXN005", description: "UPI from Amit",       amount: 500,   date: "20 May", type: "CREDIT" },
    { id: "TXN006", description: "Netflix Subscription",amount: 649,   date: "18 May", type: "DEBIT"  },
    { id: "TXN007", description: "Petrol Pump",         amount: 1800,  date: "15 May", type: "DEBIT"  },
    { id: "TXN008", description: "Insurance Premium",   amount: 3500,  date: "10 May", type: "DEBIT"  },
    { id: "TXN009", description: "ATM Withdrawal",      amount: 5000,  date: "8 May",  type: "DEBIT"  },
    { id: "TXN010", description: "Dividend Credit",     amount: 1200,  date: "5 May",  type: "CREDIT" }
];

const mockContacts = {
    "rahul": "Rahul",
    "amit": "Amit",
    "priya": "Priya",
    "neha": "Neha"
};

const mockBeneficiaries = [
    { id: "BEN001", name: "Rahul Sharma",  accountLast4: "1234", bank: "SBI" },
    { id: "BEN002", name: "Priya Gupta",   accountLast4: "5678", bank: "HDFC Bank" },
    { id: "BEN003", name: "Amit Kumar",    accountLast4: "9012", bank: "ICICI Bank" },
    { id: "BEN004", name: "Sunita Verma",  accountLast4: "3456", bank: "Axis Bank" },
    { id: "BEN005", name: "Vikram Singh",  accountLast4: "7890", bank: "Bank of Baroda" }
];

// ========== State ==========
let currentScreen = "splash";
let isListening = false;
let awaitingConfirmation = false;
let awaitingTransactionCount = false;
let awaitingPostTransactionChoice = false; // true after transactions are read
let awaitingSimpleFollowUp = false;       // true after balance or cheque book
let pendingTransfer = { amount: 0, recipient: "" };
let recentCommands = [];

// Transfer screen state
let currentTransferPhase = null; // 'selecting' | 'entering_amount' | 'confirming' | 'authenticating' | 'complete'
let selectedBeneficiary = null;
let pendingTransferAmount = 0;

// ── Feature 2: Session timeout ────────────────────────────────────────────────
const SESSION_IDLE_MS    = 3 * 60 * 1000;  // 3 min of inactivity → warning
const SESSION_WARNING_MS = 30 * 1000;       // 30 s from warning → expiry
let sessionIdleTimer    = null;
let sessionExpireTimer  = null;
let sessionCountdownInterval = null;
let speechRate = 0.88;
let selectedPitch = 0.95;
let selectedLang = "en-IN";
let selectedVoice = null;
let voiceSupported = false;

// ========== DOM Elements ==========
const screens = {
    splash: document.getElementById("splash-screen"),
    auth: document.getElementById("auth-screen"),
    home: document.getElementById("home-screen"),
    settings: document.getElementById("settings-screen"),
    transfer: document.getElementById("transfer-screen")
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

// ========== Voice Selection ==========
// Priority: Google/Microsoft neural > Google en > Microsoft en > en-IN > en-US > any en
function pickBestVoice(voices) {
    const tiers = [
        v => /google.*natural|google.*wavenet/i.test(v.name),
        v => /microsoft.*natural|microsoft.*neural/i.test(v.name),
        v => /google/i.test(v.name) && v.lang === "en-IN",
        v => /google/i.test(v.name) && v.lang === "en-GB",
        v => /google/i.test(v.name) && v.lang === "en-US",
        v => /google/i.test(v.name) && v.lang.startsWith("en"),
        v => /microsoft/i.test(v.name) && v.lang.startsWith("en"),
        v => v.lang === "en-IN",
        v => v.lang === "en-GB",
        v => v.lang === "en-US",
        v => v.lang.startsWith("en"),
    ];
    for (const test of tiers) {
        const match = voices.find(test);
        if (match) return match;
    }
    return voices[0] || null;
}

function loadVoices() {
    const voices = window.speechSynthesis.getVoices();
    if (!voices.length) return;
    if (!selectedVoice) selectedVoice = pickBestVoice(voices);
    populateVoiceDropdown(voices);
}

function populateVoiceDropdown(voices) {
    const sel = document.getElementById("voice-select");
    if (!sel) return;
    const englishVoices = voices.filter(v => v.lang.startsWith("en"));
    sel.innerHTML = englishVoices.map((v, i) =>
        `<option value="${i}" ${selectedVoice && v.name === selectedVoice.name ? "selected" : ""}>
            ${v.name} (${v.lang})
        </option>`
    ).join("");
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
    utterance.pitch = selectedPitch;

    // Use the user-selected or auto-picked voice
    const voices = window.speechSynthesis.getVoices();
    const voice = selectedVoice || pickBestVoice(voices);
    if (voice) utterance.voice = voice;

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

// ========== Transaction Count Extractor ==========
function extractTransactionCount(lower) {
    if (/last\s+(ten|10)/.test(lower) || /last\s+10\s+transaction/.test(lower)) return 10;
    if (/last\s+(five|5)/.test(lower)  || /last\s+5\s+transaction/.test(lower))  return 5;
    if (/last\s+(one|1)/.test(lower)   || /last\s+1\s+transaction/.test(lower))  return 1;
    // "last transaction" / "last transactions" with no number = the most recent one
    if (/last\s+transactions?(\s|$)/.test(lower) && !/last\s+\d/.test(lower)) return 1;
    // bare numbers when awaiting count selection
    if (awaitingTransactionCount) {
        if (/^(one|1)$/.test(lower))  return 1;
        if (/^(five|5)$/.test(lower)) return 5;
        if (/^(ten|10)$/.test(lower)) return 10;
    }
    // generic "last N"
    const m = lower.match(/last\s+(\d+)/);
    return m ? parseInt(m[1], 10) : null;
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

    // Balance
    if (/balance|kitna paisa|kitne paise|how much.*money|how much.*have|how much.*bank|how much.*account|mere.*account|mera.*balance|what.*in my.*account|money.*in.*account|tell.*balance|show.*balance|check.*balance|account.*balance|my balance|bank.*balance|savings.*balance|available.*balance|remaining.*balance|what do i have|what's in my account|whats in my account/i.test(lower)) {
        return { type: "BALANCE" };
    }

    // Transfer
    if (/transfer|send.*money|pay\b|send.*rupees|transfer.*rupees|bhejo|paisa bhejo|payment|send.*to\b|pay.*to\b|rupees.*to\b|rs\.?\s*\d/i.test(lower)) {
        const amountMatch = lower.match(/(\d[\d,]*\.?\d*)/);
        const amount = amountMatch ? parseFloat(amountMatch[1].replace(/,/g, "")) : null;
        let recipient = null;
        const recipientMatch = text.match(/(?:to|for)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)/i);
        if (recipientMatch) recipient = recipientMatch[1];
        return { type: "TRANSFER", amount, recipient };
    }

    // Cheque book
    if (/cheque.*book|check.*book|chequebook|new cheque|issue.*cheque|request.*cheque|order.*cheque|cheque\s*order/i.test(lower)) {
        return { type: "CHEQUE_BOOK" };
    }

    // ── Transaction-specific query (salary, electricity, etc.) ──
    // Must come BEFORE the generic transaction pattern
    if (/check.*salary|salary.*credit|electricity.*bill|check.*electricity|is there.*transaction|any.*transaction.*for|credit.*this month|bill.*this month|check.*if.*credit|credited.*this month|paid.*this month/i.test(lower)) {
        const knownKeywords = ["salary", "electricity", "amazon", "netflix", "petrol", "insurance", "reliance", "upi", "dividend", "atm"];
        const keyword = knownKeywords.find(k => lower.includes(k)) || lower;
        return { type: "TXN_QUERY", keyword };
    }

    // ── Transaction count (last 5, last ten, etc.) ──
    // Must come BEFORE the generic transaction pattern
    const count = extractTransactionCount(lower);
    if (count !== null) {
        return { type: "TXN_COUNT", count };
    }

    // ── Generic transaction mention → ask how many ──
    if (/transaction|recent.*activity|last.*activity|statement|what.*spent|spending|kharcha|history|recent.*purchase|last.*purchase|what.*bought|my.*activity|account.*activity|mini.*statement/i.test(lower)) {
        return { type: "TXN_ASK_COUNT" };
    }

    // Help
    if (/\bhelp\b|assist|support|call.*support|customer.*care|emergency|what.*can.*do|what.*say|kya kar sakte|madad/i.test(lower)) {
        return { type: "HELP" };
    }

    return { type: "UNKNOWN" };
}

// ========== Intent Handlers ==========
function handleVoiceInput(text) {
    resetSessionTimer();
    // Route to transfer screen handlers
    if (currentScreen === "transfer") {
        handleTransferVoiceInput(text);
        return;
    }

    addRecentCommand(text);

    // Post-transaction follow-up takes priority over normal intent parsing
    if (awaitingPostTransactionChoice) {
        showProcessing(false);
        handlePostTransactionChoice(text);
        return;
    }

    // Post-balance / post-cheque-book simple follow-up
    if (awaitingSimpleFollowUp) {
        showProcessing(false);
        handleSimpleFollowUp(text);
        return;
    }

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
        case "TXN_ASK_COUNT":
            handleAskTransactionCount();
            break;
        case "TXN_COUNT":
            handleTransactionCount(intent.count);
            break;
        case "TXN_QUERY":
            handleQueryTransaction(intent.keyword);
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
    awaitingSimpleFollowUp = true;
    respond(`${response} Say main menu to go home, or tell me what you'd like to do next.`);
}

function handleTransfer(intent) {
    // Navigate to the guided transfer screen for full beneficiary flow
    showTransferScreen();
}

function handleConfirmYes() {
    awaitingPostTransactionChoice = false;
    awaitingSimpleFollowUp = false;
    respond("There is nothing to confirm right now. How can I help you?");
}

function handleConfirmNo() {
    awaitingTransactionCount = false;
    awaitingPostTransactionChoice = false;
    awaitingSimpleFollowUp = false;
    respond("Okay. How can I help you?");
}

// ========== Transfer Screen ==========

function showTransferScreen() {
    resetSessionTimer();
    showScreen("transfer");
    currentTransferPhase = "selecting";
    selectedBeneficiary = null;
    pendingTransferAmount = 0;

    // Reset all transfer UI panels
    ["transfer-response-card", "transfer-beneficiary-section", "transfer-selected-card",
     "transfer-confirm-card", "transfer-biometric-card", "transfer-complete-card",
     "transfer-recognized-card", "transfer-processing-indicator"].forEach(id => {
        document.getElementById(id).classList.add("hidden");
    });

    renderBeneficiaryList();
    document.getElementById("transfer-beneficiary-section").classList.remove("hidden");

    const listText = mockBeneficiaries.map((b, i) => `${i + 1}. ${b.name}, ${b.bank}`).join(". ");
    transferRespond(
        `Would you like to transfer money to one of your beneficiaries? ` +
        `Here is your list. ${listText}. ` +
        `Tap a name on screen, or say the name of the beneficiary.`
    );
}

// Feature 3: direct tap selection — show beneficiary rows as large tappable buttons
function renderBeneficiaryList() {
    const listEl = document.getElementById("transfer-beneficiary-list");
    listEl.innerHTML = mockBeneficiaries.map((b, i) => `
        <div class="beneficiary-item" data-idx="${i}"
             role="button" tabindex="0"
             aria-label="Select ${b.name}, ${b.bank}"
             style="display:flex;align-items:center;gap:14px;padding:14px 8px;
                    ${i < mockBeneficiaries.length - 1 ? 'border-bottom:1px solid rgba(255,255,255,0.07);' : ''}">
            <div style="width:44px;height:44px;border-radius:50%;background:rgba(77,158,255,0.15);
                        display:flex;align-items:center;justify-content:center;flex-shrink:0">
                <span class="material-icons-round" style="color:var(--primary,#4D9EFF);font-size:22px"
                      aria-hidden="true">person</span>
            </div>
            <div style="flex:1;min-width:0">
                <div style="font-size:1rem;font-weight:600;color:var(--t1,#fff);white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${b.name}</div>
                <div style="font-size:0.89rem;color:var(--t2,#B2CCE8);margin-top:2px">${b.bank} &bull; ****${b.accountLast4}</div>
            </div>
            <span class="material-icons-round" style="color:rgba(77,158,255,0.55);font-size:1.11rem;flex-shrink:0"
                  aria-hidden="true">chevron_right</span>
        </div>
    `).join("");

    listEl.querySelectorAll(".beneficiary-item").forEach(row => {
        const select = () => {
            resetSessionTimer();
            const b = mockBeneficiaries[parseInt(row.dataset.idx)];
            handleBeneficiaryDirectSelect(b);
        };
        row.addEventListener("click", select);
        row.addEventListener("keydown", e => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); select(); } });
    });
}

// Feature 3: tap selects beneficiary directly without voice name-matching
function handleBeneficiaryDirectSelect(b) {
    selectedBeneficiary = b;
    currentTransferPhase = "entering_amount";

    document.getElementById("transfer-beneficiary-section").classList.add("hidden");
    document.getElementById("transfer-selected-content").innerHTML = `
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
            <div style="width:44px;height:44px;border-radius:50%;background:rgba(77,158,255,0.15);
                        display:flex;align-items:center;justify-content:center;flex-shrink:0">
                <span class="material-icons-round" style="color:var(--primary,#4D9EFF);font-size:24px">person</span>
            </div>
            <div>
                <div style="font-size:1rem;font-weight:600">${b.name}</div>
                <div style="font-size:0.89rem;color:var(--t2,#B2CCE8)">${b.bank} &bull; ****${b.accountLast4}</div>
            </div>
        </div>
        <div style="background:rgba(255,255,255,0.05);border-radius:8px;padding:12px;
                    display:flex;align-items:center;gap:8px">
            <span class="material-icons-round" style="color:var(--success,#00BFA5)">mic</span>
            <span style="color:var(--t2,#B2CCE8)">Say the amount to transfer</span>
        </div>
    `;
    document.getElementById("transfer-selected-card").classList.remove("hidden");
    transferRespond(
        `You selected ${b.name}, ${b.bank} account ending ${b.accountLast4}. ` +
        `How much would you like to transfer? Please say the amount.`
    );
}

function matchTransferBeneficiary(text) {
    const lower = text.toLowerCase();
    // Full name match first
    let match = mockBeneficiaries.find(b => lower.includes(b.name.toLowerCase()));
    if (match) return match;
    // First/last name match (min 3 chars to avoid noise)
    for (const b of mockBeneficiaries) {
        const parts = b.name.toLowerCase().split(" ");
        if (parts.some(part => part.length > 2 && lower.includes(part))) return b;
    }
    return null;
}

function handleTransferVoiceInput(text) {
    if (!text.trim()) return;
    resetSessionTimer();
    showRecognizedText(text);
    switch (currentTransferPhase) {
        case "selecting":       handleBeneficiarySelection(text); break;
        case "entering_amount": handleTransferAmountInput(text);  break;
        case "confirming":      handleTransferConfirmation(text); break;
        case "authenticating":  break; // fingerprint pending — ignore voice
        case "complete":        handleTransferCompleteVoice(text); break;
        default: break;
    }
}

// Feature 1: voice navigation from the transfer-complete screen
function handleTransferCompleteVoice(text) {
    const lower = text.toLowerCase().trim();
    const isDone = /^(done|home|main menu|go home|go back|back|exit|finish|ok|okay|return|menu|yes)$/.test(lower);
    if (isDone) {
        vibrate(50);
        speak("Returning to main menu.", () => {
            currentTransferPhase = null;
            showScreen("home");
        });
    } else {
        transferRespond("Transfer is complete. Say Done or Main Menu to return home, or tap the Done button.");
    }
}

function handleBeneficiarySelection(text) {
    const b = matchTransferBeneficiary(text);
    if (!b) {
        transferRespond("Sorry, I could not find that beneficiary. Please say one of the names from the list.");
        return;
    }
    selectedBeneficiary = b;
    currentTransferPhase = "entering_amount";

    document.getElementById("transfer-beneficiary-section").classList.add("hidden");
    document.getElementById("transfer-selected-content").innerHTML = `
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px">
            <div style="width:44px;height:44px;border-radius:50%;background:rgba(66,165,245,0.15);
                        display:flex;align-items:center;justify-content:center;flex-shrink:0">
                <span class="material-icons-round" style="color:#42A5F5;font-size:24px">person</span>
            </div>
            <div>
                <div class="txn-desc" style="font-size:1rem;font-weight:600">${b.name}</div>
                <div class="txn-date">${b.bank} &bull; ****${b.accountLast4}</div>
            </div>
        </div>
        <div style="background:rgba(255,255,255,0.05);border-radius:8px;padding:12px;
                    display:flex;align-items:center;gap:8px">
            <span class="material-icons-round" style="color:#66BB6A">account_balance</span>
            <span style="color:var(--text-light,#90A4AE)">Say the amount to transfer</span>
        </div>
    `;
    document.getElementById("transfer-selected-card").classList.remove("hidden");
    transferRespond(`You selected ${b.name}, ${b.bank} account ending ${b.accountLast4}. How much would you like to transfer?`);
}

function handleTransferAmountInput(text) {
    const amtMatch = text.match(/(\d[\d,]*\.?\d*)/);
    const amount = amtMatch ? parseFloat(amtMatch[1].replace(/,/g, "")) : null;

    if (!amount || amount <= 0) {
        transferRespond("Sorry, I could not understand the amount. Please say the amount. For example, say 500 rupees.");
        return;
    }

    pendingTransferAmount = amount;
    currentTransferPhase = "confirming";

    const b = selectedBeneficiary;
    const amtDisplay = amount.toLocaleString("en-IN");
    document.getElementById("transfer-selected-card").classList.add("hidden");
    document.getElementById("transfer-confirm-content").innerHTML = `
        <div style="display:flex;justify-content:space-between;margin-bottom:8px">
            <span style="color:var(--text-light,#90A4AE)">To</span>
            <span style="font-weight:600">${b.name}</span>
        </div>
        <div style="display:flex;justify-content:space-between;margin-bottom:12px">
            <span style="color:var(--text-light,#90A4AE)">Bank</span>
            <span>${b.bank} &bull; ****${b.accountLast4}</span>
        </div>
        <hr style="border:none;border-top:1px solid rgba(255,255,255,0.1);margin-bottom:12px">
        <div style="display:flex;justify-content:space-between;align-items:center">
            <span style="color:var(--text-light,#90A4AE)">Amount</span>
            <span style="font-size:1.5rem;font-weight:700;color:#66BB6A">&#8377;${amtDisplay}</span>
        </div>
    `;
    document.getElementById("transfer-confirm-card").classList.remove("hidden");
    transferRespond(`You are about to transfer ${amtDisplay} rupees to ${b.name}, ${b.bank} account ending ${b.accountLast4}. Say yes to confirm or no to cancel.`);
}

function handleTransferConfirmation(text) {
    const lower = text.toLowerCase().trim();
    const isYes = /^(yes|yeah|yep|haan|ha|ji|confirm|ok|okay|proceed)/.test(lower);
    const isNo  = /^(no|nope|cancel|nahi|stop)/.test(lower);

    if (isYes) {
        showTransferBiometric();
    } else if (isNo) {
        document.getElementById("transfer-confirm-card").classList.add("hidden");
        currentTransferPhase = "selecting";
        selectedBeneficiary = null;
        pendingTransferAmount = 0;
        renderBeneficiaryList();
        document.getElementById("transfer-beneficiary-section").classList.remove("hidden");
        transferRespond("Transfer cancelled. Please say a beneficiary name to start a new transfer.");
    } else {
        transferRespond("Please say yes to confirm or no to cancel the transfer.");
    }
}

function showTransferBiometric() {
    document.getElementById("transfer-confirm-card").classList.add("hidden");
    currentTransferPhase = "authenticating";

    const b = selectedBeneficiary;
    const amtDisplay = pendingTransferAmount.toLocaleString("en-IN");
    document.getElementById("transfer-biometric-detail").textContent =
        `₹${amtDisplay} → ${b.name} (${b.bank})`;
    document.getElementById("transfer-biometric-card").classList.remove("hidden");
    transferRespond("Please authenticate with your fingerprint to authorize this transfer.");
}

function executeWebTransfer() {
    document.getElementById("transfer-confirm-card").classList.add("hidden");
    document.getElementById("transfer-processing-indicator").classList.remove("hidden");

    setTimeout(() => {
        document.getElementById("transfer-processing-indicator").classList.add("hidden");

        const b = selectedBeneficiary;
        const amount = pendingTransferAmount;
        mockAccount.balance -= amount;
        const txnId = "TXN" + Math.floor(100000 + Math.random() * 900000);
        const amtDisplay = amount.toLocaleString("en-IN");
        const balDisplay = mockAccount.balance.toLocaleString("en-IN");

        currentTransferPhase = "complete";

        document.getElementById("transfer-complete-content").innerHTML = `
            <p style="color:var(--text-light,#90A4AE);margin-bottom:12px">
                &#8377;${amtDisplay} sent to ${b.name}
            </p>
            <div style="background:rgba(255,255,255,0.05);border-radius:8px;padding:12px">
                <div style="display:flex;justify-content:space-between;margin-bottom:6px">
                    <span style="color:var(--text-light,#90A4AE);font-size:0.85rem">Bank</span>
                    <span style="font-size:0.85rem">${b.bank} &bull; ****${b.accountLast4}</span>
                </div>
                <div style="display:flex;justify-content:space-between;margin-bottom:12px">
                    <span style="color:var(--text-light,#90A4AE);font-size:0.85rem">Ref. No.</span>
                    <span style="font-size:0.85rem">${txnId}</span>
                </div>
                <hr style="border:none;border-top:1px solid rgba(255,255,255,0.1);margin-bottom:12px">
                <div style="display:flex;justify-content:space-between;align-items:center">
                    <span style="color:var(--text-light,#90A4AE)">Remaining Balance</span>
                    <span style="font-weight:700;color:var(--primary,#4D9EFF);font-size:1.1rem">&#8377;${balDisplay}</span>
                </div>
            </div>
            <div class="voice-hint-row" style="margin-top:14px">
                <span class="material-icons-round" aria-hidden="true">mic</span>
                Say <strong>"Done"</strong> or <strong>"Main Menu"</strong> to go home
            </div>
        `;
        document.getElementById("transfer-complete-card").classList.remove("hidden");

        vibrateSuccess();
        transferRespond(
            `Transfer successful! ${amtDisplay} rupees have been sent to ${b.name}. ` +
            `Your account balance is now ${balDisplay} rupees. ` +
            `Transaction reference ${txnId}. ` +
            `Say Done or Main Menu to return home.`
        );
    }, 1000);
}

function transferRespond(text) {
    const card = document.getElementById("transfer-response-card");
    document.getElementById("transfer-response-text").textContent = text;
    card.classList.remove("hidden");
    speak(text);
}

function handleChequeBook() {
    const response = "Your cheque book request has been registered successfully and will be delivered within 5 working days.";
    awaitingSimpleFollowUp = true;
    respond(`${response} Say main menu to go home, or tell me what you'd like to do next.`);
    vibrateSuccess();
}

// Handles the single follow-up after balance or cheque book
function handleSimpleFollowUp(text) {
    awaitingSimpleFollowUp = false;
    const lower = text.toLowerCase().trim();
    const wantsHome = /main menu|home|done|back|exit|okay|no|nothing|enough|stop/.test(lower);
    if (wantsHome) {
        respond("Okay! How else can I help you? You can check balance, transfer money, request cheque book, or ask for help.");
    } else {
        // Treat as a fresh command
        const intent = parseIntent(text);
        setTimeout(() => {
            showProcessing(false);
            handleIntent(intent);
        }, 300);
    }
}

// Step 1 — ask the user how many transactions they want to hear
function handleAskTransactionCount() {
    awaitingTransactionCount = true;
    awaitingPostTransactionChoice = false;
    respond("Do you want to hear the last transaction, last 5 transactions, or last 10 transactions?");
}

// Step 2 — read out N transactions, then ask what user wants next
function handleTransactionCount(count) {
    awaitingTransactionCount = false;
    const subset = mockTransactions.slice(0, count);
    showTransactions(subset);

    if (subset.length === 0) {
        respond("You have no recent transactions.");
        return;
    }

    let details;
    if (count === 1) {
        const t = subset[0];
        const action = t.type === "CREDIT" ? "credited" : "debited";
        details = `Your last transaction: ${formatAmount(t.amount)} rupees ${action} for ${t.description} on ${t.date}.`;
    } else {
        details = `Here are your last ${subset.length} transactions. `;
        subset.forEach((t, i) => {
            const action = t.type === "CREDIT" ? "credited" : "debited";
            details += `${i + 1}. ${t.description}, ${formatAmount(t.amount)} rupees ${action} on ${t.date}. `;
        });
        details = details.trim();
    }

    // Follow-up prompt after reading transactions
    awaitingPostTransactionChoice = true;
    respond(`${details} Would you like to hear more transactions, or say main menu to go back home?`);
}

// Handle user's post-transaction choice
function handlePostTransactionChoice(text) {
    const lower = text.toLowerCase().trim();
    const wantsMore = /more|another|yes|again|transaction|last|hear|show/.test(lower);
    const wantsHome = /main menu|home|done|no|back|enough|stop|okay|that.s all|exit/.test(lower);

    if (wantsMore) {
        awaitingPostTransactionChoice = false;
        handleAskTransactionCount();
    } else if (wantsHome) {
        awaitingPostTransactionChoice = false;
        respond("Okay! How else can I help you? You can check balance, transfer money, request cheque book, or ask for help.");
    } else {
        // Re-prompt without clearing the flag
        respond("Say 'more transactions' to hear more, or 'main menu' to go back home.");
    }
}

// Query — search transactions by keyword, then follow-up
function handleQueryTransaction(keyword) {
    const matches = mockTransactions.filter(t =>
        t.description.toLowerCase().includes(keyword.toLowerCase())
    );

    if (matches.length === 0) {
        respond("There are no transactions matching your request for this month.");
        return;
    }

    const t = matches[0];
    const amt = formatAmount(t.amount);
    const details = t.type === "CREDIT"
        ? `Yes, ${t.description} of ${amt} rupees was credited on ${t.date}.`
        : `Yes, a payment of ${amt} rupees was made on ${t.date} for ${t.description}.`;

    awaitingPostTransactionChoice = true;
    respond(`${details} Would you like to check another transaction, or say main menu to go back?`);
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
    if (currentScreen === "transfer") {
        document.getElementById("transfer-recognized-text").textContent = '"' + text + '"';
        document.getElementById("transfer-recognized-card").classList.remove("hidden");
    } else {
        els.recognizedText.textContent = '"' + text + '"';
        els.recognizedCard.classList.remove("hidden");
    }
}

function showTransactions(transactions) {
    els.transactionsList.innerHTML = transactions.map(txn => `
        <div class="txn-row" role="listitem"
             aria-label="${txn.description}, ${txn.type === "DEBIT" ? "debit" : "credit"} of ${txn.amount.toLocaleString("en-IN")} rupees on ${txn.date}">
            <div>
                <div class="txn-desc">${txn.description}</div>
                <div class="txn-date">${txn.date}</div>
            </div>
            <div class="txn-amount ${txn.type.toLowerCase()}" aria-hidden="true">
                ${txn.type === "DEBIT" ? "−" : "+"}&#8377;${txn.amount.toLocaleString("en-IN")}
            </div>
        </div>
    `).join("");
    els.transactionsCard.classList.remove("hidden");
}

function updateListeningUI(listening) {
    // Home screen
    if (listening) {
        els.micBtn.classList.add("listening");
        els.listeningIndicator.classList.remove("hidden");
        els.micBtn.setAttribute("aria-label", "Microphone active. Tap to stop listening.");
    } else {
        els.micBtn.classList.remove("listening");
        els.listeningIndicator.classList.add("hidden");
        els.micBtn.setAttribute("aria-label", "Tap to speak a command.");
    }
    // Transfer screen
    const tMic = document.getElementById("transfer-mic-btn");
    const tInd = document.getElementById("transfer-listening-indicator");
    if (tMic && tInd) {
        if (listening) { tMic.classList.add("listening"); tInd.classList.remove("hidden"); }
        else           { tMic.classList.remove("listening"); tInd.classList.add("hidden"); }
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

// ========== Session Timeout (Feature 2) ==========
function resetSessionTimer() {
    // Don't run timer on splash / auth screens
    if (currentScreen === "splash" || currentScreen === "auth") return;

    clearTimeout(sessionIdleTimer);
    clearTimeout(sessionExpireTimer);
    clearInterval(sessionCountdownInterval);
    hideSessionWarning();

    sessionIdleTimer = setTimeout(() => {
        showSessionWarning();
        let remaining = Math.round(SESSION_WARNING_MS / 1000);
        const countEl = document.getElementById("session-countdown");
        sessionCountdownInterval = setInterval(() => {
            remaining--;
            if (countEl) countEl.textContent = remaining;
            if (remaining <= 0) clearInterval(sessionCountdownInterval);
        }, 1000);
        sessionExpireTimer = setTimeout(expireSession, SESSION_WARNING_MS);
    }, SESSION_IDLE_MS);
}

function showSessionWarning() {
    const banner = document.getElementById("session-warning-banner");
    if (banner) banner.classList.remove("hidden");
    speak("Your session will expire in 30 seconds due to inactivity. Tap or speak to continue.");
}

function hideSessionWarning() {
    const banner = document.getElementById("session-warning-banner");
    if (banner) banner.classList.add("hidden");
    const countEl = document.getElementById("session-countdown");
    if (countEl) countEl.textContent = "30";
}

function expireSession() {
    clearInterval(sessionCountdownInterval);
    hideSessionWarning();
    currentTransferPhase = null;
    stopSpeaking();
    speak("Session expired due to inactivity. Please authenticate again.", () => {
        showScreen("auth");
    });
}

// ========== App Flow ==========
function startApp() {
    showScreen("splash");
    speak("Welcome to Saral. Your accessible banking assistant.", () => {
        setTimeout(() => {
            showScreen("auth");
            speak("Please press your finger for authentication.");
        }, 500);
    });

    setTimeout(() => {
        if (currentScreen === "splash") {
            showScreen("auth");
            speak("Please press your finger for authentication.");
        }
    }, 4000);
}

// ========== Event Listeners ==========

// Global activity listener — any tap/keypress resets the session timer
document.addEventListener("click",   () => resetSessionTimer(), { passive: true });
document.addEventListener("keydown",  () => resetSessionTimer(), { passive: true });
document.addEventListener("touchstart", () => resetSessionTimer(), { passive: true });

// Session warning "Stay" button
document.getElementById("session-continue-btn")?.addEventListener("click", (e) => {
    e.stopPropagation();
    resetSessionTimer();
});

// Auth — biometric tap goes directly to home; start session timer on login
els.authBtn.addEventListener("click", () => {
    vibrate(100);
    vibrateSuccess();
    speak("Biometric scan successful. Welcome.", () => {
        showScreen("home");
        resetSessionTimer();   // start session clock after login
        speak("You can say: check balance, transfer money, request cheque book, show recent transactions, or help.");
    });
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
    els.speedValue.textContent = speechRate.toFixed(2) + "x";
});

// Load voices — Chrome fires onvoiceschanged asynchronously
if ("speechSynthesis" in window) {
    window.speechSynthesis.onvoiceschanged = loadVoices;
    loadVoices(); // also try immediately (Firefox loads synchronously)
}

// Voice selector
const voiceSelectEl = document.getElementById("voice-select");
if (voiceSelectEl) {
    voiceSelectEl.addEventListener("change", (e) => {
        const voices = window.speechSynthesis.getVoices().filter(v => v.lang.startsWith("en"));
        selectedVoice = voices[parseInt(e.target.value)] || null;
        speak("This is how I sound now.");
    });
}

// Pitch slider
const pitchSlider = document.getElementById("pitch-slider");
const pitchValue = document.getElementById("pitch-value");
if (pitchSlider) {
    pitchSlider.addEventListener("input", (e) => {
        selectedPitch = parseFloat(e.target.value);
        if (pitchValue) pitchValue.textContent = selectedPitch.toFixed(1);
    });
    pitchSlider.addEventListener("change", () => {
        speak("This is how I sound now.");
    });
}

// ========== Transfer Screen Event Listeners ==========

document.getElementById("transfer-back-btn").addEventListener("click", () => {
    vibrate(50);
    currentTransferPhase = null;
    showScreen("home");
});

document.getElementById("transfer-mic-btn").addEventListener("click", () => {
    if (isListening) { stopListening(); } else { startListening(); }
});

document.getElementById("transfer-text-input").addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        const text = e.target.value.trim();
        if (text) { e.target.value = ""; handleTransferVoiceInput(text); }
    }
});

document.getElementById("transfer-send-btn").addEventListener("click", () => {
    const inp = document.getElementById("transfer-text-input");
    const text = inp.value.trim();
    if (text) { inp.value = ""; handleTransferVoiceInput(text); }
});

document.getElementById("transfer-biometric-btn").addEventListener("click", () => {
    vibrate(100);
    document.getElementById("transfer-biometric-card").classList.add("hidden");
    speak("Biometric scan successful.", () => executeWebTransfer());
});

document.getElementById("transfer-confirm-yes-btn").addEventListener("click", () => {
    vibrate(50);
    showTransferBiometric();
});

document.getElementById("transfer-confirm-no-btn").addEventListener("click", () => {
    vibrate(50);
    handleTransferConfirmation("no");
});

document.getElementById("transfer-done-btn").addEventListener("click", () => {
    vibrate(50);
    currentTransferPhase = null;
    showScreen("home");
    speak("Transfer complete. How can I help you?");
});

// Update quick command to trigger guided transfer flow
document.querySelectorAll(".quick-cmd[data-cmd='Transfer 500 rupees to Rahul']").forEach(btn => {
    btn.dataset.cmd = "Transfer money";
});

// ========== Zoom / Text Size ==========
const ZOOM_STEPS  = [14, 16, 18, 20, 22, 24];          // html font-size in px
const ZOOM_LABELS = ["Smallest", "Small", "Normal", "Large", "Larger", "Largest"];
let currentZoomIdx = 2;                                  // default = 18 px (Normal)

function applyZoom(idx) {
    currentZoomIdx = Math.max(0, Math.min(ZOOM_STEPS.length - 1, idx));
    document.documentElement.style.fontSize = ZOOM_STEPS[currentZoomIdx] + "px";
    localStorage.setItem("saral-zoom", currentZoomIdx);
    updateZoomUI();
}

function updateZoomUI() {
    const isMin = currentZoomIdx <= 0;
    const isMax = currentZoomIdx >= ZOOM_STEPS.length - 1;
    const label = ZOOM_LABELS[currentZoomIdx];

    document.querySelectorAll(".zoom-in-trigger").forEach(btn => {
        btn.disabled = isMax;
    });
    document.querySelectorAll(".zoom-out-trigger").forEach(btn => {
        btn.disabled = isMin;
    });

    const labelEl = document.getElementById("zoom-level-label");
    if (labelEl) labelEl.textContent = label;
}

// Restore saved zoom on load
(function () {
    const saved = localStorage.getItem("saral-zoom");
    applyZoom(saved !== null ? parseInt(saved, 10) : 2);
})();

// Wire all zoom buttons (home top bar + settings card share the same trigger classes)
document.querySelectorAll(".zoom-in-trigger").forEach(btn => {
    btn.addEventListener("click", () => {
        vibrate(30);
        applyZoom(currentZoomIdx + 1);
    });
});
document.querySelectorAll(".zoom-out-trigger").forEach(btn => {
    btn.addEventListener("click", () => {
        vibrate(30);
        applyZoom(currentZoomIdx - 1);
    });
});

// ========== aria-pressed on language buttons ==========
document.querySelectorAll(".lang-btn").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".lang-btn").forEach(b =>
            b.setAttribute("aria-pressed", "false"));
        btn.setAttribute("aria-pressed", "true");
    });
});

// ========== Initialize ==========
window.addEventListener("DOMContentLoaded", () => {
    const speechInited = initSpeechRecognition();
    console.log("Speech recognition supported:", speechInited);

    setTimeout(startApp, 500);
});
