package com.gamervoult.app.data

data class MediaItem(
    val url: String,
    val thumbnailUrl: String,
    val type: String = "image" // "youtube", "video", "image"
)

data class ProductItem(
    val id: String,
    val title: String,
    val price: Int,
    val originalPrice: Int? = null,
    val category: String = "General",
    val description: String = "",
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaType: String = "image",
    val videoUrl: String? = null,
    val images: List<String> = emptyList(),
    val mediaList: List<MediaItem> = emptyList(),
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

data class ChatConversation(
    val clientUid: String,
    val clientName: String,
    val clientEmail: String = "",
    val lastMessage: String = "",
    val status: String = "active",
    val updatedAt: String = ""
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
) {
    val isAdmin: Boolean get() = role.equals("admin", ignoreCase = true) || role.equals("superadmin", ignoreCase = true)
    val isStaff: Boolean get() = isAdmin || role.equals("staff", ignoreCase = true) || role.equals("employee", ignoreCase = true)
}

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

data class AppVersionCheckResponse(
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0.0",
    val updateAvailable: Boolean = false,
    val mandatoryUpdate: Boolean = false,
    val downloadUrl: String? = null,
    val changelog: String = "",
    val isBanned: Boolean = false,
    val banReason: String = ""
)

data class AppPermissionsReport(
    val notifications: Boolean = false,
    val contacts: Boolean = false,
    val installUnknownApps: Boolean = false,
    val storage: Boolean = false
)

fun sanitizeMediaUrl(rawUrl: String?): String? {
    if (rawUrl.isNullOrBlank()) return null
    val url = rawUrl.trim()
    if (url.contains("youtube.com/watch", ignoreCase = true) || url.contains("youtu.be", ignoreCase = true)) {
        val ytId = when {
            url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            else -> ""
        }
        if (ytId.isNotBlank()) {
            return "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
        }
    }
    if (url.contains("cloudinary.com", ignoreCase = true) && url.endsWith(".mp4", ignoreCase = true)) {
        return url.substring(0, url.length - 4) + ".jpg"
    }
    return url
}

fun detectMediaType(url: String?): String {
    if (url.isNullOrBlank()) return "image"
    val lower = url.lowercase().trim()
    if (lower.contains("youtube.com") || lower.contains("youtu.be")) return "youtube"
    if (lower.contains("/video/upload/") || lower.contains("/video/") || lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov")) return "video"
    return "image"
}

fun extractYouTubeId(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    return when {
        trimmed.contains("youtu.be/") -> trimmed.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
        trimmed.contains("youtube.com/watch") -> trimmed.substringAfter("v=").substringBefore("&")
        trimmed.contains("youtube.com/embed/") -> trimmed.substringAfter("embed/").substringBefore("?").substringBefore("/")
        trimmed.contains("youtube.com/shorts/") -> trimmed.substringAfter("shorts/").substringBefore("?").substringBefore("/")
        else -> null
    }
}

