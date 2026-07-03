package com.madrastna.teacher.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.Teacher
import com.madrastna.teacher.ui.components.BrandCrest
import com.madrastna.teacher.ui.components.CinematicBackground
import com.madrastna.teacher.ui.components.GlassCard
import com.madrastna.teacher.ui.components.GoldGradientButton
import com.madrastna.teacher.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: (Teacher) -> Unit,
    onLogin: (String, String) -> Teacher?,
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(24.dp))

            // ── Brand crest ──
            BrandCrest(size = 96.dp)

            Spacer(Modifier.height(18.dp))

            // ── Title block ──
            Text(
                text = "إدارتنا الشاملة",
                style = MaterialTheme.typography.headlineLarge,
                color = Gold300,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "بوابة المعلمين",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(36.dp))

            // ── Glass login card ──
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
            ) {
                Column(Modifier.padding(26.dp)) {
                    Text(
                        text = "تسجيل الدخول",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Text(
                        text = "أدخل بياناتك للمتابعة",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )

                    Spacer(Modifier.height(24.dp))

                    // Username
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("اسم المستخدم") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Gold400)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors(),
                        shape = RoundedCornerShape(16.dp),
                    )

                    Spacer(Modifier.height(16.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Gold400)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(16.dp),
                    )

                    // Error
                    if (error != null) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = error!!,
                            color = Rose400,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Rose400.copy(alpha = 0.10f))
                                .padding(12.dp),
                        )
                    }

                    Spacer(Modifier.height(26.dp))

                    GoldGradientButton(
                        text = if (loading) "جارٍ الدخول…" else "دخول",
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                error = "يرجى إدخال جميع البيانات"
                                return@GoldGradientButton
                            }
                            loading = true
                            error = null
                            val result = onLogin(username.trim(), password)
                            loading = false
                            if (result != null) onLoginSuccess(result)
                            else error = "اسم المستخدم أو كلمة المرور غير صحيحة"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "آمن · مشفّر · معتمد",
                color = TextMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun fieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    focusedBorderColor = Gold400,
    unfocusedBorderColor = GlassBorder,
    focusedLabelColor = Gold400,
    unfocusedLabelColor = TextMuted,
    cursorColor = Gold400,
    focusedLeadingIconColor = Gold400,
    unfocusedLeadingIconColor = Gold500,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextSecondary,
)
