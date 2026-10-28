# Gamervoult — Official Android Native Application & Vercel Backend Service

**Gamervoult** is the premier verified escrow digital marketplace for competitive gamers. This repository houses both the high-performance native Android application (built with Kotlin and Jetpack Compose) and the dedicated serverless backend (built for Vercel).

---

## 1. Project Architecture

```
gamervoult-app/
├── backend/                  # Dedicated Vercel Serverless Backend API
│   ├── api/index.js          # Express Serverless Entrypoint (Vercel Handler)
│   ├── vercel.json           # Vercel Routing & Security Headers Configuration
│   ├── package.json          # Node.js ESM Dependencies & Scripts
│   ├── .env.example          # Safe Environment Variable Template (No secrets)
│   ├── src/
│   │   ├── firebase.js       # Firebase Admin SDK Initialization
│   │   ├── authMiddleware.js # Token & Role Verification, SSRF Host Filter
│   │   └── routes/
│   │       └── appRouter.js  # Core REST API (Store, Orders, Webhook, Escrow)
│   └── tests/
│       └── test_backend.js   # Automated Security & Invariant Regression Suite
│
└── android/                  # Native Android Client Application
    ├── app/
    │   ├── src/main/java/com/gamervoult/app/
    │   │   ├── MainActivity.kt        # Root Jetpack Compose Scaffold
    │   │   ├── data/
    │   │   │   ├── Models.kt          # Strong typed Domain Models
    │   │   │   └── GamervoultRepository.kt # StateFlow Repositories & API Client
    │   │   └── ui/
    │   │       ├── navigation/        # Bottom Navigation (5 Core Tabs)
    │   │       ├── screens/
    │   │       │   ├── HomeScreen.kt    # Public Storefront, Search, Filter & Deals
    │   │       │   ├── ChatScreen.kt    # Authenticated 24/7 Escrow Support Desk
    │   │       │   ├── ExploreScreen.kt # Interactive Stories & Security Policies
    │   │       │   ├── OrdersScreen.kt  # Orders & Secure Credentials Delivery
    │   │       │   └── ProfileScreen.kt # User Profile, VIP Pass & Version Check
    │   │       └── theme/             # Dark Gaming Theme (Material 3)
    │   └── build.gradle.kts           # Android App Module Gradle Build Script
    ├── build.gradle.kts               # Top-Level Root Gradle Build Script
    └── settings.gradle.kts            # Project Module Declarations
```

---

## 2. Core Security & Architectural Invariants

1. **Fail-Closed Payment Webhook:**
   - Server-to-server gateway callback (`/api/webhooks/payment`) strictly rejects requests if the server secret is unconfigured (HTTP 503).
   - Validates incoming secrets in constant time (`timingSafeEqualStr`) with unequal-length protection.
   - Enforces authoritative order price, currency, and mandatory payment evidence (non-empty UTR $\ge 4$ characters).
   - Prevents cross-order UTR reuse with automated replay queries.
   - Locks terminal `PAID` states to prevent unauthorized status downgrades.

2. **Authoritative Credential Vault:**
   - Account passwords and 2-step backup codes are stored in private server-side collections (`ff_store_secrets`).
   - Released to the buyer via `/api/v1/orders/:orderCode/credentials` **only after payment is verified as PAID** and ownership is verified.

3. **Strict App Release & Update Integrity (Phase 8 - CFG-03):**
   - The version-check endpoint (`/api/v1/app/version-check`) reports `isArtifactAvailable: false` if download URLs are non-HTTPS, SHA-256 digests are malformed, or file sizes indicate HTML error pages.

4. **SSRF Host Defense:**
   - Outgoing network requests filter out private, internal, and cloud metadata destinations (`127.0.0.1`, `169.254.169.254`, `10.0.0.0/8`, etc.).

---

## 3. Local Development & Testing

### 3.1 Backend
```bash
cd backend
npm install
npm test      # Executes 39 automated security regression checks
npm start     # Starts local server on http://localhost:5000
```

### 3.2 Android Application
```bash
cd android
./gradlew assembleDebug      # Compiles the debug APK (outputs to app/build/outputs/apk/debug/)
./gradlew test               # Executes local unit tests
```

---

## 4. Vercel Deployment

1. Import this repository into Vercel and set the **Root Directory** to `backend`.
2. Configure the required environment variables in the Vercel Project Settings according to `backend/.env.example`.
3. Preview deployments run automatically on pull requests.
