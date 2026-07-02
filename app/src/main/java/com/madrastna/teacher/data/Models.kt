package com.madrastna.teacher.data

/** Logged-in teacher session. */
data class Teacher(
    val teacherId: Int,
    val username: String,
    val nameAr: String,
    val role: String,
    val schoolName: String,
)

/** A class the teacher is assigned to. */
data class ClassAssignment(
    val gradeLevel: Int,
    val className: String,
    val subjectName: String,
)

/** A student in a class with their current grades. */
data class StudentWithGrades(
    val ssn: String,
    val nameAr: String,
    val gender: String,
    val className: String,
    val grades: MutableMap<String, String>, // subject -> grade value
)

/** Egyptian preparatory subjects. */
val EGYPTIAN_SUBJECTS = listOf(
    "اللغة العربية",
    "اللغة الإنجليزية",
    "الرياضيات",
    "العلوم",
    "الدراسات الاجتماعية",
    "التربية الدينية",
    "الحاسب الآلي",
)

/** Grade level labels (Arabic). */
val GRADE_LABELS = mapOf(
    7 to "الأولى الإعدادية",
    8 to "الثانية الإعدادية",
    9 to "الثالثة الإعدادية",
    10 to "الأولى الثانوية",
    11 to "الثانية الثانوية",
    12 to "الثالثة الثانوية",
)
