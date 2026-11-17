const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const serverTimestamp = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_uid_1";
const OWNER_EMAIL = "owner@adalaundry.com";
const CASHIER_UID = "cashier_uid_1";
const CASHIER_EMAIL = "kasir@adalaundry.com";
const OUTSIDER_UID = "outsider_uid_9";
const OUTSIDER_EMAIL = "outsider@evil.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function defaultPerms() {
  return {
    canCreateOrder: true,
    canUpdateStatus: true,
    canRecordPayment: true,
    canManageCustomers: true,
    canSendWhatsApp: true,
    canCancelOrder: true,
    canViewReports: true,
  };
}

async function seedShopAndCashier(active = true) {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = serverTimestamp();
    await db.collection("shops").doc(OWNER_UID).set({
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      businessName: "ADA LAUNDRY",
      address: "Jl. Merdeka No. 10",
      phone: "081234567890",
      operatingHours: "07:00 - 21:00",
      notaPrefix: "ADA",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffDirectory").doc("invite_kasir_1").set({
      shopId: OWNER_UID,
      ownerUid: OWNER_UID,
      name: "Budi Kasir",
      email: CASHIER_EMAIL,
      staffUid: CASHIER_UID,
      role: "KASIR",
      active,
      permissions: defaultPerms(),
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("shops").doc(OWNER_UID).collection("staffMembers").doc(CASHIER_UID).set({
      shopId: OWNER_UID,
      inviteId: "invite_kasir_1",
      name: "Budi Kasir",
      email: CASHIER_EMAIL,
      role: "KASIR",
      active,
      permissions: defaultPerms(),
      createdAt: now,
      updatedAt: now,
    });
  });
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
  await seedShopAndCashier(true);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("shops").doc(OWNER_UID).collection("orders").get());
  await assertFails(unauthDb.collection("staffDirectory").get());
});

test("Owner can create shop, service, and expense; Outsider is blocked", async () => {
  const ownerDb = testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID, {
    email: OUTSIDER_EMAIL,
    email_verified: true,
  }).firestore();

  const now = serverTimestamp();
  await assertSucceeds(
    ownerDb.collection("shops").doc(OWNER_UID).set({
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      businessName: "ADA LAUNDRY",
      address: "Jl. Mawar 1",
      phone: "08123456789",
      operatingHours: "08:00 - 20:00",
      notaPrefix: "ADA",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    ownerDb.collection("shops").doc(OWNER_UID).collection("services").doc("svc_kiloan").set({
      shopId: OWNER_UID,
      name: "Cuci Komplit Reguler",
      category: "KILOAN",
      unitLabel: "kg",
      price: 7000,
      isExtraFee: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    ownerDb.collection("shops").doc(OWNER_UID).collection("expenses").doc("exp_1").set({
      shopId: OWNER_UID,
      category: "Deterjen",
      amount: 50000,
      dateMillis: Date.now(),
      notes: "Beli deterjen 5L",
      createdByUid: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(outsiderDb.collection("shops").doc(OWNER_UID).get());
  await assertFails(outsiderDb.collection("shops").doc(OWNER_UID).collection("services").get());
  await assertFails(outsiderDb.collection("shops").doc(OWNER_UID).collection("expenses").get());
});

test("Cashier can bind invite only with verified email, cannot self-escalate permissions", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = serverTimestamp();
    await db.collection("shops").doc(OWNER_UID).set({
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      businessName: "ADA LAUNDRY",
      address: "Jl. Merdeka",
      phone: "08123456789",
      operatingHours: "07:00 - 21:00",
      notaPrefix: "ADA",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffDirectory").doc("inv_1").set({
      shopId: OWNER_UID,
      ownerUid: OWNER_UID,
      name: "Siti Kasir",
      email: CASHIER_EMAIL,
      staffUid: "",
      role: "KASIR",
      active: true,
      permissions: { ...defaultPerms(), canCancelOrder: false },
      createdAt: now,
      updatedAt: now,
    });
  });

  // Unverified email spoof must fail
  const unverifiedCashier = testEnv.authenticatedContext(CASHIER_UID, {
    email: CASHIER_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    unverifiedCashier.collection("staffDirectory").where("email", "==", CASHIER_EMAIL).get()
  );

  // Verified email can read invite and create matching staffMember doc
  const cashierDb = testEnv.authenticatedContext(CASHIER_UID, {
    email: CASHIER_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    cashierDb.collection("staffDirectory").where("email", "==", CASHIER_EMAIL).get()
  );

  const now = serverTimestamp();
  // Escalating canCancelOrder to true must fail
  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).collection("staffMembers").doc(CASHIER_UID).set({
      shopId: OWNER_UID,
      inviteId: "inv_1",
      name: "Siti Kasir",
      email: CASHIER_EMAIL,
      role: "KASIR",
      active: true,
      permissions: { ...defaultPerms(), canCancelOrder: true },
      createdAt: now,
      updatedAt: now,
    })
  );

  // Matching permissions succeed
  await assertSucceeds(
    cashierDb.collection("shops").doc(OWNER_UID).collection("staffMembers").doc(CASHIER_UID).set({
      shopId: OWNER_UID,
      inviteId: "inv_1",
      name: "Siti Kasir",
      email: CASHIER_EMAIL,
      role: "KASIR",
      active: true,
      permissions: { ...defaultPerms(), canCancelOrder: false },
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Active Cashier RBAC: allowed on orders/customers, denied on services/expenses/settings", async () => {
  await seedShopAndCashier(true);
  const cashierDb = testEnv.authenticatedContext(CASHIER_UID, {
    email: CASHIER_EMAIL,
    email_verified: true,
  }).firestore();
  const now = serverTimestamp();

  // Read shop & services OK
  await assertSucceeds(cashierDb.collection("shops").doc(OWNER_UID).get());
  await assertSucceeds(cashierDb.collection("shops").doc(OWNER_UID).collection("services").get());

  // Modify services / prices DENIED
  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).collection("services").doc("svc_hack").set({
      shopId: OWNER_UID,
      name: "Gratis",
      category: "KILOAN",
      unitLabel: "kg",
      price: 0,
      isExtraFee: false,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Read/write expenses DENIED
  await assertFails(cashierDb.collection("shops").doc(OWNER_UID).collection("expenses").get());
  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).collection("expenses").doc("exp_hack").set({
      shopId: OWNER_UID,
      category: "Lainnya",
      amount: 1000,
      dateMillis: Date.now(),
      createdByUid: CASHIER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Modify shop settings DENIED
  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).update({
      businessName: "Hacked Laundry",
      updatedAt: now,
    })
  );

  // Create valid order OK
  await assertSucceeds(
    cashierDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_1").set({
      shopId: OWNER_UID,
      notaNumber: "ADA-20261116-0001",
      customerId: "cust_1",
      customerName: "Andi",
      customerPhone: "628123456789",
      items: [{ serviceId: "svc_1", serviceName: "Kiloan", category: "KILOAN", unitLabel: "kg", quantity: 4, unitPrice: 7000, lineTotal: 28000 }],
      subtotal: 28000,
      discount: 0,
      extraFee: 0,
      totalAmount: 28000,
      paidAmount: 10000,
      remainingAmount: 18000,
      paymentStatus: "DP",
      paymentMethod: "CASH",
      orderStatus: "RECEIVED",
      estimatedFinishAtMillis: Date.now() + 86400000,
      transactionAtMillis: Date.now(),
      statusHistory: [{ status: "RECEIVED", changedAtMillis: Date.now(), changedByUid: CASHIER_UID, changedByName: "Budi Kasir" }],
      paymentHistory: [{ id: "pay_1", amount: 10000, receivedAmount: 10000, changeAmount: 0, method: "CASH", type: "DP", paidAtMillis: Date.now(), recordedByUid: CASHIER_UID, recordedByName: "Budi Kasir" }],
      whatsappLogs: [],
      cashierUid: CASHIER_UID,
      cashierName: "Budi Kasir",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Delete order DENIED
  await assertFails(cashierDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_1").delete());

  // Shadow update with ghost field DENIED
  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_1").update({
      ghostField: "hack",
      updatedAt: now,
    })
  );

  // Update status to COMPLETED, then try changing back to RECEIVED -> DENIED!
  await assertSucceeds(
    cashierDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_1").update({
      orderStatus: "COMPLETED",
      statusHistory: [
        { status: "RECEIVED", changedAtMillis: Date.now(), changedByUid: CASHIER_UID, changedByName: "Budi Kasir" },
        { status: "COMPLETED", changedAtMillis: Date.now(), changedByUid: CASHIER_UID, changedByName: "Budi Kasir" }
      ],
      updatedAt: now,
    })
  );

  await assertFails(
    cashierDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_1").update({
      orderStatus: "RECEIVED",
      statusHistory: [],
      updatedAt: now,
    })
  );
});

test("Deactivated Cashier loses all access immediately", async () => {
  await seedShopAndCashier(false);
  const cashierDb = testEnv.authenticatedContext(CASHIER_UID, {
    email: CASHIER_EMAIL,
    email_verified: true,
  }).firestore();

  await assertFails(cashierDb.collection("shops").doc(OWNER_UID).get());
  await assertFails(cashierDb.collection("shops").doc(OWNER_UID).collection("orders").get());
  await assertFails(cashierDb.collection("shops").doc(OWNER_UID).collection("customers").get());
});

test("Remote OWNER invite binding grants full shop access and validates PPN/customization fields", async () => {
  const REMOTE_OWNER_UID = "remote_owner_uid_2";
  const REMOTE_OWNER_EMAIL = "owner2@adalaundry.com";
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = serverTimestamp();
    await db.collection("shops").doc(OWNER_UID).set({
      ownerUid: OWNER_UID,
      ownerEmail: OWNER_EMAIL,
      businessName: "ADA LAUNDRY",
      address: "Jl. Merdeka No. 10",
      phone: "081234567890",
      operatingHours: "07:00 - 21:00",
      notaPrefix: "ADA",
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("staffDirectory").doc("inv_owner_2").set({
      shopId: OWNER_UID,
      ownerUid: OWNER_UID,
      name: "Co-Owner Sari",
      email: REMOTE_OWNER_EMAIL,
      staffUid: "",
      role: "OWNER",
      active: true,
      permissions: defaultPerms(),
      createdAt: now,
      updatedAt: now,
    });
  });

  const remoteOwnerDb = testEnv.authenticatedContext(REMOTE_OWNER_UID, {
    email: REMOTE_OWNER_EMAIL,
    email_verified: true,
  }).firestore();
  const now = serverTimestamp();

  await assertSucceeds(
    remoteOwnerDb.collection("shops").doc(OWNER_UID).collection("staffMembers").doc(REMOTE_OWNER_UID).set({
      shopId: OWNER_UID,
      inviteId: "inv_owner_2",
      name: "Co-Owner Sari",
      email: REMOTE_OWNER_EMAIL,
      role: "OWNER",
      active: true,
      permissions: defaultPerms(),
      createdAt: now,
      updatedAt: now,
    })
  );

  // Remote owner can update ShopProfile customization, PPN & whatsappTemplate fields
  await assertSucceeds(
    remoteOwnerDb.collection("shops").doc(OWNER_UID).update({
      brandColorKey: "TEAL",
      businessIconKey: "WASHER",
      taxEnabled: true,
      taxAutoApply: true,
      taxRatePercent: 11,
      paperSize: "MM_80",
      receiptShowCustomer: true,
      receiptFooterMessage: "Terima kasih atas kepercayaan Anda.",
      whatsappTemplate: "Halo {nama_pelanggan}, nota {no_nota} siap diambil di {nama_usaha}.",
      ownerPin: "1234",
      updatedAt: now,
    })
  );

  // Remote owner can also read expenses & create an order with PPN fields
  await assertSucceeds(remoteOwnerDb.collection("shops").doc(OWNER_UID).collection("expenses").get());
  await assertSucceeds(
    remoteOwnerDb.collection("shops").doc(OWNER_UID).collection("orders").doc("ord_ppn").set({
      shopId: OWNER_UID,
      notaNumber: "ADA-20261117-0001",
      customerId: "cust_1",
      customerName: "Dewi",
      customerPhone: "628111222333",
      items: [{ serviceId: "svc_1", serviceName: "Kiloan", category: "KILOAN", unitLabel: "kg", quantity: 2, unitPrice: 10000, lineTotal: 20000 }],
      subtotal: 20000,
      discount: 0,
      extraFee: 0,
      taxRatePercent: 11,
      taxAmount: 2200,
      totalAmount: 22200,
      paidAmount: 22200,
      remainingAmount: 0,
      paymentStatus: "PAID",
      paymentMethod: "QRIS",
      orderStatus: "RECEIVED",
      estimatedFinishAtMillis: Date.now() + 86400000,
      transactionAtMillis: Date.now(),
      statusHistory: [{ status: "RECEIVED", changedAtMillis: Date.now(), changedByUid: REMOTE_OWNER_UID, changedByName: "Co-Owner Sari" }],
      paymentHistory: [{ id: "pay_ppn", amount: 22200, receivedAmount: 22200, changeAmount: 0, method: "QRIS", type: "FULL", paidAtMillis: Date.now(), recordedByUid: REMOTE_OWNER_UID, recordedByName: "Co-Owner Sari" }],
      whatsappLogs: [],
      cashierUid: REMOTE_OWNER_UID,
      cashierName: "Co-Owner Sari",
      createdAt: now,
      updatedAt: now,
    })
  );
});
