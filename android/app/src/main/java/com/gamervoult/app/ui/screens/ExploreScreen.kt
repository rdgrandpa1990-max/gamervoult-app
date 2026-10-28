package com.gamervoult.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.StoryItem
import com.gamervoult.app.ui.theme.*

@Composable
fun ExploreScreen() {
    val stories by GamervoultRepository.stories.collectAsState()
    var selectedStory by remember { mutableStateOf<StoryItem?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Explore & Security",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Official Escrow Standard, Protection Policies & Offers",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }

        // Stories Row
        item {
            Column {
                Text("Featured Stories", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(stories) { story ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkCard),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier
                                .width(150.dp)
                                .height(100.dp)
                                .clickable { selectedStory = story }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    color = PrimaryIndigo.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = story.badge,
                                        color = PrimaryIndigoLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = story.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Escrow Pillars
        item {
            Text("Why Gamervoult Escrow?", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        item {
            SecurityPillarCard(
                icon = Icons.Default.Shield,
                iconColor = AccentEmerald,
                title = "100% Fund Hold Guarantee",
                description = "Your payment is locked safely on our platform. The seller only receives credit after you have verified full account credentials and logged into Free Fire."
            )
        }

        item {
            SecurityPillarCard(
                icon = Icons.Default.HourglassBottom,
                iconColor = AccentAmber,
                title = "48-Hour Security Cooldown Policy",
                description = "Strict anti-ban standard: recovery email and phone binding modifications are locked for 48 hours following delivery to avoid triggering Garena suspicious location locks."
            )
        }

        item {
            SecurityPillarCard(
                icon = Icons.Default.Key,
                iconColor = PrimaryIndigoLight,
                title = "Zero-Leak Credential Handover",
                description = "Credentials are stored in encrypted, isolated server vaults. They are released only to the confirmed buyer upon authoritative gateway confirmation."
            )
        }

        item {
            SecurityPillarCard(
                icon = Icons.Default.VpnKey,
                iconColor = AccentCyan,
                title = "One-Time Atomic License Redemption",
                description = "Digital access keys are single-use and redeemed in an atomic cloud transaction, instantly provisioning 48-hour entitlement and custom verified claims."
            )
        }
    }

    selectedStory?.let { story ->
        AlertDialog(
            onDismissRequest = { selectedStory = null },
            containerColor = DarkSurface,
            title = {
                Text(story.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Text(
                    text = "Gamervoult is the premier trusted escrow marketplace for competitive gamers in India. All transactions, accounts, and OTP extractions are verified through our official desk.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { selectedStory = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
fun SecurityPillarCard(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    title: String,
    description: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                color = iconColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}
