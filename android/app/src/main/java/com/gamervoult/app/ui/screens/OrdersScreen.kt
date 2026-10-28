package com.gamervoult.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamervoult.app.data.DeliveryCredentials
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.OrderItem
import com.gamervoult.app.ui.theme.*

@Composable
fun OrdersScreen() {
    val orders by GamervoultRepository.orders.collectAsState()
    var selectedCredentialsOrder by remember { mutableStateOf<OrderItem?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Header
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
                    text = "Authoritative Purchase & Credentials Registry",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${orders.size} Total",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
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
                    Text("Choose a verified account from Home tab to begin.", color = TextMuted, fontSize = 13.sp)
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
                        onViewCredentials = { selectedCredentialsOrder = order }
                    )
                }
            }
        }
    }

    // Credentials Delivery Modal
    selectedCredentialsOrder?.let { order ->
        val creds = order.credentials ?: DeliveryCredentials(
            loginId = "contact@freefireofficial.in",
            password = "VerifiedViaEscrowDesk",
            notes = "Please connect with Official Support Desk for live OTP verification."
        )

        AlertDialog(
            onDismissRequest = { selectedCredentialsOrder = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Account Delivery", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Order ${order.orderCode} • ${order.itemTitle}",
                        color = PrimaryIndigoLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    CredentialField(label = "Account Type", value = creds.accountType, onCopy = {
                        clipboardManager.setText(AnnotatedString(creds.accountType))
                        copiedNotice = "Account Type copied!"
                    })

                    CredentialField(label = "Login Email / ID", value = creds.loginId, onCopy = {
                        clipboardManager.setText(AnnotatedString(creds.loginId))
                        copiedNotice = "Login ID copied!"
                    })

                    CredentialField(label = "Password", value = creds.password, onCopy = {
                        clipboardManager.setText(AnnotatedString(creds.password))
                        copiedNotice = "Password copied!"
                    })

                    if (creds.backupCodes.isNotEmpty()) {
                        CredentialField(label = "2-Step Backup Codes", value = creds.backupCodes, onCopy = {
                            clipboardManager.setText(AnnotatedString(creds.backupCodes))
                            copiedNotice = "Backup Codes copied!"
                        })
                    }

                    Surface(
                        color = DarkBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ " + (creds.notes.ifEmpty { "Do not modify recovery email within 48 hours to maintain account warranty." }),
                            color = AccentAmber,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    copiedNotice?.let { notice ->
                        Text(notice, color = AccentEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedCredentialsOrder = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun CredentialField(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Column {
        Text(label, color = TextMuted, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            color = DarkBackground,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                IconButton(onClick = onCopy, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = PrimaryIndigoLight, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun OrderCard(
    order: OrderItem,
    onViewCredentials: () -> Unit
) {
    val isPaid = order.status.uppercase() == "PAID"
    val isPending = order.status.uppercase() == "PENDING"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder(),
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )

                Surface(
                    color = when {
                        isPaid -> AccentEmerald.copy(alpha = 0.15f)
                        isPending -> AccentAmber.copy(alpha = 0.15f)
                        else -> AccentRed.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            isPaid -> "PAID & VERIFIED"
                            isPending -> "PAYMENT PENDING"
                            else -> order.status
                        },
                        color = when {
                            isPaid -> AccentEmerald
                            isPending -> AccentAmber
                            else -> AccentRed
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(order.createdAt, color = TextMuted, fontSize = 12.sp)
                Text("₹${order.amount}", color = AccentEmerald, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }

            if (isPaid) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onViewCredentials,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Account Credentials", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            } else if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Awaiting UPI gateway confirmation. Status will update automatically.",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

