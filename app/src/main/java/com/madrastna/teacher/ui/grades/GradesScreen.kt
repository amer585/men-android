package com.madrastna.teacher.ui.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.GRADE_LABELS
import com.madrastna.teacher.data.StudentWithGrades
import com.madrastna.teacher.ui.components.CinematicBackground
import com.madrastna.teacher.ui.components.GlassCard
import com.madrastna.teacher.ui.components.GoldGradientButton
import com.madrastna.teacher.ui.components.StatusChip
import com.madrastna.teacher.ui.theme.*

@Composable
fun GradesScreen(
    gradeLevel: Int,
    className: String,
    subjectName: String,
    students: List<StudentWithGrades>,
    onSaveGrades: (Map<String, String>) -> Boolean,
    onBack: () -> Unit,
) {
    val drafts = remember(students) {
        mutableStateMapOf<String, String>().apply {
            students.forEach { s -> put(s.ssn, s.grades[subjectName] ?: "") }
        }
    }
    var saving by remember { mutableStateOf(false) }
    val savedCount = students.count { drafts[it.ssn]?.isNotBlank() == true }

    val filledDrafts = drafts.filter { it.value.isNotBlank() }.mapValues { it.value.trim() }
    val hasChanges = filledDrafts.isNotEmpty()

    Box(Modifier.fillMaxSize()) {
        CinematicBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Gold400)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            subjectName,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${GRADE_LABELS[gradeLevel] ?: "الصف $gradeLevel"} · $className",
                            color = Gold400,
                            fontSize = 12.sp,
                        )
                    }
                    StatusChip("$savedCount درجة", Gold300)
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "إدخال درجات الطلاب",
                        color = TextMuted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(students, key = { it.ssn }) { student ->
                    StudentGradeCard(
                        student = student,
                        subjectName = subjectName,
                        currentValue = drafts[student.ssn] ?: "",
                        onValueChange = { drafts[student.ssn] = it },
                    )
                }
                item { Spacer(Modifier.height(96.dp)) }
            }
        }

        // ── Sticky save bar ──
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            GoldGradientButton(
                text = if (saving) "جارٍ الحفظ…" else "حفظ الدرجات",
                onClick = {
                    if (!hasChanges || saving) return@GoldGradientButton
                    saving = true
                    val ok = onSaveGrades(filledDrafts)
                    saving = false
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasChanges && !saving,
                leadingIcon = {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Ink950, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentGradeCard(
    student: StudentWithGrades,
    subjectName: String,
    currentValue: String,
    onValueChange: (String) -> Unit,
) {
    val savedGrade = student.grades[subjectName]
    val isEdited = currentValue.isNotBlank() && currentValue != savedGrade

    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar circle
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(Gold500.copy(alpha = 0.22f), Gold700.copy(alpha = 0.08f)))
                    )
                    .border(1.dp, GlassBorder, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    student.nameAr.take(1),
                    color = Gold300,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    student.nameAr,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                val sub = StringBuilder()
                if (student.gender == "F") sub.append("أنثى") else sub.append("ذكر")
                if (savedGrade != null && savedGrade.isNotBlank()) sub.append(" · الحالية: $savedGrade")
                Text(sub.toString(), color = if (isEdited) Amber400 else TextMuted, fontSize = 12.sp)
            }

            // Grade input
            OutlinedTextField(
                value = currentValue,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }.take(5)) },
                modifier = Modifier.width(88.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("—", color = TextMuted) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Gold400,
                    unfocusedBorderColor = GlassBorder,
                    cursorColor = Gold400,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextSecondary,
                ),
            )
        }
    }
}
