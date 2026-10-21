// Firestore security rules tests (docs/PLAN.md section 4).
// Run with `npm test`: it starts the Firestore emulator under the demo project
// `demo-fridged`, so no real project is ever touched.

import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, test } from "node:test";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  orderBy,
  query,
  setDoc,
  updateDoc,
  where,
} from "firebase/firestore";

const DEVICE = "devices/fridge-sim";
const STATUS = `${DEVICE}/status/current`;
const SAMPLES = `${DEVICE}/samples`;
const SAMPLE = `${SAMPLES}/boot1-000000300000`;
const SETTINGS = `${DEVICE}/settings/current`;

const sample = {
  time: new Date("2026-10-20T10:00:00Z"),
  timeMs: "1792490400000",
  clockSynced: true,
  timeCorrected: false,
  bootId: "boot1",
  uptimeMs: "300000",
  temperature: 4.2,
  relay: "OFF",
  wanted: "OFF",
  reason: "inBand",
  mode: "NORMAL",
  overrunFault: false,
  targetMin: 3,
  targetMax: 5,
};

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "demo-fridged",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, STATUS), sample);
    await setDoc(doc(db, SAMPLE), sample);
    await setDoc(doc(db, SETTINGS), { targetMin: 3, targetMax: 5 });
  });
});

const owner = () => env.authenticatedContext("owner-uid", { owner: true }).firestore();
const notOwner = () => env.authenticatedContext("other-uid").firestore();
const stringClaim = () => env.authenticatedContext("string-uid", { owner: "true" }).firestore();
const anonymous = () => env.unauthenticatedContext().firestore();

const recentSamples = (db) =>
  query(collection(db, SAMPLES), where("time", ">", new Date("2026-10-19T00:00:00Z")), orderBy("time"));

describe("owner", () => {
  test("reads status", () => assertSucceeds(getDoc(doc(owner(), STATUS))));
  test("reads a sample", () => assertSucceeds(getDoc(doc(owner(), SAMPLE))));
  test("queries samples by time", () => assertSucceeds(getDocs(recentSamples(owner()))));
  test("reads settings", () => assertSucceeds(getDoc(doc(owner(), SETTINGS))));
  test("reads a missing settings document", async () => {
    await env.withSecurityRulesDisabled((context) => deleteDoc(doc(context.firestore(), SETTINGS)));
    await assertSucceeds(getDoc(doc(owner(), SETTINGS)));
  });
  test("creates settings", async () => {
    await env.withSecurityRulesDisabled((context) => deleteDoc(doc(context.firestore(), SETTINGS)));
    await assertSucceeds(setDoc(doc(owner(), SETTINGS), { targetMin: 2, targetMax: 6 }));
  });
  test("replaces settings", () =>
    assertSucceeds(setDoc(doc(owner(), SETTINGS), { targetMin: 2.5, targetMax: 4.5 })));
  test("updates one bound", () => assertSucceeds(updateDoc(doc(owner(), SETTINGS), { targetMax: 6 })));
  test("deletes settings (restore defaults)", () => assertSucceeds(deleteDoc(doc(owner(), SETTINGS))));
});

describe("valid bands at the limits", () => {
  for (const [name, band] of [
    ["whole range", { targetMin: 0, targetMax: 25 }],
    ["exactly 0.5 apart", { targetMin: 3, targetMax: 3.5 }],
    ["integers", { targetMin: 2, targetMax: 6 }],
  ]) {
    test(name, () => assertSucceeds(setDoc(doc(owner(), SETTINGS), band)));
  }
});

describe("invalid bands are rejected", () => {
  for (const [name, band] of [
    ["min below 0", { targetMin: -0.5, targetMax: 5 }],
    ["max above 25", { targetMin: 3, targetMax: 25.5 }],
    ["too narrow", { targetMin: 3, targetMax: 3.4 }],
    ["min above max", { targetMin: 6, targetMax: 3 }],
    ["missing targetMax", { targetMin: 3 }],
    ["missing targetMin", { targetMax: 5 }],
    ["extra field", { targetMin: 3, targetMax: 5, note: "x" }],
    ["string value", { targetMin: "3", targetMax: 5 }],
    ["boolean value", { targetMin: 3, targetMax: true }],
    ["null value", { targetMin: null, targetMax: 5 }],
    ["NaN", { targetMin: NaN, targetMax: 5 }],
    ["Infinity", { targetMin: 3, targetMax: Infinity }],
    ["empty document", {}],
  ]) {
    test(name, () => assertFails(setDoc(doc(owner(), SETTINGS), band)));
  }

  test("update that makes the band too narrow", () =>
    assertFails(updateDoc(doc(owner(), SETTINGS), { targetMax: 3.2 })));
  test("update that adds a field", () => assertFails(updateDoc(doc(owner(), SETTINGS), { note: "x" })));
});

describe("nobody writes status or samples", () => {
  for (const [who, db] of [
    ["owner", owner],
    ["user without claim", notOwner],
    ["anonymous", anonymous],
  ]) {
    test(`${who} cannot write status`, () => assertFails(setDoc(doc(db(), STATUS), sample)));
    test(`${who} cannot delete status`, () => assertFails(deleteDoc(doc(db(), STATUS))));
    test(`${who} cannot write a sample`, () => assertFails(setDoc(doc(db(), SAMPLE), sample)));
    test(`${who} cannot add a sample`, () =>
      assertFails(setDoc(doc(db(), `${SAMPLES}/boot1-000000600000`), sample)));
    test(`${who} cannot delete a sample`, () => assertFails(deleteDoc(doc(db(), SAMPLE))));
  }
});

describe("users without the owner claim", () => {
  for (const [who, db] of [
    ["user without claim", notOwner],
    ["claim as string", stringClaim],
    ["anonymous", anonymous],
  ]) {
    test(`${who} cannot read status`, () => assertFails(getDoc(doc(db(), STATUS))));
    test(`${who} cannot read a sample`, () => assertFails(getDoc(doc(db(), SAMPLE))));
    test(`${who} cannot query samples`, () => assertFails(getDocs(recentSamples(db()))));
    test(`${who} cannot read settings`, () => assertFails(getDoc(doc(db(), SETTINGS))));
    test(`${who} cannot write settings`, () =>
      assertFails(setDoc(doc(db(), SETTINGS), { targetMin: 2, targetMax: 6 })));
    test(`${who} cannot delete settings`, () => assertFails(deleteDoc(doc(db(), SETTINGS))));
  }
});

describe("everything else is denied, even to the owner", () => {
  test("device document", () => assertFails(getDoc(doc(owner(), DEVICE))));
  test("devices collection", () => assertFails(getDocs(collection(owner(), "devices"))));
  test("other status document", () => assertFails(getDoc(doc(owner(), `${DEVICE}/status/other`))));
  test("other settings document", () =>
    assertFails(setDoc(doc(owner(), `${DEVICE}/settings/other`), { targetMin: 2, targetMax: 6 })));
  test("unknown collection", () => assertFails(getDoc(doc(owner(), "other/doc"))));
  test("unknown device subcollection", () =>
    assertFails(setDoc(doc(owner(), `${DEVICE}/commands/next`), { relay: "ON" })));
});
