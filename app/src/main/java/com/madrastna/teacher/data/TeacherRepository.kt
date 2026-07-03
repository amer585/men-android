package com.madrastna.teacher.data

import org.json.JSONObject

/**
 * Single source of truth for app data. Every call is routed through the
 * backend [ApiClient] (HTTPS + JWT) — there is NO direct database access and
 * NO embedded database token. The backend owns the databases, caching and
 * security; the app only ever holds a short-lived JWT.
 *
 * All methods are BLOCKING and must be called from a background thread.
 *
 * Two surfaces are exposed:
 *  • Staff / grade-entry (legacy)  — POST /login, hierarchy, /grades/update.
 *  • Teacher-account workflow      — /teacher/register|login|profile|students.
 */
class TeacherRepository(private val api: ApiClient) {

    // ── Staff / grade-entry session ───────────────────────────
    private var staffSchoolName: String? = null

    // Editing context captured when a roster is loaded; reused by grade writes.
    private var ctxGrade: Int = 0
    private var ctxClass: String = ""
    private var ctxSubject: String = ""

    fun clearSession() = api.clearToken()

    /** Authenticate staff (teacher/principal/admin) and remember the JWT. */
    fun login(username: String, password: String): Teacher? = try {
        val res = api.staffLogin(username, password)
        val user = res.optJSONObject("user") ?: JSONObject()
        staffSchoolName = user.optString("school_name").takeIf { it.isNotEmpty() } ?: "ALL"
        Teacher(
            teacherId = 0,
            username = user.optString("name").takeIf { it.isNotEmpty() } ?: username,
            nameAr = user.optString("teacher_name_ar").takeIf { it.isNotEmpty() }
                ?: user.optString("name").takeIf { it.isNotEmpty() } ?: username,
            role = user.optString("role").takeIf { it.isNotEmpty() } ?: "teacher",
            schoolName = staffSchoolName ?: "ALL",
        )
    } catch (e: Exception) {
        null
    }

    /**
     * Classes the logged-in staff member may edit. The backend exposes
     * school-level classes; we expand each into one entry per subject so the
     * grade editor always has a subject context for writes.
     */
    fun getTeacherClasses(teacherId: Int): List<ClassAssignment> = try {
        val school = staffSchoolName ?: "ALL"
        val res = api.classes(school)
        val arr = res.optJSONArray("classes") ?: return emptyList()
        val out = mutableListOf<ClassAssignment>()
        for (i in 0 until arr.length()) {
            val c = arr.optJSONObject(i) ?: continue
            val grade = c.optInt("grade_level", 0)
            if (grade == 0) continue
            val className = c.optString("class_name")
            if (className.isEmpty()) continue
            for (subject in EGYPTIAN_SUBJECTS) {
                out.add(ClassAssignment(grade, className, subject))
            }
        }
        out.sortedWith(compareBy({ it.gradeLevel }, { it.className }, { it.subjectName }))
    } catch (e: Exception) {
        emptyList()
    }

    /** Load a class roster; captures the editing context for grade writes. */
    fun getStudentsInClass(
        gradeLevel: Int,
        className: String,
        schoolName: String,
        subjectName: String,
    ): List<StudentWithGrades> {
        ctxGrade = gradeLevel
        ctxClass = className
        ctxSubject = subjectName
        return try {
            val res = api.roster(schoolName, gradeLevel, className)
            val arr = res.optJSONArray("students") ?: return emptyList()
            val out = mutableListOf<StudentWithGrades>()
            for (i in 0 until arr.length()) {
                val s = arr.optJSONObject(i) ?: continue
                val ssn = s.optString("ssn_encrypted")
                if (ssn.isEmpty()) continue
                val grades = mutableMapOf<String, String>()
                val gArr = s.optJSONArray("grades")
                if (gArr != null) {
                    for (j in 0 until gArr.length()) {
                        val g = gArr.optJSONObject(j) ?: continue
                        val subj = g.optString("subject_name")
                        if (subj.isEmpty()) continue
                        grades[subj] = g.optString("grade_value")
                    }
                }
                out.add(
                    StudentWithGrades(
                        ssn = ssn,
                        nameAr = s.optString("student_name_ar").ifEmpty { "—" },
                        gender = s.optString("gender").ifEmpty { "M" },
                        className = s.optString("class_name").ifEmpty { className },
                        grades = grades,
                    ),
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Update the current subject's grade for one student. */
    fun updateStudentGrades(ssn: String, grades: Map<String, String>): Boolean = try {
        val gradeValue = grades[ctxSubject] ?: return false
        val entry = JSONObject()
            .put("ssn_encrypted", ssn)
            .put("grade_value", gradeValue)
            .put("grade_level", ctxGrade)
            .put("class_name", ctxClass)
            .put("subject_name", ctxSubject)
        api.updateGrades(listOf(entry))
        true
    } catch (e: Exception) {
        false
    }

    // ── Teacher-account workflow ──────────────────────────────

    /** Email login for a verified teacher account. */
    fun teacherLogin(email: String, password: String): CallResult = try {
        val res = api.teacherLogin(email, password)
        CallResult(
            ok = true,
            message = "",
            account = parseAccount(res.optJSONObject("account") ?: JSONObject()),
        )
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "بيانات الدخول غير صحيحة")
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Public self-registration (account is created pending admin approval). */
    fun teacherRegister(
        name: String,
        email: String,
        password: String,
        phone: String?,
        subject: String?,
    ): CallResult = try {
        val res = api.teacherRegister(name, email, password, phone, subject)
        val account = res.optJSONObject("account")?.let { parseAccount(it) }
        val msg = res.optString("message").ifEmpty { "تم استلام طلبك — بانتظار موافقة الإدارة." }
        CallResult(ok = true, message = msg, account = account)
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر التسجيل")
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Currently authenticated teacher profile (refreshes verification state). */
    fun teacherProfile(): TeacherAccount? = try {
        parseAccount(api.teacherProfile())
    } catch (e: Exception) {
        null
    }

    /** Students linked to the authenticated teacher. */
    fun getTeacherStudents(): List<LinkedStudent> = try {
        val res = api.teacherStudents()
        val arr = res.optJSONArray("students") ?: return emptyList()
        val out = mutableListOf<LinkedStudent>()
        for (i in 0 until arr.length()) {
            val s = arr.optJSONObject(i) ?: continue
            out.add(
                LinkedStudent(
                    studentId = s.optString("student_id"),
                    nameAr = s.optString("student_name_ar").takeIf { it.isNotEmpty() },
                    schoolName = s.optString("school_name").takeIf { it.isNotEmpty() },
                    gradeLevel = s.optInt("grade_level", 0),
                    className = s.optString("class_name").takeIf { it.isNotEmpty() },
                    linkedAt = s.optString("linked_at").takeIf { it.isNotEmpty() },
                ),
            )
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    /** Link a student (by ssn_encrypted) to the authenticated teacher. */
    fun linkStudent(studentId: String): CallResult = try {
        api.linkStudent(studentId)
        CallResult(ok = true, message = "تمت إضافة الطالب إلى قائمتك.")
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر إضافة الطالب")
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Read-only student portal payload (grades/attendance/schedule/…). */
    fun getStudentPortal(ssnEncrypted: String, gradeLevel: Int): JSONObject? = try {
        api.studentPortal(ssnEncrypted, gradeLevel)
    } catch (e: Exception) {
        null
    }

    private fun parseAccount(o: JSONObject): TeacherAccount = TeacherAccount(
        id = o.optString("id"),
        name = o.optString("name").ifEmpty { "—" },
        email = o.optString("email"),
        phone = o.optString("phone").takeIf { it.isNotEmpty() },
        subject = o.optString("subject").takeIf { it.isNotEmpty() },
        isVerified = o.optBoolean("is_verified", false) || o.optInt("is_verified", 0) == 1,
    )
}
