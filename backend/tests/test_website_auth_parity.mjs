import assert from 'assert';

const BASE_URL = 'https://gamervoult-backend-m7c0zeol6-fcukmedady.vercel.app';

async function testAuth() {
  console.log('Testing Deployed Backend Auth Endpoints at:', BASE_URL);

  // 1. Open registration rejection
  const regRes = await fetch(`${BASE_URL}/api/v1/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'test@gv.in', password: 'Password123' })
  });
  console.log('1. Open registration without license status:', regRes.status);
  assert.strictEqual(regRes.status, 403);
  console.log('   ✅ PASS: Open registration correctly rejected with HTTP 403');

  // 2. Signup with missing license
  const noKeyRes = await fetch(`${BASE_URL}/api/v1/auth/signup-license`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'test2@gv.in', password: 'Password123' })
  });
  assert.strictEqual(noKeyRes.status, 400);
  console.log('   ✅ PASS: Missing license rejected with HTTP 400');

  // 3. Signup with fake/invalid license
  const fakeKeyRes = await fetch(`${BASE_URL}/api/v1/auth/signup-license`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ licenseKey: 'FAK3-K3YY-0000-0000', email: 'test3@gv.in', password: 'Password123' })
  });
  assert.strictEqual(fakeKeyRes.status, 400);
  console.log('   ✅ PASS: Invalid license rejected with HTTP 400');

  // 4. Signup with already redeemed license
  const usedKeyRes = await fetch(`${BASE_URL}/api/v1/auth/signup-license`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ licenseKey: 'GVLK-USED0-2026-KEY3', email: 'test4@gv.in', password: 'Password123' })
  });
  assert.strictEqual(usedKeyRes.status, 400);
  console.log('   ✅ PASS: Already redeemed license rejected with HTTP 400');

  // 5. Signup with valid active license
  const clientEmail = `client_${Date.now()}@gamervoult.in`;
  const validKeyRes = await fetch(`${BASE_URL}/api/v1/auth/signup-license`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      licenseKey: 'GVLK-VALID-2026-KEY1',
      name: 'Real License Client',
      phone: '+919876543210',
      email: clientEmail,
      password: 'ClientPassword123'
    })
  });
  console.log('5. Valid license signup status:', validKeyRes.status);
  assert.strictEqual(validKeyRes.status, 201);
  const clientData = await validKeyRes.json();
  assert.strictEqual(clientData.success, true);
  assert.strictEqual(clientData.user.role, 'client');
  assert.ok(clientData.token.startsWith('gv_sess_'));
  console.log('   ✅ PASS: Valid license successfully creates client account and returns session token');

  // 6. Attempting to reuse the now-burned license must fail
  const reuseRes = await fetch(`${BASE_URL}/api/v1/auth/signup-license`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      licenseKey: 'GVLK-VALID-2026-KEY1',
      name: 'Attacker Attempting Reuse',
      email: 'attacker@gamervoult.in',
      password: 'ClientPassword123'
    })
  });
  assert.strictEqual(reuseRes.status, 400);
  console.log('   ✅ PASS: Burned license cannot be reused (one-time redemption enforced)');

  // 7. Login with invalid password
  const badLoginRes = await fetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: clientEmail, password: 'WrongPassword' })
  });
  assert.strictEqual(badLoginRes.status, 401);
  console.log('   ✅ PASS: Login with wrong password rejected with HTTP 401');

  // 8. Login with correct password
  const goodLoginRes = await fetch(`${BASE_URL}/api/v1/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: clientEmail, password: 'ClientPassword123' })
  });
  assert.strictEqual(goodLoginRes.status, 200);
  const loginData = await goodLoginRes.json();
  assert.strictEqual(loginData.user.email, clientEmail);
  assert.strictEqual(loginData.user.role, 'client');
  console.log('   ✅ PASS: Login with correct password succeeds and returns client profile');

  // 9. Google Sign-In endpoint
  const googleEmail = `google_gamer_${Date.now()}@gmail.com`;
  const googleRes = await fetch(`${BASE_URL}/api/v1/auth/google`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      googleEmail,
      googleName: 'Google FreeFire Player',
      googleUid: '109283019283'
    })
  });
  assert.strictEqual(googleRes.status, 200);
  const googleData = await googleRes.json();
  assert.strictEqual(googleData.success, true);
  assert.strictEqual(googleData.user.role, 'user'); // Standard user role matching website
  assert.ok(googleData.token.startsWith('gv_sess_'));
  console.log('   ✅ PASS: Google sign-in creates/retrieves standard user role matching website');

  console.log('\n🎉 ALL 9 BACKEND AUTHENTICATION SECURITY TESTS PASSED!');
}

testAuth().catch(err => {
  console.error('❌ Test failed:', err);
  process.exit(1);
});

