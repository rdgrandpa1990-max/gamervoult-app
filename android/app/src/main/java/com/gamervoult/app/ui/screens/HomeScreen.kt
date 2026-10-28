package com.gamervoult.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.ProductItem
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToOrders: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val products by GamervoultRepository.products.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedProduct by remember { mutableStateOf<ProductItem?>(null) }
    var orderCreatedMessage by remember { mutableStateOf<String?>(null) }
    var isPlacingOrder by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Dynamic category list from backend products
    val dynamicCategories = remember(products) {
        val cats = products.map { it.category }.filter { it.isNotBlank() }.distinct()
        listOf("All") + cats
    }

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter {
            (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || 
             it.title.contains(searchQuery, ignoreCase = true) || 
             it.category.contains(searchQuery, ignoreCase = true) ||
             it.description.contains(searchQuery, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkBackground)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "GAMER",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "VOULT",
                                color = PrimaryIndigoLight,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "Live Storefront • freefireofficial.in",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    GamervoultRepository.fetchProducts()
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

                        IconButton(
                            onClick = onNavigateToChat,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = "Support", tint = AccentCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search accounts, categories, items...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = PrimaryIndigo,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dynamicCategories) { cat ->
                        val isSelected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryIndigo else DarkSurface,
                            border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
                            modifier = Modifier.clickable { selectedCategory = cat }
                        ) {
                            Text(
                                text = cat,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Escrow Notice Banner
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AccentEmerald.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = "Escrow", tint = AccentEmerald)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Official Escrow Protection", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Real synchronized catalog directly from FreeFireOfficial database.", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.FilterListOff, contentDescription = "No results", tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No accounts found", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Try adjusting your search query or category filter", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(filteredProducts) { item ->
                    ProductCard(product = item, onClick = { selectedProduct = item })
                }
            }
        }
    }

    // Product Detail Bottom Sheet / Modal
    selectedProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { if (!isPlacingOrder) selectedProduct = null },
            containerColor = DarkSurface,
            title = {
                Column {
                    if (prod.badge != null) {
                        Surface(
                            color = PrimaryIndigo.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = prod.badge,
                                color = PrimaryIndigoLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    Text(prod.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (prod.description.isNotBlank()) {
                        Text(prod.description, color = TextSecondary, fontSize = 13.sp)
                        HorizontalDivider(color = DarkBorder)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Category", color = TextMuted, fontSize = 11.sp)
                            Text(prod.category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (prod.level != null) {
                            Column {
                                Text("Level", color = TextMuted, fontSize = 11.sp)
                                Text("${prod.level}", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                        if (prod.evoGuns != null) {
                            Column {
                                Text("Evo Guns", color = TextMuted, fontSize = 11.sp)
                                Text("${prod.evoGuns}", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider(color = DarkBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Authoritative Price", color = TextMuted, fontSize = 12.sp)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("₹${prod.price}", color = AccentEmerald, fontSize = 24.sp, fontWeight = FontWeight.Black)
                                if (prod.originalPrice != null && prod.originalPrice > prod.price) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("₹${prod.originalPrice}", color = TextMuted, fontSize = 14.sp, textDecoration = TextDecoration.LineThrough)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPlacingOrder = true
                        coroutineScope.launch {
                            val result = GamervoultRepository.createOrder(prod)
                            isPlacingOrder = false
                            if (result.isSuccess) {
                                val order = result.getOrThrow()
                                selectedProduct = null
                                orderCreatedMessage = "Order ${order.orderCode} created successfully! Amount: ₹${order.amount}. Check 'My Orders' to proceed."
                            } else {
                                Toast.makeText(
                                    context,
                                    result.exceptionOrNull()?.message ?: "Failed to place order",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isPlacingOrder && !prod.isSold,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (prod.isSold) "Sold Out" else "Place Real Order", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedProduct = null },
                    enabled = !isPlacingOrder
                ) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Order Success Dialog
    orderCreatedMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { orderCreatedMessage = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AccentEmerald)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Order Placed", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(msg, color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        orderCreatedMessage = null
                        onNavigateToOrders()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald)
                ) {
                    Text("View in Orders", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    product: ProductItem,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.badge != null) {
                    Surface(
                        color = PrimaryIndigo.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = product.badge,
                            color = PrimaryIndigoLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "CATALOG",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = product.category,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.title,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (product.level != null || product.evoGuns != null || product.likes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (product.level != null) {
                        Surface(color = DarkSurface, shape = RoundedCornerShape(8.dp)) {
                            Text("Lvl ${product.level}", color = TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    if (product.evoGuns != null) {
                        Surface(color = DarkSurface, shape = RoundedCornerShape(8.dp)) {
                            Text("🔥 ${product.evoGuns} Evo", color = AccentAmber, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    if (product.likes != null) {
                        Surface(color = DarkSurface, shape = RoundedCornerShape(8.dp)) {
                            Text("❤️ ${product.likes}", color = AccentCyan, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("₹${product.price}", color = AccentEmerald, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    if (product.originalPrice != null && product.originalPrice > product.price) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("₹${product.originalPrice}", color = TextMuted, fontSize = 13.sp, textDecoration = TextDecoration.LineThrough)
                    }
                }

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(if (product.isSold) "Sold" else "View", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
