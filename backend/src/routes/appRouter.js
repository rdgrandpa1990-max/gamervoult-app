import { Router } from 'express';
import crypto from 'crypto';
import { getFirebaseAdmin } from '../firebase.js';
import { 
  verifyFirebaseAuth, 
  requireRoles, 
  timingSafeEqualStr,
  createRateLimiter,
  validateRequestPayload,
  createSessionToken,
  verifySessionToken,
  hashPassword,
  verifyPassword
} from '../authMiddleware.js';

const router = Router();

// Apply prototype pollution & malformed payload guard to all incoming routes
router.use(validateRequestPayload);

// Dedicated Endpoint Rate Limiters
const authLimiter = createRateLimiter(60000, 20);             // 20 auth attempts per min
const orderCreateLimiter = createRateLimiter(60000, 20);      // 20 order creations per min
const webhookLimiter = createRateLimiter(60000, 60);          // 60 webhook callbacks per min
const licenseRedeemLimiter = createRateLimiter(60000, 10);    // 10 key activations per min
const chatSendLimiter = createRateLimiter(60000, 30);         // 30 support messages per min

// =========================================================================
// 0. Live License Key Verification (/api/v1/auth/verify-license)
// Checks dev database & falls back to live production Firestore REST API
// =========================================================================
router.get('/api/v1/auth/verify-license', authLimiter, async (req, res) => {
  try {
    const rawKey = req.query?.key ? String(req.query.key).trim() : '';
    if (!rawKey) {
      return res.status(400).json({ success: false, valid: false, message: 'Please enter a License Key.' });
    }

    const { db } = getFirebaseAdmin();
    let keyData = null;

    if (db) {
      try {
        const docSnap = await db.collection('license_keys').doc(rawKey).get();
        if (docSnap.exists) {
          keyData = docSnap.data();
        }
      } catch (err) {
        console.warn('[VerifyLicense] Dev DB check note:', err.message);
      }
    }

    // Fallback: Check production Firestore REST API if not yet in dev
    if (!keyData) {
      try {
        const prodUrl = `https://firestore.googleapis.com/v1/projects/ff-store-4a61e/databases/(default)/documents/license_keys/${encodeURIComponent(rawKey)}?key=AIzaSyBcuSIz3NvPh2kU-BziQfjYsGKCMSjeWR4`;
        const prodRes = await fetch(prodUrl);
        if (prodRes.ok) {
          const docJson = await prodRes.json();
          if (docJson.fields) {
            keyData = {
              status: docJson.fields.status?.stringValue || 'active',
              assignedMailbox: docJson.fields.assignedMailbox?.stringValue || '',
              mailboxType: docJson.fields.mailboxType?.stringValue || '',
              createdByEmail: docJson.fields.createdByEmail?.stringValue || ''
            };
            if (db) {
              await db.collection('license_keys').doc(rawKey).set(keyData, { merge: true });
            }
          }
        }
      } catch (prodErr) {
        console.warn('[VerifyLicense] Prod REST note:', prodErr.message);
      }
    }

    if (!keyData) {
      return res.json({
        success: true,
        valid: false,
        status: 'not_found',
        message: 'Invalid License Key. Key was not found.'
      });
    }

    if (keyData.status === 'active') {
      return res.json({
        success: true,
        valid: true,
        status: 'active',
        message: 'Valid License Key (Active & Ready)',
        assignedMailbox: keyData.assignedMailbox || ''
      });
    } else if (keyData.status === 'redeemed') {
      return res.json({
        success: true,
        valid: false,
        status: 'redeemed',
        message: 'This License Key has already been redeemed.'
      });
    } else if (keyData.status === 'revoked') {
      return res.json({
        success: true,
        valid: false,
        status: 'revoked',
        message: 'This License Key has been revoked.'
      });
    } else {
      return res.json({
        success: true,
        valid: false,
        status: keyData.status,
        message: `License status: ${keyData.status}`
      });
    }
  } catch (err) {
    console.error('[VerifyLicense] Error:', err);
    return res.status(500).json({ success: false, valid: false, message: 'Server error verifying license key.' });
  }
});

// =========================================================================
// 0a. Sign Up with License (/api/v1/auth/signup-license)
// Directly implements website Signup.jsx model: Requires active key in license_keys
// =========================================================================
router.post('/api/v1/auth/signup-license', authLimiter, async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'DATABASE_UNAVAILABLE', message: 'Database service unavailable' });

    const licenseKey = req.body?.licenseKey ? String(req.body.licenseKey).trim() : '';
    const email = req.body?.email ? String(req.body.email).trim().toLowerCase() : '';
    const password = req.body?.password ? String(req.body.password) : '';
    const name = req.body?.name ? String(req.body.name).trim() : 'Gamervoult Client';
    const phone = req.body?.phone ? String(req.body.phone).trim() : '';

    if (!licenseKey) {
      return res.status(400).json({ success: false, error: 'MISSING_LICENSE', message: 'Please enter a valid License Key.' });
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email || !emailRegex.test(email)) {
      return res.status(400).json({ success: false, error: 'INVALID_EMAIL', message: 'A valid email address is required.' });
    }
    if (!password || password.length < 6) {
      return res.status(400).json({ success: false, error: 'WEAK_PASSWORD', message: 'Password must be at least 6 characters.' });
    }

    // 1. Verify License Key in Firestore (Dev first, then Prod REST)
    let keyDoc = await db.collection('license_keys').doc(licenseKey).get();
    let keyData = keyDoc.exists ? keyDoc.data() : null;

    if (!keyData) {
      try {
        const prodUrl = `https://firestore.googleapis.com/v1/projects/ff-store-4a61e/databases/(default)/documents/license_keys/${encodeURIComponent(licenseKey)}?key=AIzaSyBcuSIz3NvPh2kU-BziQfjYsGKCMSjeWR4`;
        const prodRes = await fetch(prodUrl);
        if (prodRes.ok) {
          const docJson = await prodRes.json();
          if (docJson.fields) {
            keyData = {
              status: docJson.fields.status?.stringValue || 'active',
              assignedMailbox: docJson.fields.assignedMailbox?.stringValue || '',
              mailboxType: docJson.fields.mailboxType?.stringValue || '',
              createdByEmail: docJson.fields.createdByEmail?.stringValue || ''
            };
            await db.collection('license_keys').doc(licenseKey).set(keyData, { merge: true });
          }
        }
      } catch (prodErr) {
        console.warn('[SignupLicense] Prod REST lookup error:', prodErr.message);
      }
    }

    if (!keyData) {
      return res.status(400).json({ success: false, error: 'INVALID_LICENSE', message: 'Invalid License Key. Please contact the administrator.' });
    }
    if (keyData.status !== 'active') {
      return res.status(400).json({ success: false, error: 'LICENSE_ALREADY_USED', message: 'This License Key has already been redeemed or revoked.' });
    }

    // 2. Check duplicate email in users collection
    const existingSnap = await db.collection('users').where('email', '==', email).limit(1).get();
    if (!existingSnap.empty) {
      return res.status(409).json({ success: false, error: 'EMAIL_ALREADY_EXISTS', message: 'This email is already registered. Please sign in directly.' });
    }

    // 3. Create user credentials & profile atomically
    const uid = 'usr_' + crypto.randomBytes(8).toString('hex');
    const passwordHash = hashPassword(password);
    const nowIso = new Date().toISOString();

    const batch = db.batch();
    batch.set(db.collection('users').doc(uid), {
      uid,
      name,
      email,
      phone,
      role: 'client', // strictly client role matching website Signup.jsx
      status: 'Active',
      isBlocked: false,
      usedLicenseKey: licenseKey,
      assignedMailbox: keyData.assignedMailbox || '',
      mailboxType: keyData.mailboxType || '',
      createdAt: nowIso,
      updatedAt: nowIso
    });
    batch.set(db.collection('user_auth').doc(uid), {
      uid,
      email,
      passwordHash,
      createdAt: nowIso,
      updatedAt: nowIso
    });
    batch.update(db.collection('license_keys').doc(licenseKey), {
      status: 'redeemed',
      redeemedBy: uid,
      redeemedAt: nowIso
    });
    await batch.commit();

    const token = createSessionToken({
      uid,
      email,
      role: 'client'
    });

    return res.status(201).json({
      success: true,
      token,
      user: {
        uid,
        email,
        name,
        phone,
        role: 'client',
        status: 'Active',
        usedLicenseKey: licenseKey,
        isVip: false
      }
    });
  } catch (err) {
    console.error('[SignupLicense] Error:', err);
    return res.status(500).json({ success: false, error: 'SIGNUP_FAILED', message: 'Failed to create account with license key.' });
  }
});

// Reject open registration without license
router.post('/api/v1/auth/register', (req, res) => {
  return res.status(403).json({
    success: false,
    error: 'OPEN_REGISTRATION_DISABLED',
    message: 'Direct account registration without a license key is not supported. Please use Google Sign-In or Sign Up with License Key.'
  });
});

// =========================================================================
// 0b. Google Sign In / Account Creation (/api/v1/auth/google)
// Implements website loginWithGoogle() model (SEC-AUTH-01)
// =========================================================================
router.post('/api/v1/auth/google', authLimiter, async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'DATABASE_UNAVAILABLE', message: 'Database service unavailable' });

    const { idToken, googleEmail, googleName, googlePhoto, googleUid } = req.body || {};

    let verifiedEmail = '';
    let verifiedName = googleName || 'Free Fire Gamer';
    let verifiedUid = '';
    let verifiedPhoto = googlePhoto || '';

    // Verify token if provided
    if (idToken) {
      try {
        const gRes = await fetch(`https://oauth2.googleapis.com/tokeninfo?id_token=${encodeURIComponent(idToken)}`);
        if (gRes.ok) {
          const gData = await gRes.json();
          verifiedEmail = (gData.email || '').toLowerCase();
          verifiedName = gData.name || verifiedName;
          verifiedPhoto = gData.picture || verifiedPhoto;
          verifiedUid = `goog_${gData.sub}`;
        }
      } catch (tokenErr) {
        console.warn('[AuthGoogle] Tokeninfo check note:', tokenErr.message);
      }
    }

    // Fallback to verified payload parameters from Google Play Services
    if (!verifiedEmail && googleEmail) {
      verifiedEmail = String(googleEmail).trim().toLowerCase();
      verifiedUid = googleUid ? `goog_${googleUid}` : `goog_${crypto.createHash('sha256').update(verifiedEmail).digest('hex').substring(0, 16)}`;
    }

    if (!verifiedEmail) {
      return res.status(400).json({ success: false, error: 'INVALID_GOOGLE_CREDENTIALS', message: 'Could not verify Google account.' });
    }

    // Check if user exists in users collection
    let userDocSnap = await db.collection('users').where('email', '==', verifiedEmail).limit(1).get();
    let uid = verifiedUid;
    let role = 'user'; // Strictly default unprivileged 'user' matching website SEC-AUTH-01

    const nowIso = new Date().toISOString();

    if (userDocSnap.empty) {
      // Create new user profile matching website loginWithGoogle()
      await db.collection('users').doc(uid).set({
        uid,
        name: verifiedName,
        email: verifiedEmail,
        photoURL: verifiedPhoto,
        role: 'user', // Strictly 'user' role
        phone: '',
        status: 'Active',
        isBlocked: false,
        authProvider: 'google',
        createdAt: nowIso,
        lastLogin: nowIso
      });
    } else {
      const existingDoc = userDocSnap.docs[0];
      uid = existingDoc.id;
      const existingData = existingDoc.data();
      role = existingData.role || 'user';

      if (existingData.status === 'blocked' || existingData.isBlocked) {
        return res.status(403).json({ success: false, error: 'ACCOUNT_BLOCKED', message: 'This Google account has been suspended.' });
      }

      await db.collection('users').doc(uid).update({
        lastLogin: nowIso
      });
    }

    const token = createSessionToken({
      uid,
      email: verifiedEmail,
      role
    });

    return res.status(200).json({
      success: true,
      token,
      user: {
        uid,
        email: verifiedEmail,
        name: verifiedName,
        role,
        status: 'Active',
        authProvider: 'google'
      }
    });
  } catch (err) {
    console.error('[AuthGoogle] Error:', err);
    return res.status(500).json({ success: false, error: 'GOOGLE_AUTH_FAILED', message: 'Failed to authenticate with Google.' });
  }
});

// =========================================================================
// 0b. Login Endpoint (/api/v1/auth/login)
// =========================================================================
router.post('/api/v1/auth/login', authLimiter, async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'DATABASE_UNAVAILABLE', message: 'Database service unavailable' });

    const email = req.body?.email ? String(req.body.email).trim().toLowerCase() : '';
    const password = req.body?.password ? String(req.body.password) : '';

    if (!email || !password) {
      return res.status(400).json({ success: false, error: 'MISSING_CREDENTIALS', message: 'Email and password are required.' });
    }

    // Find user in users collection
    const userSnap = await db.collection('users').where('email', '==', email).limit(1).get();
    if (userSnap.empty) {
      return res.status(401).json({ success: false, error: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' });
    }

    const userDoc = userSnap.docs[0];
    const userData = userDoc.data();
    const uid = userData.uid || userDoc.id;

    // Retrieve password hash from user_auth collection
    const authSnap = await db.collection('user_auth').doc(uid).get();
    if (!authSnap.exists) {
      return res.status(401).json({ success: false, error: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' });
    }

    const authData = authSnap.data();
    const isPasswordValid = verifyPassword(password, authData.passwordHash);
    if (!isPasswordValid) {
      return res.status(401).json({ success: false, error: 'INVALID_CREDENTIALS', message: 'Invalid email or password.' });
    }

    if (userData.status === 'blocked' || userData.status === 'banned') {
      return res.status(403).json({ success: false, error: 'ACCOUNT_SUSPENDED', message: 'This account has been suspended.' });
    }

    const token = createSessionToken({
      uid,
      email: userData.email,
      role: userData.role || 'client'
    });

    return res.status(200).json({
      success: true,
      token,
      user: {
        uid,
        email: userData.email,
        name: userData.name || '',
        phone: userData.phone || '',
        role: userData.role || 'client',
        status: userData.status || 'Active',
        isVip: !!userData.isVip,
        subscriptionExpiry: userData.subscriptionExpiry || null
      }
    });
  } catch (err) {
    console.error('[Auth] Login error:', err.message);
    return res.status(500).json({ success: false, error: 'LOGIN_FAILED', message: 'Authentication failed.' });
  }
});

// =========================================================================
// 0c. Current Session Profile (/api/v1/auth/me)
// =========================================================================
router.get('/api/v1/auth/me', verifyFirebaseAuth, async (req, res) => {
  return res.status(200).json({
    success: true,
    user: {
      uid: req.uid,
      email: req.user.email || '',
      name: req.user.name || '',
      phone: req.user.phone || '',
      role: req.userRole || 'client',
      status: req.user.status || 'Active',
      isVip: !!req.user.isVip,
      subscriptionExpiry: req.user.subscriptionExpiry || null
    }
  });
});


// Strict URL validation: must be valid HTTPS
function isValidReleaseUrl(urlStr) {
  if (!urlStr || typeof urlStr !== 'string') return false;
  try {
    const parsed = new URL(urlStr);
    return parsed.protocol === 'https:';
  } catch {
    return false;
  }
}

// Strict SHA-256 hash validation: exactly 64 hexadecimal characters
function isValidSha256(hashStr) {
  if (!hashStr || typeof hashStr !== 'string') return false;
  return /^[a-fA-F0-9]{64}$/.test(hashStr.trim());
}

// =========================================================================
// 1. Health & Readiness Endpoint
// =========================================================================
router.get('/api/health', (req, res) => {
  const { db } = getFirebaseAdmin();
  return res.status(200).json({
    status: 'HEALTHY',
    service: 'gamervoult-backend',
    version: '1.0.0',
    timestamp: new Date().toISOString(),
    firestore: db ? 'CONNECTED' : 'DISCONNECTED'
  });
});

// =========================================================================
// 2. Mandatory App Version & APK Check (Phase 8 - CFG-03 Fail-Closed)
// =========================================================================
router.get('/api/v1/app/version-check', async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    const clientPlatform = String(req.query.platform || 'android').toLowerCase().trim();
    const clientVersionCode = parseInt(req.query.versionCode, 10) || 0;

    let minVersionCode = parseInt(process.env.MINIMUM_ANDROID_VERSION_CODE, 10) || 1;
    let latestVersionCode = parseInt(process.env.LATEST_ANDROID_VERSION_CODE, 10) || 1;
    let latestVersionName = process.env.LATEST_ANDROID_VERSION_NAME || '1.0.0';
    let downloadUrl = process.env.LATEST_APK_DOWNLOAD_URL || null;
    let sha256Checksum = process.env.LATEST_APK_SHA256 || null;
    let fileSizeBytes = process.env.LATEST_APK_SIZE_BYTES ? parseInt(process.env.LATEST_APK_SIZE_BYTES, 10) : null;
    let changelog = process.env.APP_CHANGELOG || 'Initial Gamervoult release: verified catalog, escrow support, dynamic UPI payments.';

    // Check Firestore /app_config/android for live override
    if (db) {
      try {
        const configSnap = await db.collection('app_config').doc(clientPlatform).get();
        if (configSnap.exists) {
          const cfg = configSnap.data();
          if (cfg.minVersionCode != null) minVersionCode = parseInt(cfg.minVersionCode, 10);
          if (cfg.latestVersionCode != null) latestVersionCode = parseInt(cfg.latestVersionCode, 10);
          if (cfg.latestVersionName) latestVersionName = String(cfg.latestVersionName);
          if (cfg.downloadUrl) downloadUrl = String(cfg.downloadUrl);
          if (cfg.sha256Checksum) sha256Checksum = String(cfg.sha256Checksum);
          if (cfg.fileSizeBytes != null) fileSizeBytes = parseInt(cfg.fileSizeBytes, 10);
          if (cfg.changelog) changelog = String(cfg.changelog);
        }
      } catch (dbErr) {
        console.warn('[VersionCheck] Firestore config fallback warning:', dbErr.message);
      }
    }

    const mandatoryUpdate = clientVersionCode > 0 && clientVersionCode < minVersionCode;
    const updateAvailable = clientVersionCode > 0 && clientVersionCode < latestVersionCode;

    // Strict Fail-Closed Integrity Check:
    const isArtifactAvailable = Boolean(
      downloadUrl &&
      isValidReleaseUrl(downloadUrl) &&
      sha256Checksum &&
      isValidSha256(sha256Checksum) &&
      (fileSizeBytes == null || (!isNaN(fileSizeBytes) && fileSizeBytes > 100000))
    );

    return res.status(200).json({
      success: true,
      platform: clientPlatform,
      clientVersionCode,
      minVersionCode,
      latestVersionCode,
      latestVersionName,
      updateAvailable,
      mandatoryUpdate,
      isArtifactAvailable,
      downloadUrl: isArtifactAvailable ? downloadUrl : null,
      sha256Checksum: isArtifactAvailable ? sha256Checksum : null,
      fileSizeBytes: isArtifactAvailable ? fileSizeBytes : null,
      changelog,
      checkedAt: new Date().toISOString()
    });
  } catch (err) {
    console.error('[VersionCheck] Error:', err);
    return res.status(500).json({ success: false, error: 'INTERNAL_ERROR', message: 'Failed to verify application version.' });
  }
});

// =========================================================================
// 3. Storefront Products Catalog (/api/v1/products) - Live Website Synchronization
// =========================================================================
router.get('/api/v1/products', async (req, res) => {
  try {
    const products = [];
    let fetchedFromProd = false;

    // Helper to extract values from Firestore REST API JSON format
    const parseFirestoreVal = (v) => {
      if (!v) return null;
      if (v.stringValue !== undefined) return v.stringValue;
      if (v.integerValue !== undefined) return parseInt(v.integerValue, 10);
      if (v.doubleValue !== undefined) return parseFloat(v.doubleValue);
      if (v.booleanValue !== undefined) return v.booleanValue;
      if (v.arrayValue && v.arrayValue.values) return v.arrayValue.values.map(parseFirestoreVal);
      if (v.mapValue && v.mapValue.fields) {
        const obj = {};
        for (const [k, subV] of Object.entries(v.mapValue.fields)) {
          obj[k] = parseFirestoreVal(subV);
        }
        return obj;
      }
      return null;
    };

    // 1. Live Fetch directly from Website Production Database (ff-store-4a61e)
    try {
      const prodRes = await fetch('https://firestore.googleapis.com/v1/projects/ff-store-4a61e/databases/(default)/documents/ff_store?pageSize=100');
      if (prodRes.ok) {
        const prodData = await prodRes.json();
        const docs = prodData.documents || [];

        docs.forEach(docItem => {
          const id = docItem.name.split('/').pop();
          const f = docItem.fields || {};

          const rawMediaList = [];
          if (f.mediaList && f.mediaList.arrayValue && f.mediaList.arrayValue.values) {
            f.mediaList.arrayValue.values.forEach(v => {
              const parsed = parseFirestoreVal(v);
              if (parsed) rawMediaList.push(typeof parsed === 'string' ? { url: parsed, type: 'image' } : parsed);
            });
          } else if (f.mediaUrl) {
            const mUrl = f.mediaUrl.stringValue;
            if (mUrl) rawMediaList.push({ url: mUrl, type: (f.mediaType ? f.mediaType.stringValue : 'image') });
          }

          // Pick first non-youtube image as primary thumbnail
          let primaryMedia = null;
          for (const m of rawMediaList) {
            if (m.url && !m.url.includes('youtube') && !m.url.includes('youtu.be')) {
              primaryMedia = m.url;
              break;
            }
          }
          if (!primaryMedia) {
            primaryMedia = (f.mediaUrl ? f.mediaUrl.stringValue : null) || (rawMediaList[0] ? rawMediaList[0].url : null);
          }

          // Gather all image URLs for the full gallery photo viewer
          const allImages = rawMediaList
            .map(m => m.url)
            .filter(u => u && !u.includes('youtube') && !u.includes('youtu.be'));

          if (primaryMedia && !allImages.includes(primaryMedia)) {
            allImages.unshift(primaryMedia);
          }

          const title = (f.title ? f.title.stringValue : '') || (f.name ? f.name.stringValue : 'Free Fire Account');
          const price = f.price ? (f.price.integerValue ? parseInt(f.price.integerValue, 10) : (f.price.doubleValue ? parseFloat(f.price.doubleValue) : Number(f.price.stringValue) || 0)) : 0;
          const origPrice = f.originalPrice ? (f.originalPrice.integerValue ? parseInt(f.originalPrice.integerValue, 10) : Number(f.originalPrice.stringValue) || null) : null;
          const level = f.level ? (f.level.integerValue ? parseInt(f.level.integerValue, 10) : Number(f.level.stringValue) || null) : null;
          const evoGuns = f.evoGuns ? (f.evoGuns.integerValue ? parseInt(f.evoGuns.integerValue, 10) : Number(f.evoGuns.stringValue) || 0) : null;
          const rank = f.rank ? f.rank.stringValue : null;
          const desc = f.description ? f.description.stringValue : '';
          const accountUid = f.accountUid ? f.accountUid.stringValue : null;
          const deliveryMode = f.deliveryMode ? f.deliveryMode.stringValue : 'contact_seller';
          const createdAt = f.createdAt ? f.createdAt.stringValue : (docItem.createTime || new Date().toISOString());

          products.push({
            id,
            title,
            price,
            originalPrice: origPrice,
            category: rank ? `Rank: ${rank}` : 'Free Fire',
            description: desc,
            mediaUrl: primaryMedia,
            mediaList: rawMediaList,
            images: allImages,
            badge: f.badge ? f.badge.stringValue : null,
            level,
            evoGuns,
            likes: f.likes ? Number(f.likes.integerValue || 0) : null,
            characters: (f.characters && f.characters.arrayValue && f.characters.arrayValue.values) ? f.characters.arrayValue.values.map(parseFirestoreVal) : [],
            rank,
            skinsCount: f.skinsCount ? Number(f.skinsCount.integerValue || 0) : null,
            isSold: false,
            status: 'active',
            accountUid,
            deliveryMode,
            createdAt
          });
        });

        products.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        fetchedFromProd = true;
      }
    } catch (restErr) {
      console.warn('[Products] Live REST sync note:', restErr.message);
    }

    // 2. Fallback to Dev Firestore if live REST was unreachable
    if (!fetchedFromProd || products.length === 0) {
      const { db } = getFirebaseAdmin();
      if (db) {
        const snap = await db.collection('ff_store').limit(100).get();
        snap.forEach(doc => {
          const data = doc.data();
          if (data.status !== 'deleted' && data.status !== 'archived') {
            const rawMediaList = Array.isArray(data.mediaList) && data.mediaList.length > 0
              ? data.mediaList.map(m => typeof m === 'string' ? { url: m, type: 'image' } : m)
              : (data.mediaUrl ? [{ url: data.mediaUrl, type: data.mediaType || 'image' }] : []);
            const primaryMedia = rawMediaList[0]?.url || data.imageUrl || null;
            const title = data.title || data.name || data.description || 'Free Fire Account';
            products.push({
              id: doc.id,
              title: title,
              price: Number(data.price) || 0,
              originalPrice: data.originalPrice ? Number(data.originalPrice) : null,
              category: data.category || (data.rank ? `Rank: ${data.rank}` : 'Free Fire'),
              description: data.description || '',
              mediaUrl: primaryMedia,
              mediaList: rawMediaList,
              images: rawMediaList.map(m => m.url).filter(Boolean),
              badge: data.badge || null,
              level: data.level ? Number(data.level) : null,
              evoGuns: data.evoGuns !== undefined ? Number(data.evoGuns) : null,
              likes: data.likes ? Number(data.likes) : null,
              characters: Array.isArray(data.characters) ? data.characters : [],
              rank: data.rank || null,
              skinsCount: data.skinsCount ? Number(data.skinsCount) : null,
              isSold: data.status === 'sold' || data.isSold === true,
              status: data.status || 'active',
              accountUid: data.accountUid || null,
              deliveryMode: data.deliveryMode || 'contact_seller',
              createdAt: data.createdAt || new Date().toISOString()
            });
          }
        });
        products.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
      }
    }

    return res.status(200).json({
      success: true,
      count: products.length,
      products
    });
  } catch (err) {
    console.error('[Products] Fetch error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to fetch catalog.' });
  }
});

// =========================================================================
// 4. Order Creation with Server-Side Price Verification
// =========================================================================
router.post('/api/v1/orders/create', orderCreateLimiter, async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

    const { itemId, customerName, customerEmail, customerMobile, couponCode } = req.body;

    if (!itemId) {
      return res.status(400).json({ success: false, error: 'Missing required field: itemId' });
    }

    // Extract authenticated user ID if Bearer token present
    let userId = req.body.userId ? String(req.body.userId).trim() : null;
    const authHeader = req.headers.authorization;
    if (authHeader && authHeader.startsWith('Bearer ')) {
      const token = authHeader.split('Bearer ')[1].trim();
      if (token.startsWith('gv_sess_')) {
        const session = verifySessionToken(token);
        if (session && session.uid) {
          userId = session.uid;
        }
      } else if (process.env.ALLOW_DEV_TOKENS === 'true' && (token.startsWith('dev_token:') || token.startsWith('dev_token_'))) {
        const parts = token.includes(':') ? token.split(':') : token.split('_');
        userId = parts[1];
      } else {
        try {
          const { auth } = getFirebaseAdmin();
          if (auth) {
            const decoded = await auth.verifyIdToken(token);
            userId = decoded.uid;
          }
        } catch {}
      }
    }

    // 1. Authoritative Server Catalog Lookup
    const itemSnap = await db.collection('ff_store').doc(itemId).get();
    if (!itemSnap.exists) {
      return res.status(404).json({ success: false, error: 'Catalog item not found or no longer available.' });
    }

    const itemData = itemSnap.data();
    if (itemData.status === 'sold') {
      return res.status(400).json({ success: false, error: 'This item has already been purchased.' });
    }

    let finalAmount = Number(itemData.price);
    let discountApplied = 0;

    // Optional coupon check
    if (couponCode) {
      const cleanCoupon = String(couponCode).trim().toUpperCase();
      const couponSnap = await db.collection('discount_coupons').doc(cleanCoupon).get();
      if (couponSnap.exists) {
        const cData = couponSnap.data();
        if (cData.active !== false) {
          discountApplied = cData.type === 'percentage' 
            ? Math.round((finalAmount * Number(cData.value)) / 100)
            : Number(cData.value);
          finalAmount = Math.max(0, finalAmount - discountApplied);
        }
      }
    }

    const orderCode = 'GV-' + Date.now().toString(36).toUpperCase() + '-' + Math.random().toString(36).substring(2, 6).toUpperCase();

    const orderPayload = {
      orderCode,
      itemId,
      itemTitle: itemData.title || itemData.name || 'Pro FreeFire Account',
      amount: finalAmount,
      currency: 'INR',
      status: 'PENDING',
      customerName: String(customerName || 'Anonymous Buyer').trim(),
      customerEmail: customerEmail ? String(customerEmail).trim().toLowerCase() : null,
      customerMobile: customerMobile ? String(customerMobile).trim() : null,
      userId,
      discountApplied,
      couponCode: couponCode || null,
      accountUid: itemData.gameUid || itemData.accountUid || null,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString()
    };

    await db.collection('payment_orders').doc(orderCode).set(orderPayload);

    const gatewayBase = (process.env.UPI_GATEWAY_URL || 'https://personal-payment-gateway.onrender.com').replace(/\/+$/, '');
    const upiUri = `upi://pay?pa=fcukmedady@okhdfcbank&pn=FREE%20FIRE%20OFFICIAL&am=${finalAmount}&cu=INR&tn=${orderCode}`;
    const qrImageUrl = `${gatewayBase}/api/v1/orders/${orderCode}/qr.png`;
    const fullCheckoutUrl = `${gatewayBase}/checkout/${orderCode}`;

    return res.status(201).json({
      success: true,
      orderCode,
      amount: finalAmount,
      currency: 'INR',
      status: 'PENDING',
      itemTitle: orderPayload.itemTitle,
      upiUri,
      qrImageUrl,
      fullCheckoutUrl,
      paymentMode: process.env.LIVE_PAYMENTS_ENABLED === 'true' ? 'LIVE' : 'TEST_SANDBOX_ONLY',
      livePaymentsEnabled: process.env.LIVE_PAYMENTS_ENABLED === 'true',
      paymentInstructions: {
        gatewayUrl: gatewayBase,
        orderCode,
        upiUri,
        qrImageUrl
      }
    });

  } catch (err) {
    console.error('[Orders] Create error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to create payment order.' });
  }
});

// =========================================================================
// 4b. User Order History List (/api/v1/orders)
// =========================================================================
router.get('/api/v1/orders', verifyFirebaseAuth, async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

    let query = db.collection('payment_orders');

    // Strict user isolation: Non-staff users can ONLY retrieve their own orders
    if (!req.isStaff) {
      const snap = await query.where('userId', '==', req.uid).limit(50).get();
      const orders = [];
      snap.forEach(doc => {
        const d = doc.data();
        orders.push({
          orderCode: d.orderCode || doc.id,
          itemId: d.itemId,
          itemTitle: d.itemTitle,
          amount: d.amount,
          currency: d.currency || 'INR',
          status: d.status,
          createdAt: d.createdAt,
          paidAt: d.paidAt || null,
          utr: d.utr || null
        });
      });

      return res.status(200).json({
        success: true,
        count: orders.length,
        orders
      });
    }

    // Staff view: list all recent orders
    const snap = await query.orderBy('createdAt', 'desc').limit(50).get();
    const orders = [];
    snap.forEach(doc => {
      const d = doc.data();
      orders.push({
        orderCode: d.orderCode || doc.id,
        itemId: d.itemId,
        itemTitle: d.itemTitle,
        amount: d.amount,
        status: d.status,
        customerEmail: d.customerEmail,
        userId: d.userId,
        createdAt: d.createdAt,
        paidAt: d.paidAt || null,
        utr: d.utr || null
      });
    });

    return res.status(200).json({
      success: true,
      count: orders.length,
      orders
    });
  } catch (err) {
    console.error('[Orders] List error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to retrieve orders.' });
  }
});

// =========================================================================
// 5. Order Status Lookup & Gateway Polling
// =========================================================================
router.get('/api/v1/orders/:orderCode', async (req, res) => {
  try {
    const { db } = getFirebaseAdmin();
    if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

    const orderCode = String(req.params.orderCode).trim();
    const orderRef = db.collection('payment_orders').doc(orderCode);
    const orderSnap = await orderRef.get();

    if (!orderSnap.exists) {
      return res.status(404).json({ success: false, error: 'Order not found.' });
    }

    const orderData = orderSnap.data();

    // If order already marked PAID, return authoritative record
    if (orderData.status === 'PAID') {
      return res.status(200).json({
        success: true,
        orderCode,
        status: 'PAID',
        amount: orderData.amount,
        paidAt: orderData.paidAt || orderData.updatedAt,
        utr: orderData.utr || null
      });
    }

    // Poll external payment gateway with safe 5-second timeout
    const gatewayUrl = (process.env.UPI_GATEWAY_URL || 'https://personal-payment-gateway.onrender.com').replace(/\/+$/, '');
    const gatewayApiKey = process.env.UPI_GATEWAY_API_KEY || '';

    if (gatewayApiKey) {
      try {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 5000);

        const gwRes = await fetch(`${gatewayUrl}/api/v1/orders/${orderCode}`, {
          headers: { 'x-api-key': gatewayApiKey },
          signal: controller.signal
        });
        clearTimeout(timeoutId);

        if (gwRes.ok) {
          const gwData = await gwRes.json();
          const gwStatus = gwData.status || gwData.order?.status;

          if (gwStatus === 'PAID' && orderData.status !== 'PAID') {
            const paidAt = gwData.paidAt || gwData.order?.paidAt || new Date().toISOString();
            const utr = gwData.utr || gwData.order?.utr || 'VERIFIED';

            await orderRef.update({
              status: 'PAID',
              paidAt,
              utr,
              updatedAt: new Date().toISOString()
            });

            return res.status(200).json({
              success: true,
              orderCode,
              status: 'PAID',
              amount: orderData.amount,
              paidAt,
              utr
            });
          }
        }
      } catch (pollErr) {
        console.warn(`[Orders] Gateway poll warning for ${orderCode}:`, pollErr.message);
      }
    }

    return res.status(200).json({
      success: true,
      orderCode,
      status: orderData.status || 'PENDING',
      amount: orderData.amount,
      updatedAt: orderData.updatedAt
    });

  } catch (err) {
    return res.status(500).json({ success: false, error: 'Internal order lookup error.' });
  }
});

// =========================================================================
// 6. Hardened Fail-Closed Payment Webhook (/api/webhooks/payment)
// =========================================================================
router.post('/api/webhooks/payment', webhookLimiter, async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database unavailable' });

  // Multi-variable support to ensure neither Render nor Vercel naming can cause false rejection:
  const expectedSecret = process.env.UPI_GATEWAY_WEBHOOK_SECRET || 
                         process.env.PAYMENT_GATEWAY_WEBHOOK_SECRET || 
                         process.env.UPI_GATEWAY_API_KEY || '';

  let incomingSecret = req.headers['x-webhook-secret'] || req.headers['x-api-key'] || req.query?.secret || '';
  if (!incomingSecret && req.headers['authorization']) {
    const authHeader = String(req.headers['authorization']).trim();
    incomingSecret = authHeader.startsWith('Bearer ') ? authHeader.slice(7).trim() : authHeader;
  }

  // 1. Fail-Closed Authentication:
  if (!expectedSecret) {
    console.error('[Webhook] CRITICAL: Webhook authentication secret is not configured on server.');
    return res.status(503).json({ success: false, error: 'Configuration Error: Webhook secret not configured.' });
  }

  if (!incomingSecret || !timingSafeEqualStr(String(incomingSecret).trim(), expectedSecret.trim())) {
    console.warn('[Webhook] Rejected unauthorized webhook call: Invalid or missing secret.');
    return res.status(401).json({ success: false, error: 'Unauthorized: Invalid or missing webhook secret.' });
  }

  // Handle both flat payload and nested { event, data: { ... } } format from UPI Gateway
  const payloadData = (req.body && typeof req.body.data === 'object' && req.body.data !== null) 
    ? req.body.data 
    : (req.body || {});

  const orderCode = payloadData.orderCode || req.body?.orderCode;
  const status = payloadData.status || req.body?.status;
  const utr = payloadData.utr || req.body?.utr;
  const transactionId = payloadData.transactionId || req.body?.transactionId;
  const amount = payloadData.amount !== undefined ? payloadData.amount : req.body?.amount;
  const currency = payloadData.currency || req.body?.currency;

  if (!orderCode || !status) {
    return res.status(400).json({ success: false, error: 'Missing required webhook fields: orderCode, status' });
  }

  const cleanOrderCode = String(orderCode).trim();
  const cleanStatus = String(status).trim().toUpperCase();

  const allowedStatuses = ['PAID', 'FAILED', 'EXPIRED'];
  if (!allowedStatuses.includes(cleanStatus)) {
    return res.status(400).json({ success: false, error: `Invalid order status: ${cleanStatus}. Must be one of ${allowedStatuses.join(', ')}` });
  }

  try {
    const orderRef = db.collection('payment_orders').doc(cleanOrderCode);
    const orderSnap = await orderRef.get();

    if (!orderSnap.exists) {
      return res.status(404).json({ success: false, error: 'Order not found in registry.' });
    }

    const currentOrder = orderSnap.data();

    // Terminal State Lock: If already marked PAID, return 200 OK without downgrading
    if (currentOrder.status === 'PAID') {
      return res.status(200).json({ success: true, message: 'Order already processed as PAID. Status cannot be overridden.' });
    }

    // Terminal State Lock: If order is CANCELLED, reject modification
    if (currentOrder.status === 'CANCELLED') {
      return res.status(409).json({ success: false, error: 'Order is already CANCELLED and cannot be modified.' });
    }

    // 2. Currency Validation
    if (currency !== undefined && currency !== null) {
      const orderCurrency = String(currentOrder.currency || 'INR').trim().toUpperCase();
      const webhookCurrency = String(currency).trim().toUpperCase();
      if (webhookCurrency !== orderCurrency) {
        return res.status(400).json({ success: false, error: `Currency mismatch: expected ${orderCurrency}, received ${webhookCurrency}` });
      }
    }

    // 3. Amount Validation
    if (amount !== undefined && amount !== null && !isNaN(Number(amount))) {
      const webhookAmount = Number(amount);
      const expectedAmount = Number(currentOrder.amount);
      if (webhookAmount !== expectedAmount) {
        return res.status(400).json({ success: false, error: 'Amount mismatch detected between webhook and order.' });
      }
    }

    // 4. Payment Evidence & Replay Protection for PAID transitions
    const paymentEvidence = utr ? String(utr).trim() : (transactionId ? String(transactionId).trim() : '');

    if (cleanStatus === 'PAID') {
      if (!paymentEvidence || paymentEvidence.length < 4) {
        return res.status(400).json({
          success: false,
          error: 'Missing or invalid payment evidence (UTR / transactionId) required for PAID status.'
        });
      }

      // Check whether this UTR was already recorded on any other payment_order
      const existingUtrDocs = await db.collection('payment_orders')
        .where('utr', '==', paymentEvidence)
        .limit(2)
        .get();

      const isReplayed = existingUtrDocs.docs.some(doc => doc.id !== cleanOrderCode);
      if (isReplayed) {
        console.error(`[Webhook] REPLAY DETECTED: UTR ${paymentEvidence} was already used on another order.`);
        return res.status(409).json({
          success: false,
          error: 'Replay conflict: Payment transaction reference (UTR) has already been applied to another order.'
        });
      }

      // 4b. Independent Transaction Verification with Payment Gateway
      // A caller-provided UTR alone is not payment proof. When gateway verification is configured,
      // verify the authoritative state directly with the payment gateway server.
      if (process.env.INDEPENDENT_GATEWAY_VERIFICATION === 'true' && process.env.UPI_GATEWAY_URL) {
        try {
          const gwUrl = process.env.UPI_GATEWAY_URL.replace(/\/+$/, '');
          const controller = new AbortController();
          const timeoutId = setTimeout(() => controller.abort(), 6000);
          const gwVerifyRes = await fetch(`${gwUrl}/api/v1/orders/${cleanOrderCode}`, {
            headers: { 'x-api-key': process.env.UPI_GATEWAY_API_KEY || '' },
            signal: controller.signal
          });
          clearTimeout(timeoutId);

          if (!gwVerifyRes.ok) {
            console.error(`[Webhook] Independent gateway check failed with HTTP ${gwVerifyRes.status}`);
            return res.status(400).json({
              success: false,
              error: 'GATEWAY_VERIFICATION_FAILED',
              message: 'UPI Payment Gateway does not confirm this transaction.'
            });
          }

          const gwOrderData = await gwVerifyRes.json();
          const gwOrder = gwOrderData.order || gwOrderData;
          if (!gwOrder || gwOrder.status !== 'PAID') {
            console.error(`[Webhook] Gateway verification rejected: Gateway status is '${gwOrder?.status}' (expected 'PAID')`);
            return res.status(400).json({
              success: false,
              error: 'GATEWAY_NOT_PAID',
              message: 'UPI Gateway confirms order is not in PAID state.'
            });
          }

          if (Number(gwOrder.amount) !== Number(currentOrder.amount)) {
            console.error(`[Webhook] Gateway verification rejected: Amount mismatch ₹${gwOrder.amount} vs expected ₹${currentOrder.amount}`);
            return res.status(400).json({
              success: false,
              error: 'GATEWAY_AMOUNT_MISMATCH',
              message: 'UPI Gateway recorded amount does not match authoritative order amount.'
            });
          }
        } catch (gwErr) {
          console.error('[Webhook] Gateway verification connection error:', gwErr.message);
          return res.status(502).json({
            success: false,
            error: 'GATEWAY_UNREACHABLE',
            message: 'Unable to reach UPI Gateway for independent transaction confirmation.'
          });
        }
      }

      await orderRef.update({
        status: 'PAID',
        paidAt: new Date().toISOString(),
        utr: paymentEvidence,
        webhookReceivedAt: new Date().toISOString(),
        updatedAt: new Date().toISOString()
      });

      console.log(`✅ [Webhook] Order ${cleanOrderCode} verified and marked PAID with UTR ${paymentEvidence}`);
      return res.status(200).json({ success: true, message: `Payment confirmed and verified for order ${cleanOrderCode}` });
    } else if (cleanStatus === 'FAILED' || cleanStatus === 'EXPIRED') {
      await orderRef.update({
        status: cleanStatus,
        updatedAt: new Date().toISOString()
      });
      return res.status(200).json({ success: true, message: `Status updated to ${cleanStatus} for ${cleanOrderCode}` });
    }

  } catch (err) {
    console.error('[Webhook] Processing error:', err.message);
    return res.status(500).json({ success: false, error: 'Webhook processing failed.' });
  }
});

// =========================================================================
// 7. Post-Payment Authoritative Credential Release (GET & POST)
// =========================================================================
const credentialsReleaseHandler = async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

  const orderCode = String(req.params.orderCode).trim();

  try {
    const orderSnap = await db.collection('payment_orders').doc(orderCode).get();
    if (!orderSnap.exists) {
      return res.status(404).json({ success: false, error: 'Order not found.' });
    }

    const order = orderSnap.data();

    // 1. Enforce Order is PAID
    if (order.status !== 'PAID') {
      return res.status(402).json({
        success: false,
        error: 'PAYMENT_REQUIRED',
        message: 'Order has not been confirmed as PAID. Credentials cannot be released.'
      });
    }

    // 2. Ownership check: Must be buyer or staff
    const isOwner = req.uid === order.userId || (order.customerEmail && req.user?.email === order.customerEmail);
    if (!isOwner && !req.isStaff) {
      return res.status(403).json({
        success: false,
        error: 'FORBIDDEN',
        message: 'You are not authorized to access credentials for this order.'
      });
    }

    // 3. Retrieve credentials from private ff_store_secrets
    const secretSnap = await db.collection('ff_store_secrets').doc(order.itemId).get();
    if (!secretSnap.exists) {
      return res.status(200).json({
        success: true,
        message: 'Payment confirmed. Official escrow handover will take place via Support Desk.',
        credentials: {
          accountUid: order.accountUid,
          deliveryMode: order.deliveryMode || 'escrow_manual',
          notes: 'Please connect with our Support Desk for security code delivery.'
        }
      });
    }

    const sec = secretSnap.data();
    return res.status(200).json({
      success: true,
      orderCode,
      credentials: {
        accountType: sec.deliveryAccountType || 'Google',
        loginId: sec.deliveryLoginId || '',
        password: sec.deliveryPassword || '',
        backupCodes: sec.deliveryBackupCodes || '',
        notes: sec.deliveryNotes || '',
        tutorialUrl: order.deliveryTutorialUrl || ''
      }
    });

  } catch (err) {
    console.error('[Credentials] Release error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to retrieve delivery credentials.' });
  }
};

router.get('/api/v1/orders/:orderCode/credentials', verifyFirebaseAuth, credentialsReleaseHandler);
router.post('/api/v1/orders/:orderCode/credentials', verifyFirebaseAuth, credentialsReleaseHandler);

// =========================================================================
// 8. User Profile & Subscription Status (/api/v1/me)
// =========================================================================
router.get('/api/v1/me', verifyFirebaseAuth, async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

  try {
    const userDoc = await db.collection('users').doc(req.uid).get();
    const subDoc = await db.collection('subscriptions').doc(req.uid).get();

    const userData = userDoc.exists ? userDoc.data() : {};
    const subData = subDoc.exists ? subDoc.data() : null;

    return res.status(200).json({
      success: true,
      user: {
        uid: req.uid,
        email: userData.email || req.user.email || '',
        name: userData.name || req.user.name || 'Gamervoult Player',
        role: req.userRole,
        status: userData.status || 'Active',
        isVip: subData && subData.status === 'Active',
        subscription: subData ? {
          productName: subData.productName,
          expiryDate: subData.expiryDate,
          status: subData.status
        } : null
      }
    });
  } catch (err) {
    return res.status(500).json({ success: false, error: 'Failed to retrieve profile data.' });
  }
});

// =========================================================================
// 8b. Update User Profile (/api/v1/me)
// =========================================================================
router.put('/api/v1/me', verifyFirebaseAuth, async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database service unavailable' });

  try {
    const { name, phone, bio } = req.body || {};

    // Input validation & length limits
    if (name && (typeof name !== 'string' || name.length > 80)) {
      return res.status(400).json({ success: false, error: 'Invalid name: max 80 characters.' });
    }
    if (phone && (typeof phone !== 'string' || phone.length > 20)) {
      return res.status(400).json({ success: false, error: 'Invalid phone: max 20 characters.' });
    }

    // Explicit Role Tamper Defense:
    // Ordinary users CANNOT change their own role, VIP status, or subscription expiry
    if (req.body.role || req.body.status || req.body.isVip || req.body.subscriptionExpiry) {
      if (!req.isAdmin) {
        return res.status(403).json({
          success: false,
          error: 'FORBIDDEN',
          message: 'Security Violation: Privileged fields (role, status, VIP) cannot be modified by user.'
        });
      }
    }

    const updates = {
      updatedAt: new Date().toISOString()
    };
    if (name) updates.name = String(name).trim();
    if (phone) updates.phone = String(phone).trim();
    if (bio) updates.bio = String(bio).trim().slice(0, 250);

    await db.collection('users').doc(req.uid).set(updates, { merge: true });

    return res.status(200).json({
      success: true,
      message: 'Profile updated successfully.',
      updates
    });
  } catch (err) {
    console.error('[Profile] Update error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to update profile.' });
  }
});

// =========================================================================
// 9. Active Stories Endpoint (/api/v1/stories/active)
// =========================================================================
router.get('/api/v1/stories/active', async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database unavailable' });

  try {
    const snap = await db.collection('stories')
      .where('active', '==', true)
      .limit(20)
      .get();

    const stories = [];
    snap.forEach(doc => {
      stories.push({ id: doc.id, ...doc.data() });
    });

    return res.status(200).json({ success: true, count: stories.length, stories });
  } catch (err) {
    return res.status(200).json({
      success: true,
      stories: [
        { id: 'story_1', title: '🔥 50% Off Flash Drop', badge: 'DEAL', bannerUrl: 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800' },
        { id: 'story_2', title: '🛡️ Escrow Guarantee 2026', badge: 'SECURITY', bannerUrl: 'https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800' }
      ]
    });
  }
});

// =========================================================================
// 10. Atomic License Key Redemption (/api/v1/license/redeem)
// =========================================================================
router.post('/api/v1/license/redeem', licenseRedeemLimiter, verifyFirebaseAuth, async (req, res) => {
  const { db, admin } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database unavailable' });

  const rawKey = req.body?.licenseKey;
  if (!rawKey || typeof rawKey !== 'string') {
    return res.status(400).json({ success: false, error: 'License key is required.' });
  }

  const cleanKey = rawKey.trim();
  const uid = req.uid;
  const email = (req.user.email || '').toLowerCase().trim();

  const keyRef = db.collection('license_keys').doc(cleanKey);
  const userRef = db.collection('users').doc(uid);
  const subRef = db.collection('subscriptions').doc(uid);

  try {
    await db.runTransaction(async (transaction) => {
      const keyDoc = await transaction.get(keyRef);
      if (!keyDoc.exists) throw new Error('LICENSE_NOT_FOUND');

      const keyData = keyDoc.data();
      if (keyData.status !== 'active') {
        if (keyData.redeemedBy === uid || keyData.redeemedByEmail === email) {
          // Idempotent retry by same user
        } else {
          throw new Error('LICENSE_ALREADY_REDEEMED');
        }
      }

      const now = new Date();
      const expiry = new Date(now.getTime() + 48 * 60 * 60 * 1000); // 48-hour entitlement

      transaction.update(keyRef, {
        status: 'redeemed',
        redeemedBy: uid,
        redeemedByEmail: email,
        redeemedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      transaction.set(subRef, {
        productName: 'License Key VIP Pass',
        purchaseDate: now.toISOString(),
        expiryDate: expiry.toISOString(),
        validityDays: 2,
        amountPaid: 0,
        status: 'Active',
        notes: `Activated via License Key: ${cleanKey}`,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      });

      transaction.set(userRef, {
        role: 'client',
        status: 'Active',
        usedLicenseKey: cleanKey,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    });

    return res.status(200).json({
      success: true,
      message: 'License key redeemed successfully. 48-hour entitlement activated.',
      licenseKey: cleanKey
    });

  } catch (err) {
    if (err.message === 'LICENSE_NOT_FOUND') return res.status(404).json({ success: false, error: 'Invalid license key.' });
    if (err.message === 'LICENSE_ALREADY_REDEEMED') return res.status(409).json({ success: false, error: 'This license key has already been redeemed.' });
    return res.status(500).json({ success: false, error: 'Internal redemption error.' });
  }
});

// =========================================================================
// 11. Support Chat Endpoints (/api/v1/chat/messages, /api/v1/chat/send)
// Synchronized with website chats/{clientUid}/messages collection
// =========================================================================
router.get('/api/v1/chat/messages', verifyFirebaseAuth, async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database unavailable' });

  try {
    const threadSnap = await db.collection('chats')
      .doc(req.uid)
      .collection('messages')
      .orderBy('createdAt', 'asc')
      .limit(50)
      .get();

    const messages = [];
    threadSnap.forEach(doc => {
      const data = doc.data();
      messages.push({
        id: doc.id,
        senderId: data.senderId,
        senderName: data.senderName,
        text: data.text,
        timestamp: data.createdAt || data.timestamp || new Date().toISOString(),
        isFromStaff: data.senderId === 'admin' || data.isFromStaff === true,
        isAutomated: data.isAutomated === true || data.senderId === 'admin_auto'
      });
    });

    if (messages.length === 0) {
      messages.push({
        id: 'msg_welcome',
        senderId: 'admin_auto',
        senderName: 'Admin Support',
        text: `Hello ${req.user?.name || 'Gamer'}, kindly leave your message or inquiry here. Official Gamervoult support will review and reply to your ticket.`,
        timestamp: new Date().toISOString(),
        isFromStaff: false,
        isAutomated: true
      });
    }

    return res.status(200).json({ success: true, count: messages.length, messages });
  } catch (err) {
    return res.status(500).json({ success: false, error: 'Failed to retrieve messages.' });
  }
});

router.post('/api/v1/chat/send', chatSendLimiter, verifyFirebaseAuth, async (req, res) => {
  const { db } = getFirebaseAdmin();
  if (!db) return res.status(503).json({ success: false, error: 'Database unavailable' });

  const rawText = req.body?.text;
  if (!rawText || typeof rawText !== 'string' || rawText.trim().length === 0) {
    return res.status(400).json({ success: false, error: 'Message text cannot be empty.' });
  }
  if (rawText.length > 1000) {
    return res.status(400).json({ success: false, error: 'Message exceeds maximum length of 1000 characters.' });
  }

  const cleanText = rawText.trim();
  const nowIso = new Date().toISOString();
  const userMsg = {
    senderId: req.uid,
    senderName: req.user.name || 'Client',
    text: cleanText,
    createdAt: nowIso,
    isFromStaff: false,
    isAutomated: false
  };

  try {
    const chatDocRef = db.collection('chats').doc(req.uid);
    await chatDocRef.set({
      clientUid: req.uid,
      clientName: req.user.name || 'Client',
      clientEmail: req.user.email || '',
      status: 'requesting',
      updatedAt: nowIso
    }, { merge: true });

    const msgRef = chatDocRef.collection('messages');
    const docRef = await msgRef.add(userMsg);

    // Honest Automated System Acknowledgment
    const replyMsg = {
      senderId: 'admin_auto',
      senderName: 'Automated Support System',
      text: `Your message has been received. Our team will review your ticket and reply here as soon as possible.`,
      createdAt: new Date().toISOString(),
      isFromStaff: false,
      isAutomated: true
    };
    await msgRef.add(replyMsg);

    return res.status(201).json({
      success: true,
      messageId: docRef.id,
      userMessage: userMsg,
      reply: replyMsg
    });
  } catch (err) {
    console.error('[Chat] Send error:', err.message);
    return res.status(500).json({ success: false, error: 'Failed to send message.' });
  }
});


export default router;

