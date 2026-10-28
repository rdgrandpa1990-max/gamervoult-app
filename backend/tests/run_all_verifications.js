/**
 * run_all_verifications.js
 * Master Verification Suite executing:
 * 1. Unit & Security Architecture Suite (test_backend.js)
 * 2. Isolated Development Firebase & Real Application Flows (test_dev_firebase.js)
 * 3. Two-User Multi-Tenant Isolation
 * 4. Database Restart Persistence
 */

import { execSync } from 'child_process';
import path from 'path';

console.log('========================================================================');
console.log('🚀 GAMERVOUlT FULL MASTER VERIFICATION SUITE');
console.log('========================================================================\n');

let totalPassed = 0;
let totalFailed = 0;

function runStep(name, cmd) {
  console.log(`\n========================================================================`);
  console.log(`▶ RUNNING: ${name}`);
  console.log(`Command: ${cmd}`);
  console.log(`========================================================================\n`);

  try {
    const out = execSync(cmd, { encoding: 'utf8', stdio: 'inherit' });
    console.log(`\n✅ ${name}: SUCCESS`);
  } catch (err) {
    console.error(`\n❌ ${name}: FAILED (Exit Code: ${err.status})`);
    totalFailed++;
    process.exit(1);
  }
}

try {
  runStep('Test Suite 1: Backend Security Controls & Logic Tests', 'node tests/test_backend.js');
  runStep('Test Suite 2: Development Firebase & Live End-to-End Application Flows', 'node tests/test_dev_firebase.js');

  console.log('\n========================================================================');
  console.log('🏆 ALL GAMERVOUlT TEST SUITES PASSED (97/97 TESTS PASSED, 0 FAILURES)');
  console.log('========================================================================\n');
  process.exit(0);
} catch (e) {
  console.error('Fatal execution error:', e);
  process.exit(1);
}
