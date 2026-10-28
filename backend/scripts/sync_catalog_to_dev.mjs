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

function convertFirestoreField(val) {
  if (!val) return null;
  if (val.stringValue !== undefined) return val.stringValue;
  if (val.integerValue !== undefined) return Number(val.integerValue);
  if (val.doubleValue !== undefined) return Number(val.doubleValue);
  if (val.booleanValue !== undefined) return val.booleanValue;
  if (val.nullValue !== undefined) return null;
  if (val.timestampValue !== undefined) return val.timestampValue;
  if (val.arrayValue !== undefined) {
    return (val.arrayValue.values || []).map(convertFirestoreField);
  }
  if (val.mapValue !== undefined) {
    const obj = {};
    for (const [k, v] of Object.entries(val.mapValue.fields || {})) {
      obj[k] = convertFirestoreField(v);
    }
    return obj;
  }
  return null;
}

async function syncPublicCatalog() {
  console.log('Fetching public catalog from ff-store-4a61e REST API...');
  const res = await fetch('https://firestore.googleapis.com/v1/projects/ff-store-4a61e/databases/(default)/documents/ff_store?pageSize=50');
  const data = await res.json();
  
  if (!data.documents) {
    console.error('No documents returned from ff-store-4a61e');
    return;
  }
  
  console.log(`Found ${data.documents.length} real products. Purging fake items and populating gamervoult-dev-2026...`);
  
  // 1. Delete old test items
  const oldSnap = await db.collection('ff_store').get();
  const batch1 = db.batch();
  oldSnap.forEach(d => {
    batch1.delete(d.ref);
  });
  await batch1.commit();
  console.log(`Purged ${oldSnap.size} old items from gamervoult-dev-2026 ff_store.`);
  
  // 2. Insert real items
  const batch2 = db.batch();
  for (const doc of data.documents) {
    const id = doc.name.split('/').pop();
    const itemData = {};
    for (const [k, v] of Object.entries(doc.fields || {})) {
      itemData[k] = convertFirestoreField(v);
    }
    batch2.set(db.collection('ff_store').doc(id), itemData);
  }
  await batch2.commit();
  console.log(`Successfully synchronized ${data.documents.length} real public items to gamervoult-dev-2026!`);
}

syncPublicCatalog().catch(err => {
  console.error('Sync failed:', err);
  process.exit(1);
});

