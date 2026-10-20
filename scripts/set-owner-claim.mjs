// Grants the `owner` custom claim that the Firestore security rules require.
//
// Usage (service account JSON is local only, never in the repo):
//   cd scripts && npm install
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
//     node set-owner-claim.mjs you@example.com
//
// The user must sign out and in again (or wait up to an hour) for the new
// claim to reach the ID token on the phone.

import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

const email = process.argv[2];
if (!email) {
  console.error("Usage: node set-owner-claim.mjs <email>");
  process.exit(2);
}
if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error("GOOGLE_APPLICATION_CREDENTIALS must point to a service account JSON file.");
  process.exit(2);
}

initializeApp({ credential: applicationDefault() });
const auth = getAuth();

const user = await auth.getUserByEmail(email);
await auth.setCustomUserClaims(user.uid, { ...user.customClaims, owner: true });
const updated = await auth.getUser(user.uid);
console.log(`Custom claims for ${email}:`, updated.customClaims);
