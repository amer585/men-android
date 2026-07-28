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

/**
 * Egyptian Ministry of Education administrative hierarchy — who is above whom,
 * from the national Ministry down to the student. Each level defines its scope
 * and whether it may add/manage students.
 *
 *   🏛️ Ministry (وزارة التربية والتعليم) — national, can manage ANY school
 *      ↓
 *   🏷️ Directorate (المديرية التعليمية) — governorate-wide
 *      ↓
 *   🗺️ Administration (الإدارة التعليمية) — district / markaz
 *      ↓
 *   🏫 Principal (ناظر المدرسة) — their OWN school only
 *      ↓
 *   👨‍🏫 Teacher (المعلم) — edits grades for assigned classes
 *      ↓
 *   🎒 Student (الطالب)
 */
data class HierarchyLevel(
    val order: Int,
    val titleAr: String,
    val scopeAr: String,
    val canAddStudents: Boolean,
    val icon: String,
)

val EGYPTIAN_HIERARCHY = listOf(
    HierarchyLevel(1, "وزارة التربية والتعليم", "مستوى وطني · كل المدارس", true, "🏛️"),
    HierarchyLevel(2, "المديرية التعليمية", "مستوى المحافظة", true, "🏷️"),
    HierarchyLevel(3, "الإدارة التعليمية", "مستوى المركز / الحي", true, "🗺️"),
    HierarchyLevel(4, "مدير المدرسة", "مدرسته فقط", true, "🏫"),
    HierarchyLevel(5, "المعلم", "يُدخِل الدرجات فقط", false, "👨‍🏫"),
    HierarchyLevel(6, "الطالب", "—", false, "🎒"),
)

/** Map backend role strings → human Arabic role names. */
val ROLE_LABELS_AR = mapOf(
    "admin" to "مدير النظام",
    "principal" to "مدير المدرسة",
    "teacher" to "معلم",
    "directorate" to "المديرية التعليمية",
    "directorate_manager" to "مدير المديرية",
    "district" to "الإدارة التعليمية",
    "district_manager" to "مدير الإدارة",
)

// ── Teacher-account workflow (email self-registration → admin approval → JWT) ─

/** A registered teacher account (mirrors backend teacher_accounts row). */
data class TeacherAccount(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val subject: String?,
    val isVerified: Boolean,
)

/** A student linked to a teacher (teacher-DB relation + student-DB enrichment). */
data class LinkedStudent(
    val studentId: String,
    val nameAr: String?,
    val schoolName: String?,
    val gradeLevel: Int,
    val className: String?,
    val linkedAt: String?,
)

/** Generic call outcome used by login / register / link flows. */
data class CallResult(
    val ok: Boolean,
    val message: String,
    val account: TeacherAccount? = null,
    /** HTTP status when the call failed at the API layer (0 = network/other). */
    val httpStatus: Int = 0,
) {
    /** True when the backend rejected login with 403 PENDING_APPROVAL. */
    val isPendingApproval: Boolean get() = !ok && httpStatus == 403
}

/** Result of polling /teacher/verification-status with credentials. */
data class VerificationCheck(
    val verified: Boolean,
    val status: String, // "pending" | "approved"
    val message: String,
)

// ── Teacher ⇄ student bridge (v6) ────────────────────────────────────────
//
// The teacher's database stores identity + a list of student ids. NOTHING here
// is persisted on the device or in the teacher DB — these are transient view
// models for data the backend imported from the STUDENT database and cached.

/** A student profile as returned by the teacher-scoped endpoints. */
data class StudentProfile(
    val studentId: String,
    val nameAr: String?,
    val gender: String?,
    val schoolName: String?,
    val adminZone: String?,
    val gradeLevel: Int,
    val className: String?,
)

/** A hit from searching the STUDENT database, flagged with roster membership. */
data class StudentSearchHit(
    val profile: StudentProfile,
    val imported: Boolean,
)

/** One subject grade stored in the STUDENT database. */
data class GradeRow(
    val subjectName: String,
    val gradeValue: String,
    val updatedAt: String?,
)

/** One attendance day stored in the STUDENT database. */
data class AttendanceRow(
    val date: String,
    val status: String, // present | absent | late | excused
    val note: String?,
)

/** One weekly assessment stored in the STUDENT database. */
data class WeeklyRow(
    val subjectName: String,
    val weekNumber: Int,
    val score: Double,
    val maxScore: Double,
)

/** The full academic view of one owned student (imported + cached by backend). */
data class StudentDetail(
    val profile: StudentProfile,
    val grades: List<GradeRow>,
    val attendance: List<AttendanceRow>,
    val weekly: List<WeeklyRow>,
    val cached: Boolean,
)

/** Teacher dashboard header stats. */
data class TeacherStats(
    val students: Int,
    val classes: Int,
    val schools: Int,
    val grades: List<Int>,
)

/** Attendance status codes + their Arabic labels. */
val ATTENDANCE_STATUSES = listOf(
    "present" to "حاضر",
    "absent" to "غائب",
    "late" to "متأخر",
    "excused" to "بعذر",
)
