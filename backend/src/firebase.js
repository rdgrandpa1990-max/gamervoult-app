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
    // 0. Development Firestore Emulator Support (Strictly Isolated & Non-Production)
    if (process.env.FIRESTORE_EMULATOR_HOST) {
      const devProjectId = process.env.FIREBASE_PROJECT_ID || 'gamervoult-dev';
      if (devProjectId === 'ff-store-4a61e') {
        throw new Error('FATAL: Cannot use production project ID (ff-store-4a61e) with development emulator.');
      }
      admin.initializeApp({
        projectId: devProjectId
      });
      console.log(`✅ [Firebase] Initialized with isolated development Firestore emulator at ${process.env.FIRESTORE_EMULATOR_HOST} (Project: ${devProjectId})`);
    }
    // 1. Environment Variable Credentials (Preferred on Vercel)
    else if (process.env.FIREBASE_PROJECT_ID && process.env.FIREBASE_CLIENT_EMAIL && process.env.FIREBASE_PRIVATE_KEY) {
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
    // 2. Service Account JSON file (Dedicated Development service account only)
    else {
      const candidatePaths = [
        path.join(process.cwd(), 'dev-serviceAccountKey.json'),
        path.join(process.cwd(), 'serviceAccountKey.json')
      ];

      let loaded = false;
      for (const p of candidatePaths) {
        if (fs.existsSync(p)) {
          const serviceAccount = JSON.parse(fs.readFileSync(p, 'utf8'));
          admin.initializeApp({
            credential: admin.credential.cert(serviceAccount)
          });
          console.log(`✅ [Firebase] Initialized using dedicated service account file: ${p}`);
          loaded = true;
          break;
        }
      }

      if (!loaded) {
        console.warn('⚠️ [Firebase] No development service account provided. Running in secure isolated mock/fallback mode.');
        return {
          admin: null,
          db: null,
          auth: null
        };
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

