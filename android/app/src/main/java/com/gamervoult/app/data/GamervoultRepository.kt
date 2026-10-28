package com.gamervoult.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GamervoultRepository {

    // Current User Session
    private val _currentUser = MutableStateFlow(
        UserProfile(
            uid = "gv_user_7788",
            email = "player@gamervoult.in",
            name = "Harsh Pandey",
            role = "client",
            status = "Active",
            isVip = true,
            subscriptionExpiry = "30 Nov 2026"
        )
    )
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    // Products Flow
    private val _products = MutableStateFlow<List<ProductItem>>(
        listOf(
            ProductItem(
                id = "prod_01",
                title = "Garena VIP ID 75 • 7 Evo Guns Maxed",
                price = 1499,
                originalPrice = 2499,
                category = "Evo Guns",
                description = "Level 75 ID with Maxed Blue Flame Draco, Cobra MP40, Megalodon Alpha Scar. 100% verified single-owner account with recovery warranty.",
                badge = "⭐ TOP DEAL",
                level = 75,
                evoGuns = 7,
                likes = 28400
            ),
            ProductItem(
                id = "prod_02",
                title = "Season 1 Sakura + Hip Hop Bundle OG ID",
                price = 3999,
                originalPrice = 5999,
                category = "VIP Account",
                description = "Original Season 1 Sakura and Season 2 Hip Hop passes unlocked. Includes Old Criminal Bundles (Red, Yellow) and Titan Scar.",
                badge = "👑 ULTRA RARE",
                level = 78,
                evoGuns = 9,
                likes = 45200
            ),
            ProductItem(
                id = "prod_03",
                title = "Grandmaster Rank Push ID • 4 Evo Level 6",
                price = 899,
                originalPrice = 1499,
                category = "Flash Deals",
                description = "Current BR-Ranked Grandmaster 3 stars. MP40 Predatory Cobra, AK Blue Flame, UMP Booyah Day. Clean login.",
                badge = "⚡ FLASH SALE",
                level = 71,
                evoGuns = 4,
                likes = 14900
            ),
            ProductItem(
                id = "prod_04",
                title = "Level 72 Pure Clean Gmail Bound Account",
                price = 599,
                originalPrice = 999,
                category = "Level 70+",
                description = "Clean single Gmail binding. Perfect starter ID for tournament play with 15 Elite Passes and 2 Evo Guns.",
                badge = "VERIFIED",
                level = 72,
                evoGuns = 2,
                likes = 9800
            )
        )
    )
    val products: StateFlow<List<ProductItem>> = _products.asStateFlow()

    // Stories Flow
    private val _stories = MutableStateFlow<List<StoryItem>>(
        listOf(
            StoryItem("st_1", "🔥 50% Off Flash Drop", "DEAL"),
            StoryItem("st_2", "🛡️ Escrow Guarantee 2026", "SECURITY"),
            StoryItem("st_3", "⚡ Instant Handover Guide", "TUTORIAL"),
            StoryItem("st_4", "👑 Sakura Bundle Drop", "RARE")
        )
    )
    val stories: StateFlow<List<StoryItem>> = _stories.asStateFlow()

    // Orders Flow
    private val _orders = MutableStateFlow<List<OrderItem>>(
        listOf(
            OrderItem(
                orderCode = "GV-8X9P-4421",
                itemId = "prod_01",
                itemTitle = "Garena VIP ID 75 • 7 Evo Guns Maxed",
                amount = 1499,
                status = "PAID",
                createdAt = "28 Oct 2026, 10:15 AM",
                paidAt = "28 Oct 2026, 10:18 AM",
                utr = "430192847192",
                credentials = DeliveryCredentials(
                    accountType = "Google",
                    loginId = "vip_escrow_player75@gmail.com",
                    password = "SecureEscrowPass#2026",
                    backupCodes = "1829 4019, 8492 0184, 9182 3019",
                    notes = "Login via Google Account. Do not modify recovery email within the first 48 hours to prevent security lock.",
                    tutorialUrl = "https://freefireofficial.in/handover-tutorial"
                )
            ),
            OrderItem(
                orderCode = "GV-7Y1K-9022",
                itemId = "prod_03",
                itemTitle = "Grandmaster Rank Push ID • 4 Evo Level 6",
                amount = 899,
                status = "PENDING",
                createdAt = "28 Oct 2026, 11:05 AM"
            )
        )
    )
    val orders: StateFlow<List<OrderItem>> = _orders.asStateFlow()

    // Chat Messages Flow
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "msg_1",
                senderName = "Escrow Desk Official",
                text = "Welcome to Gamervoult Official Support Desk! How can we assist you with account verification or delivery today?",
                timestamp = "10:00 AM",
                isFromStaff = true
            ),
            ChatMessage(
                id = "msg_2",
                senderName = "Harsh Pandey",
                text = "Hi, I just paid for order GV-8X9P-4421. Where can I find my account backup codes?",
                timestamp = "10:19 AM",
                isFromStaff = false
            ),
            ChatMessage(
                id = "msg_3",
                senderName = "Escrow Desk Official",
                text = "✅ Your payment was verified via gateway! You can now access your login details, password, and backup codes in the 'My Orders' tab. We are here if you need binding help.",
                timestamp = "10:20 AM",
                isFromStaff = true
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val userMsg = ChatMessage(
            id = "msg_" + System.currentTimeMillis(),
            senderName = _currentUser.value?.name ?: "User",
            text = text,
            timestamp = timeStr,
            isFromStaff = false
        )
        _chatMessages.value = _chatMessages.value + userMsg

        // Auto-reply simulation for responsive experience
        val replyMsg = ChatMessage(
            id = "msg_reply_" + System.currentTimeMillis(),
            senderName = "AI Copilot Desk",
            text = "Thank you for connecting! Our Escrow Specialist has received your message: \"$text\". Official response will be logged immediately.",
            timestamp = timeStr,
            isFromStaff = true
        )
        _chatMessages.value = _chatMessages.value + replyMsg
    }

    fun createOrder(product: ProductItem): OrderItem {
        val timeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        val code = "GV-" + (1000..9999).random() + "-" + (1000..9999).random()
        val newOrder = OrderItem(
            orderCode = code,
            itemId = product.id,
            itemTitle = product.title,
            amount = product.price,
            status = "PENDING",
            createdAt = timeStr
        )
        _orders.value = listOf(newOrder) + _orders.value
        return newOrder
    }
}
