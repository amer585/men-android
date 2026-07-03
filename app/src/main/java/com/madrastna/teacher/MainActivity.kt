package com.madrastna.teacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.ApiClient
import com.madrastna.teacher.data.ClassAssignment
import com.madrastna.teacher.data.StudentWithGrades
import com.madrastna.teacher.data.Teacher
import com.madrastna.teacher.data.TeacherRepository
import com.madrastna.teacher.ui.account.TeacherAccountScreen
import com.madrastna.teacher.ui.grades.ClassSelectScreen
import com.madrastna.teacher.ui.grades.GradesScreen
import com.madrastna.teacher.ui.login.LoginScreen
import com.madrastna.teacher.ui.theme.Gold400
import com.madrastna.teacher.ui.theme.MadrastnaTheme
import com.madrastna.teacher.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // One backend client + repository. No database token lives in the APK.
        val apiClient = ApiClient(applicationContext)
        val repository = TeacherRepository(apiClient)
        setContent {
            MadrastnaTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}

@Composable
private fun AppNavigation(repository: TeacherRepository) {
    // "staff" = legacy username grade-entry · "teacher" = teacher-account workflow.
    var mode by remember { mutableStateOf("staff") }

    // Staff / grade-entry session state.
    var teacher by remember { mutableStateOf<Teacher?>(null) }
    var classes by remember { mutableStateOf<List<ClassAssignment>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<ClassAssignment?>(null) }
    var students by remember { mutableStateOf<List<StudentWithGrades>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val currentTeacher = teacher
    val currentClass = selectedClass

    when {
        // ── Teacher-account workflow (email login / register / dashboard) ──
        mode == "teacher" -> TeacherAccountScreen(
            repository = repository,
            onBack = { mode = "staff" },
        )

        // ── Staff login ──
        currentTeacher == null -> LoginScreen(
            onLogin = { user, pass -> repository.login(user, pass) },
            onLoginSuccess = { t ->
                teacher = t
                loading = true
                Thread {
                    val cls = repository.getTeacherClasses(t.teacherId)
                    classes = cls
                    loading = false
                }.start()
            },
            onSwitchToTeacher = { mode = "teacher" },
        )

        // ── Class select ──
        currentClass == null -> Box(Modifier.fillMaxSize()) {
            ClassSelectScreen(
                teacher = currentTeacher,
                classes = classes,
                onClassClick = { cls ->
                    selectedClass = cls
                    loading = true
                    Thread {
                        val roster = repository.getStudentsInClass(
                            cls.gradeLevel,
                            cls.className,
                            currentTeacher.schoolName,
                            cls.subjectName,
                        )
                        students = roster
                        loading = false
                    }.start()
                },
                onLogout = {
                    repository.clearSession()
                    teacher = null
                    classes = emptyList()
                    selectedClass = null
                    students = emptyList()
                },
            )
            if (loading) LoadingOverlay("جارٍ تحميل الفصول…")
        }

        // ── Grade editing ──
        else -> Box(Modifier.fillMaxSize()) {
            GradesScreen(
                gradeLevel = currentClass.gradeLevel,
                className = currentClass.className,
                subjectName = currentClass.subjectName,
                students = students,
                onSaveGrades = { ssnToGrade ->
                    var allOk = true
                    for ((ssn, grade) in ssnToGrade) {
                        val student = students.find { it.ssn == ssn }
                        if (student != null) {
                            val updatedGrades = student.grades.toMutableMap()
                            updatedGrades[currentClass.subjectName] = grade
                            if (!repository.updateStudentGrades(ssn, updatedGrades)) allOk = false
                            student.grades[currentClass.subjectName] = grade
                        }
                    }
                    allOk
                },
                onBack = {
                    selectedClass = null
                    students = emptyList()
                },
            )
            if (loading) LoadingOverlay("جارٍ تحميل الطلاب…")
        }
    }
}

/** Full-screen translucent loading overlay with a gold spinner + caption. */
@Composable
private fun LoadingOverlay(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC060A13)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Gold400, strokeWidth = 3.dp, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(14.dp))
            Text(text, color = TextSecondary, fontSize = 14.sp)
        }
    }
}
