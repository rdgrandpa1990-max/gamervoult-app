package com.gamervoult.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object GamervoultRepository {

    private const val PREFS_NAME = "gamervoult_session_prefs"
    private const val KEY_TOKEN = "session_token"

    private var sessionToken: String? = null

    // State Flows
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _products = MutableStateFlow<List<ProductItem>>(emptyList())
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<OrderItem>>(emptyList())
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _storeSettings = MutableStateFlow(StoreContactSettings())
    val storeSettings: StateFlow<StoreContactSettings> = _storeSettings.asStateFlow()

    fun updateStoreSettings(settings: StoreContactSettings) {
        _storeSettings.value = settings
    }

    suspend fun fetchStoreSettings(): Result<StoreContactSettings> {
        val res = ApiClient.getStoreSettings()
        if (res.isSuccess) {
            _storeSettings.value = res.getOrThrow()
        }
        return res
    }

    fun getSessionToken(): String? = sessionToken

    fun initSession(context: Context, coroutineScope: CoroutineScope) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedToken = prefs.getString(KEY_TOKEN, null)

        coroutineScope.launch {
            _isInitializing.value = true
            // Always fetch fresh catalog from real server
            fetchProducts()

            if (!savedToken.isNullOrBlank()) {
                sessionToken = savedToken
                val meResult = ApiClient.getMe(savedToken)
                if (meResult.isSuccess) {
                    _currentUser.value = meResult.getOrNull()
                    fetchOrders()
                    fetchChatMessages()
                } else {
                    // Invalid or expired token
                    sessionToken = null
                    prefs.edit().remove(KEY_TOKEN).apply()
                    _currentUser.value = null
                }
            } else {
                _currentUser.value = null
            }
            _isInitializing.value = false
        }
    }

    suspend fun login(context: Context, email: String, pass: String): Result<UserProfile> {
        _isLoading.value = true
        val res = ApiClient.login(email, pass)
        _isLoading.value = false

        return if (res.isSuccess) {
            val (token, profile) = res.getOrThrow()
            sessionToken = token
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply()

            _currentUser.value = profile
            fetchOrders()
            fetchChatMessages()
            Result.success(profile)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Login failed"))
        }
    }

    suspend fun signUpWithLicense(
        context: Context,
        licenseKey: String,
        name: String,
        phone: String,
        email: String,
        pass: String
    ): Result<UserProfile> {
        _isLoading.value = true
        val res = ApiClient.signUpWithLicense(licenseKey, name, phone, email, pass)
        _isLoading.value = false

        return if (res.isSuccess) {
            val (token, profile) = res.getOrThrow()
            sessionToken = token
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply()

            _currentUser.value = profile
            fetchOrders()
            fetchChatMessages()
            Result.success(profile)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Sign up with license failed"))
        }
    }

    suspend fun loginWithGoogle(
        context: Context,
        email: String,
        name: String,
        idToken: String? = null,
        photoUrl: String? = null
    ): Result<UserProfile> {
        _isLoading.value = true
        val res = ApiClient.loginWithGoogle(email, name, idToken, photoUrl)
        _isLoading.value = false

        return if (res.isSuccess) {
            val (token, profile) = res.getOrThrow()
            sessionToken = token
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_TOKEN, token)
                .apply()

            _currentUser.value = profile
            fetchOrders()
            fetchChatMessages()
            Result.success(profile)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Google sign in failed"))
        }
    }

    fun logout(context: Context) {
        sessionToken = null
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_TOKEN)
            .apply()

        _currentUser.value = null
        _orders.value = emptyList()
        _chatMessages.value = emptyList()
    }

    suspend fun fetchProducts(): Result<List<ProductItem>> {
        val res = ApiClient.getProducts()
        if (res.isSuccess) {
            _products.value = res.getOrNull() ?: emptyList()
        }
        fetchStoreSettings()
        return res
    }

    suspend fun fetchOrders(): Result<List<OrderItem>> {
        val token = sessionToken ?: return Result.failure(Exception("Not authenticated"))
        val res = ApiClient.getOrders(token)
        if (res.isSuccess) {
            _orders.value = res.getOrNull() ?: emptyList()
        }
        return res
    }

    suspend fun fetchChatMessages(): Result<List<ChatMessage>> {
        val token = sessionToken ?: return Result.failure(Exception("Not authenticated"))
        val res = ApiClient.getChatMessages(token)
        if (res.isSuccess) {
            _chatMessages.value = res.getOrNull() ?: emptyList()
        }
        return res
    }

    suspend fun sendMessage(text: String): Result<Unit> {
        val token = sessionToken ?: return Result.failure(Exception("Not authenticated"))
        val res = ApiClient.sendChatMessage(token, text)
        return if (res.isSuccess) {
            val (userMsg, replyMsg) = res.getOrThrow()
            val updated = _chatMessages.value.toMutableList()
            updated.add(userMsg)
            if (replyMsg != null) {
                updated.add(replyMsg)
            }
            _chatMessages.value = updated
            Result.success(Unit)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Failed to send message"))
        }
    }

    suspend fun verifyLicenseKey(key: String): Result<LicenseVerifyResult> {
        return ApiClient.verifyLicenseKey(key)
    }

    suspend fun createOrder(
        product: ProductItem,
        customerName: String = "",
        customerEmail: String = "",
        customerMobile: String = ""
    ): Result<OrderItem> {
        val user = _currentUser.value
        val res = ApiClient.createOrder(
            token = sessionToken,
            itemId = product.id,
            customerName = if (customerName.isNotBlank()) customerName else (user?.name ?: "Guest Buyer"),
            customerEmail = if (customerEmail.isNotBlank()) customerEmail else (user?.email ?: "guest@freefireofficial.in"),
            customerMobile = if (customerMobile.isNotBlank()) customerMobile else (user?.phone ?: "")
        )

        if (res.isSuccess) {
            val newOrder = res.getOrThrow()
            _orders.value = listOf(newOrder) + _orders.value
        }
        return res
    }

    suspend fun fetchCredentials(orderCode: String): Result<DeliveryCredentials> {
        val token = sessionToken ?: return Result.failure(Exception("Not authenticated"))
        return ApiClient.getOrderCredentials(token, orderCode)
    }

    suspend fun redeemLicense(licenseKey: String): Result<String> {
        val token = sessionToken ?: return Result.failure(Exception("Not authenticated"))
        val res = ApiClient.redeemLicense(token, licenseKey)
        if (res.isSuccess) {
            // Refresh user profile after redemption
            val meRes = ApiClient.getMe(token)
            if (meRes.isSuccess) {
                _currentUser.value = meRes.getOrNull()
            }
        }
        return res
    }
}
