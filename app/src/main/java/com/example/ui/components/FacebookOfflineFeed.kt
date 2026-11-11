package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FbBlue

/**
 * Authentic Facebook Mobile Home Page matching user's reference screenshot:
 * Centered "Facebook" header, Mobile number or email, Password, Log in,
 * Forgot password?, or divider, Create new account button, and language selector.
 */
@Composable
fun FacebookOfflineFeed(
    onRetryConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoggedIn by remember { mutableStateOf(false) }
    var loginStatusMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("facebook_home_screen"),
        color = Color(0xFFF0F2F5)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Facebook Top Header with centered logo (matching screenshot)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Facebook",
                    color = Color(0xFF1877F2),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            HorizontalDivider(color = Color(0xFFCED0D4), thickness = 0.8.dp)

            if (isLoggedIn) {
                // Logged-in feed state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Welcome to Facebook!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF1C1E21)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Logged in as $emailOrPhone",
                                fontSize = 13.sp,
                                color = Color(0xFF65676B)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { isLoggedIn = false; emailOrPhone = ""; password = "" },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE4E6EB)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Log Out", color = Color(0xFF050505), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                // Authentic Login & Home Form (matching screenshot)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mobile number or email label
                    Text(
                        text = "Mobile number or email",
                        fontSize = 14.sp,
                        color = Color(0xFF1C1E21),
                        fontWeight = FontWeight.Normal
                    )

                    // Mobile number or email field with info icon
                    OutlinedTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_email_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Info",
                                tint = Color(0xFF8A8D91),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F6F7),
                            unfocusedContainerColor = Color(0xFFF5F6F7),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    // Password label
                    Text(
                        text = "Password",
                        fontSize = 14.sp,
                        color = Color(0xFF1C1E21),
                        fontWeight = FontWeight.Normal
                    )

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_password_input"),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF5F6F7),
                            unfocusedContainerColor = Color(0xFFF5F6F7),
                            focusedBorderColor = Color(0xFF1877F2),
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Log in button (blue, full width)
                    Button(
                        onClick = {
                            if (emailOrPhone.isNotBlank() && password.isNotBlank()) {
                                isLoggedIn = true
                                loginStatusMessage = null
                            } else {
                                loginStatusMessage = "Please enter your mobile number/email and password."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("login_submit_btn"),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1877F2),
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Log in",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (loginStatusMessage != null) {
                        Text(
                            text = loginStatusMessage!!,
                            color = Color(0xFFE81123),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Forgot password link
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = Color(0xFF1877F2),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                loginStatusMessage = "To reset password, visit m.facebook.com/recover"
                            }
                        )
                    }

                    // "──────── or ────────" divider
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCCD0D5),
                            thickness = 1.dp
                        )
                        Text(
                            text = "or",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFFCCD0D5),
                            thickness = 1.dp
                        )
                    }

                    // Create new account button (green, centered, matching screenshot)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                loginStatusMessage = "To create a new account, visit m.facebook.com/reg"
                            },
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("create_new_account_btn"),
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF42B72A),
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Create new account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Languages list (matching screenshot)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "English (US)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1C1E21)
                        )
                        Text(
                            text = "বাংলা",
                            fontSize = 14.sp,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.clickable { }
                        )
                        Text(
                            text = "অসমীয়া",
                            fontSize = 14.sp,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.clickable { }
                        )
                        Text(
                            text = "Español",
                            fontSize = 14.sp,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.clickable { }
                        )
                        Text(
                            text = "हिन्दी",
                            fontSize = 14.sp,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.clickable { }
                        )
                        Text(
                            text = "Français (France)",
                            fontSize = 14.sp,
                            color = Color(0xFF1877F2),
                            modifier = Modifier.clickable { }
                        )
                        Text(
                            text = "More languages…",
                            fontSize = 13.sp,
                            color = Color(0xFF1877F2),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Retry online connection button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = onRetryConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connect to Live Facebook Web", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
