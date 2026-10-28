package com.gamervoult.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.OrderItem
import com.gamervoult.app.data.ProductItem
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToOrders: () -> Unit = {},
    onNavigateToExplore: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val products by GamervoultRepository.products.collectAsStateWithLifecycle()
    val storeSettings by GamervoultRepository.storeSettings.collectAsStateWithLifecycle()
    val isGlobalLoading by GamervoultRepository.isLoading.collectAsStateWithLifecycle()

    val cleanWa = remember(storeSettings.whatsapp) {
        storeSettings.whatsapp.replace(Regex("[^0-9]"), "").ifBlank { "917318211010" }
    }

    var selectedCategory by remember { mutableStateOf("All IDs") }
    var selectedProductForBuy by remember { mutableStateOf<ProductItem?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<ProductItem?>(null) }
    var currentOrderResult by remember { mutableStateOf<OrderItem?>(null) }
    var isPlacingOrder by remember { mutableStateOf(false) }

    val categories = listOf("All IDs", "Rare & OG", "Evo 5–7 Max", "Grandmaster", "Under ₹1,000")

    val filteredProducts = remember(products, selectedCategory) {
        products.filter { p ->
            when (selectedCategory) {
                "All IDs" -> true
                "Rare & OG" -> (p.level ?: 0) >= 65 || (p.evoGuns ?: 0) >= 3 || (p.badge?.contains("OG", ignoreCase = true) == true)
                "Evo 5–7 Max" -> (p.evoGuns ?: 0) >= 3
                "Grandmaster" -> p.rank?.contains("Grandmaster", ignoreCase = true) == true || (p.level ?: 0) >= 70
                "Under ₹1,000" -> p.price <= 1000
                else -> true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOP HEADER (Spans full width)
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Store Icon + Brand + Verified
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Logo Box
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurface)
                                    .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = "Logo", tint = PrimaryIndigoLight, modifier = Modifier.size(26.dp))
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "GAMERVAULT STORE",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    // Green VERIFIED Pill Badge
                                    Surface(
                                        color = AccentEmerald.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "VERIFIED",
                                            color = AccentEmerald,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(AccentEmerald))
                                    Text(text = "Live Marketplace", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }

                        // Right: Search, WhatsApp & Refresh Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Search Button (Navigates to Explore screen)
                            IconButton(
                                onClick = onNavigateToExplore,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(1.dp, DarkBorder, CircleShape)
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }

                            // WhatsApp Button (Opens official real-time WhatsApp desk)
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanWa?text=${Uri.encode("Hello Support, I am browsing the GamerVault Free Fire Store and want to buy an account.")}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp Desk: ${storeSettings.whatsapp}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(18.dp))
                            }

                            // Refresh Button
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        GamervoultRepository.fetchProducts()
                                        Toast.makeText(context, "Catalog updated", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(1.dp, DarkBorder, CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. NOTICE BANNER (matching reference Image 1)
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = PrimaryIndigo,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "NOTICE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = "⚡ 100% Verified UIDs • Instant WhatsApp Handover • Safe 2-Way Escrow",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. PROMO HERO CARDS (Horizontal scrolling row matching reference Image 1)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Purple Gradient (Rare Sakura & Hip-Hop)
                        PromoHeroCard(
                            tag = "FEATURED DROP",
                            title = "RARE SAKURA & HIP-HOP IDS",
                            subtitle = "OG Season 1 & 2 passes with rare badges and clean login credentials.",
                            buttonText = "Explore OG IDs",
                            brush = Brush.horizontalGradient(listOf(Color(0xFF3B1669), Color(0xFF6B21A8), Color(0xFF2E1065))),
                            onClick = { selectedCategory = "Rare & OG" }
                        )

                        // Card 2: Burgundy/Red Gradient (Maxed Evo Guns)
                        PromoHeroCard(
                            tag = "TOP FIREPOWER",
                            title = "MAXED EVO GUN ACCOUNTS",
                            subtitle = "MP40 Predatory Cobra, AK47 Blue Flame Draco & M1014 Green Flame Draco Max.",
                            buttonText = "View Evo Max",
                            brush = Brush.horizontalGradient(listOf(Color(0xFF5A1523), Color(0xFF881337), Color(0xFF4C0519))),
                            onClick = { selectedCategory = "Evo 5–7 Max" }
                        )

                        // Card 3: Dark Teal Gradient (Direct Deal)
                        PromoHeroCard(
                            tag = "BUYER TRUST",
                            title = "DIRECT DEAL WITH VERIFIED ESCROW",
                            subtitle = "Zero middleman risk. Personal video call verification and instant handover.",
                            buttonText = "Shop Safely",
                            brush = Brush.horizontalGradient(listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF022C22))),
                            onClick = { selectedCategory = "All IDs" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. CATEGORY PILLS (matching reference Image 1)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = selectedCategory == cat
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) PrimaryIndigo else DarkSurface,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
                                modifier = Modifier.clickable { selectedCategory = cat }
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. SECTION HEADER: Featured Accounts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "⭐", fontSize = 14.sp)
                            Text(
                                text = "Featured Accounts",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "View All",
                            color = PrimaryIndigoLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { selectedCategory = "All IDs" }
                        )
                    }
                }
            }

            // 6. TWO-COLUMN PRODUCT GRID (Matching reference Image 1 & Image 2)
            items(filteredProducts) { item ->
                ProductGridCard(
                    product = item,
                    onCardClick = { selectedProductForDetail = item },
                    onBuyClick = { selectedProductForBuy = item }
                )
            }
        }

        // 6b. ACCOUNT FULL PHOTO & SPECS VIEWER DIALOG (Triggered when user clicks ANY ID)
        selectedProductForDetail?.let { prod ->
            ProductDetailPhotoDialog(
                product = prod,
                onDismiss = { selectedProductForDetail = null },
                onBuyClick = {
                    selectedProductForDetail = null
                    selectedProductForBuy = prod
                }
            )
        }

        // 7. UPI PAYMENT GATEWAY DIALOG (Triggered when user taps Buy)
        selectedProductForBuy?.let { prod ->
            AlertDialog(
                onDismissRequest = {
                    if (!isPlacingOrder) {
                        selectedProductForBuy = null
                        currentOrderResult = null
                    }
                },
                containerColor = DarkCardElevated,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Column {
                        Text("Instant UPI Checkout", color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(prod.title, color = PrimaryIndigoLight, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Product Preview Thumbnail & Price
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Product Photo
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(prod.mediaUrl ?: prod.images.firstOrNull())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp))
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Payable Amount", color = TextSecondary, fontSize = 11.sp)
                                    Text("₹${prod.price}", color = AccentEmerald, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    if (prod.accountUid != null) {
                                        Text("UID: ${prod.accountUid}", color = TextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        // Order Created State: Show UPI Intent & QR Code
                        if (currentOrderResult != null) {
                            val order = currentOrderResult!!
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DarkSurface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text("Order Code: ${order.orderCode}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                                    // UPI Intent Launch Button (Direct PhonePe/GPay/Paytm)
                                    if (!order.upiUri.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(order.upiUri))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "No UPI app found. Please scan QR below.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                            modifier = Modifier.fillMaxWidth().height(44.dp)
                                        ) {
                                            Text("⚡ Pay via UPI App (GPay / PhonePe)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }

                                    // QR Code Image
                                    if (!order.qrImageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(order.qrImageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "UPI QR Code",
                                            modifier = Modifier.size(160.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(8.dp)
                                        )
                                        Text("Scan QR code using any UPI app", color = TextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            // Pay Now Action Trigger
                            Text("Click below to generate your authoritative UPI payment intent & QR code.", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    if (currentOrderResult == null) {
                        Button(
                            onClick = {
                                isPlacingOrder = true
                                coroutineScope.launch {
                                    val res = GamervoultRepository.createOrder(prod)
                                    isPlacingOrder = false
                                    if (res.isSuccess) {
                                        currentOrderResult = res.getOrNull()
                                        Toast.makeText(context, "UPI Payment Gateway Initiated!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Order failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            enabled = !isPlacingOrder
                        ) {
                            if (isPlacingOrder) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Generate UPI Order", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                selectedProductForBuy = null
                                currentOrderResult = null
                                onNavigateToOrders()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Text("View in Orders", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedProductForBuy = null
                        currentOrderResult = null
                    }) {
                        Text("Close", color = TextMuted)
                    }
                }
            )
        }
    }
}

// =========================================================================
// PROMO HERO CARD COMPONENT (Matching reference Image 1)
// =========================================================================
@Composable
fun PromoHeroCard(
    tag: String,
    title: String,
    subtitle: String,
    buttonText: String,
    brush: Brush,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .width(290.dp)
            .height(170.dp)
            .background(brush, RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Amber Tag
                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Text(
                        text = tag,
                        color = AccentAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Pill Button
            Surface(
                color = PrimaryIndigo,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.clickable(onClick = onClick)
            ) {
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// =========================================================================
// 2-COLUMN PRODUCT GRID CARD (Matching reference Image 1 & Image 2)
// =========================================================================
@Composable
fun ProductGridCard(
    product: ProductItem,
    onCardClick: () -> Unit = {},
    onBuyClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. TOP PHOTO WITH BADGES (16:9 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                // Real Free Fire Cloudinary Screenshot from Catalog
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(product.mediaUrl ?: product.images.firstOrNull())
                        .crossfade(true)
                        .build(),
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Top-Left Badge: Level
                Surface(
                    color = Color.Black.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "LVL ${product.level ?: 70}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }

                // Top-Right Badge: RARE OG
                val isRareOg = (product.level ?: 0) >= 65 || (product.evoGuns ?: 0) >= 2 || (product.badge?.contains("OG", ignoreCase = true) == true)
                if (isRareOg) {
                    Surface(
                        color = AccentAmber,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("🔥 RARE OG", color = Color(0xFF1F2937), fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Bottom-Right Pill: Photo count
                val photoCount = product.images.size.coerceAtLeast(1)
                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = "📷 $photoCount Photos",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // 2. CARD BODY CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                // Title
                Text(
                    text = product.title,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tags: Evo & Rank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.evoGuns != null && product.evoGuns > 0) {
                        Surface(
                            color = DarkSurface,
                            shape = RoundedCornerShape(4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                        ) {
                            Text(
                                text = "⚡ ${product.evoGuns} Evo",
                                color = AccentAmber,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(4.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                    ) {
                        Text(
                            text = "🏆 ${product.rank ?: "Heroic"}",
                            color = PrimaryIndigoLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Price & Buy Button Row (Matching reference Image 1 & 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "₹${product.price}",
                            color = AccentEmerald,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        val orig = product.originalPrice ?: (product.price * 13 / 10)
                        if (orig > product.price) {
                            Text(
                                text = "₹$orig",
                                color = TextMuted,
                                fontSize = 10.sp,
                                textDecoration = TextDecoration.LineThrough
                            )
                        }
                    }

                    // Purple Buy Button
                    Button(
                        onClick = onBuyClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Buy", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 8. FULL PHOTO & SPECIFICATIONS VIEWER DIALOG
// =========================================================================
@Composable
fun ProductDetailPhotoDialog(
    product: ProductItem,
    onDismiss: () -> Unit,
    onBuyClick: () -> Unit
) {
    val context = LocalContext.current
    val allImages = remember(product) {
        val list = mutableListOf<String>()
        if (!product.mediaUrl.isNullOrBlank()) list.add(product.mediaUrl)
        product.images.forEach { img ->
            if (img.isNotBlank() && !list.contains(img)) {
                list.add(img)
            }
        }
        if (list.isEmpty()) {
            list.add("https://res.cloudinary.com/dolc2kuyx/image/upload/v1793185282/IMG_4229_yz1sgb.png")
        }
        list
    }

    var currentPhotoIndex by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Top Header: Title + Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            lineHeight = 22.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Surface(
                                color = AccentEmerald.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "100% ESCROW VERIFIED",
                                    color = AccentEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (!product.accountUid.isNullOrBlank()) {
                                Text(
                                    text = "UID: ${product.accountUid}",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. MAIN FULL-SIZE PHOTO PREVIEW
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                        .border(1.dp, DarkBorder, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(allImages.getOrNull(currentPhotoIndex))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full Account Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Photo Counter Badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Photo ${currentPhotoIndex + 1} of ${allImages.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Left Nav Arrow
                    if (allImages.size > 1 && currentPhotoIndex > 0) {
                        IconButton(
                            onClick = { currentPhotoIndex-- },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", tint = Color.White)
                        }
                    }

                    // Right Nav Arrow
                    if (allImages.size > 1 && currentPhotoIndex < allImages.size - 1) {
                        IconButton(
                            onClick = { currentPhotoIndex++ },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. HORIZONTAL THUMBNAILS GALLERY (Tap any thumbnail to see it)
                if (allImages.size > 1) {
                    Text(
                        text = "All Account Photos (${allImages.size})",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(allImages) { idx, imgUrl ->
                            val isSelected = idx == currentPhotoIndex
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurface)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) PrimaryIndigoLight else DarkBorder,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { currentPhotoIndex = idx }
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context).data(imgUrl).crossfade(true).build(),
                                    contentDescription = "Thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. PRICE & SPECIFICATIONS CARD
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Escrow Purchase Price", color = TextMuted, fontSize = 11.sp)
                                Text("₹${product.price}", color = AccentEmerald, fontSize = 24.sp, fontWeight = FontWeight.Black)
                            }

                            Surface(
                                color = PrimaryIndigo.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryIndigo.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "SAFE 2-WAY ESCROW",
                                    color = PrimaryIndigoLight,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = DarkBorder)

                        // Specification Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(color = DarkCardElevated, shape = RoundedCornerShape(6.dp)) {
                                Text("Level: ${product.level ?: 60}", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Surface(color = DarkCardElevated, shape = RoundedCornerShape(6.dp)) {
                                Text("Rank: ${product.rank ?: "Heroic"}", color = PrimaryIndigoLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            if (product.evoGuns != null && product.evoGuns > 0) {
                                Surface(color = DarkCardElevated, shape = RoundedCornerShape(6.dp)) {
                                    Text("⚡ ${product.evoGuns} Evo Guns", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            if (product.skinsCount != null && product.skinsCount > 0) {
                                Surface(color = DarkCardElevated, shape = RoundedCornerShape(6.dp)) {
                                    Text("${product.skinsCount} Skins", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // Description
                        if (product.description.isNotBlank()) {
                            Text(
                                text = product.description,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 5. ACTION BUTTONS: Instant Buy via UPI + WhatsApp Handover
                Button(
                    onClick = onBuyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Instant Buy (UPI) — ₹${product.price}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val liveWa = GamervoultRepository.storeSettings.value.whatsapp.replace(Regex("[^0-9]"), "").ifBlank { "917318211010" }
                            val msg = "Hello, I want to inspect and purchase this Free Fire Account from GamerVault Store:\nID: ${product.title}\nPrice: ₹${product.price}\nRef: ${product.id}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$liveWa?text=${Uri.encode(msg)}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "WhatsApp Support: ${GamervoultRepository.storeSettings.value.whatsapp}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contact Support on WhatsApp", color = WhatsAppGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
