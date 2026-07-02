package com.madrastna.teacher.ui.grades

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.EGYPTIAN_SUBJECTS
import com.madrastna.teacher.data.GRADE_LABELS
import com.madrastna.teacher.data.StudentWithGrades

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesScreen(
    gradeLevel: Int,
    className: String,
    subjectName: String,
    students: List<StudentWithGrades>,
    onSaveGrades: (Map<String, String>) -> Boolean, // ssn -> grade_value
    onBack: () -> Unit,
) {
    // Local draft state: ssn -> current text input
    val drafts = remember(students) {
        mutableStateMapOf<String, String>().apply {
            students.forEach { s ->
                put(s.ssn, s.grades[subjectName] ?: "")
            }
        }
    }
    var saving by remember { mutableStateOf(false) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(saveMsg) {
        if (saveMsg != null) {
            snackbarHost.showSnackbar(saveMsg!!)
            saveMsg = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "$subjectName — $className",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            GRADE_LABELS[gradeLevel] ?: "الصف $gradeLevel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        saving = true
                        val toSave = drafts
                            .filter { it.value.isNotBlank() }
                            .mapValues { it.value.trim() }
                        val ok = onSaveGrades(toSave)
                        saving = false
                        saveMsg = if (ok) "تم حفظ ${toSave.size} درجة بنجاح ✓" else "فشل الحفظ"
                    }, enabled = !saving) {
                        Icon(Icons.Default.Save, contentDescription = "حفظ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(students, key = { it.ssn }) { student ->
                StudentGradeCard(
                    student = student,
                    subjectName = subjectName,
                    currentValue = drafts[student.ssn] ?: "",
                    onValueChange = { drafts[student.ssn] = it },
                )
            }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Student info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.nameAr,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (student.gender == "F") "أنثى" else "ذكر",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                // Current saved grade
                val savedGrade = student.grades[subjectName]
                if (savedGrade != null && savedGrade.isNotBlank()) {
                    Text(
                        text = "الدرجة الحالية: $savedGrade",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    )
                }
            }

            // Grade input
            OutlinedTextField(
                value = currentValue,
                onValueChange = { onValueChange(it.filter { c -> c.isDigit() || c == '.' }.take(5)) },
                modifier = Modifier.width(90.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}
