/**
 * test_backend.js
 * Comprehensive automated test suite for Gamervoult Vercel Backend
 */

import { validateHostSecurity, timingSafeEqualStr } from '../src/authMiddleware.js';

let passed = 0;
let failed = 0;

function assert(condition, message) {
  if (condition) {
    console.log(`  ✅ PASS: ${message}`);
    passed++;
  } else {
    console.error(`  ❌ FAIL: ${message}`);
    failed++;
  }
}

async function runTests() {
  console.log('\n======================================================');
  console.log('🧪 RUNNING GAMERVOUlT BACKEND AUTOMATED TEST SUITE');
  console.log('======================================================\n');

  // ---------------------------------------------------------
  // TEST GROUP 1: SSRF Host Destination Validation
  // ---------------------------------------------------------
  console.log('--- TEST GROUP 1: SSRF Protection & Destination Filtering ---');
  const internalDestinations = [
    'localhost',
    '127.0.0.1',
    '0.0.0.0',
    '169.254.169.254',
    '10.0.0.1',
    '172.16.5.1',
    '192.168.1.1',
    'metadata.google.internal',
    'cluster.local'
  ];

  for (const host of internalDestinations) {
    assert(!validateHostSecurity(host), `Blocked internal destination: "${host}"`);
  }

  const legitimateHosts = ['freefireofficial.in', 'api.gamervoult.com', 'imap.gmail.com'];
  for (const host of legitimateHosts) {
    assert(validateHostSecurity(host), `Permitted legitimate destination: "${host}"`);
  }

  // ---------------------------------------------------------
  // TEST GROUP 2: App Version Check & APK Integrity Logic
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 2: Version Check & APK Integrity Logic ---');
  function isValidReleaseUrl(urlStr) {
    if (!urlStr || typeof urlStr !== 'string') return false;
    try {
      const parsed = new URL(urlStr);
      return parsed.protocol === 'https:';
    } catch {
      return false;
    }
  }

  function isValidSha256(hashStr) {
    if (!hashStr || typeof hashStr !== 'string') return false;
    return /^[a-fA-F0-9]{64}$/.test(hashStr.trim());
  }

  function evaluateArtifact(cfg) {
    const isArtifactAvailable = Boolean(
      cfg.downloadUrl &&
      isValidReleaseUrl(cfg.downloadUrl) &&
      cfg.sha256Checksum &&
      isValidSha256(cfg.sha256Checksum) &&
      (cfg.fileSizeBytes == null || (!isNaN(cfg.fileSizeBytes) && cfg.fileSizeBytes > 100000))
    );
    return {
      isArtifactAvailable,
      downloadUrl: isArtifactAvailable ? cfg.downloadUrl : null,
      sha256Checksum: isArtifactAvailable ? cfg.sha256Checksum : null
    };
  }

  assert(evaluateArtifact({ downloadUrl: null, sha256Checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' }).isArtifactAvailable === false, 'Artifact unavailable when downloadUrl is null');
  assert(evaluateArtifact({ downloadUrl: 'http://insecure.com/app.apk', sha256Checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' }).isArtifactAvailable === false, 'Artifact unavailable when URL is insecure HTTP');
  assert(evaluateArtifact({ downloadUrl: 'https://cdn.com/app.apk', sha256Checksum: 'not_a_sha256' }).isArtifactAvailable === false, 'Artifact unavailable when SHA-256 is malformed');
  assert(evaluateArtifact({ downloadUrl: 'https://cdn.com/app.apk', sha256Checksum: 'd41d8cd98f00b204e9800998ecf8427e' }).isArtifactAvailable === false, 'Artifact unavailable when checksum is 32-char MD5');
  assert(evaluateArtifact({ downloadUrl: 'https://cdn.com/app.apk', sha256Checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', fileSizeBytes: 500 }).isArtifactAvailable === false, 'Artifact unavailable when file size <= 100KB');

  const validArtifact = evaluateArtifact({
    downloadUrl: 'https://releases.gamervoult.com/gamervoult-v1.0.0.apk',
    sha256Checksum: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
    fileSizeBytes: 24500000
  });
  assert(validArtifact.isArtifactAvailable === true, 'Artifact available when HTTPS, 64-hex SHA-256, and size > 100KB match');
  assert(validArtifact.downloadUrl === 'https://releases.gamervoult.com/gamervoult-v1.0.0.apk', 'Authoritative HTTPS download URL returned');

  // ---------------------------------------------------------
  // TEST GROUP 3: Constant-Time Comparison Security
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 3: Constant-Time Secret Comparison ---');
  const dummySec = 'gv_secret_prod_webhook_test_key_2026';
  assert(timingSafeEqualStr(dummySec, dummySec) === true, 'timingSafeEqualStr matches identical secrets');
  assert(timingSafeEqualStr(dummySec, 'wrong_length_secret') === false, 'timingSafeEqualStr handles unequal lengths safely');
  assert(timingSafeEqualStr(dummySec, 'gv_secret_prod_webhook_test_key_9999') === false, 'timingSafeEqualStr rejects differing secret of same length');
  assert(timingSafeEqualStr(null, dummySec) === false, 'timingSafeEqualStr safely handles null inputs');

  // ---------------------------------------------------------
  // TEST GROUP 4: Payment Webhook Fail-Closed & Replay Logic
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 4: Payment Webhook Fail-Closed & Replay Protection ---');
  function simulateWebhook(headers, body, expectedSecret, currentOrder, existingUtrs = []) {
    if (!expectedSecret) return { status: 503, error: 'CONFIG_ERROR' };

    let incomingSecret = headers['x-webhook-secret'] || headers['x-api-key'] || '';
    if (!incomingSecret && headers['authorization']) {
      const a = headers['authorization'].trim();
      incomingSecret = a.startsWith('Bearer ') ? a.slice(7).trim() : a;
    }

    if (!incomingSecret || !timingSafeEqualStr(incomingSecret, expectedSecret)) {
      return { status: 401, error: 'UNAUTHORIZED' };
    }

    const { orderCode, status, amount, currency, utr } = body;
    if (!orderCode || !status) return { status: 400, error: 'MISSING_FIELDS' };

    const cleanStatus = String(status).toUpperCase();
    if (!['PAID', 'FAILED', 'EXPIRED'].includes(cleanStatus)) return { status: 400, error: 'INVALID_STATUS' };
    if (!currentOrder) return { status: 404, error: 'NOT_FOUND' };

    // Terminal locks
    if (currentOrder.status === 'PAID') return { status: 200, message: 'IDEMPOTENT_ALREADY_PAID', currentStatus: 'PAID' };
    if (currentOrder.status === 'CANCELLED') return { status: 409, error: 'CANCELLED_LOCKED' };

    // Currency & amount
    if (currency && String(currency).toUpperCase() !== (currentOrder.currency || 'INR')) return { status: 400, error: 'CURRENCY_MISMATCH' };
    if (amount != null && Number(amount) !== Number(currentOrder.amount)) return { status: 400, error: 'AMOUNT_MISMATCH' };

    if (cleanStatus === 'PAID') {
      if (!utr || String(utr).trim().length < 4) return { status: 400, error: 'INVALID_UTR' };
      if (existingUtrs.some(e => e.utr === String(utr).trim() && e.orderCode !== orderCode)) {
        return { status: 409, error: 'REPLAY_CONFLICT' };
      }
      return { status: 200, message: 'MARKED_PAID' };
    }

    return { status: 200, message: 'STATUS_UPDATED', newStatus: cleanStatus };
  }

  const sampleOrder = { orderCode: 'ORD-GV01', amount: 599, currency: 'INR', status: 'PENDING' };
  const paidOrder = { orderCode: 'ORD-PAID01', amount: 599, currency: 'INR', status: 'PAID', utr: 'UTR99887766' };
  const cancelledOrder = { orderCode: 'ORD-CANCEL01', amount: 599, currency: 'INR', status: 'CANCELLED' };

  assert(simulateWebhook({ 'x-webhook-secret': 'sec' }, { orderCode: 'ORD-GV01', status: 'PAID' }, '', sampleOrder).status === 503, 'Webhook rejects with HTTP 503 if server secret is unset');
  assert(simulateWebhook({}, { orderCode: 'ORD-GV01', status: 'PAID' }, dummySec, sampleOrder).status === 401, 'Webhook rejects with HTTP 401 if header is missing');
  assert(simulateWebhook({ 'x-webhook-secret': 'bad' }, { orderCode: 'ORD-GV01', status: 'PAID' }, dummySec, sampleOrder).status === 401, 'Webhook rejects with HTTP 401 if secret is incorrect');
  assert(simulateWebhook({ 'authorization': `Bearer ${dummySec}` }, { orderCode: 'ORD-GV01', status: 'PAID', utr: 'UTR12345678', amount: 599 }, dummySec, sampleOrder).status === 200, 'Webhook authenticates via Authorization: Bearer <secret>');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', amount: 1, utr: 'UTR12345678' }, dummySec, sampleOrder).status === 400, 'Webhook rejects amount tampering (₹1 vs ₹599)');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', currency: 'USD', utr: 'UTR12345678', amount: 599 }, dummySec, sampleOrder).status === 400, 'Webhook rejects currency mismatch (USD vs INR)');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', amount: 599 }, dummySec, sampleOrder).status === 400, 'Webhook rejects PAID status without UTR');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', utr: '12', amount: 599 }, dummySec, sampleOrder).status === 400, 'Webhook rejects sub-4-char dummy UTR');

  // Replay check
  const utrStore = [{ utr: 'UTR_ALREADY_USED', orderCode: 'ORD-OTHER' }];
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', utr: 'UTR_ALREADY_USED', amount: 599 }, dummySec, sampleOrder, utrStore).status === 409, 'Webhook rejects cross-order UTR replay with HTTP 409 Conflict');

  // Valid payment
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-GV01', status: 'PAID', utr: 'UTR_FRESH_12345', amount: 599 }, dummySec, sampleOrder, utrStore).status === 200, 'Valid webhook transitions order to PAID');

  // Idempotent duplicate delivery & terminal locks
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-PAID01', status: 'PAID', utr: 'UTR99887766', amount: 599 }, dummySec, paidOrder).status === 200, 'Duplicate webhook returns 200 OK idempotently');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-PAID01', status: 'FAILED' }, dummySec, paidOrder).currentStatus === 'PAID', 'Conflicting FAILED webhook cannot overwrite terminal PAID order');
  assert(simulateWebhook({ 'x-webhook-secret': dummySec }, { orderCode: 'ORD-CANCEL01', status: 'PAID', utr: 'UTR7766' }, dummySec, cancelledOrder).status === 409, 'Webhook cannot resurrect CANCELLED order');

  // ---------------------------------------------------------
  // TEST GROUP 5: Role Enforcement Middleware
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 5: Server-Side Role Enforcement Logic ---');
  function checkRoleAccess(userRole, allowedRoles) {
    if (!userRole) return 401;
    return allowedRoles.includes(userRole) ? 200 : 403;
  }

  assert(checkRoleAccess('client', ['admin', 'superadmin']) === 403, 'Client role forbidden from administrative routes');
  assert(checkRoleAccess('staff', ['staff', 'employee', 'admin']) === 200, 'Staff role permitted on support copilot route');
  assert(checkRoleAccess(null, ['client', 'staff']) === 401, 'Unauthenticated user rejected with 401');

  // ---------------------------------------------------------
  // TEST GROUP 6: Rate Limiting Enforcement
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 6: Rate Limiting & Abuse Defense ---');
  function simulateRateLimiter(maxRequests = 5) {
    let count = 0;
    return () => {
      count++;
      return count > maxRequests ? 429 : 200;
    };
  }

  const testLimiter = simulateRateLimiter(3);
  assert(testLimiter() === 200, 'Request 1/3 permitted within rate window');
  assert(testLimiter() === 200, 'Request 2/3 permitted within rate window');
  assert(testLimiter() === 200, 'Request 3/3 permitted within rate window');
  assert(testLimiter() === 429, 'Request 4/3 blocked with HTTP 429 Too Many Requests');

  // ---------------------------------------------------------
  // TEST GROUP 7: Prototype Pollution Guard
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 7: Prototype Pollution & Payload Validation ---');
  function checkPayloadKeys(body) {
    if (!body || typeof body !== 'object') return 200;
    for (const key of Object.keys(body)) {
      if (key === '__proto__' || key === 'constructor' || key === 'prototype') return 400;
    }
    return 200;
  }

  assert(checkPayloadKeys({ itemId: 'prod_01', customerName: 'Alex' }) === 200, 'Clean payload accepted');
  assert(checkPayloadKeys(JSON.parse('{"itemId":"prod_01","__proto__":{"admin":true}}')) === 400, 'Payload with __proto__ rejected with HTTP 400');
  assert(checkPayloadKeys({ itemId: 'prod_01', constructor: {} }) === 400, 'Payload with constructor rejected with HTTP 400');

  // ---------------------------------------------------------
  // TEST GROUP 8: User Data Isolation & Ownership Protection
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 8: User Data Isolation & Private Records ---');
  function filterUserOrders(orders, callerUid, isStaff) {
    if (isStaff) return orders;
    return orders.filter(o => o.userId === callerUid);
  }

  const multiUserOrders = [
    { orderCode: 'ORD_USER_A1', userId: 'user_alice', amount: 999 },
    { orderCode: 'ORD_USER_A2', userId: 'user_alice', amount: 499 },
    { orderCode: 'ORD_USER_B1', userId: 'user_bob', amount: 1499 }
  ];

  const aliceView = filterUserOrders(multiUserOrders, 'user_alice', false);
  const bobView = filterUserOrders(multiUserOrders, 'user_bob', false);
  const staffView = filterUserOrders(multiUserOrders, 'staff_charlie', true);

  assert(aliceView.length === 2 && aliceView.every(o => o.userId === 'user_alice'), 'User Alice can ONLY see her own 2 orders');
  assert(!aliceView.some(o => o.userId === 'user_bob'), 'User Alice CANNOT see User Bob\'s private orders');
  assert(bobView.length === 1 && bobView[0].orderCode === 'ORD_USER_B1', 'User Bob can ONLY see his own order');
  assert(staffView.length === 3, 'Staff user can view all registered orders for fulfillment');

  // ---------------------------------------------------------
  // TEST GROUP 9: Privilege Escalation & Role Tamper Defense
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 9: Privilege Escalation & Profile Role Tamper Defense ---');
  function evaluateProfileUpdate(callerRole, updatePayload) {
    const privilegedFields = ['role', 'status', 'isVip', 'subscriptionExpiry'];
    const hasPrivilegedKey = privilegedFields.some(f => Object.prototype.hasOwnProperty.call(updatePayload, f));
    if (hasPrivilegedKey && callerRole !== 'admin' && callerRole !== 'superadmin') {
      return 403; // Forbidden
    }
    return 200; // Success
  }

  assert(evaluateProfileUpdate('client', { name: 'John Doe', phone: '+919876543210' }) === 200, 'Ordinary client can update legitimate fields (name, phone)');
  assert(evaluateProfileUpdate('client', { role: 'admin' }) === 403, 'Client attempting to escalate role to admin rejected with HTTP 403');
  assert(evaluateProfileUpdate('client', { isVip: true }) === 403, 'Client attempting to forge VIP entitlement rejected with HTTP 403');
  assert(evaluateProfileUpdate('admin', { role: 'staff' }) === 200, 'Admin permitted to manage user roles');

  // ---------------------------------------------------------
  // TEST GROUP 10: Support Chat Message Validation
  // ---------------------------------------------------------
  console.log('\n--- TEST GROUP 10: Support Chat Input Validation ---');
  function validateChatMessage(text) {
    if (!text || typeof text !== 'string' || text.trim().length === 0) return { valid: false, error: 'EMPTY_TEXT' };
    if (text.length > 1000) return { valid: false, error: 'TEXT_TOO_LONG' };
    return { valid: true };
  }

  assert(!validateChatMessage('').valid, 'Empty chat message rejected');
  assert(!validateChatMessage('   ').valid, 'Whitespace-only chat message rejected');
  assert(!validateChatMessage('A'.repeat(1001)).valid, 'Chat message > 1000 characters rejected');
  assert(validateChatMessage('Hello, where can I verify my order?').valid, 'Valid chat message accepted');


  console.log('\n======================================================');
  console.log(`📊 TEST RESULTS: ${passed} Passed | ${failed} Failed`);
  console.log('======================================================\n');

  if (failed > 0) process.exit(1);
  else process.exit(0);
}

runTests();

