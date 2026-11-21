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

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: cannot read or write sessions or messages", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .set({
        userId: ALICE_UID,
        title: "Unauthorized",
        mode: "GENERAL",
        lastMessagePreview: "",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Authenticated owner: valid ChatSession create, partial update, list, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("sess_1");

  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "HSC Physics Vectors",
      mode: "HSC_PHYSICS",
      lastMessagePreview: "Explain vector cross product",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );

  await assertSucceeds(
    sessionRef.update({
      title: "HSC Physics Vector Calculus",
      lastMessagePreview: "Here is the step-by-step derivation...",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt modification
  await assertFails(
    sessionRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject shadow field
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_shadow")
      .set({
        id: "sess_shadow",
        userId: ALICE_UID,
        title: "Bad Session",
        mode: "GENERAL",
        lastMessagePreview: "",
        createdAt: created,
        updatedAt: created,
      })
  );
});

test("Authenticated owner: valid ChatMessage create, update, and list succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 3000);

  const msgRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("sessions")
    .doc("sess_1")
    .collection("messages")
    .doc("msg_1");

  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      sessionId: "sess_1",
      role: "USER",
      content: "নিউটনের দ্বিতীয় সূত্র সহজভাবে বুঝিয়ে দাও।",
      mode: "HSC_PHYSICS",
      attachmentNames: ["notes.pdf"],
      errorFlag: false,
      createdAt: created,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_1")
      .collection("messages")
      .get()
  );

  await assertSucceeds(
    msgRef.update({
      content: "Updated content",
      errorFlag: false,
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice sessions or messages", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_alice")
      .set({
        userId: ALICE_UID,
        title: "Private Chemistry Notes",
        mode: "HSC_CHEMISTRY",
        lastMessagePreview: "Organic reaction mechanisms",
        createdAt: new Date(Date.now() - 5000),
        updatedAt: new Date(Date.now() - 5000),
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_alice")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("sessions").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_alice")
      .update({
        title: "Hijacked",
        updatedAt: new Date(Date.now() - 1000),
      })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("sessions")
      .doc("sess_bob_spoof")
      .set({
        userId: ALICE_UID,
        title: "Spoof",
        mode: "GENERAL",
        lastMessagePreview: "",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});
