import admin from 'firebase-admin';
import fs from 'fs';
import path from 'path';

let db = null;
let auth = null;

export function getFirebaseAdmin() {
  if (admin.apps.length > 0) {
    return {
      admin,
      db: admin.firestore(),
      auth: admin.auth()
    };
  }

  try {
    // 1. Environment Variable Credentials (Preferred on Vercel)
    if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY) {
      const privateKey = process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n');
      admin.initializeApp({
        credential: admin.credential.cert({
          projectId: process.env.FIREBASE_PROJECT_ID,
          clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
          privateKey
        })
      });
      console.log('✅ [Firebase] Initialized using environment variables.');
    }
    // 2. Service Account JSON file (Local development)
    else {
      const candidatePaths = [
        path.join(process.cwd(), 'serviceAccountKey.json'),
        path.join(process.cwd(), '..', 'imap-proxy-server', 'serviceAccountKey.json'),
        path.join(process.cwd(), '..', 'serviceAccountKey.json')
      ];

      let loaded = false;
      for (const p of candidatePaths) {
        if (fs.existsSync(p)) {
          const serviceAccount = JSON.parse(fs.readFileSync(p, 'utf8'));
          admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
          });
          console.log(`✅ [Firebase] Initialized using service account file: ${p}`);
          loaded = true;
          break;
        }
      }

      if (!loaded) {
        // Fallback to default application credentials if running in GCP/Firebase
        admin.initializeApp({
          projectId: process.env.FIREBASE_PROJECT_ID || 'ff-store-4a61e'
        });
        console.warn('⚠️ [Firebase] Initialized with fallback project ID (read-only/mock mode).');
      }
    }

    db = admin.firestore();
    auth = admin.auth();
    return { admin, db, auth };
  } catch (err) {
    console.error('❌ [Firebase] Initialization failed:', err.message);
    return { admin: null, db: null, auth: null };
  }
}

export { admin, db, auth };
