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

const NUT_33 = Array.from({ length: 33 }, (_, i) => (i === 0 ? 150 : i < 4 ? 10 : -1));

function validConfig(uid, ts) {
  return {
    userId: uid,
    startDate: "2026-11-18",
    restSeconds: 180,
    themeMode: "system",
    selectedBodyMetric: "w",
    sex: "m",
    age: 25,
    heightCm: 175,
    weightOverride: 0,
    bodyFatOverride: 0,
    activityFactor: 1.55,
    goal: "bulk",
    targetBodyFat: 20,
    kcalOverride: 0,
    proteinOverride: 0,
    carbsOverride: 0,
    fatOverride: 0,
    alternatives: {},
    customPortions: {},
    splitDays: [],
    createdAt: ts,
    updatedAt: ts,
  };
}

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

test("Unauthenticated user cannot read or write any collection", async () => {
  const db = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(db.collection("users").doc(ALICE_UID).get());
  await assertFails(db.collection("users").doc(ALICE_UID).set(validConfig(ALICE_UID, ts)));
  await assertFails(db.collection("users").doc(ALICE_UID).collection("workoutSessions").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("nutritionDays").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("bodyRecords").get());
  await assertFails(db.collection("users").doc(ALICE_UID).collection("customFoods").get());
});

test("UserConfig: owner create, partial update, cross-user denial, shadow field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validConfig(ALICE_UID, created))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  // Partial update succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      restSeconds: 240,
      themeMode: "dark",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt mutation fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Shadow field injection fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user access fails
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      restSeconds: 120,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  // Listing all users is forbidden
  await assertFails(aliceDb.collection("users").get());
});

test("WorkoutSessions: owner CRUD, list, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const sessionRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("workoutSessions")
    .doc("2026-11-18_0");

  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      date: "2026-11-18",
      dayIndex: 0,
      note: "تمرين قوي",
      exercises: {
        "0": {
          name: "Incline Chest Press Machine",
          sets: [{ weight: "60", reps: "6" }],
        },
      },
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("workoutSessions").get()
  );

  await assertSucceeds(
    sessionRef.update({
      note: "ممتاز",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Future timestamp rejected
  await assertFails(
    sessionRef.update({
      note: "future",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user read/write rejected
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("workoutSessions").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("workoutSessions")
      .doc("2026-11-18_0")
      .delete()
  );
});

test("NutritionDays: owner CRUD, list, and validation bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  const dayRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("nutritionDays")
    .doc("2026-11-18");

  await assertSucceeds(
    dayRef.set({
      userId: ALICE_UID,
      date: "2026-11-18",
      waterMl: 500,
      items: [
        {
          id: "item_1",
          name: "فول مدمس",
          source: "e",
          grams: 150,
          mealIndex: 0,
          nutrients: NUT_33,
        },
      ],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    dayRef.update({
      waterMl: 750,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Out-of-bounds water rejected
  await assertFails(
    dayRef.update({
      waterMl: 99999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("nutritionDays").get()
  );
});

test("BodyRecords & CustomFoods: owner CRUD, optional field omission, and validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Body record with optional fields omitted + some provided
  const bodyRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bodyRecords")
    .doc("2026-11-18");

  await assertSucceeds(
    bodyRef.set({
      userId: ALICE_UID,
      date: "2026-11-18",
      weightKg: 80.5,
      smmKg: 38.2,
      pbfPercent: 18.4,
      segmentalLeanKg: { la: 3.5, ra: 3.6, tr: 28.1, ll: 9.8, rl: 9.9 },
      ranges: { w: [65.0, 82.0] },
      createdAt: created,
      updatedAt: created,
    })
  );

  // CustomFood with 33 nutrients (no id key inside body)
  const foodRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("customFoods")
    .doc("food_1");

  await assertSucceeds(
    foodRef.set({
      userId: ALICE_UID,
      name: "بروتين واي",
      source: "l",
      barcode: "6221234567890",
      refusePercent: 0,
      favorite: true,
      custom: true,
      nutrients: NUT_33,
      portions: [{ name: "سكوب", grams: 30 }],
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject wrong nutrient length
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customFoods")
      .doc("food_bad")
      .set({
        userId: ALICE_UID,
        name: "Bad",
        source: "c",
        barcode: "",
        refusePercent: 0,
        favorite: false,
        custom: true,
        nutrients: [100],
        portions: [],
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject explicit id key on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("customFoods")
      .doc("food_id_key")
      .set({
        id: "food_id_key",
        userId: ALICE_UID,
        name: "Extra id",
        source: "c",
        barcode: "",
        refusePercent: 0,
        favorite: false,
        custom: true,
        nutrients: NUT_33,
        portions: [],
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bodyRecords").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("customFoods").get()
  );
});
