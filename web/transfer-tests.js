/**
 * Saral Web — Transfer Flow Unit Tests
 *
 * Run with Node.js:  node transfer-tests.js
 *
 * Tests all pure-logic functions extracted from app.js without requiring
 * a browser, DOM, or Speech APIs.
 */

"use strict";

// ── Test harness ─────────────────────────────────────────────────────────────

let passed = 0;
let failed = 0;
const failures = [];

function test(name, fn) {
    try {
        fn();
        passed++;
        console.log(`  ✓  ${name}`);
    } catch (err) {
        failed++;
        failures.push({ name, err });
        console.error(`  ✗  ${name}`);
        console.error(`       ${err.message}`);
    }
}

function assertEqual(actual, expected, msg = "") {
    if (actual !== expected) {
        throw new Error(
            `Expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)}${msg ? " — " + msg : ""}`
        );
    }
}

function assertTrue(val, msg = "") {
    if (!val) throw new Error(`Expected truthy but got ${JSON.stringify(val)}${msg ? " — " + msg : ""}`);
}

function assertNull(val, msg = "") {
    if (val !== null) throw new Error(`Expected null but got ${JSON.stringify(val)}${msg ? " — " + msg : ""}`);
}

// ── Logic under test (copied from app.js, without DOM/Browser references) ───

const mockBeneficiaries = [
    { id: "BEN001", name: "Rahul Sharma",  accountLast4: "1234", bank: "SBI" },
    { id: "BEN002", name: "Priya Gupta",   accountLast4: "5678", bank: "HDFC Bank" },
    { id: "BEN003", name: "Amit Kumar",    accountLast4: "9012", bank: "ICICI Bank" },
    { id: "BEN004", name: "Sunita Verma",  accountLast4: "3456", bank: "Axis Bank" },
    { id: "BEN005", name: "Vikram Singh",  accountLast4: "7890", bank: "Bank of Baroda" }
];

function matchTransferBeneficiary(text) {
    const lower = text.toLowerCase();
    let match = mockBeneficiaries.find(b => lower.includes(b.name.toLowerCase()));
    if (match) return match;
    for (const b of mockBeneficiaries) {
        const parts = b.name.toLowerCase().split(" ");
        if (parts.some(part => part.length > 2 && lower.includes(part))) return b;
    }
    return null;
}

function parseTransferAmount(text) {
    const amtMatch = text.match(/(\d[\d,]*\.?\d*)/);
    return amtMatch ? parseFloat(amtMatch[1].replace(/,/g, "")) : null;
}

function isTransferYes(lower) {
    return /^(yes|yeah|yep|haan|ha|ji|confirm|ok|okay|proceed)/.test(lower.trim());
}

function isTransferNo(lower) {
    return /^(no|nope|cancel|nahi|stop)/.test(lower.trim());
}

function normalizeOtpInput(text) {
    const wordMap = { zero:"0", one:"1", two:"2", three:"3", four:"4",
                      five:"5", six:"6", seven:"7", eight:"8", nine:"9" };
    let result = text.toLowerCase();
    Object.entries(wordMap).forEach(([word, digit]) => {
        result = result.replace(new RegExp(`\\b${word}\\b`, "g"), digit);
    });
    return result.replace(/\D/g, "");
}

function extractTransactionCount(lower) {
    if (/last\s+(ten|10)/.test(lower) || /last\s+10\s+transaction/.test(lower)) return 10;
    if (/last\s+(five|5)/.test(lower)  || /last\s+5\s+transaction/.test(lower))  return 5;
    if (/last\s+(one|1)/.test(lower)   || /last\s+1\s+transaction/.test(lower))  return 1;
    if (/last\s+transactions?(\s|$)/.test(lower) && !/last\s+\d/.test(lower)) return 1;
    const m = lower.match(/last\s+(\d+)/);
    return m ? parseInt(m[1], 10) : null;
}

function parseIntentType(text) {
    const lower = text.toLowerCase().trim();
    if (/^(yes|yeah|yep|yup|confirm|ok|okay|sure|go ahead|proceed|do it|haan|ha|ji|haa|theek hai|theek)$/i.test(lower)) return "CONFIRM_YES";
    if (/^(no|nope|cancel|stop|nahi|mat karo|don't|na|ruko|band karo)$/i.test(lower)) return "CONFIRM_NO";
    if (/balance|kitna paisa|how much.*money|my balance|account.*balance|check.*balance/i.test(lower)) return "BALANCE";
    if (/transfer|send.*money|pay\b|send.*rupees|transfer.*rupees/i.test(lower)) return "TRANSFER";
    if (/cheque.*book|check.*book|chequebook|new cheque/i.test(lower)) return "CHEQUE_BOOK";
    if (/check.*salary|salary.*credit|electricity.*bill|is there.*transaction|any.*transaction.*for/i.test(lower)) return "TXN_QUERY";
    const count = extractTransactionCount(lower);
    if (count !== null) return "TXN_COUNT";
    if (/transaction|recent.*activity|statement|history/i.test(lower)) return "TXN_ASK_COUNT";
    if (/\bhelp\b|assist|support/i.test(lower)) return "HELP";
    return "UNKNOWN";
}

// ── Test Suite ────────────────────────────────────────────────────────────────

console.log("\n── Beneficiary Matching ──────────────────────────────────\n");

test("matches by full name", () => {
    const b = matchTransferBeneficiary("Rahul Sharma");
    assertTrue(b !== null);
    assertEqual(b.name, "Rahul Sharma");
});

test("matches by first name", () => {
    const b = matchTransferBeneficiary("Priya");
    assertTrue(b !== null);
    assertEqual(b.name, "Priya Gupta");
});

test("matches by last name", () => {
    const b = matchTransferBeneficiary("Kumar");
    assertTrue(b !== null);
    assertEqual(b.name, "Amit Kumar");
});

test("matches in a phrase — 'transfer to Rahul'", () => {
    const b = matchTransferBeneficiary("transfer to Rahul");
    assertTrue(b !== null);
    assertEqual(b.name, "Rahul Sharma");
});

test("matches Sunita by first name", () => {
    const b = matchTransferBeneficiary("Sunita");
    assertTrue(b !== null);
    assertEqual(b.name, "Sunita Verma");
});

test("matches Vikram by first name", () => {
    const b = matchTransferBeneficiary("Vikram");
    assertTrue(b !== null);
    assertEqual(b.name, "Vikram Singh");
});

test("case-insensitive match", () => {
    const b = matchTransferBeneficiary("rahul sharma");
    assertTrue(b !== null);
    assertEqual(b.name, "Rahul Sharma");
});

test("no match returns null", () => {
    const b = matchTransferBeneficiary("Ranjit Kashyap Mehrotra");
    assertNull(b);
});

test("short noise word (2 chars) does not match", () => {
    // "to" appears in names — should NOT match anything on its own
    const b = matchTransferBeneficiary("to");
    assertNull(b);
});

console.log("\n── Amount Parsing ────────────────────────────────────────\n");

test("parses plain integer", () => {
    assertEqual(parseTransferAmount("500 rupees"), 500);
});

test("parses comma-formatted amount", () => {
    assertEqual(parseTransferAmount("1,250 rupees"), 1250);
});

test("parses decimal amount", () => {
    assertEqual(parseTransferAmount("750.50"), 750.50);
});

test("parses large comma-formatted", () => {
    assertEqual(parseTransferAmount("10,000"), 10000);
});

test("parses bare number", () => {
    assertEqual(parseTransferAmount("2000"), 2000);
});

test("returns null when no number present", () => {
    assertNull(parseTransferAmount("rupees please"));
});

test("returns null for empty string", () => {
    assertNull(parseTransferAmount(""));
});

console.log("\n── Transfer Confirmation Detection ──────────────────────\n");

test("'yes' is a confirm", ()   => assertTrue(isTransferYes("yes")));
test("'haan' is a confirm", ()  => assertTrue(isTransferYes("haan")));
test("'ok' is a confirm", ()    => assertTrue(isTransferYes("ok")));
test("'okay' is a confirm", ()  => assertTrue(isTransferYes("okay")));
test("'confirm' is a confirm",  () => assertTrue(isTransferYes("confirm")));
test("'proceed' is a confirm",  () => assertTrue(isTransferYes("proceed")));
test("'no' is a cancel", ()     => assertTrue(isTransferNo("no")));
test("'nahi' is a cancel", ()   => assertTrue(isTransferNo("nahi")));
test("'cancel' is a cancel", () => assertTrue(isTransferNo("cancel")));
test("'nope' is a cancel", ()   => assertTrue(isTransferNo("nope")));
test("'maybe later' is neither yes nor no", () => {
    const lower = "maybe later";
    assertTrue(!isTransferYes(lower) && !isTransferNo(lower));
});

console.log("\n── OTP Normalization ─────────────────────────────────────\n");

test("normalizes digit words", () => {
    assertEqual(normalizeOtpInput("one two three four"), "1234");
});

test("normalizes mixed — word and digit", () => {
    assertEqual(normalizeOtpInput("5 six 7 eight"), "5678");
});

test("strips non-digit characters", () => {
    assertEqual(normalizeOtpInput("1-2-3-4"), "1234");
});

test("all-digit string unchanged", () => {
    assertEqual(normalizeOtpInput("4567"), "4567");
});

test("uppercase word forms normalised", () => {
    assertEqual(normalizeOtpInput("NINE EIGHT SEVEN SIX"), "9876");
});

console.log("\n── Transaction Count Extraction ─────────────────────────\n");

test("'last 10 transactions' → 10", () => {
    assertEqual(extractTransactionCount("last 10 transactions"), 10);
});

test("'last ten transactions' → 10", () => {
    assertEqual(extractTransactionCount("last ten transactions"), 10);
});

test("'last 5 transactions' → 5", () => {
    assertEqual(extractTransactionCount("last 5 transactions"), 5);
});

test("'last five transactions' → 5", () => {
    assertEqual(extractTransactionCount("last five transactions"), 5);
});

test("'last 1 transaction' → 1", () => {
    assertEqual(extractTransactionCount("last 1 transaction"), 1);
});

test("'last transaction' (bare, no number) → 1", () => {
    assertEqual(extractTransactionCount("last transaction"), 1);
});

test("'last transactions' (bare, no number) → 1", () => {
    assertEqual(extractTransactionCount("last transactions"), 1);
});

test("'last 3' → 3", () => {
    assertEqual(extractTransactionCount("last 3"), 3);
});

test("unrelated string → null", () => {
    assertNull(extractTransactionCount("check my balance"));
});

console.log("\n── Intent Parser ─────────────────────────────────────────\n");

test("'check my balance' → BALANCE", () => {
    assertEqual(parseIntentType("check my balance"), "BALANCE");
});

test("'transfer money' → TRANSFER", () => {
    assertEqual(parseIntentType("transfer money"), "TRANSFER");
});

test("'transfer 500 to Rahul' → TRANSFER", () => {
    assertEqual(parseIntentType("Transfer 500 rupees to Rahul"), "TRANSFER");
});

test("'send money to Priya' → TRANSFER", () => {
    assertEqual(parseIntentType("Send money to Priya"), "TRANSFER");
});

test("'request cheque book' → CHEQUE_BOOK", () => {
    assertEqual(parseIntentType("Request cheque book"), "CHEQUE_BOOK");
});

test("'check salary' → TXN_QUERY", () => {
    assertEqual(parseIntentType("Check salary credit"), "TXN_QUERY");
});

test("'last 5 transactions' → TXN_COUNT", () => {
    assertEqual(parseIntentType("last 5 transactions"), "TXN_COUNT");
});

test("'show recent transactions' → TXN_ASK_COUNT", () => {
    assertEqual(parseIntentType("Show recent transactions"), "TXN_ASK_COUNT");
});

test("'Help' → HELP", () => {
    assertEqual(parseIntentType("Help"), "HELP");
});

test("'yes' → CONFIRM_YES", () => {
    assertEqual(parseIntentType("yes"), "CONFIRM_YES");
});

test("'no' → CONFIRM_NO", () => {
    assertEqual(parseIntentType("no"), "CONFIRM_NO");
});

test("gibberish → UNKNOWN", () => {
    assertEqual(parseIntentType("blah blah bloop"), "UNKNOWN");
});

// ── Summary ───────────────────────────────────────────────────────────────────

console.log(`\n${"─".repeat(55)}`);
console.log(`Results: ${passed} passed, ${failed} failed`);

if (failures.length) {
    console.log("\nFailed tests:");
    failures.forEach(f => console.error(`  ✗ ${f.name}: ${f.err.message}`));
    process.exit(1);
} else {
    console.log("All tests passed ✓");
    process.exit(0);
}
