const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");
const crypto = require("crypto");

admin.initializeApp();
const db = admin.firestore();

/**
 * Helper to get YooKassa credentials from environment or functions.config()
 */
function getYooKassaCredentials() {
  const shopId =
    process.env.YOOKASSA_SHOP_ID ||
    (functions.config().yookassa && functions.config().yookassa.shop_id);
  const secretKey =
    process.env.YOOKASSA_SECRET_KEY ||
    (functions.config().yookassa && functions.config().yookassa.secret_key);

  if (!shopId || !secretKey) {
    console.warn("YooKassa credentials not fully configured in environment/config.");
  }
  return { shopId, secretKey };
}

/**
 * 1. createPayment (HTTPS Callable)
 * Creates a payment redirect URL in YooKassa
 */
exports.createPayment = onCall(async (request) => {
  const data = request.data || {};
  const userId = data.userId || (request.auth && request.auth.uid);
  const planType = data.planType || "monthly";

  console.log(`[createPayment] Request received for userId: ${userId}, planType: ${planType}`);

  if (!userId) {
    throw new HttpsError("invalid-argument", "Missing required field: userId");
  }

  const isYearly = planType === "yearly";
  const amountValue = isYearly ? "999.00" : "150.00";
  const description = isYearly ? "GameLingo Pro Yearly" : "GameLingo Pro Monthly";

  const { shopId, secretKey } = getYooKassaCredentials();
  if (!shopId || !secretKey) {
    console.error("[createPayment] YooKassa shop_id or secret_key is missing");
    throw new HttpsError(
      "failed-precondition",
      "YooKassa credentials are not configured on server. Please set yookassa.shop_id and yookassa.secret_key"
    );
  }

  const idempotenceKey = crypto.randomUUID();
  const basicAuth = Buffer.from(`${shopId}:${secretKey}`).toString("base64");

  const requestBody = {
    amount: {
      value: amountValue,
      currency: "RUB"
    },
    confirmation: {
      type: "redirect",
      return_url: "https://gamelingo.netlify.app"
    },
    capture: true,
    description: description,
    metadata: {
      userId: userId,
      planType: planType
    }
  };

  try {
    console.log(`[createPayment] Sending request to YooKassa API for user ${userId}...`);
    const response = await axios.post("https://api.yookassa.ru/v3/payments", requestBody, {
      headers: {
        "Content-Type": "application/json",
        "Idempotence-Key": idempotenceKey,
        "Authorization": `Basic ${basicAuth}`
      }
    });

    const paymentData = response.data;
    console.log(`[createPayment] Payment created successfully: id=${paymentData.id}, status=${paymentData.status}`);

    const confirmationUrl = paymentData.confirmation && paymentData.confirmation.confirmation_url;
    if (!confirmationUrl) {
      console.error("[createPayment] Confirmation URL missing in YooKassa response", paymentData);
      throw new HttpsError("internal", "No confirmation URL returned from YooKassa");
    }

    return {
      confirmation_url: confirmationUrl,
      payment_id: paymentData.id
    };
  } catch (error) {
    const errorDetails = error.response ? JSON.stringify(error.response.data) : error.message;
    console.error(`[createPayment] Error creating payment: ${errorDetails}`);
    throw new HttpsError("internal", `YooKassa error: ${errorDetails}`);
  }
});

/**
 * 2. yookassaWebhook (HTTPS onRequest)
 * Webhook handler for YooKassa payment events
 */
exports.yookassaWebhook = onRequest(async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).send("Method Not Allowed");
  }

  const event = req.body;
  console.log(`[yookassaWebhook] Received event: ${event?.event}, id: ${event?.object?.id}`);

  if (event?.event !== "payment.succeeded") {
    console.log(`[yookassaWebhook] Skipping unhandled event type: ${event?.event}`);
    return res.status(200).send("Ignored event type");
  }

  const payment = event.object;
  const metadata = payment?.metadata || {};
  const userId = metadata.userId;
  const planType = metadata.planType || "monthly";

  if (!userId) {
    console.warn(`[yookassaWebhook] Payment ${payment?.id} succeeded but missing metadata.userId`, metadata);
    return res.status(200).send("No userId in metadata");
  }

  const now = Date.now();
  const durationMs = planType === "yearly"
    ? 365 * 24 * 60 * 60 * 1000
    : 30 * 24 * 60 * 60 * 1000;
  const premiumExpiry = now + durationMs;

  console.log(`[yookassaWebhook] Granting Pro for userId: ${userId}, planType: ${planType}, expiry: ${new Date(premiumExpiry).toISOString()}`);

  try {
    const userRef = db.collection("users").document(userId);
    await userRef.set(
      {
        isPremium: true,
        premiumExpiry: premiumExpiry,
        planType: planType,
        paymentId: payment.id,
        lastPaymentDate: admin.firestore.FieldValue.serverTimestamp()
      },
      { merge: true }
    );

    console.log(`[yookassaWebhook] Firestore updated successfully for user ${userId}`);
    return res.status(200).send("OK");
  } catch (error) {
    console.error(`[yookassaWebhook] Error updating Firestore for user ${userId}:`, error);
    return res.status(500).send(`Internal Error: ${error.message}`);
  }
});
