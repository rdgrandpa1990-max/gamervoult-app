package com.gamervoult.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.OrderItem
import com.gamervoult.app.data.ProductItem
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val products by GamervoultRepository.products.collectAsStateWithLifecycle()
    val storeSettings by GamervoultRepository.storeSettings.collectAsStateWithLifecycle()
    val cleanWa = remember(storeSettings.whatsapp) {
        storeSettings.whatsapp.replace(Regex("[^0-9]"), "").ifBlank { "917318211010" }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All IDs") }
    var sortBy by remember { mutableStateOf("Newest First") }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedProductForBuy by remember { mutableStateOf<ProductItem?>(null) }
    var selectedProductForDetail by remember { mutableStateOf<ProductItem?>(null) }
    var currentOrderResult by remember { mutableStateOf<OrderItem?>(null) }
    var isPlacingOrder by remember { mutableStateOf(false) }

    val filterOptions = listOf("All IDs", "Rare / OG", "Evo Guns", "Grandmaster", "Heroic", "Under ₹1,000")
    val sortOptions = listOf("Newest First", "Price: Low to High", "Price: High to Low", "Level: High to Low")

    val filteredList = remember(products, searchQuery, selectedFilter, sortBy) {
        val filtered = products.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                p.title.contains(searchQuery, ignoreCase = true) ||
                (p.accountUid?.contains(searchQuery, ignoreCase = true) == true) ||
                p.category.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "All IDs" -> true
                "Rare / OG" -> (p.level ?: 0) >= 65 || (p.evoGuns ?: 0) >= 2 || (p.badge?.contains("OG", ignoreCase = true) == true)
                "Evo Guns" -> (p.evoGuns ?: 0) > 0
                "Grandmaster" -> p.rank?.contains("Grandmaster", ignoreCase = true) == true
                "Heroic" -> p.rank?.contains("Heroic", ignoreCase = true) == true || p.rank == null
                "Under ₹1,000" -> p.price <= 1000
                else -> true
            }
            matchQuery && matchFilter
        }

        when (sortBy) {
            "Price: Low to High" -> filtered.sortedBy { it.price }
            "Price: High to Low" -> filtered.sortedByDescending { it.price }
            "Level: High to Low" -> filtered.sortedByDescending { it.level ?: 0 }
            else -> filtered
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
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section (Spans across both columns)
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Top Bar with Brand & Live Store Title (matching reference Image 2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurface)
                                    .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SportsEsports, contentDescription = "Logo", tint = PrimaryIndigoLight, modifier = Modifier.size(24.dp))
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
                                        fontSize = 16.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        color = AccentEmerald.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f))
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

                        // Right: Live WhatsApp Desk + Sync Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$cleanWa?text=${Uri.encode("Hello Support, I am browsing the GamerVault Explore Store.")}"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "WhatsApp: ${storeSettings.whatsapp}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(WhatsAppGreen)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = "WhatsApp", tint = Color.White, modifier = Modifier.size(17.dp))
                            }

                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        GamervoultRepository.fetchProducts()
                                        Toast.makeText(context, "Catalog updated", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(1.dp, DarkBorder, CircleShape)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = TextSecondary, modifier = Modifier.size(17.dp))
                            }
                        }
                    }

                    // Search Bar (matching reference Image 2)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by ID title or UID...", color = TextMuted, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter Pills
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filterOptions) { filter ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) PrimaryIndigo else DarkSurface,
                                border = if (!isSelected) BorderStroke(1.dp, DarkBorder) else null,
                                modifier = Modifier.clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Accounts Count & Interactive Sort Bar (matching reference Image 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredList.size} Free Fire Accounts",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurface,
                                border = BorderStroke(1.dp, DarkBorder),
                                modifier = Modifier.clickable { showSortMenu = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Sort: $sortBy", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                }
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                sortOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = if (sortBy == opt) PrimaryIndigoLight else TextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            sortBy = opt
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 2-Column Product Grid (Matching reference Image 2)
            items(filteredList) { item ->
                ProductGridCard(
                    product = item,
                    onCardClick = { selectedProductForDetail = item },
                    onBuyClick = { selectedProductForBuy = item }
                )
            }
        }

        // Account Full Photo & Specs Viewer Dialog (Triggered when clicking ANY ID)
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

        // UPI Payment Gateway Checkout Dialog
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
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
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
