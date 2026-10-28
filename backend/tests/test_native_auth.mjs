import assert from 'assert';

const BASE_URL = 'https://gamervoult-backend-8o8424d3a-fcukmedady.vercel.app';

async function run() {
  console.log('========================================================================');
  console.log('🚀 GAMERVOUlT NATIVE AUTH & AUTHORITATIVE BACKEND SUITE');
  console.log(`Target: ${BASE_URL}`);
  console.log('========================================================================\n');

  let passed = 0;
  let failed = 0;

  function pass(msg) {
    passed++;
    console.log(`  ✅ PASS: ${msg}`);
  }

  function fail(msg, err) {
    failed++;
    console.error(`  ❌ FAIL: ${msg}`, err);
  }

  // 1. Missing fields rejection
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'bad@gv.in' })
    });
    assert.strictEqual(res.status, 400);
    pass('Registration rejects missing password with HTTP 400');
  } catch (e) { fail('Missing password rejection', e); }

  // 2. Short password rejection
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'bad2@gv.in', password: '123' })
    });
    assert.strictEqual(res.status, 400);
    pass('Registration rejects sub-6-character password with HTTP 400');
  } catch (e) { fail('Short password rejection', e); }

  // 3. Valid Registration
  const testEmail = `test_native_${Date.now()}@gamervoult.in`;
  let token = null;
  let uid = null;
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testEmail,
        password: 'ValidPassword@2026',
        name: 'Native Suite Player',
        phone: '+919876543210'
      })
    });
    const data = await res.json();
    assert.strictEqual(res.status, 201);
    assert.strictEqual(data.success, true);
    assert.ok(data.token.startsWith('gv_sess_'));
    assert.strictEqual(data.user.role, 'client');
    token = data.token;
    uid = data.user.uid;
    pass('Registration creates user with strict default role "client" and returns session token');
  } catch (e) { fail('Valid registration', e); }

  // 4. Duplicate Registration Rejection
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testEmail,
        password: 'AnotherPassword',
        name: 'Duplicate'
      })
    });
    assert.strictEqual(res.status, 409);
    pass('Duplicate email registration rejected with HTTP 409 Conflict');
  } catch (e) { fail('Duplicate registration rejection', e); }

  // 5. Login with Wrong Password
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testEmail,
        password: 'WrongPassword123'
      })
    });
    assert.strictEqual(res.status, 401);
    pass('Login with incorrect password rejected with HTTP 401');
  } catch (e) { fail('Wrong password rejection', e); }

  // 6. Login with Correct Password
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: testEmail,
        password: 'ValidPassword@2026'
      })
    });
    const data = await res.json();
    assert.strictEqual(res.status, 200);
    assert.strictEqual(data.success, true);
    assert.ok(data.token.startsWith('gv_sess_'));
    assert.strictEqual(data.user.email, testEmail);
    token = data.token;
    pass('Login succeeds with valid credentials and returns active session token');
  } catch (e) { fail('Correct login', e); }

  // 7. Authenticated Profile Query (/api/v1/auth/me)
  try {
    const res = await fetch(`${BASE_URL}/api/v1/auth/me`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await res.json();
    assert.strictEqual(res.status, 200);
    assert.strictEqual(data.user.email, testEmail);
    assert.strictEqual(data.user.role, 'client');
    pass('Profile endpoint (/api/v1/auth/me) validates session token and returns authoritative user');
  } catch (e) { fail('Auth me endpoint', e); }

  // 8. Role Escalation Attack Defense
  try {
    const res = await fetch(`${BASE_URL}/api/v1/me`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ role: 'admin' })
    });
    assert.strictEqual(res.status, 403);
    pass('Client attempting role escalation via session token blocked with HTTP 403 Forbidden');
  } catch (e) { fail('Role escalation attack defense', e); }

  console.log('\n========================================================================');
  console.log(`📊 NATIVE AUTH RESULTS: ${passed} Passed | ${failed} Failed`);
  console.log('========================================================================');
  if (failed > 0) process.exit(1);
}

run();
