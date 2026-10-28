package com.gamervoult.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.gamervoult.app.data.ApiClient
import com.gamervoult.app.data.AppVersionCheckResponse
import com.gamervoult.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

enum class UpdateDownloadState {
    IDLE,
    DOWNLOADING,
    READY_TO_INSTALL,
    FAILED
}

fun triggerApkInstall(context: Context, apkFile: File) {
    try {
        if (!apkFile.exists()) {
            Toast.makeText(context, "APK file not found. Please re-download.", Toast.LENGTH_SHORT).show()
            return
        }

        // On Android 8.0+, verify REQUEST_INSTALL_PACKAGES permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Please toggle 'Allow from this source' to install update", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Installation error: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AppUpdateDialog(
    updateInfo: AppVersionCheckResponse,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var downloadState by remember { mutableStateOf(UpdateDownloadState.IDLE) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var downloadStatusText by remember { mutableStateOf("Preparing download...") }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun startDownload() {
        downloadState = UpdateDownloadState.DOWNLOADING
        errorMessage = null
        downloadProgress = 0f
        downloadStatusText = "Connecting to GamerVault server..."

        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val targetUrl = updateInfo.downloadUrl ?: "https://gamervoult-backend.vercel.app/api/v1/app/download-apk"
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("User-Agent", "GamerVault-Android-Updater")
                        .build()

                    val response = ApiClient.client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("Download failed: HTTP ${response.code}")
                    }

                    val body = response.body ?: throw Exception("Server returned empty payload")
                    val totalBytes = body.contentLength()
                    val updatesDir = File(context.getExternalFilesDir(null), "updates").apply { mkdirs() }
                    val targetFile = File(updatesDir, "gamervoult_v${updateInfo.latestVersionName}.apk")

                    val inputStream = body.byteStream()
                    val outputStream = FileOutputStream(targetFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val prog = totalRead.toFloat() / totalBytes.toFloat()
                            val mbRead = totalRead / (1024f * 1024f)
                            val mbTotal = totalBytes / (1024f * 1024f)
                            withContext(Dispatchers.Main) {
                                downloadProgress = prog
                                downloadStatusText = String.format("%.1f MB / %.1f MB (%d%%)", mbRead, mbTotal, (prog * 100).toInt())
                            }
                        } else {
                            val mbRead = totalRead / (1024f * 1024f)
                            withContext(Dispatchers.Main) {
                                downloadProgress = 0.5f
                                downloadStatusText = String.format("%.1f MB downloaded", mbRead)
                            }
                        }
                    }

                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    withContext(Dispatchers.Main) {
                        downloadedApkFile = targetFile
                        downloadState = UpdateDownloadState.READY_TO_INSTALL
                        downloadStatusText = "Download complete!"
                        // Automatically trigger installer
                        triggerApkInstall(context, targetFile)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    downloadState = UpdateDownloadState.FAILED
                    errorMessage = e.message ?: "Failed to download update"
                }
            }
        }
    }

    Dialog(
        onDismissRequest = {
            // App strictly requires matching server version to run; exit if dismissed
            (context as? android.app.Activity)?.finishAffinity()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, PrimaryIndigo.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Glowing Icon Header
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFF818CF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update Available",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "New Update Available!",
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 0.3.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = AccentEmerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})",
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        if (updateInfo.mandatoryUpdate) {
                            Surface(
                                color = AccentRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "MANDATORY",
                                    color = AccentRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Changelog Box
                    Surface(
                        color = DarkSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "What's New in this Build:",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val changelogText = if (updateInfo.changelog.isNotBlank()) {
                                updateInfo.changelog
                            } else {
                                "• Fixed black image thumbnails for video and YouTube accounts\n• Real-time server sync & in-app OTA APK updater\n• Local file system caching & storage permissions"
                            }

                            Text(
                                text = changelogText,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Dynamic Content based on State
                    when (downloadState) {
                        UpdateDownloadState.IDLE -> {
                            Button(
                                onClick = { startDownload() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download & Install Now",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            TextButton(
                                onClick = {
                                    (context as? android.app.Activity)?.finishAffinity()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Exit GamerVault",
                                        color = TextMuted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        UpdateDownloadState.DOWNLOADING -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = PrimaryIndigo,
                                    trackColor = DarkSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = downloadStatusText,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Downloading APK in background... The installer will launch automatically.",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        UpdateDownloadState.READY_TO_INSTALL -> {
                            Button(
                                onClick = {
                                    downloadedApkFile?.let { triggerApkInstall(context, it) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                                shape = RoundedCornerShape(14.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Install Update Now",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "APK file is ready in local cache. Tap to install.",
                                color = AccentEmerald,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        UpdateDownloadState.FAILED -> {
                            Text(
                                text = errorMessage ?: "Download encountered an error.",
                                color = AccentRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { startDownload() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Retry Download", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { (context as? android.app.Activity)?.finishAffinity() }) {
                                Text("Exit App", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
