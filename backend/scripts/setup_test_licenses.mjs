import admin from 'firebase-admin';
import fs from 'fs';

const serviceAccount = JSON.parse(
  fs.readFileSync('C:\\Users\\harsh\\.gemini\\antigravity\\brain\\0cf3bac1-ca23-4777-baf4-a7633553deae\\scratch\\dev-serviceAccountKey.json', 'utf8')
);

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
}

const db = admin.firestore();

async function setupTestLicenses() {
  const keys = [
    {
      id: 'GVLK-VALID-2026-KEY1',
      status: 'active',
      assignedMailbox: 'client.test1@freefireofficial.in',
      mailboxType: 'imap',
      createdAt: new Date().toISOString(),
      redeemedBy: null,
      redeemedAt: null
    },
    {
      id: 'GVLK-VALID-2026-KEY2',
      status: 'active',
      assignedMailbox: 'client.test2@freefireofficial.in',
      mailboxType: 'imap',
      createdAt: new Date().toISOString(),
      redeemedBy: null,
      redeemedAt: null
    },
    {
      id: 'GVLK-USED0-2026-KEY3',
      status: 'redeemed',
      assignedMailbox: 'used@freefireofficial.in',
      mailboxType: 'imap',
      createdAt: new Date().toISOString(),
      redeemedBy: 'usr_previous_client',
      redeemedAt: new Date().toISOString()
    },
    {
      id: 'GVLK-REVK0-2026-KEY4',
      status: 'revoked',
      assignedMailbox: 'revoked@freefireofficial.in',
      mailboxType: 'imap',
      createdAt: new Date().toISOString(),
      redeemedBy: null,
      redeemedAt: null
    }
  ];

  const batch = db.batch();
  for (const k of keys) {
    batch.set(db.collection('license_keys').doc(k.id), k);
  }
  await batch.commit();
  console.log('✅ Configured test license keys in gamervoult-dev-2026.');
}

setupTestLicenses().catch(console.error);

