package com.gamervoult.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamervoult.app.data.DeliveryCredentials
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.OrderItem
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun OrdersScreen(
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val currentUser by GamervoultRepository.currentUser.collectAsStateWithLifecycle()
    val orders by GamervoultRepository.orders.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    var selectedOrderForCreds by remember { mutableStateOf<OrderItem?>(null) }
    var fetchedCredentials by remember { mutableStateOf<DeliveryCredentials?>(null) }
    var isLoadingCreds by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Orders",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "UPI Escrow Purchases & Handover Vault",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (currentUser != null) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            isRefreshing = true
                            GamervoultRepository.fetchOrders()
                            isRefreshing = false
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(DarkSurface)
                        .border(1.dp, DarkBorder, CircleShape)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = PrimaryIndigo, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guest State: User is not logged in
        if (currentUser == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = PrimaryIndigo.copy(alpha = 0.15f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Inventory2, contentDescription = null, tint = PrimaryIndigoLight, modifier = Modifier.size(28.dp))
                            }
                        }

                        Text("Track Your Orders", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                        Text(
                            text = "Sign in to view your order history, verified transaction proofs, and account credential handover.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = onNavigateToAuth,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Sign In / Register", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else if (orders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No orders placed yet", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Browse the live catalog to place your first escrow order.", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        onViewCredentials = {
                            selectedOrderForCreds = order
                            isLoadingCreds = true
                            fetchedCredentials = null
                            coroutineScope.launch {
                                val res = GamervoultRepository.fetchCredentials(order.orderCode)
                                isLoadingCreds = false
                                if (res.isSuccess) {
                                    fetchedCredentials = res.getOrThrow()
                                } else {
                                    Toast.makeText(
                                        context,
                                        res.exceptionOrNull()?.message ?: "Credentials unavailable until order is marked PAID.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    selectedOrderForCreds = null
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    // Credentials Delivery Modal
    selectedOrderForCreds?.let { order ->
        AlertDialog(
            onDismissRequest = {
                selectedOrderForCreds = null
                fetchedCredentials = null
            },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account Delivery", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                if (isLoadingCreds) {
                    Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else if (fetchedCredentials != null) {
                    val creds = fetchedCredentials!!
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Order: ${order.orderCode}", color = TextMuted, fontSize = 12.sp)

                        Surface(
                            color = DarkCard,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Login Email / ID:", color = TextMuted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(creds.loginId, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(creds.loginId))
                                        Toast.makeText(context, "Copied ID", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Text("Password:", color = TextMuted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(creds.password, color = AccentEmerald, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    IconButton(onClick = {
                                        clipboardManager.setText(AnnotatedString(creds.password))
                                        Toast.makeText(context, "Copied Password", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }
                                }

                                if (creds.backupCodes.isNotBlank()) {
                                    Text("Backup Codes: ${creds.backupCodes}", color = TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text("Credentials not yet released. Please verify payment.", color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedOrderForCreds = null
                        fetchedCredentials = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: OrderItem,
    onViewCredentials: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.orderCode, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                val (badgeColor, textColor) = when (order.status) {
                    "PAID" -> Pair(AccentEmerald.copy(alpha = 0.15f), AccentEmerald)
                    "PENDING" -> Pair(AccentAmber.copy(alpha = 0.15f), AccentAmber)
                    else -> Pair(AccentRed.copy(alpha = 0.15f), AccentRed)
                }

                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.status,
                        color = textColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(order.itemTitle, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("₹${order.amount}", color = AccentEmerald, fontSize = 18.sp, fontWeight = FontWeight.Black)

                if (order.status == "PAID") {
                    Button(
                        onClick = onViewCredentials,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Credentials", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Awaiting Payment",
                            color = TextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
