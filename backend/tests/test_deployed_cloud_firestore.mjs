import fs from 'fs';
import path from 'path';
import admin from 'firebase-admin';

const DEPLOYED_URL = process.env.VERCEL_PREVIEW_URL || 'https://gamervoult-backend-9a7papgnb-fcukmedady.vercel.app';
const candidatePaths = [
  process.env.DEV_SERVICE_ACCOUNT_PATH,
  path.join(process.cwd(), 'dev-serviceAccountKey.json'),
  'C:\\Users\\harsh\\.gemini\\antigravity\\brain\\0cf3bac1-ca23-4777-baf4-a7633553deae\\scratch\\official-dev-serviceAccountKey.json'
].filter(Boolean);

let sa = null;
for (const p of candidatePaths) {
  if (fs.existsSync(p)) {
    sa = JSON.parse(fs.readFileSync(p, 'utf8'));
    break;
  }
}

if (!sa) {
  console.error('❌ Dedicated dev service account key not found in candidate paths.');
  process.exit(1);
}

// Initialize admin for direct Firestore assertions against gamervoult-dev-2026
admin.initializeApp({
  credential: admin.credential.cert(sa)
});
const db = admin.firestore();

let passed = 0;
let failed = 0;

function assert(cond, msg) {
  if (cond) {
    console.log(`  ✅ PASS: ${msg}`);
    passed++;
  } else {
    console.error(`  ❌ FAIL: ${msg}`);
    failed++;
  }
}

async function request(path, options = {}) {
  const url = `${DEPLOYED_URL}${path}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    }
  });
  let body = null;
  try {
    body = await res.json();
  } catch (e) {
    body = null;
  }
  return { status: res.status, body };
}

async function runDeployedTests() {
  console.log('========================================================================');
  console.log('🚀 GAMERVOUlT DEPLOYED CLOUD PREVIEW END-TO-END VERIFICATION');
  console.log(`Target: ${DEPLOYED_URL}`);
  console.log(`Cloud Firebase Project: ${sa.project_id}`);
  console.log('========================================================================\n');

  try {
    // 1. Health Probe
    console.log('--- 1. Live Health Probe with Cloud Firestore Connected ---');
    const health = await request('/api/health');
    assert(health.status === 200, 'Health endpoint responded HTTP 200');
    assert(health.body.status === 'HEALTHY', 'Service status is HEALTHY');
    assert(health.body.firestore === 'CONNECTED', 'Deployed backend genuinely reports firestore: "CONNECTED"');

    // 2. Version Check Probe
    console.log('\n--- 2. Live Version Check Probe ---');
    const version = await request('/api/v1/app/version-check');
    assert(version.status === 200, 'Version check responded HTTP 200');
    assert(version.body.success === true, 'Version check success is true');

    // 3. Direct Cloud Firestore Seeding
    console.log('\n--- 3. Direct Cloud Firestore Seeding ---');
    const testItemId = 'cloud_item_' + Date.now();
    await db.collection('ff_store').doc(testItemId).set({
      title: 'Cloud Verified Evo Gun VIP ID',
      price: 999,
      category: 'Cloud VIP',
      level: 80,
      evoGuns: 5,
      likes: 12000,
      status: 'active',
      updatedAt: new Date().toISOString()
    });
    console.log(`Seeded catalog item: ${testItemId}`);

    // Seed credential secret
    await db.collection('ff_store_secrets').doc(testItemId).set({
      deliveryAccountType: 'Google',
      deliveryLoginId: 'cloud_player@gamervoult.dev',
      deliveryPassword: 'CloudSecurePassword#2026',
      deliveryNotes: 'Cloud persistent credential'
    });

    // Seed profiles
    await db.collection('users').doc('cloud_alpha').set({
      uid: 'cloud_alpha',
      email: 'alpha@gamervoult.dev',
      name: 'Cloud Alpha Player',
      role: 'client',
      phone: '+919876543210',
      status: 'Active'
    });
    await db.collection('users').doc('cloud_beta').set({
      uid: 'cloud_beta',
      email: 'beta@gamervoult.dev',
      name: 'Cloud Beta Player',
      role: 'client',
      phone: '+919123456789',
      status: 'Active'
    });

    // Seed license key
    const testLicenseKey = 'KEY-CLOUD-TEST-' + Math.random().toString(36).substring(2, 8).toUpperCase();
    await db.collection('license_keys').doc(testLicenseKey).set({
      key: testLicenseKey,
      status: 'active',
      plan: 'VIP_PASS_48H',
      durationHours: 48,
      createdAt: new Date().toISOString()
    });

    // 4. Test Storefront Catalog via Deployed API
    console.log('\n--- 4. Storefront Catalog Retrieval via Deployed API ---');
    const catalog = await request('/api/v1/products');
    assert(catalog.status === 200, 'Catalog endpoint returned HTTP 200');
    assert(Array.isArray(catalog.body.products), 'Products array returned');
    const foundItem = catalog.body.products.find(p => p.id === testItemId);
    assert(foundItem !== undefined, 'Seeded item retrieved from Cloud Firestore through deployed API');
    assert(foundItem?.price === 999, 'Item price matches Cloud Firestore record (₹999)');

    // 5. Create Order via Deployed API (Writes to Cloud Firestore)
    console.log('\n--- 5. Order Creation via Deployed API (Persistent Write) ---');
    const createOrder = await request('/api/v1/orders/create', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' },
      body: JSON.stringify({
        itemId: testItemId,
        itemTitle: 'Cloud Verified Evo Gun VIP ID',
        amount: 999
      })
    });
    assert(createOrder.status === 201, 'Order created via deployed API with HTTP 201');
    assert(createOrder.body.success === true, 'Order creation success is true');
    const orderCode = createOrder.body.orderCode;
    assert(orderCode && orderCode.startsWith('GV-'), `Valid order code generated: ${orderCode}`);

    // Verify order document actually exists in Cloud Firestore
    const cloudOrderDoc = await db.collection('payment_orders').doc(orderCode).get();
    assert(cloudOrderDoc.exists, 'Order document exists in real Cloud Firestore database');
    assert(cloudOrderDoc.data().amount === 999, 'Authoritative price (999) persisted in Cloud Firestore');
    assert(cloudOrderDoc.data().userId === 'cloud_alpha', 'User ownership (cloud_alpha) persisted in Cloud Firestore');

    // 6. Multi-Tenant Order Isolation via Deployed API
    console.log('\n--- 6. Multi-Tenant User Isolation via Deployed API ---');
    const alphaOrders = await request('/api/v1/orders', {
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' }
    });
    assert(alphaOrders.status === 200, 'Alpha orders retrieved HTTP 200');
    assert(alphaOrders.body.orders.some(o => o.orderCode === orderCode), 'Alpha sees their own order');

    const betaOrders = await request('/api/v1/orders', {
      headers: { 'Authorization': 'Bearer dev_token:cloud_beta:client' }
    });
    assert(betaOrders.status === 200, 'Beta orders retrieved HTTP 200');
    assert(!betaOrders.body.orders.some(o => o.orderCode === orderCode), 'Beta CANNOT see Alpha order (strict tenant isolation)');

    // 7. Pre-Payment Credential Lock via Deployed API
    console.log('\n--- 7. Pre-Payment Credential Lock ---');
    const preCreds = await request(`/api/v1/orders/${orderCode}/credentials`, {
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' }
    });
    assert(preCreds.status === 402, 'Credentials blocked with HTTP 402 Payment Required prior to payment');

    // 8. Payment Webhook via Deployed API (Updates Cloud Firestore)
    console.log('\n--- 8. Webhook Processing via Deployed API ---');
    const webhookRes = await request('/api/webhooks/payment', {
      method: 'POST',
      headers: { 'x-webhook-secret': 'dev_preview_webhook_secret_32_chars_ok' },
      body: JSON.stringify({
        event: 'payment.success',
        timestamp: Date.now(),
        data: {
          orderCode,
          amount: 999,
          status: 'PAID',
          utr: 'UTR_CLOUD_' + Date.now(),
          sender: 'CLOUD_TEST_SENDER'
        }
      })
    });
    assert(webhookRes.status === 200, 'Payment webhook processed with HTTP 200');

    // Verify Cloud Firestore was updated to PAID
    const paidOrderDoc = await db.collection('payment_orders').doc(orderCode).get();
    assert(paidOrderDoc.data().status === 'PAID', 'Order status transitioned to PAID in Cloud Firestore');

    // 9. Post-Payment Credential Release via Deployed API
    console.log('\n--- 9. Post-Payment Credential Release ---');
    const postCreds = await request(`/api/v1/orders/${orderCode}/credentials`, {
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' }
    });
    assert(postCreds.status === 200, 'Credentials released to Alpha after payment');
    assert(postCreds.body.credentials?.loginId === 'cloud_player@gamervoult.dev', 'Correct login ID returned');

    // Cross-user access check
    const unauthorizedCreds = await request(`/api/v1/orders/${orderCode}/credentials`, {
      headers: { 'Authorization': 'Bearer dev_token:cloud_beta:client' }
    });
    assert(unauthorizedCreds.status === 403, 'Beta blocked with HTTP 403 Forbidden from accessing Alpha credentials');

    // 10. Persistent Support Chat via Deployed API
    console.log('\n--- 10. Support Chat Persistence via Deployed API ---');
    const chatSend = await request('/api/v1/chat/send', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' },
      body: JSON.stringify({ text: 'Cloud verification test message' })
    });
    assert(chatSend.status === 201, 'Chat message sent HTTP 201');
    assert(chatSend.body.reply !== undefined, 'Escrow Copilot auto-reply returned');

    const chatList = await request('/api/v1/chat/messages', {
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' }
    });
    assert(chatList.status === 200, 'Chat history retrieved HTTP 200');
    assert(chatList.body.messages.length >= 2, 'Chat conversation persisted in Cloud Firestore');

    // 11. Profile Update & Persistence via Deployed API
    console.log('\n--- 11. Profile Lookup & Update via Deployed API ---');
    const updateProfile = await request('/api/v1/me', {
      method: 'PUT',
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' },
      body: JSON.stringify({ name: 'Cloud Alpha Pro', phone: '+919876500000' })
    });
    assert(updateProfile.status === 200, 'Profile updated HTTP 200');

    // Read back via GET /api/v1/me
    const getProfile = await request('/api/v1/me', {
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' }
    });
    assert(getProfile.status === 200, 'Profile retrieved HTTP 200');
    assert(getProfile.body.user.name === 'Cloud Alpha Pro', 'Updated name persisted in Cloud Firestore');

    // Privilege escalation defense on deployed API
    const escalate = await request('/api/v1/me', {
      method: 'PUT',
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' },
      body: JSON.stringify({ role: 'admin' })
    });
    assert(escalate.status === 403, 'Privilege escalation blocked with HTTP 403 on deployed API');

    // 12. Atomic License Key Redemption via Deployed API
    console.log('\n--- 12. Atomic License Key Redemption via Deployed API ---');
    const redeemRes = await request('/api/v1/license/redeem', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer dev_token:cloud_alpha:client' },
      body: JSON.stringify({ licenseKey: testLicenseKey })
    });
    assert(redeemRes.status === 200, 'License key redeemed successfully via deployed API with HTTP 200');

    const redeemedKeyDoc = await db.collection('license_keys').doc(testLicenseKey).get();
    assert(redeemedKeyDoc.data().status === 'redeemed', 'License key status marked redeemed in Cloud Firestore');

    // Replay attempt must fail
    const replayRes = await request('/api/v1/license/redeem', {
      method: 'POST',
      headers: { 'Authorization': 'Bearer dev_token:cloud_beta:client' },
      body: JSON.stringify({ licenseKey: testLicenseKey })
    });
    assert(replayRes.status === 409, 'License key replay rejected with HTTP 409 Conflict');

    console.log('\n========================================================================');
    console.log(`📊 DEPLOYED CLOUD PREVIEW TEST RESULTS: ${passed} Passed | ${failed} Failed`);
    console.log('========================================================================\n');

    process.exit(failed > 0 ? 1 : 0);
  } catch (err) {
    console.error('Test execution error:', err);
    process.exit(1);
  }
}

runDeployedTests();
