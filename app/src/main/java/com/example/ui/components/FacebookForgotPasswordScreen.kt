package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FbBlue

enum class ForgotStep {
    SEARCH,
    SELECT_METHOD,
    ENTER_CODE,
    SET_NEW_PASSWORD,
    SUCCESS
}

@Composable
fun FacebookForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    onOpenWebRecovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableStateOf(ForgotStep.SEARCH) }
    var searchInput by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("sms") }
    var codeInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // Facebook Top Header Bar with Back Arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier.testTag("forgot_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1C1E21)
                )
            }
            Text(
                text = "facebook",
                color = FbBlue,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = (-1).sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        HorizontalDivider(color = Color(0xFFE4E6EB), thickness = 1.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (step) {
                // STEP 1: Search Account by Mobile or Email
                ForgotStep.SEARCH -> {
                    Text(
                        text = "Find Your Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Text(
                        text = "Enter your mobile number or email address associated with your account to search.",
                        fontSize = 14.sp,
                        color = Color(0xFF65676B)
                    )

                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = {
                            searchInput = it
                            errorMessage = null
                        },
                        label = { Text("Mobile number or email") },
                        placeholder = { Text("e.g. 017xxxxxxxx or name@email.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            if (searchInput.trim().isBlank()) {
                                errorMessage = "Please enter your mobile number or email address."
                            } else {
                                isSearching = true
                                errorMessage = null
                                // Advance to method selection
                                step = ForgotStep.SELECT_METHOD
                                isSearching = false
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("forgot_search_submit_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }

                    // Web Official Option
                    OutlinedButton(
                        onClick = onOpenWebRecovery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open Facebook Official Recovery Web", color = Color(0xFF1C1E21), fontSize = 13.sp)
                    }
                }

                // STEP 2: Choose Recovery Method (SMS / WhatsApp / Email)
                ForgotStep.SELECT_METHOD -> {
                    Text(
                        text = "Choose a way to log in",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Text(
                        text = "We found your account. Where would you like us to send the 6-digit confirmation code?",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )

                    // Profile found preview card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F2F5)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FbBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("SN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Column {
                                Text("Facebook User", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1C1E21))
                                Text(searchInput, fontSize = 12.sp, color = Color(0xFF65676B))
                            }
                        }
                    }

                    // Method Options
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMethod == "sms") Color(0xFFE7F3FF) else Color(0xFFF8F9FA))
                                .clickable { selectedMethod = "sms" }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == "sms",
                                onClick = { selectedMethod = "sms" },
                                colors = RadioButtonDefaults.colors(selectedColor = FbBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = FbBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Send code via SMS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Receive SMS code on your mobile", fontSize = 11.sp, color = Color(0xFF65676B))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedMethod == "email") Color(0xFFE7F3FF) else Color(0xFFF8F9FA))
                                .clickable { selectedMethod = "email" }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == "email",
                                onClick = { selectedMethod = "email" },
                                colors = RadioButtonDefaults.colors(selectedColor = FbBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = FbBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Send code via Email", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Receive verification link in email", fontSize = 11.sp, color = Color(0xFF65676B))
                            }
                        }
                    }

                    Button(
                        onClick = { step = ForgotStep.ENTER_CODE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("forgot_continue_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    TextButton(
                        onClick = { step = ForgotStep.SEARCH },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Not your account?", color = Color(0xFF65676B))
                    }
                }

                // STEP 3: Enter 6-digit Code
                ForgotStep.ENTER_CODE -> {
                    Text(
                        text = "Enter 6-digit code",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Text(
                        text = "Please check your phone/email for a text message with your 6-digit verification code.",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 6) codeInput = it.filter { c -> c.isDigit() } },
                        label = { Text("6-digit code") },
                        placeholder = { Text("e.g. 123456") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_code_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )

                    Button(
                        onClick = {
                            if (codeInput.length == 6 || codeInput.isNotBlank()) {
                                step = ForgotStep.SET_NEW_PASSWORD
                            }
                        },
                        enabled = codeInput.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                    ) {
                        Text("Confirm Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // STEP 4: Choose a New Password
                ForgotStep.SET_NEW_PASSWORD -> {
                    Text(
                        text = "Choose a New Password",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF1C1E21)
                    )
                    Text(
                        text = "Create a new password that is at least 6 characters long. A strong password has letters and numbers.",
                        fontSize = 13.sp,
                        color = Color(0xFF65676B)
                    )

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        placeholder = { Text("Enter new password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("forgot_new_password_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )

                    Button(
                        onClick = {
                            if (newPassword.length >= 6) {
                                step = ForgotStep.SUCCESS
                            }
                        },
                        enabled = newPassword.length >= 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                    ) {
                        Text("Change Password & Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // STEP 5: Success
                ForgotStep.SUCCESS -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "Password Reset Successful!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1C1E21)
                        )
                        Text(
                            text = "Your password has been successfully updated. You can now use your new password to log in.",
                            fontSize = 13.sp,
                            color = Color(0xFF65676B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onBackToLogin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                        ) {
                            Text("Back to Facebook Home", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
