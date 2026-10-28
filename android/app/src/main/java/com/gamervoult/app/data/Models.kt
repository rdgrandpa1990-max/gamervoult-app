package com.gamervoult.app.data

data class ProductItem(
    val id: String,
    val title: String,
    val price: Int,
    val originalPrice: Int = (price * 1.5).toInt(),
    val category: String = "VIP Account",
    val description: String = "",
    val badge: String = "VERIFIED",
    val level: Int = 70,
    val evoGuns: Int = 4,
    val likes: Int = 12500,
    val isSold: Boolean = false
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
    val status: String, // PENDING, PAID, EXPIRED, CANCELLED
    val createdAt: String,
    val paidAt: String? = null,
    val utr: String? = null,
    val credentials: DeliveryCredentials? = null
)

data class StoryItem(
    val id: String,
    val title: String,
    val badge: String,
    val bannerUrl: String = ""
)

data class ChatMessage(
    val id: String,
    val senderName: String,
    val text: String,
    val timestamp: String,
    val isFromStaff: Boolean
)

data class UserProfile(
    val uid: String,
    val email: String,
    val name: String,
    val role: String, // client, staff, admin, superadmin
    val status: String,
    val isVip: Boolean = false,
    val subscriptionExpiry: String? = null
)

data class AppVersionInfo(
    val latestVersionName: String = "1.0.0",
    val updateAvailable: Boolean = false,
    val mandatoryUpdate: Boolean = false,
    val isArtifactAvailable: Boolean = false,
    val changelog: String = ""
)
