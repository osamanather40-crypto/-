const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";

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

test("Owner can configure WhatsApp witness and auto-punishment state", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "أسامة",
      sovereigntyScore: 500,
      zeroBreachStreak: 0,
      timeDebtMinutes: 0,
      completedCount: 0,
      breachCount: 0,
      restrictedMode: false,
      autoWhatsApp: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      witnessName: "أحمد الرقيب",
      witnessPhone: "+962791234567",
      autoWhatsApp: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Active task supports automatic timeout punishment transition and photo completion", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_punish_1").set({
      userId: ALICE_UID,
      title: "إنجاز التقرير",
      description: "تصوير التقرير",
      orderIndex: 0,
      durationMinutes: 30,
      proofType: "PHOTO_PROOF",
      status: "ACTIVE",
      contractDate: "2026-11-25",
      targetTimeLabel: "الساعة 12:00",
      targetEffort: 1,
      deadlineEpochMs: Date.now() - 1000,
      debtPenaltyMinutes: 0,
      recoveryTask: false,
      penaltyApplied: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Automatic timeout punishment update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_punish_1").update({
      penaltyApplied: true,
      debtPenaltyMinutes: 30,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Subsequent photo completion still succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_punish_1").update({
      status: "COMPLETED",
      proofSummary: "تم التصوير بعد العقاب",
      proofPhotoUri: "/data/user/0/com.aistudio.qayd.mzkvtr/files/proof_photos/qayd_1.jpg",
      debtPenaltyMinutes: 35,
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
