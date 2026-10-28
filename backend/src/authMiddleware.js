import crypto from 'crypto';
import { getFirebaseAdmin } from './firebase.js';

// Constant-time string comparison
export function timingSafeEqualStr(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false;
  const bufA = Buffer.from(a, 'utf8');
  const bufB = Buffer.from(b, 'utf8');
  if (bufA.length === 0 || bufB.length === 0 || bufA.length !== bufB.length) {
    const dummy = Buffer.alloc(32, 0);
    crypto.timingSafeEqual(dummy, dummy);
    return false;
  }
  return crypto.timingSafeEqual(bufA, bufB);
}

// Strict SSRF destination validation
export function validateHostSecurity(host) {
  if (!host || typeof host !== 'string') return false;
  const cleanHost = host.trim().toLowerCase().replace(/:\d+$/, '');

  const blockedPatterns = [
    'localhost',
    '127.0.0.1',
    '0.0.0.0',
    '::1',
    '169.254.169.254', // Cloud metadata
    'metadata.google.internal',
    '.internal',
    '.local'
  ];

  for (const pattern of blockedPatterns) {
    if (cleanHost === pattern || cleanHost.endsWith(pattern) || cleanHost.includes(pattern)) {
      return false;
    }
  }

  // IPv4 Private Range Check (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
  const ipv4Regex = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/;
  const match = cleanHost.match(ipv4Regex);
  if (match) {
    const [_, o1, o2] = match.map(Number);
    if (o1 === 10) return false;
    if (o1 === 172 && o2 >= 16 && o2 <= 31) return false;
    if (o1 === 192 && o2 === 168) return false;
    if (o1 === 127 || o1 === 0) return false;
  }

  return true;
}

// Verify Firebase ID Token Middleware
export async function verifyFirebaseAuth(req, res, next) {
  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({
      success: false,
      error: 'UNAUTHORIZED',
      message: 'Authentication required. Missing Bearer token.'
    });
  }

  const idToken = authHeader.split('Bearer ')[1].trim();
  const { auth, db } = getFirebaseAdmin();

  if (!auth) {
    return res.status(503).json({
      success: false,
      error: 'AUTH_SERVICE_UNAVAILABLE',
      message: 'Authentication service is uninitialized.'
    });
  }

  try {
    const decodedToken = await auth.verifyIdToken(idToken);
    req.user = decodedToken;
    req.uid = decodedToken.uid;

    // Check custom claims role
    let role = decodedToken.role || 'user';

    // Verify against Firestore users collection if role claim is not present
    if (!decodedToken.role && db) {
      try {
        const userDoc = await db.collection('users').doc(req.uid).get();
        if (userDoc.exists) {
          role = userDoc.data().role || role;
        }
      } catch (err) {
        console.warn('[Auth] Firestore role fallback warning:', err.message);
      }
    }

    req.userRole = role;
    req.isStaff = ['staff', 'employee', 'admin', 'superadmin'].includes(role);
    req.isAdmin = ['admin', 'superadmin'].includes(role);
    req.isSuperAdmin = role === 'superadmin';

    next();
  } catch (err) {
    return res.status(401).json({
      success: false,
      error: 'INVALID_TOKEN',
      message: 'Token verification failed: ' + err.message
    });
  }
}

// Require Roles Middleware
export function requireRoles(allowedRoles) {
  const rolesArray = Array.isArray(allowedRoles) ? allowedRoles : [allowedRoles];
  return (req, res, next) => {
    if (!req.user || !req.userRole) {
      return res.status(401).json({ success: false, error: 'UNAUTHORIZED', message: 'Identity not established.' });
    }
    if (!rolesArray.includes(req.userRole)) {
      return res.status(403).json({
        success: false,
        error: 'FORBIDDEN',
        message: `Access denied. Requires one of roles: [${rolesArray.join(', ')}]. Current role: ${req.userRole}`
      });
    }
    next();
  };
}
