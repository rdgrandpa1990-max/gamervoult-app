package com.gamervoult.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "https://gamervoult-backend.vercel.app"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    // 0. Live License Key Verification
    suspend fun verifyLicenseKey(key: String): Result<LicenseVerifyResult> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = key.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(Exception("Please enter a license key."))
            }
            val encodedKey = java.net.URLEncoder.encode(cleanKey, "UTF-8")
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/verify-license?key=$encodedKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            val valid = obj.optBoolean("valid", false)
            val status = obj.optString("status", if (valid) "active" else "invalid")
            val message = obj.optString("message", if (valid) "Valid License Key (Active & Ready)" else "Invalid License Key")
            val assignedMailbox = if (obj.has("assignedMailbox") && !obj.isNull("assignedMailbox")) obj.getString("assignedMailbox") else null

            Result.success(LicenseVerifyResult(valid, status, message, assignedMailbox))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. Sign Up with License Key (Matches website Signup.jsx)
    suspend fun signUpWithLicense(
        licenseKey: String,
        name: String,
        phone: String,
        email: String,
        pass: String
    ): Result<Pair<String, UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("licenseKey", licenseKey.trim())
                put("name", name.trim())
                put("phone", phone.trim())
                put("email", email.trim().lowercase())
                put("password", pass)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/signup-license")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Registration failed"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val token = obj.getString("token")
            val userObj = obj.getJSONObject("user")
            val profile = parseUserProfile(userObj)
            Result.success(Pair(token, profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1b. Google Sign In / Account Creation (Matches website loginWithGoogle)
    suspend fun loginWithGoogle(
        email: String,
        name: String,
        idToken: String? = null,
        photoUrl: String? = null
    ): Result<Pair<String, UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("googleEmail", email.trim().lowercase())
                put("googleName", name.trim())
                if (idToken != null) put("idToken", idToken)
                if (photoUrl != null) put("googlePhoto", photoUrl)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/google")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Google authentication failed"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val token = obj.getString("token")
            val userObj = obj.getJSONObject("user")
            val profile = parseUserProfile(userObj)
            Result.success(Pair(token, profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. User Login
    suspend fun login(email: String, pass: String): Result<Pair<String, UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/login")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Invalid credentials"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val token = obj.getString("token")
            val userObj = obj.getJSONObject("user")
            val profile = parseUserProfile(userObj)
            Result.success(Pair(token, profile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Current User Profile
    suspend fun getMe(token: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/auth/me")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                return@withContext Result.failure(Exception(obj.optString("message", "Session invalid")))
            }

            val userObj = obj.getJSONObject("user")
            Result.success(parseUserProfile(userObj))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4. Products Catalog
    suspend fun getProducts(): Result<List<ProductItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/products")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("Failed to load products"))
            }

            val arr = obj.optJSONArray("products") ?: JSONArray()
            val list = mutableListOf<ProductItem>()
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val imagesList = mutableListOf<String>()
                val imgArr = p.optJSONArray("images")
                if (imgArr != null) {
                    for (j in 0 until imgArr.length()) {
                        imagesList.add(imgArr.getString(j))
                    }
                }

                val charsList = mutableListOf<String>()
                val charsArr = p.optJSONArray("characters")
                if (charsArr != null) {
                    for (j in 0 until charsArr.length()) {
                        charsList.add(charsArr.getString(j))
                    }
                }

                val rawMedia = if (p.has("mediaUrl") && !p.isNull("mediaUrl")) p.getString("mediaUrl") else null
                val finalMedia = rawMedia ?: imagesList.firstOrNull()
                val acctUid = if (p.has("accountUid") && !p.isNull("accountUid")) p.getString("accountUid") else (if (p.has("gameUid") && !p.isNull("gameUid")) p.getString("gameUid") else null)

                list.add(
                    ProductItem(
                        id = p.getString("id"),
                        title = p.optString("title", "Game Account"),
                        price = p.optInt("price", 0),
                        originalPrice = if (p.has("originalPrice") && !p.isNull("originalPrice")) p.getInt("originalPrice") else null,
                        category = p.optString("category", "General"),
                        description = p.optString("description", ""),
                        mediaUrl = finalMedia,
                        images = imagesList,
                        badge = if (p.has("badge") && !p.isNull("badge")) p.getString("badge") else null,
                        level = if (p.has("level") && !p.isNull("level")) p.getInt("level") else null,
                        evoGuns = if (p.has("evoGuns") && !p.isNull("evoGuns")) p.getInt("evoGuns") else null,
                        likes = if (p.has("likes") && !p.isNull("likes")) p.getInt("likes") else null,
                        rank = if (p.has("rank") && !p.isNull("rank")) p.getString("rank") else null,
                        characters = charsList,
                        skinsCount = if (p.has("skinsCount") && !p.isNull("skinsCount")) p.getInt("skinsCount") else null,
                        accountUid = acctUid,
                        isSold = p.optBoolean("isSold", false),
                        deliveryMode = p.optString("deliveryMode", "contact_seller")
                    )
                )
            }
            val contactObj = obj.optJSONObject("storeContact")
            if (contactObj != null) {
                val wa = contactObj.optString("whatsapp", "+917318211010")
                val tg = contactObj.optString("telegram", "alexzzzff")
                val bn = contactObj.optString("brandName", "GAMERVAULT STORE")
                GamervoultRepository.updateStoreSettings(StoreContactSettings(wa, tg, bn))
            }

            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 4b. Live Store Settings & Contact Channels
    suspend fun getStoreSettings(): Result<StoreContactSettings> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/store/settings")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)
            if (response.isSuccessful && obj.optBoolean("success", false)) {
                val wa = obj.optString("whatsapp", "+917318211010")
                val tg = obj.optString("telegram", "alexzzzff")
                val bn = obj.optString("brandName", "GAMERVAULT STORE")
                Result.success(StoreContactSettings(wa, tg, bn))
            } else {
                Result.failure(Exception("Failed to fetch store settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. Create Order (Supports both Guest and Authenticated Checkout)
    suspend fun createOrder(
        token: String?,
        itemId: String,
        customerName: String,
        customerEmail: String,
        customerMobile: String
    ): Result<OrderItem> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("itemId", itemId)
                put("customerName", customerName.ifBlank { "Guest Buyer" })
                put("customerEmail", customerEmail)
                put("customerMobile", customerMobile)
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val builder = Request.Builder()
                .url("$BASE_URL/api/v1/orders/create")
                .post(body)

            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(builder.build()).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Failed to create order"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val orderItem = OrderItem(
                orderCode = obj.getString("orderCode"),
                itemId = itemId,
                itemTitle = obj.optString("itemTitle", "Game Account"),
                amount = obj.getInt("amount"),
                status = obj.optString("status", "PENDING"),
                createdAt = "Just now",
                upiUri = if (obj.has("upiUri") && !obj.isNull("upiUri")) obj.getString("upiUri") else null,
                qrImageUrl = if (obj.has("qrImageUrl") && !obj.isNull("qrImageUrl")) obj.getString("qrImageUrl") else null
            )
            Result.success(orderItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 6. User Orders
    suspend fun getOrders(token: String): Result<List<OrderItem>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/orders")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("Failed to load orders"))
            }

            val arr = obj.optJSONArray("orders") ?: JSONArray()
            val list = mutableListOf<OrderItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    OrderItem(
                        orderCode = o.getString("orderCode"),
                        itemId = o.optString("itemId", ""),
                        itemTitle = o.optString("itemTitle", "Game Account"),
                        amount = o.optInt("amount", 0),
                        status = o.optString("status", "PENDING"),
                        createdAt = o.optString("createdAt", ""),
                        paidAt = if (o.has("paidAt") && !o.isNull("paidAt")) o.getString("paidAt") else null,
                        utr = if (o.has("utr") && !o.isNull("utr")) o.getString("utr") else null
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. Get Credentials for PAID Order
    suspend fun getOrderCredentials(token: String, orderCode: String): Result<DeliveryCredentials> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/orders/$orderCode/credentials")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Credentials not released yet"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val credObj = obj.optJSONObject("credentials")
            if (credObj != null) {
                val creds = DeliveryCredentials(
                    accountType = credObj.optString("accountType", "Google"),
                    loginId = credObj.optString("loginId", ""),
                    password = credObj.optString("password", ""),
                    backupCodes = credObj.optString("backupCodes", ""),
                    notes = credObj.optString("notes", ""),
                    tutorialUrl = credObj.optString("tutorialUrl", "")
                )
                Result.success(creds)
            } else {
                Result.failure(Exception("No credential object returned"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 8. Support Chat Messages
    suspend fun getChatMessages(token: String): Result<List<ChatMessage>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/chat/messages")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                return@withContext Result.failure(Exception("Failed to fetch chat"))
            }

            val arr = obj.optJSONArray("messages") ?: JSONArray()
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until arr.length()) {
                val m = arr.getJSONObject(i)
                list.add(
                    ChatMessage(
                        id = m.optString("id", "msg_$i"),
                        senderId = if (m.has("senderId")) m.getString("senderId") else null,
                        senderName = m.optString("senderName", "Support"),
                        text = m.optString("text", ""),
                        timestamp = m.optString("timestamp", ""),
                        isFromStaff = m.optBoolean("isFromStaff", false),
                        isAutomated = m.optBoolean("isAutomated", false)
                    )
                )
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 9. Send Support Chat Message
    suspend fun sendChatMessage(token: String, text: String): Result<Pair<ChatMessage, ChatMessage?>> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("text", text.trim())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/chat/send")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Failed to send message"))
                return@withContext Result.failure(Exception(errMsg))
            }

            val userObj = obj.getJSONObject("userMessage")
            val userMsg = ChatMessage(
                id = obj.optString("messageId", "msg_${System.currentTimeMillis()}"),
                senderId = userObj.optString("senderId"),
                senderName = userObj.optString("senderName", "You"),
                text = userObj.optString("text"),
                timestamp = userObj.optString("timestamp"),
                isFromStaff = userObj.optBoolean("isFromStaff", false),
                isAutomated = userObj.optBoolean("isAutomated", false)
            )

            var replyMsg: ChatMessage? = null
            if (obj.has("reply") && !obj.isNull("reply")) {
                val replyObj = obj.getJSONObject("reply")
                replyMsg = ChatMessage(
                    id = "reply_${System.currentTimeMillis()}",
                    senderId = replyObj.optString("senderId"),
                    senderName = replyObj.optString("senderName", "Automated Support"),
                    text = replyObj.optString("text"),
                    timestamp = replyObj.optString("timestamp"),
                    isFromStaff = replyObj.optBoolean("isFromStaff", false),
                    isAutomated = replyObj.optBoolean("isAutomated", true)
                )
            }

            Result.success(Pair(userMsg, replyMsg))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 10. Redeem License Key
    suspend fun redeemLicense(token: String, licenseKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("licenseKey", licenseKey.trim())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA)
            val request = Request.Builder()
                .url("$BASE_URL/api/v1/license/redeem")
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val raw = response.body?.string() ?: ""
            val obj = JSONObject(raw)

            if (!response.isSuccessful || !obj.optBoolean("success", false)) {
                val errMsg = obj.optString("message", obj.optString("error", "Redemption failed"))
                return@withContext Result.failure(Exception(errMsg))
            }

            Result.success(obj.optString("message", "License key redeemed successfully"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseUserProfile(obj: JSONObject): UserProfile {
        return UserProfile(
            uid = obj.getString("uid"),
            email = obj.optString("email", ""),
            name = obj.optString("name", "Player"),
            phone = if (obj.has("phone") && !obj.isNull("phone")) obj.getString("phone") else null,
            role = obj.optString("role", "client"),
            status = obj.optString("status", "Active"),
            isVip = obj.optBoolean("isVip", false),
            subscriptionExpiry = if (obj.has("subscriptionExpiry") && !obj.isNull("subscriptionExpiry")) obj.getString("subscriptionExpiry") else null,
            authProvider = if (obj.has("authProvider") && !obj.isNull("authProvider")) obj.getString("authProvider") else null,
            usedLicenseKey = if (obj.has("usedLicenseKey") && !obj.isNull("usedLicenseKey")) obj.getString("usedLicenseKey") else null
        )
    }
}
