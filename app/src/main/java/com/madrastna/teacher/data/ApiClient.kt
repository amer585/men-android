package com.madrastna.teacher.data

import android.content.Context
import com.madrastna.teacher.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Backend API client.
 *
 * All data access now goes through the deployed backend
 * (https://amer585-intlaqa-backend.hf.space/api) over HTTPS. NO Turso/libSQL database
 * token is embedded in the APK — the backend owns the database, caching and
 * security, and authenticates every request with a short-lived JWT.
 *
 * Methods are BLOCKING and must be called from a background thread.
 */
class ApiClient(context: Context) {

    /** Raised for any non-2xx response; carries the server's message. */
    class ApiException(val status: Int, message: String) : RuntimeException(message)

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val baseUrl: String = BuildConfig.BACKEND_URL.trimEnd('/')

    /** The JWT for the current session (staff OR teacher account). */
    var token: String?
        get() = prefs.getString(KEY_JWT, null)
        set(value) {
            prefs.edit().putString(KEY_JWT, value).apply()
        }

    fun clearToken() {
        prefs.edit().remove(KEY_JWT).apply()
    }

    // ── Core HTTP ──────────────────────────────────────────────

    private fun send(path: String, method: String, bodyString: String?, withAuth: Boolean): JSONObject {
        val builder = Request.Builder()
            .url("$baseUrl/${path.trimStart('/')}")
            .addHeader("Content-Type", "application/json")
        if (withAuth) {
            token?.let { builder.addHeader("Authorization", "Bearer $it") }
        }
        val reqBody = bodyString?.toRequestBody(JSON_MEDIA)
        builder.method(method, reqBody)
        client.newCall(builder.build()).execute().use { res ->
            val raw = res.body?.string() ?: "{}"
            val json = try {
                JSONObject(raw)
            } catch (_: Exception) {
                JSONObject().put("raw", raw)
            }
            if (!res.isSuccessful) {
                val msg = json.optString("error")
                    .ifEmpty { json.optString("message") }
                    .ifEmpty { "HTTP ${res.code}" }
                throw ApiException(res.code, msg)
            }
            return json
        }
    }

    private fun post(path: String, body: JSONObject, withAuth: Boolean): JSONObject =
        send(path, "POST", body.toString(), withAuth)

    private fun get(path: String, withAuth: Boolean): JSONObject =
        send(path, "GET", null, withAuth)

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    // ── Staff (legacy grade-entry) ─────────────────────────────

    /** POST /login → { success, token, user }. Stores the JWT. */
    fun staffLogin(username: String, password: String): JSONObject {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
        val res = post("login", body, withAuth = false)
        token = res.optString("token").ifEmpty { null }
        return res
    }

    /** GET /hierarchy/classes?school_name= → { classes: [...] }. */
    fun classes(schoolName: String): JSONObject =
        get("hierarchy/classes?school_name=${enc(schoolName)}", withAuth = true)

    /** GET /hierarchy/students?... → { students: [...] }. */
    fun roster(schoolName: String, gradeLevel: Int, className: String): JSONObject {
        val q = "school_name=${enc(schoolName)}&grade_level=$gradeLevel&class_name=${enc(className)}"
        return get("hierarchy/students?$q", withAuth = true)
    }

    /** POST /grades/update (body is a JSON ARRAY of entries). */
    fun updateGrades(entries: List<JSONObject>): JSONObject {
        val arr = JSONArray()
        entries.forEach { arr.put(it) }
        return send("grades/update", "POST", arr.toString(), withAuth = true)
    }

    // ── Teacher account (email self-registration → approval → JWT) ──

    /** POST /teacher/register → { message, account }. */
    fun teacherRegister(
        name: String,
        email: String,
        password: String,
        phone: String?,
        subject: String?,
    ): JSONObject {
        val body = JSONObject()
            .put("name", name)
            .put("email", email)
            .put("password", password)
        phone?.takeIf { it.isNotBlank() }?.let { body.put("phone", it) }
        subject?.takeIf { it.isNotBlank() }?.let { body.put("subject", it) }
        return post("teacher/register", body, withAuth = false)
    }

    /** POST /teacher/login → { success, token, account }. Stores the JWT. */
    fun teacherLogin(email: String, password: String): JSONObject {
        val body = JSONObject()
            .put("email", email)
            .put("password", password)
        val res = post("teacher/login", body, withAuth = false)
        token = res.optString("token").ifEmpty { null }
        return res
    }

    /** GET /teacher/profile → account object. */
    fun teacherProfile(): JSONObject = get("teacher/profile", withAuth = true)

    /** GET /teacher/students → { teacher_id, students: [...] }. */
    fun teacherStudents(): JSONObject = get("teacher/students", withAuth = true)

    /** POST /teacher/students { student_id }. */
    fun linkStudent(studentId: String): JSONObject =
        post("teacher/students", JSONObject().put("student_id", studentId), withAuth = true)

    // ── Student portal (read-only, used by the teacher dashboard) ──

    /** GET /student/portal?ssn_encrypted=&grade_level= → full portal payload. */
    fun studentPortal(ssnEncrypted: String, gradeLevel: Int): JSONObject =
        get(
            "student/portal?ssn_encrypted=${enc(ssnEncrypted)}&grade_level=$gradeLevel",
            withAuth = false,
        )

    companion object {
        private const val PREFS = "madrastna_api"
        private const val KEY_JWT = "jwt"
        private val JSON_MEDIA = "application/json".toMediaType()
    }
}
