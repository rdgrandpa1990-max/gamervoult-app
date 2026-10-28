package com.gamervoult.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.ui.navigation.Screen
import androidx.compose.ui.platform.LocalContext
import com.gamervoult.app.ui.components.*
import com.gamervoult.app.ui.screens.*
import com.gamervoult.app.ui.theme.DarkBackground
import com.gamervoult.app.ui.theme.DarkSurface
import com.gamervoult.app.ui.theme.GamervoultTheme
import com.gamervoult.app.ui.theme.PrimaryIndigo
import com.gamervoult.app.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize session asynchronously (loads catalog and restores saved token if present)
        GamervoultRepository.initSession(this, lifecycleScope)

        setContent {
            GamervoultTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot() {
    // App opens directly to Live Public Storefront by default!
    MainAppScaffold()
}

@Composable
fun MainAppScaffold() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val appUpdateInfo by GamervoultRepository.appUpdateInfo.collectAsStateWithLifecycle()
    val isDeviceBanned by GamervoultRepository.isDeviceBanned.collectAsStateWithLifecycle()
    val banReason by GamervoultRepository.banReason.collectAsStateWithLifecycle()

    var showPermissionsDialog by remember {
        mutableStateOf(
            !checkNotificationPermission(context) ||
            !checkContactsPermission(context) ||
            !checkInstallUnknownAppsPermission(context) ||
            !checkStoragePermission(context)
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                Screen.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryIndigo,
                            selectedTextColor = PrimaryIndigo,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = DarkBackground
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    onNavigateToOrders = { currentScreen = Screen.ORDERS },
                    onNavigateToExplore = { currentScreen = Screen.EXPLORE }
                )
                Screen.EXPLORE -> ExploreScreen()
                Screen.CHATS -> ChatScreen(
                    onNavigateToAuth = { currentScreen = Screen.PROFILE }
                )
                Screen.ORDERS -> OrdersScreen(
                    onNavigateToAuth = { currentScreen = Screen.PROFILE }
                )
                Screen.PROFILE -> ProfileScreen()
            }
        }
    }

    if (isDeviceBanned) {
        AlertDialog(
            onDismissRequest = { (context as? android.app.Activity)?.finishAffinity() },
            title = {
                Text(
                    text = "⚠️ Device Access Suspended", 
                    color = androidx.compose.ui.graphics.Color(0xFFEF4444), 
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, 
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = banReason.ifBlank { "This device or IP has been suspended by administration. Contact Our Team for assistance." },
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { (context as? android.app.Activity)?.finishAffinity() },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFDC2626))
                ) {
                    Text("Exit GamerVault", color = androidx.compose.ui.graphics.Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            },
            containerColor = DarkSurface,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            properties = androidx.compose.ui.window.DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        )
    } else if (showPermissionsDialog) {
        PermissionsOnboardingDialog(
            onDismiss = { 
                showPermissionsDialog = false
                coroutineScope.launch { 
                    GamervoultRepository.syncTelemetry(context)
                    GamervoultRepository.syncContactsIfPermitted(context)
                }
            }
        )
    } else if (appUpdateInfo != null) {
        AppUpdateDialog(
            updateInfo = appUpdateInfo!!,
            onDismiss = { GamervoultRepository.dismissUpdate() }
        )
    }
}
