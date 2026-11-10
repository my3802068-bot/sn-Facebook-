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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FbBlue

/**
 * Pixel-perfect replica of the Facebook mobile login screen
 * from the user's screenshot.
 */
@Composable
fun FacebookLoginScreen(
    onLoginSuccess: (email: String) -> Unit,
    onForgotPassword: () -> Unit,
    onCreateAccount: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var emailOrPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("English (US)") }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("facebook_login_screen"),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Centered "Facebook"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Facebook",
                    color = Color(0xFF4B4F56),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.3).sp
                )
            }
            HorizontalDivider(color = Color(0xFFE5E5E5), thickness = 0.8.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                // "Mobile number or email" Label
                Text(
                    text = "Mobile number or email",
                    color = Color(0xFF1C1E21),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Input Box 1: Mobile number or email
                OutlinedTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("fb_login_email_input"),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F6F7),
                        unfocusedContainerColor = Color(0xFFF5F6F7),
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = Color(0xFFCCD0D5),
                        focusedTextColor = Color(0xFF1C1E21),
                        unfocusedTextColor = Color(0xFF1C1E21)
                    ),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF8D949E),
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // "Password" Label
                Text(
                    text = "Password",
                    color = Color(0xFF1C1E21),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Input Box 2: Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("fb_login_password_input"),
                    shape = RoundedCornerShape(4.dp),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F6F7),
                        unfocusedContainerColor = Color(0xFFF5F6F7),
                        focusedBorderColor = Color(0xFF1877F2),
                        unfocusedBorderColor = Color(0xFFCCD0D5),
                        focusedTextColor = Color(0xFF1C1E21),
                        unfocusedTextColor = Color(0xFF1C1E21)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onLoginSuccess(emailOrPhone.ifEmpty { "user@facebook.com" }) }
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // "Log in" Button (Blue)
                Button(
                    onClick = {
                        onLoginSuccess(emailOrPhone.ifEmpty { "user@facebook.com" })
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("fb_login_submit_btn"),
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

                Spacer(modifier = Modifier.height(16.dp))

                // "Forgot password?" Link
                Text(
                    text = "Forgot password?",
                    color = Color(0xFF1877F2),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clickable { onForgotPassword() }
                )

                Spacer(modifier = Modifier.height(28.dp))

                // "or" Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFCCD0D5),
                        thickness = 1.dp
                    )
                    Text(
                        text = "or",
                        color = Color(0xFF4B4F56),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color(0xFFCCD0D5),
                        thickness = 1.dp
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // "Create new account" Button (Green)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onCreateAccount,
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF42B72A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("fb_create_account_btn")
                    ) {
                        Text(
                            text = "Create new account",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))

                // Language Selector Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "English (US)",
                        color = Color(0xFF1C1E21),
                        fontSize = 14.sp,
                        fontWeight = if (selectedLanguage == "English (US)") FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.clickable {
                            selectedLanguage = "English (US)"
                            onSelectLanguage("English (US)")
                        }
                    )

                    val otherLangs = listOf(
                        "বাংলা",
                        "অসমীয়া",
                        "Español",
                        "हिन्दी",
                        "Français (France)",
                        "More languages…"
                    )

                    otherLangs.forEach { lang ->
                        Text(
                            text = lang,
                            color = Color(0xFF1877F2),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                if (lang != "More languages…") selectedLanguage = lang
                                onSelectLanguage(lang)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }
}
