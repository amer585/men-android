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
        // httpStatus 403 ⇒ account is pending admin approval (PENDING_APPROVAL).
        CallResult(ok = false, message = e.message ?: "بيانات الدخول غير صحيحة", httpStatus = e.status)
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /**
     * Poll the approval state of a registered account using its CREDENTIALS
     * (no JWT exists yet while the account is pending). Returns null when the
     * credentials are rejected or the server is unreachable.
     */
    fun checkVerificationStatus(email: String, password: String): VerificationCheck? = try {
        val res = api.teacherVerificationStatus(email, password)
        VerificationCheck(
            verified = res.optBoolean("is_verified", false) || res.optInt("is_verified", 0) == 1,
            status = res.optString("status").ifEmpty { "pending" },
            message = res.optString("message"),
        )
    } catch (e: Exception) {
        null
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

    /**
     * Unlink a student from the authenticated teacher. The student record is
     * NEVER deleted — it stays in the student DB; only the relation (and the
     * backend's cached roster) is removed.
     */
    fun unlinkStudent(studentId: String): CallResult = try {
        api.unlinkStudent(studentId)
        CallResult(ok = true, message = "تم إلغاء ربط الطالب.")
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر إلغاء الربط")
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Read-only student portal payload (grades/attendance/schedule/…). */
    fun getStudentPortal(ssnEncrypted: String, gradeLevel: Int): JSONObject? = try {
        api.studentPortal(ssnEncrypted, gradeLevel)
    } catch (e: Exception) {
        null
    }

    // ── Teacher ⇄ student bridge (v6) ─────────────────────────
    //
    // The teacher DB holds ONLY the account + student-id pointers. Every call
    // below hits a backend endpoint that reads/writes the STUDENT database,
    // handles the cross-database import, owns the cache, and authorizes the
    // teacher against the relation table. No student data is persisted on the
    // device and none is copied into the teacher database.

    /**
     * Search the STUDENT database for a student who is NOT in the teacher's own
     * database. Accepts a 14-digit id, an id prefix or part of an Arabic name.
     */
    fun searchStudents(
        query: String?,
        schoolName: String? = null,
        gradeLevel: Int? = null,
        className: String? = null,
    ): List<StudentSearchHit> = try {
        val res = api.searchStudents(query, schoolName, gradeLevel, className)
        val arr = res.optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<StudentSearchHit>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(StudentSearchHit(parseProfile(o), o.optBoolean("imported", false)))
        }
        out
    } catch (e: Exception) {
        emptyList()
    }

    /** Import an EXISTING student (student DB) into the roster — pointer only. */
    fun importStudent(studentId: String): CallResult = try {
        val res = api.importStudent(studentId)
        CallResult(ok = true, message = res.optString("message").ifEmpty { "تم استيراد الطالب من قاعدة بيانات الطلاب." })
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر الاستيراد", httpStatus = e.status)
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /**
     * Add a NEW student. The row is created in the STUDENT DATABASE by the
     * backend (which mints the 14-digit id when [ssnEncrypted] is blank) and
     * then linked to this teacher.
     */
    fun addStudent(
        nameAr: String,
        gradeLevel: Int,
        className: String? = null,
        schoolName: String? = null,
        adminZone: String? = null,
        gender: String? = null,
        ssnEncrypted: String? = null,
    ): Pair<CallResult, StudentProfile?> = try {
        val res = api.addStudent(nameAr, gradeLevel, className, schoolName, adminZone, gender, ssnEncrypted)
        val profile = res.optJSONObject("student")?.let { parseProfile(it) }
        val msg = res.optString("message").ifEmpty { "تم حفظ الطالب في قاعدة بيانات الطلاب." }
        CallResult(ok = true, message = msg) to profile
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر إضافة الطالب", httpStatus = e.status) to null
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم") to null
    }

    /** Full academic view of one owned student, imported + cached by the backend. */
    fun studentDetail(studentId: String): StudentDetail? = try {
        val res = api.studentDetail(studentId)
        val profileJson = res.optJSONObject("student") ?: return null
        StudentDetail(
            profile = parseProfile(profileJson),
            grades = res.optJSONArray("grades").mapObjects { o ->
                GradeRow(
                    subjectName = o.optString("subject_name"),
                    gradeValue = o.optString("grade_value"),
                    updatedAt = o.optString("updated_at").takeIf { it.isNotEmpty() },
                )
            },
            attendance = res.optJSONArray("attendance").mapObjects { o ->
                AttendanceRow(
                    date = o.optString("date"),
                    status = o.optString("status"),
                    note = o.optString("note").takeIf { it.isNotEmpty() },
                )
            },
            weekly = res.optJSONArray("weekly").mapObjects { o ->
                WeeklyRow(
                    subjectName = o.optString("subject_name"),
                    weekNumber = o.optInt("week_number", 0),
                    score = o.optDouble("score", 0.0),
                    maxScore = o.optDouble("max_score", 10.0),
                )
            },
            cached = res.optBoolean("cached", false),
        )
    } catch (e: Exception) {
        null
    }

    /** Edit an owned student — the UPDATE lands in the STUDENT database. */
    fun updateStudent(
        studentId: String,
        nameAr: String? = null,
        gradeLevel: Int? = null,
        className: String? = null,
        schoolName: String? = null,
        gender: String? = null,
    ): CallResult = try {
        val res = api.updateStudent(studentId, nameAr, gradeLevel, className, schoolName, gender)
        CallResult(ok = true, message = res.optString("message").ifEmpty { "تم حفظ التعديل في قاعدة بيانات الطلاب." })
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر حفظ التعديل", httpStatus = e.status)
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Save grades for an owned student → student_grades in the STUDENT database. */
    fun saveStudentGrades(studentId: String, grades: Map<String, String>): CallResult = try {
        val res = api.saveStudentGrades(studentId, grades)
        CallResult(ok = true, message = res.optString("message").ifEmpty { "تم حفظ الدرجات." })
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر حفظ الدرجات", httpStatus = e.status)
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Record attendance for an owned student → attendance in the STUDENT database. */
    fun saveStudentAttendance(studentId: String, status: String, date: String? = null): CallResult = try {
        val res = api.saveStudentAttendance(studentId, status, date)
        CallResult(ok = true, message = res.optString("message").ifEmpty { "تم تسجيل الحضور." })
    } catch (e: ApiClient.ApiException) {
        CallResult(ok = false, message = e.message ?: "تعذّر تسجيل الحضور", httpStatus = e.status)
    } catch (e: Exception) {
        CallResult(ok = false, message = "تعذّر الوصول إلى الخادم")
    }

    /** Dashboard header stats (served from the backend's cached roster). */
    fun teacherStats(): TeacherStats? = try {
        val totals = api.teacherDashboard().optJSONObject("totals") ?: return null
        val gradesArr = totals.optJSONArray("grades")
        val grades = mutableListOf<Int>()
        if (gradesArr != null) for (i in 0 until gradesArr.length()) grades.add(gradesArr.optInt(i, 0))
        TeacherStats(
            students = totals.optInt("students", 0),
            classes = totals.optInt("classes", 0),
            schools = totals.optInt("schools", 0),
            grades = grades.filter { it > 0 },
        )
    } catch (e: Exception) {
        null
    }

    private fun parseProfile(o: JSONObject): StudentProfile = StudentProfile(
        studentId = o.optString("student_id").ifEmpty { o.optString("ssn_encrypted") },
        nameAr = o.optString("student_name_ar").takeIf { it.isNotEmpty() },
        gender = o.optString("gender").takeIf { it.isNotEmpty() },
        schoolName = o.optString("school_name").takeIf { it.isNotEmpty() },
        adminZone = o.optString("admin_zone").takeIf { it.isNotEmpty() },
        gradeLevel = o.optInt("grade_level", 0),
        className = o.optString("class_name").takeIf { it.isNotEmpty() },
    )

    private fun <T> org.json.JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        val out = mutableListOf<T>()
        for (i in 0 until length()) {
            val o = optJSONObject(i) ?: continue
            out.add(transform(o))
        }
        return out
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
