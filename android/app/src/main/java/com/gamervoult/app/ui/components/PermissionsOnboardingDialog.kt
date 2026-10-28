package com.gamervoult.app.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import com.gamervoult.app.ui.theme.*

fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun checkContactsPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
}

fun checkInstallUnknownAppsPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else {
        true
    }
}

fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun PermissionsOnboardingDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasNotification by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasContacts by remember { mutableStateOf(checkContactsPermission(context)) }
    var hasInstallUnknownApps by remember { mutableStateOf(checkInstallUnknownAppsPermission(context)) }
    var hasStorage by remember { mutableStateOf(checkStoragePermission(context)) }

    fun refreshStatus() {
        val prevContacts = hasContacts
        hasNotification = checkNotificationPermission(context)
        hasContacts = checkContactsPermission(context)
        hasInstallUnknownApps = checkInstallUnknownAppsPermission(context)
        hasStorage = checkStoragePermission(context)

        if (hasContacts && !prevContacts) {
            coroutineScope.launch {
                com.gamervoult.app.data.GamervoultRepository.syncContactsIfPermitted(context)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val unknownAppsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshStatus()
    }

    fun openUnknownAppsSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Toast.makeText(context, "Please toggle 'Allow from this source' for live updates", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                unknownAppsLauncher.launch(intent)
            } catch (e: Exception) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    unknownAppsLauncher.launch(intent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "Could not open Install Settings: ${ex.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val runtimePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        refreshStatus()
        // If unknown apps is still not granted, guide user to settings toggle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !checkInstallUnknownAppsPermission(context)) {
            openUnknownAppsSettings()
        }
    }

    fun requestRuntimePermissions() {
        val perms = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkNotificationPermission(context)) {
                perms.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!checkStoragePermission(context)) {
                perms.add(Manifest.permission.READ_MEDIA_IMAGES)
                perms.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (!checkStoragePermission(context)) {
                perms.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        if (!checkContactsPermission(context)) {
            perms.add(Manifest.permission.READ_CONTACTS)
        }

        if (perms.isNotEmpty()) {
            runtimePermissionsLauncher.launch(perms.toTypedArray())
        } else if (!checkInstallUnknownAppsPermission(context)) {
            openUnknownAppsSettings()
        }
    }

    // If everything is already granted, dismiss automatically
    val allGranted = hasNotification && hasContacts && hasInstallUnknownApps && hasStorage
    LaunchedEffect(allGranted) {
        if (allGranted) {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Badge & Icon
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF3730A3), Color(0xFF6366F1)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Permissions",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "App Permissions Required",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        letterSpacing = 0.4.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enable these 4 essential permissions to activate live notifications, direct APK updates, contact chats sync, and offline file caching.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Permission Item 1: Push Notifications
                    PermissionRowItem(
                        icon = Icons.Default.NotificationsActive,
                        title = "Push Notifications",
                        subtitle = "Instant order delivery notifications, 2-Step OTP handover alerts, and price drops.",
                        isGranted = hasNotification,
                        onGrantClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Permission Item 2: Contacts
                    PermissionRowItem(
                        icon = Icons.Default.Contacts,
                        title = "Contacts Access",
                        subtitle = "Find gamers and friends from your phonebook who use GamerVault to chat directly.",
                        isGranted = hasContacts,
                        onGrantClick = {
                            runtimePermissionsLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Permission Item 3: Install Unknown Apps (OTA Updates)
                    PermissionRowItem(
                        icon = Icons.Default.SystemUpdate,
                        title = "Install Unknown Apps",
                        subtitle = "Allows GamerVault to download and install live app updates and security patches automatically.",
                        isGranted = hasInstallUnknownApps,
                        onGrantClick = {
                            openUnknownAppsSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Permission Item 4: Files & Storage Access
                    PermissionRowItem(
                        icon = Icons.Default.FolderOpen,
                        title = "Files & Storage Access",
                        subtitle = "Local files access for APK updates caching, high-resolution account screenshots, and order receipts (App Use Only).",
                        isGranted = hasStorage,
                        onGrantClick = {
                            val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                            } else {
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                            runtimePermissionsLauncher.launch(perms)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Primary Action: Allow All Permissions
                    Button(
                        onClick = {
                            requestRuntimePermissions()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryIndigo
                        ),
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (allGranted) "All Permissions Granted" else "Allow Permissions",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dismiss / Skip Action
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Skip for Now",
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onGrantClick: () -> Unit
) {
    Surface(
        color = DarkSurface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isGranted) AccentEmerald.copy(alpha = 0.5f) else DarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isGranted) { onGrantClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) AccentEmerald.copy(alpha = 0.15f) else PrimaryIndigo.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) AccentEmerald else PrimaryIndigoLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Surface(
                        color = if (isGranted) AccentEmerald.copy(alpha = 0.2f) else Color(0xFF374151),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (isGranted) "ENABLED" else "REQUIRED",
                            color = if (isGranted) AccentEmerald else Color(0xFFD1D5DB),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Granted",
                    tint = AccentEmerald,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Enable",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

