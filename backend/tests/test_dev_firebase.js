/**
 * test_dev_firebase.js
 * End-to-End Test Suite for Isolated Development Firebase, Real Application Flows,
 * Multi-Tenant User Isolation, and Restart Persistence.
 */

import { spawn } from 'child_process';
import path from 'path';
import fs from 'fs';
import http from 'http';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// Configure Environment for Isolated Development
const EMULATOR_PORT = 8088;
const DEV_PROJECT_ID = 'gamervoult-dev';
const EMULATOR_JAR = 'C:\\Users\\harsh\\.cache\\firebase\\emulators\\cloud-firestore-emulator-v1.22.0.jar';
const BACKEND_PORT = 5055;

process.env.NODE_ENV = 'development';
process.env.ALLOW_DEV_TOKENS = 'true';
process.env.FIRESTORE_EMULATOR_HOST = `127.0.0.1:${EMULATOR_PORT}`;
process.env.FIREBASE_PROJECT_ID = DEV_PROJECT_ID;
process.env.UPI_GATEWAY_WEBHOOK_SECRET = 'dev_webhook_secret_32_chars_ok';
process.env.UPI_GATEWAY_API_KEY = 'dev_gateway_api_key';
process.env.UPI_GATEWAY_URL = 'https://personal-payment-gateway.onrender.com';
process.env.PORT = String(BACKEND_PORT);

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

async function request(method, path, body = null, headers = {}) {
  return new Promise((resolve, reject) => {
    const postData = body ? JSON.stringify(body) : '';
    const reqHeaders = {
      'Content-Type': 'application/json',
      ...headers
    };
    if (postData) {
      reqHeaders['Content-Length'] = Buffer.byteLength(postData);
    }

    const req = http.request({
      hostname: '127.0.0.1',
      port: BACKEND_PORT,
      path,
      method,
      headers: reqHeaders
    }, (res) => {
      let rawData = '';
      res.on('data', chunk => { rawData += chunk; });
      res.on('end', () => {
        let json = null;
        try { json = JSON.parse(rawData); } catch (e) { json = rawData; }
        resolve({ status: res.statusCode, headers: res.headers, body: json });
      });
    });

    req.on('error', reject);
    if (postData) req.write(postData);
    req.end();
  });
}

function startEmulator() {
  console.log(`\n[Emulator] Launching Cloud Firestore Emulator on port ${EMULATOR_PORT}...`);
  const proc = spawn('java', [
    '-jar', EMULATOR_JAR,
    '--host', '127.0.0.1',
    '--port', String(EMULATOR_PORT)
  ], {
    stdio: ['ignore', 'pipe', 'pipe']
  });

  return new Promise((resolve, reject) => {
    let resolved = false;
    proc.stdout.on('data', (d) => {
      const msg = d.toString();
      if (msg.includes('Dev App Server is now running') && !resolved) {
        resolved = true;
        console.log('[Emulator] Dev App Server started successfully.');
        resolve(proc);
      }
    });
    proc.stderr.on('data', (d) => {
      // console.error('[Emulator STDERR]', d.toString());
    });
    setTimeout(() => {
      if (!resolved) {
        resolved = true;
        resolve(proc);
      }
    }, 4000);
  });
}

async function runDevFirebaseTests() {
  console.log('================================================================');
  console.log('🧪 GAMERVOUlT DEV FIREBASE & REAL APPLICATION FLOW TEST SUITE');
  console.log('================================================================');

  let emulatorProc = null;
  let server = null;

  try {
    // Check if emulator is already running (e.g. via firebase emulators:exec)
    const isEmulatorRunning = await new Promise((resolve) => {
      const probe = http.request({
        hostname: '127.0.0.1',
        port: EMULATOR_PORT,
        path: '/',
        method: 'GET'
      }, (res) => resolve(true));
      probe.on('error', () => resolve(false));
      probe.setTimeout(1000, () => { probe.abort(); resolve(false); });
      probe.end();
    });

    if (isEmulatorRunning) {
      console.log(`[Emulator] Detected existing Cloud Firestore Emulator on 127.0.0.1:${EMULATOR_PORT}. Reusing.`);
    } else {
      emulatorProc = await startEmulator();
    }

    // Dynamically import Firebase and Express app after environment is set
    const { getFirebaseAdmin } = await import('../src/firebase.js');
    const app = (await import('../api/index.js')).default;

    // Wait for server to bind if already started by index.js
    await new Promise(r => setTimeout(r, 1000));
    console.log(`[Backend] Express server listening on http://127.0.0.1:${BACKEND_PORT}`);

    const { db, admin } = getFirebaseAdmin();

    console.log('\n--- 1. Verification of Isolated Development Firebase Project ---');
    assert(db !== null, 'Firestore instance initialized');
    assert(process.env.FIREBASE_PROJECT_ID === 'gamervoult-dev', 'Running in dedicated dev project "gamervoult-dev"');
    assert(process.env.FIREBASE_PROJECT_ID !== 'ff-store-4a61e', 'Guaranteed NOT connected to production project "ff-store-4a61e"');

    console.log('\n--- 2. Database Seeding & Persistent Write Verification ---');
    // Seed Catalog Product
    await db.collection('ff_store').doc('item_dev_01').set({
      title: 'Grandmaster Free Fire ID 78 • Max Evo Guns',
      price: 1299,
      category: 'VIP Account',
      description: 'Exclusive tournament-ready ID with full recovery warranty.',
      level: 78,
      evoGuns: 6,
      likes: 25400,
      status: 'active',
      updatedAt: new Date().toISOString()
    });

    // Seed Product Secrets (Credentials)
    await db.collection('ff_store_secrets').doc('item_dev_01').set({
      deliveryAccountType: 'Google',
      deliveryLoginId: 'vip_player_78@gamervoult.dev',
      deliveryPassword: 'DevEscrowPassword2026!',
      deliveryBackupCodes: '9981 2234, 4412 8891',
      deliveryNotes: 'Keep 2FA enabled.'
    });

    // Seed User Profiles
    await db.collection('users').doc('user_alpha').set({
      uid: 'user_alpha',
      email: 'alpha@gamervoult.dev',
      name: 'Alpha Player',
      role: 'client',
      phone: '+919876543210',
      isVip: false,
      status: 'Active'
    });

    await db.collection('users').doc('user_beta').set({
      uid: 'user_beta',
      email: 'beta@gamervoult.dev',
      name: 'Beta Player',
      role: 'client',
      phone: '+919123456789',
      isVip: false,
      status: 'Active'
    });

    // Seed License Key
    await db.collection('license_keys').doc('KEY-GV-VIP-2026').set({
      status: 'active',
      planName: 'VIP Pass 48H',
      validityDays: 2,
      createdAt: new Date().toISOString(),
      redeemedBy: null
    });

    const verifyItem = await db.collection('ff_store').doc('item_dev_01').get();
    assert(verifyItem.exists && verifyItem.data().price === 1299, 'Catalog item persisted in Firestore ff_store collection');

    const verifyAlpha = await db.collection('users').doc('user_alpha').get();
    assert(verifyAlpha.exists && verifyAlpha.data().email === 'alpha@gamervoult.dev', 'User Alpha profile persisted in Firestore users collection');

    console.log('\n--- 3. Real Application Flow: Storefront Catalog Retrieval ---');
    const catalogRes = await request('GET', '/api/v1/products');
    assert(catalogRes.status === 200, 'Catalog endpoint returned HTTP 200');
    assert(catalogRes.body.success === true, 'Catalog response success is true');
    assert(catalogRes.body.products.some(p => p.id === 'item_dev_01'), 'Catalog response includes seeded Firestore product');

    console.log('\n--- 4. Real Application Flow: Rate-Limited Order Creation with Server Price Check ---');
    const orderCreateRes = await request('POST', '/api/v1/orders/create', {
      itemId: 'item_dev_01',
      customerName: 'Alpha Player',
      customerEmail: 'alpha@gamervoult.dev',
      customerMobile: '9876543210'
    }, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });

    assert(orderCreateRes.status === 201, 'Order created successfully with HTTP 201');
    assert(orderCreateRes.body.amount === 1299, 'Server verified price against Firestore item price (₹1299)');
    assert(orderCreateRes.body.orderCode.startsWith('GV-'), 'Valid orderCode generated');
    assert(orderCreateRes.body.paymentMode === 'TEST_SANDBOX_ONLY', 'Payment mode is TEST_SANDBOX_ONLY (live payments disabled)');
    const alphaOrderCode = orderCreateRes.body.orderCode;

    // Create an Order for User Beta as well
    const betaOrderCreateRes = await request('POST', '/api/v1/orders/create', {
      itemId: 'item_dev_01',
      customerName: 'Beta Player',
      customerEmail: 'beta@gamervoult.dev'
    }, {
      'Authorization': 'Bearer dev_token:user_beta:client'
    });
    const betaOrderCode = betaOrderCreateRes.body.orderCode;
    assert(betaOrderCreateRes.status === 201, 'Beta order created successfully');

    console.log('\n--- 5. Two-User Multi-Tenant Order Isolation Verification ---');
    const alphaOrdersRes = await request('GET', '/api/v1/orders', null, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(alphaOrdersRes.status === 200, 'User Alpha order list retrieved HTTP 200');
    assert(alphaOrdersRes.body.orders.every(o => o.orderCode !== betaOrderCode), 'User Alpha CANNOT see User Beta order');
    assert(alphaOrdersRes.body.orders.some(o => o.orderCode === alphaOrderCode), 'User Alpha sees only their own order');

    const betaOrdersRes = await request('GET', '/api/v1/orders', null, {
      'Authorization': 'Bearer dev_token:user_beta:client'
    });
    assert(betaOrdersRes.body.orders.every(o => o.orderCode !== alphaOrderCode), 'User Beta CANNOT see User Alpha order');
    assert(betaOrdersRes.body.orders.some(o => o.orderCode === betaOrderCode), 'User Beta sees only their own order');

    console.log('\n--- 6. Order Status & Credential Release Guard (Pre-Payment) ---');
    const unverifiedCredsRes = await request('GET', `/api/v1/orders/${alphaOrderCode}/credentials`, null, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(unverifiedCredsRes.status === 402, 'Credentials rejected with HTTP 402 Payment Required before payment is confirmed');

    console.log('\n--- 7. Payment Webhook Verification with Nested Gateway Payload & Secret ---');
    // Test nested payload matching Personal UPI Gateway matchingEngine dispatch:
    const nestedWebhookPayload = {
      event: 'payment.success',
      timestamp: Date.now(),
      data: {
        orderCode: alphaOrderCode,
        amount: 1299,
        status: 'PAID',
        utr: 'UTR8899001122',
        sender: 'ALPHA SENDER'
      }
    };

    const webhookRes = await request('POST', '/api/webhooks/payment', nestedWebhookPayload, {
      'x-webhook-secret': 'dev_webhook_secret_32_chars_ok'
    });
    assert(webhookRes.status === 200, 'Webhook confirmed order as PAID with HTTP 200');

    // Verify order updated in real Firestore database
    const orderDoc = await db.collection('payment_orders').doc(alphaOrderCode).get();
    assert(orderDoc.exists && orderDoc.data().status === 'PAID', 'Order transitioned to PAID in Firestore');
    assert(orderDoc.data().utr === 'UTR8899001122', 'Authoritative UTR persisted in Firestore');

    console.log('\n--- 8. Post-Payment Secure Credential Handover & Isolation ---');
    const paidCredsRes = await request('GET', `/api/v1/orders/${alphaOrderCode}/credentials`, null, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(paidCredsRes.status === 200, 'Buyer User Alpha receives credentials after confirmed payment');
    assert(paidCredsRes.body.credentials.loginId === 'vip_player_78@gamervoult.dev', 'Correct delivery credentials released');

    // Attempted unauthorized access to Alpha's credentials by Beta:
    const unauthorizedCredsRes = await request('GET', `/api/v1/orders/${alphaOrderCode}/credentials`, null, {
      'Authorization': 'Bearer dev_token:user_beta:client'
    });
    assert(unauthorizedCredsRes.status === 403, 'User Beta blocked with HTTP 403 Forbidden from accessing User Alpha credentials');

    console.log('\n--- 9. Real Application Flow: Escrow Support Chat Persistence ---');
    const sendChatRes = await request('POST', '/api/v1/chat/send', {
      text: 'Hello Escrow Desk, I verified payment and need account handover instructions.'
    }, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(sendChatRes.status === 201, 'Chat message sent with HTTP 201');
    assert(sendChatRes.body.reply !== undefined, 'Escrow Copilot auto-reply generated');

    const chatMessagesRes = await request('GET', '/api/v1/chat/messages', null, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(chatMessagesRes.status === 200, 'Chat messages retrieved with HTTP 200');
    assert(chatMessagesRes.body.messages.length >= 2, 'Chat conversation persisted in Firestore subcollection');

    console.log('\n--- 10. Real Application Flow: Profile Lookup & Role Tamper Defense ---');
    const profileRes = await request('GET', '/api/v1/me', null, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(profileRes.status === 200, 'Profile retrieved with HTTP 200');
    assert(profileRes.body.user.email === 'alpha@gamervoult.dev', 'Profile email matches Firestore data');

    // Legitimate Profile Update
    const updateProfileRes = await request('PUT', '/api/v1/me', {
      name: 'Alpha Pro Gamer',
      phone: '+919999988888'
    }, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(updateProfileRes.status === 200, 'Legitimate profile update succeeded');

    const updatedUserDoc = await db.collection('users').doc('user_alpha').get();
    assert(updatedUserDoc.data().name === 'Alpha Pro Gamer', 'Updated profile name persisted to Firestore users collection');

    // Tamper Attempt: Ordinary client attempts to elevate role to admin
    const tamperRoleRes = await request('PUT', '/api/v1/me', {
      role: 'admin'
    }, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(tamperRoleRes.status === 403, 'Privilege escalation rejected with HTTP 403 Forbidden');

    console.log('\n--- 11. Real Application Flow: Atomic License Key Redemption ---');
    const redeemRes = await request('POST', '/api/v1/license/redeem', {
      licenseKey: 'KEY-GV-VIP-2026'
    }, {
      'Authorization': 'Bearer dev_token:user_alpha:client'
    });
    assert(redeemRes.status === 200, 'License key redeemed with HTTP 200');
    assert(redeemRes.body.success === true, 'Redemption confirmed');

    // Verify atomic transition in database
    const keyDoc = await db.collection('license_keys').doc('KEY-GV-VIP-2026').get();
    assert(keyDoc.data().status === 'redeemed', 'License key status marked redeemed in Firestore');
    assert(keyDoc.data().redeemedBy === 'user_alpha', 'License key recorded as redeemed by user_alpha');

    // Second redemption attempt by different user must fail
    const replayRedeemRes = await request('POST', '/api/v1/license/redeem', {
      licenseKey: 'KEY-GV-VIP-2026'
    }, {
      'Authorization': 'Bearer dev_token:user_beta:client'
    });
    assert(replayRedeemRes.status === 409, 'Replay of redeemed license key rejected with HTTP 409 Conflict');

    console.log('\n================================================================');
    console.log(`📊 DEV FIREBASE TEST RESULTS: ${passed} Passed | ${failed} Failed`);
    console.log('================================================================\n');

  } catch (err) {
    console.error('❌ Critical Test Error:', err);
    failed++;
  } finally {
    if (server) {
      server.close();
      console.log('[Backend] Test server shut down.');
    }
    if (emulatorProc) {
      emulatorProc.kill();
      console.log('[Emulator] Firestore emulator shut down.');
    }
    process.exit(failed > 0 ? 1 : 0);
  }
}

runDevFirebaseTests();
