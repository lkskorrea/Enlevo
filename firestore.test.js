const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const FAMILY_ID = "familia_principal";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("goals").where("familyId", "==", FAMILY_ID).get());
  await assertFails(unauthDb.collection("messages").where("familyId", "==", FAMILY_ID).get());
  await assertFails(unauthDb.collection("profiles").doc(ALICE_UID).get());
});

test("Authenticated user: can create and read own profile, cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("profiles").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Mãe Alice",
      avatarEmoji: "👩",
      familyId: FAMILY_ID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("profiles").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("profiles").doc(ALICE_UID).get());
});

test("Shadow update test: rejects unexpected extra fields on create and update", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("goals").doc("goal_1").set({
      familyId: FAMILY_ID,
      name: "Férias",
      iconEmoji: "🏖️",
      targetAmount: 5000,
      savedAmount: 1000,
      authorUid: ALICE_UID,
      authorName: "Alice",
      isAdminHack: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated family members: can collaborate on shared family goals and messages with aligned query", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("goals").doc("goal_ferias").set({
      familyId: FAMILY_ID,
      name: "Viagem em Família",
      iconEmoji: "✈️",
      targetAmount: 4000,
      savedAmount: 1200,
      authorUid: ALICE_UID,
      authorName: "Alice",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Query with familyId filter succeeds for Bob
  await assertSucceeds(
    bobDb.collection("goals").where("familyId", "==", FAMILY_ID).get()
  );

  // Unfiltered query without familyId fails (rules are not filters)
  await assertFails(
    bobDb.collection("goals").get()
  );

  // Bob can update savedAmount on the shared family goal while keeping authorUid immutable
  await assertSucceeds(
    bobDb.collection("goals").doc("goal_ferias").update({
      savedAmount: 1800,
      updatedAt: new Date(),
    })
  );

  // Bob cannot spoof authorUid on update
  await assertFails(
    bobDb.collection("goals").doc("goal_ferias").update({
      authorUid: BOB_UID,
      updatedAt: new Date(),
    })
  );
});
