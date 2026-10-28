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
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.ui.navigation.Screen
import com.gamervoult.app.ui.screens.*
import com.gamervoult.app.ui.theme.DarkBackground
import com.gamervoult.app.ui.theme.DarkSurface
import com.gamervoult.app.ui.theme.GamervoultTheme
import com.gamervoult.app.ui.theme.PrimaryIndigo
import com.gamervoult.app.ui.theme.TextMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize session asynchronously using real backend & secure preferences
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
    val isInitializing by GamervoultRepository.isInitializing.collectAsStateWithLifecycle()
    val currentUser by GamervoultRepository.currentUser.collectAsStateWithLifecycle()

    if (isInitializing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = PrimaryIndigo,
                strokeWidth = 3.dp
            )
        }
    } else if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                // currentUser will automatically update and trigger navigation to MainAppScaffold
            }
        )
    } else {
        MainAppScaffold()
    }
}

@Composable
fun MainAppScaffold() {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

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
                    onNavigateToChat = { currentScreen = Screen.CHAT }
                )
                Screen.CHAT -> ChatScreen()
                Screen.EXPLORE -> ExploreScreen()
                Screen.ORDERS -> OrdersScreen()
                Screen.PROFILE -> ProfileScreen()
            }
        }
    }
}
