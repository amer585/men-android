package com.madrastna.teacher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import com.madrastna.teacher.data.*
import com.madrastna.teacher.ui.theme.MadrastnaTheme
import com.madrastna.teacher.ui.login.LoginScreen
import com.madrastna.teacher.ui.grades.ClassSelectScreen
import com.madrastna.teacher.ui.grades.GradesScreen

class MainActivity : ComponentActivity() {

    private val tursoClient by lazy { TursoClient() }
    private val repository by lazy { TeacherRepository(tursoClient) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MadrastnaTheme {
                AppNavigation(
                    repository = repository,
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(repository: TeacherRepository) {
    var teacher by remember { mutableStateOf<Teacher?>(null) }
    var classes by remember { mutableStateOf<List<ClassAssignment>>(emptyList()) }
    var selectedClass by remember { mutableStateOf<ClassAssignment?>(null) }
    var students by remember { mutableStateOf<List<StudentWithGrades>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val currentTeacher = teacher
    val currentClass = selectedClass

    if (currentTeacher == null) {
        // ── LOGIN SCREEN ──
        LoginScreen(
            onLogin = { user, pass ->
                repository.login(user, pass)
            },
            onLoginSuccess = { t ->
                teacher = t
                // Load classes in background
                loading = true
                Thread {
                    val cls = repository.getTeacherClasses(t.teacherId)
                    classes = cls
                    loading = false
                }.start()
            },
        )
    } else if (currentClass == null) {
        // ── CLASS SELECT SCREEN ──
        ClassSelectScreen(
            teacher = currentTeacher,
            classes = classes,
            onClassClick = { cls ->
                selectedClass = cls
                loading = true
                Thread {
                    val roster = repository.getStudentsInClass(
                        cls.gradeLevel, cls.className, currentTeacher.schoolName
                    )
                    students = roster
                    loading = false
                }.start()
            },
            onLogout = {
                teacher = null
                classes = emptyList()
                selectedClass = null
                students = emptyList()
            },
        )
    } else {
        // ── GRADES EDIT SCREEN ──
        GradesScreen(
            gradeLevel = currentClass.gradeLevel,
            className = currentClass.className,
            subjectName = currentClass.subjectName,
            students = students,
            onSaveGrades = { ssnToGrade ->
                // Save each student's updated grade
                var allOk = true
                for ((ssn, grade) in ssnToGrade) {
                    // Find the student, merge the new grade, save
                    val student = students.find { it.ssn == ssn }
                    if (student != null) {
                        val updatedGrades = student.grades.toMutableMap()
                        updatedGrades[currentClass.subjectName] = grade
                        val ok = repository.updateStudentGrades(ssn, updatedGrades)
                        if (!ok) allOk = false
                        // Update local state
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
    }
}
