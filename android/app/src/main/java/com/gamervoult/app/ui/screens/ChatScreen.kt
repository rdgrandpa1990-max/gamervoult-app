package com.gamervoult.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamervoult.app.data.ChatConversation
import com.gamervoult.app.data.ChatMessage
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(onNavigateToAuth: () -> Unit = {}) {
    val currentUser by GamervoultRepository.currentUser.collectAsStateWithLifecycle()
    val isAdmin = currentUser?.isAdmin == true

    if (isAdmin) {
        AdminChatScreen()
    } else {
        ClientChatScreen(onNavigateToAuth = onNavigateToAuth)
    }
}

// =========================================================================
// 1. CLIENT / USER CHAT VIEW (Connected by default to Admin & Superadmin)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientChatScreen(onNavigateToAuth: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val messages by GamervoultRepository.chatMessages.collectAsStateWithLifecycle()
    val currentUser by GamervoultRepository.currentUser.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val quickInquiries = listOf(
        "Order Delivery Status",
        "How Escrow Handover Works",
        "Direct Deal with Our Team",
        "UPI & Payment Verification",
        "Free Fire ID Binding Support"
    )

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Periodic live sync every 6 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(6000)
            GamervoultRepository.fetchChatMessages()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.SupportAgent, 
                                    contentDescription = null, 
                                    tint = PrimaryIndigoLight,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "GamerVault Admin Desk",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = AccentEmerald.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "VERIFIED",
                                        color = AccentEmerald,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "🟢 Connected with Our Team",
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    GamervoultRepository.fetchChatMessages()
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryIndigo, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Escrow Trust Banner
                    Surface(
                        color = Color(0xFF1E1B4B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Direct Live Escrow Support • Real-time backend sync with Our Team",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(8.dp)
            ) {
                // Quick prompt suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(quickInquiries) { prompt ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DarkCard,
                            border = BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.clickable(enabled = !isSending) {
                                inputText = prompt
                            }
                        ) {
                            Text(
                                text = prompt,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Input bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type message for Our Team...", color = TextMuted, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        maxLines = 3,
                        enabled = !isSending
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isBlank()) return@IconButton
                            val textToSend = inputText.trim()
                            inputText = ""
                            isSending = true
                            coroutineScope.launch {
                                val result = GamervoultRepository.sendMessage(textToSend)
                                isSending = false
                                if (!result.isSuccess) {
                                    Toast.makeText(
                                        context,
                                        result.exceptionOrNull()?.message ?: "Failed to send message",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        enabled = !isSending && inputText.isNotBlank(),
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isSending) PrimaryIndigo else DarkBorder)
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) Color.White else TextMuted
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(messages) { msg ->
                ChatMessageBubble(msg, isCurrentViewerAdmin = false)
            }
        }
    }
}

// =========================================================================
// 2. ADMIN SUPPORT CONSOLE (Admin connected to all clients & users)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminChatScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val conversations by GamervoultRepository.adminConversations.collectAsStateWithLifecycle()
    val selectedClient by GamervoultRepository.selectedAdminClient.collectAsStateWithLifecycle()
    val messages by GamervoultRepository.chatMessages.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var adminReplyText by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    // Auto-refresh admin conversations or selected client messages
    LaunchedEffect(selectedClient) {
        if (selectedClient != null) {
            GamervoultRepository.fetchChatMessages(selectedClient!!.clientUid)
        } else {
            GamervoultRepository.fetchAdminConversations()
        }
    }

    // Auto-scroll in active chat
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (selectedClient == null) {
        // --- SCREEN A: ALL CONNECTED CLIENTS LIST ---
        Scaffold(
            topBar = {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentAmber.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👑", fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Admin Support Console",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Live Connected Clients (${conversations.size})",
                                color = AccentEmerald,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    GamervoultRepository.fetchAdminConversations()
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AccentAmber, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            containerColor = DarkBackground
        ) { padding ->
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Surface(
                        color = DarkCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "All clients and users are connected to you by default. Tap any client below to reply.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (conversations.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No client conversations found yet.\nClients will appear automatically upon opening the app.",
                                color = TextMuted,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(conversations) { conv ->
                        ClientConversationCard(
                            conversation = conv,
                            onClick = { GamervoultRepository.selectAdminClient(conv) }
                        )
                    }
                }
            }
        }
    } else {
        // --- SCREEN B: ADMIN 1-ON-1 CHAT WITH SELECTED CLIENT ---
        val client = selectedClient!!
        Scaffold(
            topBar = {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { GamervoultRepository.selectAdminClient(null) }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                        }

                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.2f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigoLight)
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = client.clientName,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (client.clientEmail.isNotBlank()) client.clientEmail else client.clientUid.take(16),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    GamervoultRepository.fetchChatMessages(client.clientUid)
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBackground)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryIndigo, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            containerColor = DarkBackground,
            bottomBar = {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = adminReplyText,
                            onValueChange = { adminReplyText = it },
                            placeholder = { Text("Reply as Our Team to ${client.clientName}...", color = TextMuted, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkBackground,
                                unfocusedContainerColor = DarkBackground,
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            maxLines = 3,
                            enabled = !isSendingReply
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (adminReplyText.isBlank()) return@IconButton
                                val text = adminReplyText.trim()
                                adminReplyText = ""
                                isSendingReply = true
                                coroutineScope.launch {
                                    val res = GamervoultRepository.sendMessage(text, targetClientUid = client.clientUid)
                                    isSendingReply = false
                                    if (!res.isSuccess) {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to reply", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isSendingReply && adminReplyText.isNotBlank(),
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(if (adminReplyText.isNotBlank() && !isSendingReply) AccentAmber else DarkBorder)
                        ) {
                            if (isSendingReply) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = if (adminReplyText.isNotBlank()) Color.Black else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(messages) { msg ->
                    ChatMessageBubble(msg, isCurrentViewerAdmin = true)
                }
            }
        }
    }
}

// =========================================================================
// 3. REUSABLE CONVERSATION CARD (For Admin List)
// =========================================================================
@Composable
fun ClientConversationCard(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = PrimaryIndigo.copy(alpha = 0.2f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryIndigoLight)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.clientName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Surface(
                        color = if (conversation.status == "requesting") AccentAmber.copy(alpha = 0.2f) else AccentEmerald.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = conversation.status.uppercase(),
                            color = if (conversation.status == "requesting") AccentAmber else AccentEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (conversation.clientEmail.isNotBlank()) {
                    Text(
                        text = conversation.clientEmail,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = conversation.lastMessage.ifBlank { "Connected with Admin Desk" },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 4. CHAT MESSAGE BUBBLE
// =========================================================================
@Composable
fun ChatMessageBubble(msg: ChatMessage, isCurrentViewerAdmin: Boolean = false) {
    // When viewed by Client:
    // Staff messages are on Left (with Staff badge). User's own messages are on Right.
    // When viewed by Admin:
    // Staff messages are on Right (sent by Admin). Client messages are on Left.
    val isSentByMe = if (isCurrentViewerAdmin) msg.isFromStaff else (!msg.isFromStaff && !msg.isAutomated)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        // Sender Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 3.dp)
        ) {
            if (isSentByMe) {
                Text(
                    text = if (isCurrentViewerAdmin) "👑 You (Our Team)" else "You",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            } else if (msg.isFromStaff) {
                Surface(
                    color = AccentEmerald.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, AccentEmerald.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🛡️ ${msg.senderName}",
                        color = AccentEmerald,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else if (msg.isAutomated) {
                Surface(
                    color = AccentAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, AccentAmber.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "🤖 System Greeting",
                        color = AccentAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Surface(
                    color = PrimaryIndigo.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, PrimaryIndigo.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "👤 ${msg.senderName}",
                        color = PrimaryIndigoLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Message Card
        Surface(
            color = when {
                isSentByMe -> if (isCurrentViewerAdmin) AccentAmber.copy(alpha = 0.9f) else PrimaryIndigo
                msg.isFromStaff -> DarkSurface
                msg.isAutomated -> DarkCard
                else -> DarkCard
            },
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isSentByMe) 14.dp else 4.dp,
                bottomEnd = if (isSentByMe) 4.dp else 14.dp
            ),
            border = if (!isSentByMe) BorderStroke(1.dp, if (msg.isFromStaff) AccentEmerald.copy(alpha = 0.35f) else DarkBorder) else null,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = msg.text,
                    color = if (isSentByMe && isCurrentViewerAdmin) Color.Black else if (isSentByMe) Color.White else TextPrimary,
                    fontSize = 14.sp
                )

                if (msg.timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = msg.timestamp.take(19).replace("T", " "),
                        color = if (isSentByMe && isCurrentViewerAdmin) Color.Black.copy(alpha = 0.7f) else if (isSentByMe) Color.White.copy(alpha = 0.7f) else TextMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
