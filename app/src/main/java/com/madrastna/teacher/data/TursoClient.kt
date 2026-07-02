package com.madrastna.teacher.data

import com.madrastna.teacher.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Turso HTTP API client. Connects directly to the Turso database via the
 * /v2/pipeline endpoint. No backend server needed — the app talks to Turso
 * over HTTPS using a database token.
 *
 * SECURITY NOTE: The token is embedded in the APK via BuildConfig. For
 * production, consider proxying through a backend so the token isn't
 * extractable from the APK.
 */
class TursoClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val url = "${BuildConfig.TURSO_URL}/v2/pipeline"
    private val token = BuildConfig.TURSO_TOKEN

    /**
     * Execute a SQL query and return rows as a list of Maps.
     * @param sql The SQL statement (use ? for params — replaced inline here for simplicity)
     * @return List of row objects (column name -> string value)
     */
    fun query(sql: String): List<Map<String, String>> {
        val body = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "execute")
                    put("stmt", JSONObject().apply {
                        put("sql", sql)
                    })
                })
            })
        }.toString()

        val response = doRequest(body)
        return parseRows(response)
    }

    /**
     * Execute a SQL statement (INSERT/UPDATE/DELETE). Returns rows affected.
     */
    fun execute(sql: String): Int {
        val body = JSONObject().apply {
            put("requests", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "execute")
                    put("stmt", JSONObject().apply {
                        put("sql", sql)
                    })
                })
            })
        }.toString()

        val response = doRequest(body)
        return try {
            response
                .getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("response")
                .getJSONObject("result")
                .getInt("affected_row_count")
        } catch (e: Exception) { 0 }
    }

    private fun doRequest(body: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { res ->
            val responseBody = res.body?.string() ?: "{}"
            if (!res.isSuccessful) {
                throw RuntimeException("Turso error ${res.code}: $responseBody")
            }
            return JSONObject(responseBody)
        }
    }

    /**
     * Parse the Turso v2/pipeline response into a list of row maps.
     * Each row is a Map<String, String> (column name -> value).
     */
    private fun parseRows(response: JSONObject): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        try {
            val resultObj = response
                .getJSONArray("results")
                .getJSONObject(0)
                .getJSONObject("response")
                .getJSONObject("result")

            val cols = resultObj.getJSONArray("cols")
            val colNames = mutableListOf<String>()
            for (i in 0 until cols.length()) {
                colNames.add(cols.getJSONObject(i).getString("name"))
            }

            val rows = resultObj.optJSONArray("rows") ?: return results
            for (i in 0 until rows.length()) {
                val rowVals = rows.getJSONArray(i)
                val rowMap = mutableMapOf<String, String>()
                for (j in 0 until rowVals.length()) {
                    val valObj = rowVals.getJSONObject(j)
                    rowMap[colNames[j]] = valObj.optString("value", "")
                }
                results.add(rowMap)
            }
        } catch (e: Exception) {
            // Empty result or error — return empty list
        }
        return results
    }

    /** SHA-256 hash for password comparison (matches DB storage format). */
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
