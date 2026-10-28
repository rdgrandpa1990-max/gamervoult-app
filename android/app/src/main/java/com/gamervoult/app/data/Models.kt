package com.gamervoult.app.data

data class ProductItem(
    val id: String,
    val title: String,
    val price: Int,
    val originalPrice: Int? = null,
    val category: String = "General",
    val description: String = "",
    val mediaUrl: String? = null,
    val images: List<String> = emptyList(),
    val badge: String? = null,
    val level: Int? = null,
    val evoGuns: Int? = null,
    val likes: Int? = null,
    val rank: String? = null,
    val characters: List<String> = emptyList(),
    val skinsCount: Int? = null,
    val accountUid: String? = null,
    val isSold: Boolean = false,
    val deliveryMode: String = "contact_seller"
)

data class DeliveryCredentials(
    val accountType: String = "Google",
    val loginId: String = "",
    val password: String = "",
    val backupCodes: String = "",
    val notes: String = "",
    val tutorialUrl: String = ""
)

data class OrderItem(
    val orderCode: String,
    val itemId: String,
    val itemTitle: String,
    val amount: Int,
    val status: String, // PENDING, PAID, CANCELLED
    val createdAt: String,
    val paidAt: String? = null,
    val utr: String? = null,
    val upiUri: String? = null,
    val qrImageUrl: String? = null,
    val credentials: DeliveryCredentials? = null
)

data class OrderCreateResult(
    val orderCode: String,
    val amount: Int,
    val status: String,
    val itemTitle: String,
    val upiUri: String? = null,
    val qrImageUrl: String? = null,
    val fullCheckoutUrl: String? = null
)

data class LicenseVerifyResult(
    val valid: Boolean,
    val status: String,
    val message: String,
    val assignedMailbox: String? = null
)

data class ChatMessage(
    val id: String,
    val senderId: String? = null,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromStaff: Boolean = false,
    val isAutomated: Boolean = false
)

data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val phone: String? = null,
    val role: String = "client", // client, user, staff, admin, superadmin
    val status: String = "Active",
    val isVip: Boolean = false,
    val subscriptionExpiry: String? = null,
    val authProvider: String? = null,
    val usedLicenseKey: String? = null
)

data class AppVersionInfo(
    val latestVersionName: String = "1.0.0",
    val updateAvailable: Boolean = false,
    val mandatoryUpdate: Boolean = false,
    val isArtifactAvailable: Boolean = false,
    val changelog: String = ""
)

data class StoreContactSettings(
    val whatsapp: String = "+917318211010",
    val telegram: String = "alexzzzff",
    val brandName: String = "GAMERVAULT STORE"
)
