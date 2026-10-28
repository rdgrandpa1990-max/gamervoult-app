package com.gamervoult.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.ProductItem
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.launch

enum class SortOrder(val label: String) {
    DEFAULT("Featured"),
    PRICE_LOW_HIGH("Price: Low → High"),
    PRICE_HIGH_LOW("Price: High → Low")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val products by GamervoultRepository.products.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var sortOrder by remember { mutableStateOf(SortOrder.DEFAULT) }
    var selectedProduct by remember { mutableStateOf<ProductItem?>(null) }
    var isPlacingOrder by remember { mutableStateOf(false) }

    val categories = remember(products) {
        val cats = products.map { it.category }.filter { it.isNotBlank() }.distinct()
        listOf("All") + cats
    }

    val displayedProducts = remember(products, searchQuery, selectedCategory, sortOrder) {
        var list = products.filter {
            (selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)) &&
            (searchQuery.isBlank() || 
             it.title.contains(searchQuery, ignoreCase = true) || 
             it.category.contains(searchQuery, ignoreCase = true) ||
             it.description.contains(searchQuery, ignoreCase = true))
        }

        list = when (sortOrder) {
            SortOrder.PRICE_LOW_HIGH -> list.sortedBy { it.price }
            SortOrder.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
            SortOrder.DEFAULT -> list
        }
        list
    }

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
                    text = "Catalog Explore",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Discover & Filter Official Database Accounts",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
            Surface(
                color = DarkSurface,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${displayedProducts.size} Items",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Filter by keyword, level, evo...", color = TextMuted, fontSize = 14.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) PrimaryIndigo else DarkSurface,
                    border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, DarkBorder) else null,
                    modifier = Modifier.clickable { selectedCategory = cat }
                ) {
                    Text(
                        text = cat,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sort Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Sort, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            SortOrder.values().forEach { order ->
                val isSelected = sortOrder == order
                Text(
                    text = order.label,
                    color = if (isSelected) AccentCyan else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { sortOrder = order }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Product Catalog List
        if (displayedProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No matching accounts found", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Try clearing your filters or search keywords", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(displayedProducts) { product ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProduct = product }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = DarkSurface,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = product.category,
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (product.level != null) {
                                        Text("Lvl ${product.level}", color = TextMuted, fontSize = 10.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = product.title,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (product.description.isNotBlank()) {
                                    Text(
                                        text = product.description,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${product.price}",
                                    color = AccentEmerald,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (product.isSold) "SOLD" else "AVAILABLE",
                                    color = if (product.isSold) AccentRed else AccentEmerald,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Product Detail Dialog
    selectedProduct?.let { prod ->
        AlertDialog(
            onDismissRequest = { if (!isPlacingOrder) selectedProduct = null },
            containerColor = DarkSurface,
            title = {
                Text(prod.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Category: ${prod.category}", color = PrimaryIndigoLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (prod.description.isNotBlank()) {
                        Text(prod.description, color = TextSecondary, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = DarkBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Authoritative Price:", color = TextMuted, fontSize = 13.sp)
                        Text("₹${prod.price}", color = AccentEmerald, fontSize = 20.sp, fontWeight = FontWeight.Black)
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
                                Toast.makeText(context, "Order ${order.orderCode} placed successfully!", Toast.LENGTH_LONG).show()
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
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    if (isPlacingOrder) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (prod.isSold) "Item Sold" else "Place Order")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProduct = null }, enabled = !isPlacingOrder) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }
}
