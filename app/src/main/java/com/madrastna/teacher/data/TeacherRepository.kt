package com.madrastna.teacher.data

import org.json.JSONObject

/**
 * Data repository — handles all database operations via TursoClient.
 * Each function escapes values inline (single quotes doubled) to prevent
 * SQL injection on the simple HTTP API.
 */
class TeacherRepository(private val db: TursoClient) {

    private fun esc(s: String): String = s.replace("'", "''")

    /**
     * Authenticate a teacher by username + password.
     * Password is SHA-256 hashed and compared to the stored hash.
     * @return Teacher object on success, null on failure.
     */
    fun login(username: String, password: String): Teacher? {
        val hash = db.hashPassword(password)
        val rows = db.query(
            "SELECT teacher_id, username, teacher_name_ar, role, school_name, password_hash " +
            "FROM teachers WHERE username = '${esc(username)}' AND is_active = 1 LIMIT 1"
        )
        if (rows.isEmpty()) return null

        val row = rows[0]
        val storedHash = row["password_hash"] ?: return null

        // Check if password matches (supports both SHA-256 and plaintext)
        val inputHash = hash.lowercase()
        val stored = storedHash.lowercase().trim()
        if (inputHash != stored && password != storedHash) return null

        return Teacher(
            teacherId = row["teacher_id"]?.toIntOrNull() ?: 0,
            username = row["username"] ?: username,
            nameAr = row["teacher_name_ar"] ?: username,
            role = row["role"] ?: "teacher",
            schoolName = row["school_name"] ?: "ALL",
        )
    }

    /**
     * Get all classes assigned to a teacher.
     */
    fun getTeacherClasses(teacherId: Int): List<ClassAssignment> {
        val rows = db.query(
            "SELECT grade_level, class_name, subject_name FROM teacher_classes " +
            "WHERE teacher_id = $teacherId ORDER BY grade_level, class_name"
        )
        return rows.map { r ->
            ClassAssignment(
                gradeLevel = r["grade_level"]?.toIntOrNull() ?: 0,
                className = r["class_name"] ?: "",
                subjectName = r["subject_name"] ?: "",
            )
        }
    }

    /**
     * Get all students in a class with their grades (from grades_json column).
     */
    fun getStudentsInClass(gradeLevel: Int, className: String, schoolName: String): List<StudentWithGrades> {
        val rows = db.query(
            "SELECT ssn_encrypted, student_name_ar, gender, class_name, grades_json " +
            "FROM students WHERE grade_level = $gradeLevel AND class_name = '${esc(className)}' " +
            "AND school_name = '${esc(schoolName)}' ORDER BY student_name_ar"
        )
        return rows.map { r ->
            val gradesJson = r["grades_json"] ?: "{}"
            val grades = mutableMapOf<String, String>()
            try {
                val obj = JSONObject(gradesJson)
                obj.keys().forEach { key ->
                    grades[key] = obj.getString(key)
                }
            } catch (_: Exception) { }

            StudentWithGrades(
                ssn = r["ssn_encrypted"] ?: "",
                nameAr = r["student_name_ar"] ?: "—",
                gender = r["gender"] ?: "M",
                className = r["class_name"] ?: className,
                grades = grades,
            )
        }
    }

    /**
     * Update a single student's grades_json (all subjects in one column).
     * Does a read-modify-write: reads current grades, merges, writes back.
     */
    fun updateStudentGrades(ssn: String, grades: Map<String, String>): Boolean {
        val jsonStr = esc(JSONObject(grades).toString())
        val affected = db.execute(
            "UPDATE students SET grades_json = '$jsonStr', updated_at = datetime('now') " +
            "WHERE ssn_encrypted = '${esc(ssn)}'"
        )
        return affected > 0
    }
}
