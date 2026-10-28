package com.gamervoult.app.ui.screens

import android.app.Activity
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamervoult.app.data.GamervoultRepository
import com.gamervoult.app.data.LicenseVerifyResult
import com.gamervoult.app.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0 = Sign In, 1 = Sign Up with License
    var activeTab by remember { mutableStateOf(0) }

    // Login Form State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // License Sign-Up Form State
    var licenseKey by remember { mutableStateOf("") }
    var isVerifyingKey by remember { mutableStateOf(false) }
    var keyVerifyResult by remember { mutableStateOf<LicenseVerifyResult?>(null) }
    var licenseName by remember { mutableStateOf("") }
    var licensePhone by remember { mutableStateOf("") }
    var licenseEmail by remember { mutableStateOf("") }
    var licensePassword by remember { mutableStateOf("") }
    var licensePasswordVisible by remember { mutableStateOf(false) }
    var dpdpConsent by remember { mutableStateOf(true) }
    var isLicenseSubmitting by remember { mutableStateOf(false) }

    // Native Google Sign-In Setup
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(ApiException::class.java)
                val userEmail = account.email
                val userName = account.displayName ?: userEmail?.substringBefore("@") ?: "Gamer"
                val photoUrl = account.photoUrl?.toString()

                if (!userEmail.isNullOrBlank()) {
                    coroutineScope.launch {
                        isGoogleLoading = true
                        errorMessage = null
                        val res = GamervoultRepository.loginWithGoogle(
                            context = context,
                            email = userEmail,
                            name = userName,
                            photoUrl = photoUrl
                        )
                        isGoogleLoading = false
                        if (res.isSuccess) {
                            Toast.makeText(context, "Welcome back, $userName!", Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                        } else {
                            errorMessage = res.exceptionOrNull()?.message ?: "Google sign-in failed"
                        }
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Google Sign-In: ${e.message}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Main Auth Card Container (matching reference Image 3 & website Login.jsx)
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Verified Escrow Banner Pill (synced with website Login.jsx)
                Surface(
                    color = AccentEmerald.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(13.dp))
                        Text(
                            text = "VERIFIED ESCROW & TRADER DESK",
                            color = AccentEmerald,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // GamerVault Logo Box
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = "GamerVault Logo",
                        tint = PrimaryIndigoLight,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "GAMERVAULT ESCROW",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Pro Trader Desk & Secure Escrow Portal",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Continue with Google Button (Native Google Sign-In)
                Button(
                    onClick = {
                        isGoogleLoading = true
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1F2937)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    enabled = !isGoogleLoading && !isLoading
                ) {
                    if (isGoogleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = PrimaryIndigo,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Google G Logo
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "G",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        color = Color(0xFF4285F4)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Continue with Google",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 0.3.sp,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // OR CONTINUE WITH EMAIL Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
                    Text(
                        text = "OR CONTINUE WITH EMAIL",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = DarkBorder)
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Tab Switcher: Sign In vs Sign Up with License (matching Image 3)
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        // Sign In Tab
                        Surface(
                            color = if (activeTab == 0) DarkCardElevated else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    activeTab = 0
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = "Sign In",
                                color = if (activeTab == 0) TextPrimary else TextSecondary,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }

                        // Sign Up with License Tab
                        Surface(
                            color = if (activeTab == 1) DarkCardElevated else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .clickable {
                                    activeTab = 1
                                    errorMessage = null
                                }
                        ) {
                            Text(
                                text = "Sign Up with License",
                                color = if (activeTab == 1) TextPrimary else TextSecondary,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .padding(vertical = 10.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error Banner
                if (errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        color = AccentRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Text(text = errorMessage ?: "", color = AccentRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // TAB 0: SIGN IN
                if (activeTab == 0) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column {
                            Text("Email Address", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = {
                                    email = it
                                    errorMessage = null
                                },
                                placeholder = { Text("your@email.com", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Column {
                            Text("Password", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                placeholder = { Text("••••••••", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                if (email.isBlank() || password.isBlank()) {
                                    errorMessage = "Please enter both email and password."
                                    return@Button
                                }
                                isLoading = true
                                errorMessage = null
                                coroutineScope.launch {
                                    val res = GamervoultRepository.login(context, email, password)
                                    isLoading = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "Invalid email or password."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Sign In to GAMERVAULT", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    // TAB 1: SIGN UP WITH LICENSE KEY (WITH LIVE VERIFICATION)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // License Key input + Check Key button
                        Column {
                            Text("License Key (Required)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = licenseKey,
                                    onValueChange = {
                                        licenseKey = it
                                        keyVerifyResult = null
                                        errorMessage = null
                                    },
                                    placeholder = { Text("e.g. zz5T-m7Ih-sm3v-iaQr", color = TextMuted, fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurface,
                                        unfocusedContainerColor = DarkSurface,
                                        focusedBorderColor = if (keyVerifyResult?.valid == true) AccentEmerald else PrimaryIndigo,
                                        unfocusedBorderColor = if (keyVerifyResult?.valid == true) AccentEmerald else DarkBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )

                                Button(
                                    onClick = {
                                        val clean = licenseKey.trim()
                                        if (clean.isBlank()) {
                                            errorMessage = "Please enter a license key to verify."
                                            return@Button
                                        }
                                        isVerifyingKey = true
                                        errorMessage = null
                                        coroutineScope.launch {
                                            val res = GamervoultRepository.verifyLicenseKey(clean)
                                            isVerifyingKey = false
                                            if (res.isSuccess) {
                                                keyVerifyResult = res.getOrNull()
                                            } else {
                                                errorMessage = res.exceptionOrNull()?.message ?: "Check failed"
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                                    enabled = !isVerifyingKey
                                ) {
                                    if (isVerifyingKey) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Check Key", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Live Key Status Badge
                            keyVerifyResult?.let { result ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (result.valid) AccentEmerald.copy(alpha = 0.15f) else AccentRed.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (result.valid) AccentEmerald.copy(alpha = 0.5f) else AccentRed.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (result.valid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (result.valid) AccentEmerald else AccentRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = result.message,
                                            color = if (result.valid) AccentEmerald else AccentRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Full Name
                        Column {
                            Text("Full Name", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = licenseName,
                                onValueChange = { licenseName = it },
                                placeholder = { Text("Gamer Name", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // Mobile Number
                        Column {
                            Text("Mobile Number (Optional)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = licensePhone,
                                onValueChange = { licensePhone = it },
                                placeholder = { Text("9876543210", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // Email Address
                        Column {
                            Text("Email Address", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = licenseEmail,
                                onValueChange = { licenseEmail = it },
                                placeholder = { Text("client@gmail.com", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // Password
                        Column {
                            Text("Password (Min 6 chars)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = licensePassword,
                                onValueChange = { licensePassword = it },
                                placeholder = { Text("••••••••", color = TextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                visualTransformation = if (licensePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { licensePasswordVisible = !licensePasswordVisible }) {
                                        Icon(
                                            imageVector = if (licensePasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = TextMuted
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurface,
                                    unfocusedContainerColor = DarkSurface,
                                    focusedBorderColor = PrimaryIndigo,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        // DPDP Consent Checkbox
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { dpdpConsent = !dpdpConsent }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = dpdpConsent,
                                onCheckedChange = { dpdpConsent = it },
                                colors = CheckboxDefaults.colors(checkedColor = AccentEmerald)
                            )
                            Text(
                                text = "I agree to Terms & DPDP data policy",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                val cleanKey = licenseKey.trim()
                                if (cleanKey.isBlank()) {
                                    errorMessage = "Please enter your License Key."
                                    return@Button
                                }
                                if (licenseEmail.isBlank() || licensePassword.isBlank()) {
                                    errorMessage = "Please enter both email and password."
                                    return@Button
                                }
                                if (licensePassword.length < 6) {
                                    errorMessage = "Password must be at least 6 characters."
                                    return@Button
                                }
                                if (!dpdpConsent) {
                                    errorMessage = "Please agree to the DPDP policy."
                                    return@Button
                                }

                                isLicenseSubmitting = true
                                errorMessage = null

                                coroutineScope.launch {
                                    val res = GamervoultRepository.signUpWithLicense(
                                        context = context,
                                        licenseKey = cleanKey,
                                        name = licenseName.ifBlank { "Client" },
                                        phone = licensePhone.trim(),
                                        email = licenseEmail.trim(),
                                        pass = licensePassword
                                    )
                                    isLicenseSubmitting = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Account activated! Welcome, ${res.getOrNull()?.name}", Toast.LENGTH_LONG).show()
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = res.exceptionOrNull()?.message ?: "License activation failed."
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            enabled = !isLicenseSubmitting
                        ) {
                            if (isLicenseSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Activate & Register", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Footer Notice (matching reference Image 3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🛡️ Guest checkout is fully supported! You do not need to create an account to purchase any Free Fire ID.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
