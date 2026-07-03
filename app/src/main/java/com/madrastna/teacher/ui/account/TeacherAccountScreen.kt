package com.madrastna.teacher.ui.account

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.GRADE_LABELS
import com.madrastna.teacher.data.LinkedStudent
import com.madrastna.teacher.data.TeacherAccount
import com.madrastna.teacher.data.TeacherRepository
import com.madrastna.teacher.ui.components.BrandCrest
import com.madrastna.teacher.ui.components.CinematicBackground
import com.madrastna.teacher.ui.components.GlassCard
import com.madrastna.teacher.ui.components.GoldGradientButton
import com.madrastna.teacher.ui.theme.*
import org.json.JSONObject

/**
 * Full teacher-account surface: email login, self-registration (pending admin
 * approval) and a dashboard of linked students with read-only portal access.
 * All network calls run on background threads and post results back to UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherAccountScreen(
    repository: TeacherRepository,
    onBack: () -> Unit,
) {
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var account by remember { mutableStateOf<TeacherAccount?>(null) }
    var isRegister by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // form fields
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    // dashboard state
    var students by remember { mutableStateOf<List<LinkedStudent>>(emptyList()) }
    var addStudentId by remember { mutableStateOf("") }
    var portal by remember { mutableStateOf<JSONObject?>(null) }
    var portalFor by remember { mutableStateOf<String?>(null) }

    val current = account

    fun loadStudents() {
        Thread {
            val list = repository.getTeacherStudents()
            mainHandler.post { students = list }
        }.start()
    }

    fun doLogin() {
        if (email.isBlank() || password.isBlank()) {
            message = "أدخل البريد الإلكتروني وكلمة المرور"; return
        }
        loading = true; message = null
        Thread {
            val res = repository.teacherLogin(email.trim(), password)
            mainHandler.post {
                loading = false
                if (res.ok && res.account != null) {
                    account = res.account
                    portal = null; portalFor = null
                    loadStudents()
                } else {
                    message = res.message
                }
            }
        }.start()
    }

    fun doRegister() {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            message = "الاسم والبريد وكلمة المرور مطلوبة"; return
        }
        loading = true; message = null
        Thread {
            val res = repository.teacherRegister(
                name.trim(), email.trim(), password,
                phone.trim().ifEmpty { null },
                subject.trim().ifEmpty { null },
            )
            mainHandler.post {
                loading = false
                message = res.message
                if (res.ok) {
                    // Created pending approval — switch back to login.
                    isRegister = false
                    password = ""
                }
            }
        }.start()
    }

    fun doLink() {
        val id = addStudentId.trim()
        if (id.isEmpty()) { message = "أدخل الرقم القومي للطالب"; return }
        loading = true; message = null
        Thread {
            val res = repository.linkStudent(id)
            mainHandler.post {
                loading = false
                message = res.message
                if (res.ok) {
                    addStudentId = ""
                    loadStudents()
                }
            }
        }.start()
    }

    fun showPortal(s: LinkedStudent) {
        portal = null; portalFor = s.studentId
        Thread {
            val p = repository.getStudentPortal(s.studentId, s.gradeLevel)
            mainHandler.post { portal = p }
        }.start()
    }

    fun logout() {
        repository.clearSession()
        account = null
        students = emptyList()
        portal = null; portalFor = null
        isRegister = false
        email = ""; password = ""; name = ""; phone = ""; subject = ""
    }

    Box(Modifier.fillMaxSize()) {
        CinematicBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))
            BrandCrest(size = 84.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                "بوابة المعلم",
                color = Gold300,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (current == null) "حساب المعلم" else current.name,
                color = TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(22.dp))

            if (current == null) {
                // ── LOGIN / REGISTER ──
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(Modifier.padding(24.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            FilterChipTab("تسجيل الدخول", !isRegister) { isRegister = false; message = null }
                            Spacer(Modifier.width(10.dp))
                            FilterChipTab("حساب جديد", isRegister) { isRegister = true; message = null }
                        }
                        Spacer(Modifier.height(18.dp))

                        if (isRegister) {
                            Field(name, { name = it }, "الاسم", Icons.Default.Person)
                            Spacer(Modifier.height(12.dp))
                        }
                        Field(email, { email = it }, "البريد الإلكتروني", Icons.Default.AlternateEmail,
                            KeyboardType.Email)
                        Spacer(Modifier.height(12.dp))
                        Field(password, { password = it }, "كلمة المرور", Icons.Default.Lock,
                            KeyboardType.Password, secret = true)
                        if (isRegister) {
                            Spacer(Modifier.height(12.dp))
                            Field(phone, { phone = it }, "رقم الهاتف", Icons.Default.Phone,
                                KeyboardType.Phone)
                            Spacer(Modifier.height(12.dp))
                            Field(subject, { subject = it }, "المادة الدراسية", Icons.Default.School)
                        }

                        MessageBanner(message)

                        Spacer(Modifier.height(20.dp))
                        GoldGradientButton(
                            text = if (loading) "جارٍ المعالجة…" else if (isRegister) "تسجيل" else "دخول",
                            onClick = { if (!loading) if (isRegister) doRegister() else doLogin() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading,
                        )
                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                            Text("دخول الموظفين (اسم المستخدم)", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                // ── DASHBOARD ──
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 28.dp) {
                    Column(Modifier.padding(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(current.name, color = TextPrimary,
                                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(current.email, color = TextSecondary, fontSize = 13.sp)
                                current.subject?.let {
                                    Text("المادة: $it", color = TextMuted, fontSize = 12.sp)
                                }
                                current.phone?.let {
                                    Text("الهاتف: $it", color = TextMuted, fontSize = 12.sp)
                                }
                            }
                            VerificationBadge(current.isVerified)
                        }

                        if (!current.isVerified) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "حسابك بانتظار موافقة الإدارة. بعض العمليات قد تكون محدودة حتى الاعتماد.",
                                color = Rose400, fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Rose400.copy(alpha = 0.10f))
                                    .padding(12.dp),
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        Text("طلابي", color = Gold300, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))

                        // Add a student (imported by the backend from the student DB).
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = addStudentId,
                                onValueChange = { addStudentId = it },
                                label = { Text("إضافة طالب (الرقم القومي)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = fieldColors(),
                                shape = RoundedCornerShape(14.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Button(
                                onClick = { doLink() },
                                enabled = !loading,
                                colors = ButtonDefaults.buttonColors(containerColor = Gold400),
                            ) { Text("إضافة", color = Color(0xFF0B0F1A)) }
                        }

                        MessageBanner(message)

                        Spacer(Modifier.height(14.dp))
                        if (students.isEmpty()) {
                            Text(
                                "لا يوجد طلاب بعد. أضف طالباً برقمه القومي ليستورده الخادم من قاعدة بيانات الطلاب.",
                                color = TextMuted, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 12.dp),
                            )
                        } else {
                            students.forEach { s -> StudentRow(s, portalFor, portal) { showPortal(s) } }
                        }

                        Spacer(Modifier.height(18.dp))
                        TextButton(onClick = { logout() }, modifier = Modifier.fillMaxWidth()) {
                            Text("تسجيل الخروج", color = Rose400)
                        }
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        if (loading) LoadingOverlay()
    }
}

@Composable
private fun StudentRow(
    s: LinkedStudent,
    portalFor: String?,
    portal: JSONObject?,
    onShow: () -> Unit,
) {
    val gradeLabel = GRADE_LABELS[s.gradeLevel] ?: "الصف ${s.gradeLevel}"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(GlassBorder.copy(alpha = 0.25f))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.nameAr ?: "طالب", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text("$gradeLabel · ${s.className ?: "—"}", color = TextMuted, fontSize = 12.sp)
                s.schoolName?.let { Text(it, color = TextMuted, fontSize = 11.sp) }
            }
            TextButton(onClick = onShow) {
                Text(if (portalFor == s.studentId) "إخفاء" else "عرض الملف", color = Gold400)
            }
        }
        if (portalFor == s.studentId) {
            Spacer(Modifier.height(8.dp))
            PortalBlock(portal)
        }
    }
}

@Composable
private fun PortalBlock(portal: JSONObject?) {
    if (portal == null) {
        Text("جارٍ تحميل الملف…", color = TextMuted, fontSize = 13.sp)
        return
    }
    val student = portal.optJSONObject("student")
    val average = portal.optString("average").ifEmpty { null }
    val grades = portal.optJSONArray("grades")
    Column {
        student?.optString("student_name_ar")?.takeIf { it.isNotEmpty() }?.let {
            Text(it, color = Gold300, fontWeight = FontWeight.Bold)
        }
        average?.let { Text("المعدل: $it", color = TextSecondary, fontSize = 13.sp) }
        if (grades != null && grades.length() > 0) {
            Spacer(Modifier.height(6.dp))
            for (i in 0 until grades.length()) {
                val g = grades.optJSONObject(i) ?: continue
                val subj = g.optString("subject_name").ifEmpty { continue }
                Row(Modifier.fillMaxWidth()) {
                    Text(subj, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(g.optString("grade_value").ifEmpty { "—" }, color = TextPrimary, fontSize = 13.sp)
                }
            }
        } else {
            Text("لا توجد درجات مسجّلة.", color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun VerificationBadge(verified: Boolean) {
    val (text, color) = if (verified) "معتمد" to Emerald400 else "بانتظار الاعتماد" to Amber400
    Text(
        text,
        color = Color(0xFF0B0F1A),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun FilterChipTab(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Gold400 else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp)),
    ) {
        TextButton(onClick = onClick) {
            Text(label, color = if (selected) Color(0xFF0B0F1A) else TextSecondary, fontSize = 13.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    secret: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Gold400) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = fieldColors(),
        shape = RoundedCornerShape(16.dp),
    )
}

@Composable
private fun MessageBanner(message: String?) {
    if (message.isNullOrBlank()) return
    Spacer(Modifier.height(14.dp))
    Text(
        message,
        color = TextSecondary,
        fontSize = 13.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gold400.copy(alpha = 0.10f))
            .padding(12.dp),
    )
}

@Composable
private fun LoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060A13)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Gold400, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text("جارٍ التحميل…", color = TextSecondary, fontSize = 14.sp)
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
