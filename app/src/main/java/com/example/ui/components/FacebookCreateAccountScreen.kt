package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

@Composable
fun FacebookCreateAccountScreen(
    onBackToLogin: () -> Unit,
    onOpenWebRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var birthDate by remember { mutableStateOf("15 Jan 2000") }
    var gender by remember { mutableStateOf("Male") }
    var isSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // Facebook Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier.testTag("create_account_back_btn")
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

        if (isSubmitted) {
            // Success Screen
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "Welcome to Facebook, $firstName!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = "Your account has been created successfully. We've sent a confirmation message to $mobileOrEmail.",
                    fontSize = 14.sp,
                    color = Color(0xFF65676B),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onBackToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FbBlue)
                ) {
                    Text("Go to Facebook Home", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        } else {
            // Registration Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Create a New Account",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF1C1E21)
                )
                Text(
                    text = "It's quick and easy. Join over 3 billion people on Facebook.",
                    fontSize = 13.sp,
                    color = Color(0xFF65676B)
                )

                // First and Last Name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            errorMessage = null
                        },
                        label = { Text("First name") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_first_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            errorMessage = null
                        },
                        label = { Text("Surname") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reg_last_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )
                }

                // Mobile or Email
                OutlinedTextField(
                    value = mobileOrEmail,
                    onValueChange = {
                        mobileOrEmail = it
                        errorMessage = null
                    },
                    label = { Text("Mobile number or email address") },
                    placeholder = { Text("e.g. 017xxxxxxxx or name@email.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reg_contact_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FbBlue,
                        unfocusedBorderColor = Color(0xFFCCD0D5)
                    )
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("New password") },
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
                        .testTag("reg_password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FbBlue,
                        unfocusedBorderColor = Color(0xFFCCD0D5)
                    )
                )

                // Date of Birth
                Column {
                    Text(
                        text = "Date of Birth",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF65676B)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FbBlue,
                            unfocusedBorderColor = Color(0xFFCCD0D5)
                        )
                    )
                }

                // Gender Selection
                Column {
                    Text(
                        text = "Gender",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF65676B)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Female", "Male", "Custom").forEach { opt ->
                            val isSelected = gender == opt
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFFE7F3FF) else Color(0xFFF8F9FA))
                                    .clickable { gender = opt }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { gender = opt },
                                    colors = RadioButtonDefaults.colors(selectedColor = FbBlue),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "By clicking Sign Up, you agree to our Terms, Privacy Policy and Cookies Policy. You may receive SMS notifications from us.",
                    fontSize = 11.sp,
                    color = Color(0xFF8A8D91),
                    lineHeight = 15.sp
                )

                // Sign Up Button
                Button(
                    onClick = {
                        when {
                            firstName.trim().isBlank() -> errorMessage = "Please enter your first name."
                            lastName.trim().isBlank() -> errorMessage = "Please enter your surname."
                            mobileOrEmail.trim().isBlank() -> errorMessage = "Please enter your mobile number or email."
                            password.length < 6 -> errorMessage = "Password must be at least 6 characters long."
                            else -> {
                                errorMessage = null
                                isSubmitted = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("reg_submit_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42B72A)) // Facebook Green Sign Up
                ) {
                    Text("Sign Up", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                }

                // Official Web Alternative
                OutlinedButton(
                    onClick = onOpenWebRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Open Official Facebook Web Sign-Up", color = Color(0xFF1C1E21), fontSize = 13.sp)
                }

                TextButton(
                    onClick = onBackToLogin,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Already have an account? Log In", color = FbBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
