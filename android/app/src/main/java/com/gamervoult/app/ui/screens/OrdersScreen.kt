package com.gamervoult.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
fun OrdersScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Authoritative Server Purchases & Handover Vault",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        isRefreshing = true
                        GamervoultRepository.fetchOrders()
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkSurface)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = PrimaryIndigo,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (orders.isEmpty()) {
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
                                        res.exceptionOrNull()?.message ?: "Credentials unavailable",
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
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account Delivery", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                if (isLoadingCreds) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryIndigo)
                    }
                } else if (fetchedCredentials != null) {
                    val creds = fetchedCredentials!!
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Order ${order.orderCode} • ${order.itemTitle}",
                            color = PrimaryIndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        CredentialField(
                            label = "Account Type",
                            value = creds.accountType,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(creds.accountType))
                                Toast.makeText(context, "Account type copied", Toast.LENGTH_SHORT).show()
                            }
                        )

                        CredentialField(
                            label = "Login Email / ID",
                            value = creds.loginId,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(creds.loginId))
                                Toast.makeText(context, "Login ID copied", Toast.LENGTH_SHORT).show()
                            }
                        )

                        CredentialField(
                            label = "Password",
                            value = creds.password,
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(creds.password))
                                Toast.makeText(context, "Password copied", Toast.LENGTH_SHORT).show()
                            }
                        )

                        if (creds.backupCodes.isNotBlank()) {
                            CredentialField(
                                label = "Backup Codes",
                                value = creds.backupCodes,
                                onCopy = {
                                    clipboardManager.setText(AnnotatedString(creds.backupCodes))
                                    Toast.makeText(context, "Backup codes copied", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (creds.notes.isNotBlank()) {
                            Surface(
                                color = DarkBackground,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Notes: ${creds.notes}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
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
                    Text("Done")
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
    val isPaid = order.status.equals("PAID", ignoreCase = true)
    val statusColor = when (order.status.uppercase()) {
        "PAID" -> AccentEmerald
        "PENDING" -> AccentAmber
        else -> AccentRed
    }

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
                Text(
                    text = order.orderCode,
                    color = PrimaryIndigoLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.status.uppercase(),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = order.itemTitle,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Amount: ₹${order.amount}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (order.paidAt != null) {
                    Text(
                        text = "Paid: ${order.paidAt.take(16)}",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            if (order.utr != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UTR: ${order.utr}",
                    color = AccentCyan,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isPaid) {
                Button(
                    onClick = onViewCredentials,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Account Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Awaiting Payment Confirmation. Credentials lock automatically releases upon gateway verification.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialField(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Surface(
        color = DarkBackground,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, color = TextMuted, fontSize = 11.sp)
                Text(text = value, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onCopy) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryIndigoLight, modifier = Modifier.size(18.dp))
            }
        }
    }
}
