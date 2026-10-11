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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTimestamp = () => new Date(Date.now() - 1000);

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

test("1. Unauthenticated user cannot read or create tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Draft report",
      blockerType: "perfectionism",
      dreadLevel: 8,
      microSteps: ["Open doc"],
      completedStepsCount: 0,
      status: "active",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("2. Authenticated owner can create and query valid task, thought log, focus session, and worry vault item", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "File quarterly taxes",
      blockerType: "overwhelmed",
      dreadLevel: 7,
      microSteps: ["Find folder", "Log into portal"],
      completedStepsCount: 0,
      status: "active",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").where("userId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("thought_logs").doc("log_1").set({
      userId: ALICE_UID,
      anxiousThought: "If I start now it won't be good enough",
      distortion: "All-or-Nothing Perfectionism",
      reframeText: "An imperfect first draft is 100% better than a blank page.",
      tinyAction: "Write 3 bullet points in 2 minutes",
      anxietyBefore: 80,
      anxietyAfter: 35,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("focus_sessions").doc("session_1").set({
      userId: ALICE_UID,
      modeName: "5-Min Icebreaker",
      durationMinutes: 5,
      taskTitle: "File quarterly taxes",
      reliefRating: 8,
      createdAt: nowTimestamp(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("worry_vault").doc("worry_1").set({
      userId: ALICE_UID,
      worryText: "What if I forget a point during the meeting?",
      scheduledWindow: "Today at 5:30 PM (10-Min Worry Window)",
      isResolved: false,
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );
});

test("3. Cross-user isolation: Bob cannot read or modify Alice's task", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      title: "Private task",
      blockerType: "ambiguous",
      dreadLevel: 6,
      microSteps: ["Step 1"],
      completedStepsCount: 0,
      status: "active",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("4. Shadow update (ghost key) and terminal state locking on tasks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1");

  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      title: "Clean desk",
      blockerType: "low_energy",
      dreadLevel: 4,
      microSteps: ["Put cups in sink"],
      completedStepsCount: 0,
      status: "active",
      createdAt: nowTimestamp(),
      updatedAt: nowTimestamp(),
    })
  );

  // Reject ghost field
  await assertFails(
    taskRef.update({
      completedStepsCount: 1,
      isVerified: true,
      updatedAt: nowTimestamp(),
    })
  );

  // Valid completion transition
  await assertSucceeds(
    taskRef.update({
      completedStepsCount: 1,
      status: "completed",
      updatedAt: nowTimestamp(),
    })
  );

  // Terminal state lock: once completed, cannot update again
  await assertFails(
    taskRef.update({
      status: "active",
      updatedAt: nowTimestamp(),
    })
  );
});
