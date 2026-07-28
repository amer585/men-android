package com.madrastna.teacher.ui.students

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.ATTENDANCE_STATUSES
import com.madrastna.teacher.data.EGYPTIAN_SUBJECTS
import com.madrastna.teacher.data.GRADE_LABELS
import com.madrastna.teacher.data.LinkedStudent
import com.madrastna.teacher.data.StudentDetail
import com.madrastna.teacher.data.StudentSearchHit
import com.madrastna.teacher.data.TeacherRepository
import com.madrastna.teacher.data.TeacherStats
import com.madrastna.teacher.ui.theme.*

/**
 * ══════════════════════════════════════════════════════════════════════
 * TEACHER STUDENT MANAGER (v6)
 * ══════════════════════════════════════════════════════════════════════
 *
 * The teacher's own database holds ONLY their account and a list of student
 * ids. This screen surfaces that split explicitly:
 *
 *   طلابي     → the imported roster, enriched live from the STUDENT database
 *                and cached by the backend. Tap a student to read/edit their
 *                grades and attendance — every write hits the STUDENT database.
 *   استيراد   → searches the STUDENT database (the student is NOT in the
 *                teacher's DB) and imports the pointer into the roster.
 *   إضافة     → creates a NEW student. The row is saved in the STUDENT
 *                database and the backend mints the 14-digit id.
 *
 * No student data is persisted on the device, and nothing is copied into the
 * teacher database. Networking runs on plain background threads and posts
 * results back to the main looper (same pattern as TeacherAccountScreen).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherStudentsScreen(
    repository: TeacherRepository,
    onBack: () -> Unit,
) {
    val main = remember { Handler(Looper.getMainLooper()) }

    var tab by remember { mutableStateOf(0) } // 0 roster · 1 import · 2 add
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    var roster by remember { mutableStateOf<List<LinkedStudent>>(emptyList()) }
    var stats by remember { mutableStateOf<TeacherStats?>(null) }
    var openId by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<StudentDetail?>(null) }

    // Import tab state
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<StudentSearchHit>?>(null) }

    // Add tab state
    var newName by remember { mutableStateOf("") }
    var newGrade by remember { mutableStateOf("7") }
    var newClass by remember { mutableStateOf("") }
    var newSchool by remember { mutableStateOf("") }

    // Detail editors
    var gradeSubject by remember { mutableStateOf(EGYPTIAN_SUBJECTS.first()) }
    var gradeValue by remember { mutableStateOf("") }

    fun notify(text: String) {
        message = text
        main.postDelayed({ if (message == text) message = null }, 4500)
    }

    fun reload() {
        loading = true
        Thread {
            val list = repository.getTeacherStudents()
            val s = repository.teacherStats()
            main.post {
                roster = list
                stats = s
                loading = false
            }
        }.start()
    }

    fun loadDetail(id: String) {
        detail = null
        Thread {
            val d = repository.studentDetail(id)
            main.post {
                detail = d
                if (d == null) notify("تعذّر قراءة بيانات الطالب من قاعدة بيانات الطلاب.")
            }
        }.start()
    }

    LaunchedEffect(Unit) { reload() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink950),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(22.dp))

            // ── Header ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("رجوع", color = Gold400, fontSize = 13.sp) }
                Spacer(Modifier.weight(1f))
                Text("إدارة الطلاب", color = TextPrimary, fontSize = 19.sp, fontWeight = FontWeight.Black)
            }
            Text(
                "بيانات الطلاب محفوظة في قاعدة بيانات الطلاب — قاعدة بيانات المعلّم تحتوي على حسابك وأرقام طلابك فقط.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 6.dp),
            )

            Spacer(Modifier.height(14.dp))

            // ── Stats ──
            stats?.let { s ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile("طلابي", s.students, Modifier.weight(1f))
                    StatTile("الفصول", s.classes, Modifier.weight(1f))
                    StatTile("المدارس", s.schools, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
            }

            // ── Tabs ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("طلابي (${roster.size})", tab == 0, Modifier.weight(1f)) { tab = 0 }
                TabChip("استيراد", tab == 1, Modifier.weight(1f)) { tab = 1 }
                TabChip("إضافة", tab == 2, Modifier.weight(1f)) { tab = 2 }
            }

            Spacer(Modifier.height(12.dp))

            message?.let { text ->
                Surface(
                    color = GoldGlow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text, color = Gold200, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(10.dp))
            }

            when (tab) {
                // ── ROSTER ──
                0 -> {
                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold400, strokeWidth = 3.dp)
                        }
                    } else if (roster.isEmpty()) {
                        EmptyHint("لا يوجد طلاب في قائمتك بعد. استخدم «استيراد» للبحث في قاعدة بيانات الطلاب.")
                    } else {
                        roster.forEach { s ->
                            RosterCard(
                                student = s,
                                expanded = openId == s.studentId,
                                onToggle = {
                                    if (openId == s.studentId) {
                                        openId = null
                                    } else {
                                        openId = s.studentId
                                        loadDetail(s.studentId)
                                    }
                                },
                                onRemove = {
                                    Thread {
                                        val res = repository.unlinkStudent(s.studentId)
                                        main.post {
                                            notify(res.message)
                                            if (res.ok) reload()
                                        }
                                    }.start()
                                },
                            )
                            if (openId == s.studentId) {
                                DetailPanel(
                                    detail = detail,
                                    subject = gradeSubject,
                                    onSubjectChange = { gradeSubject = it },
                                    value = gradeValue,
                                    onValueChange = { gradeValue = it },
                                    onSaveGrade = {
                                        if (gradeValue.isBlank()) {
                                            notify("أدخل الدرجة أولًا.")
                                        } else {
                                            val v = gradeValue.trim()
                                            Thread {
                                                val res = repository.saveStudentGrades(s.studentId, mapOf(gradeSubject to v))
                                                main.post {
                                                    notify(res.message)
                                                    if (res.ok) {
                                                        gradeValue = ""
                                                        loadDetail(s.studentId)
                                                    }
                                                }
                                            }.start()
                                        }
                                    },
                                    onSaveAttendance = { status ->
                                        Thread {
                                            val res = repository.saveStudentAttendance(s.studentId, status)
                                            main.post {
                                                notify(res.message)
                                                if (res.ok) loadDetail(s.studentId)
                                            }
                                        }.start()
                                    },
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                // ── IMPORT (search the STUDENT database) ──
                1 -> {
                    Text(
                        "الطالب غير موجود في قاعدة بيانات المعلّم — البحث يجري في قاعدة بيانات الطلاب عبر الخادم، ثم يُستورد سجلّه ويُخزَّن مؤقتًا.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("رقم الطالب (14 رقمًا) أو الاسم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (query.isBlank()) {
                                notify("أدخل رقم الطالب أو اسمه.")
                            } else {
                                loading = true
                                val term = query.trim()
                                Thread {
                                    val found = repository.searchStudents(term)
                                    main.post {
                                        hits = found
                                        loading = false
                                    }
                                }.start()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("بحث في قاعدة بيانات الطلاب", fontSize = 13.sp, fontWeight = FontWeight.Bold) }

                    Spacer(Modifier.height(12.dp))

                    if (loading) {
                        Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold400, strokeWidth = 3.dp)
                        }
                    }
                    hits?.let { list ->
                        if (list.isEmpty()) {
                            EmptyHint("لا توجد نتائج مطابقة في قاعدة بيانات الطلاب.")
                        } else {
                            list.forEach { hit ->
                                SearchHitCard(hit) {
                                    Thread {
                                        val res = repository.importStudent(hit.profile.studentId)
                                        main.post {
                                            notify(res.message)
                                            if (res.ok) {
                                                hits = hits?.map {
                                                    if (it.profile.studentId == hit.profile.studentId) it.copy(imported = true) else it
                                                }
                                                reload()
                                            }
                                        }
                                    }.start()
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // ── ADD (row created in the STUDENT database) ──
                else -> {
                    Text(
                        "سواء أضفت طالبًا جديدًا أو استوردت طالبًا موجودًا، يُحفظ السجل في قاعدة بيانات الطلاب ثم يُربط برقمه في قائمتك. الخادم يولّد رقم الطالب تلقائيًا.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("اسم الطالب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newGrade,
                        onValueChange = { newGrade = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("الصف (1-12)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newClass,
                        onValueChange = { newClass = it },
                        label = { Text("الفصل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSchool,
                        onValueChange = { newSchool = it },
                        label = { Text("المدرسة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val g = newGrade.toIntOrNull() ?: 0
                            if (newName.isBlank()) {
                                notify("اسم الطالب مطلوب.")
                            } else if (g < 1 || g > 12) {
                                notify("الصف يجب أن يكون بين 1 و 12.")
                            } else {
                                val n = newName.trim()
                                val c = newClass.trim()
                                val sc = newSchool.trim()
                                Thread {
                                    val (res, profile) = repository.addStudent(n, g, c, sc)
                                    main.post {
                                        notify(
                                            if (res.ok && profile != null) {
                                                "${res.message} · رقم الطالب: ${profile.studentId}"
                                            } else {
                                                res.message
                                            },
                                        )
                                        if (res.ok) {
                                            newName = ""
                                            newClass = ""
                                            tab = 0
                                            reload()
                                        }
                                    }
                                }.start()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold600),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("حفظ في قاعدة بيانات الطلاب", fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

/* ── pieces ─────────────────────────────────────────────────── */

@Composable
private fun StatTile(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(color = GlassSurface, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 10.dp)) {
            Text(label, color = Gold500, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Text("$value", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TabChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        color = if (selected) GoldGlow else GlassSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Gold200 else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Surface(color = GlassSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(18.dp))
    }
}

@Composable
private fun RosterCard(
    student: LinkedStudent,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = GlassSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    student.nameAr ?: "بدون اسم",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(student.studentId, color = TextMuted, fontSize = 10.sp)
                Text(
                    buildString {
                        append(GRADE_LABELS[student.gradeLevel] ?: "الصف ${student.gradeLevel}")
                        student.className?.let { append(" · $it") }
                        student.schoolName?.let { append(" · $it") }
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
            TextButton(onClick = onRemove) { Text("إزالة", color = Rose400, fontSize = 11.sp) }
            Text(if (expanded) "▾" else "‹", color = Gold400, fontSize = 15.sp)
        }
    }
}

@Composable
private fun SearchHitCard(hit: StudentSearchHit, onImport: () -> Unit) {
    Surface(color = GlassSurface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    hit.profile.nameAr ?: "بدون اسم",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(hit.profile.studentId, color = TextMuted, fontSize = 10.sp)
                Text(
                    buildString {
                        append(GRADE_LABELS[hit.profile.gradeLevel] ?: "الصف ${hit.profile.gradeLevel}")
                        hit.profile.className?.let { append(" · $it") }
                        hit.profile.schoolName?.let { append(" · $it") }
                    },
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }
            if (hit.imported) {
                Text("✓ في قائمتك", color = Emerald400, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Button(
                    onClick = onImport,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold600),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) { Text("استيراد", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    detail: StudentDetail?,
    subject: String,
    onSubjectChange: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    onSaveGrade: () -> Unit,
    onSaveAttendance: (String) -> Unit,
) {
    Surface(
        color = Ink850,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            if (detail == null) {
                Box(Modifier.fillMaxWidth().height(70.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Gold400, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                }
                return@Column
            }

            Text(
                if (detail.cached) "المصدر: قاعدة بيانات الطلاب · من الذاكرة المؤقتة" else "المصدر: قاعدة بيانات الطلاب",
                color = TextMuted,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(10.dp))

            // Grades
            Text("الدرجات", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (detail.grades.isEmpty()) {
                Text("لا توجد درجات مسجّلة.", color = TextMuted, fontSize = 11.sp)
            } else {
                detail.grades.forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(g.subjectName, color = TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(g.gradeValue, color = Gold300, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                EGYPTIAN_SUBJECTS.forEach { name ->
                    TabChip(name, subject == name) { onSubjectChange(name) }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("الدرجة") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onSaveGrade,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold600),
                    shape = RoundedCornerShape(10.dp),
                ) { Text("حفظ", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }

            Spacer(Modifier.height(14.dp))

            // Attendance
            Text("الحضور", color = Gold500, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (detail.attendance.isEmpty()) {
                Text("لا يوجد سجل حضور.", color = TextMuted, fontSize = 11.sp)
            } else {
                detail.attendance.take(6).forEach { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(a.date, color = TextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(
                            ATTENDANCE_STATUSES.firstOrNull { it.first == a.status }?.second ?: a.status,
                            color = when (a.status) {
                                "present" -> Emerald400
                                "absent" -> Rose400
                                else -> Amber400
                            },
                            fontSize = 11.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ATTENDANCE_STATUSES.forEach { (code, label) ->
                    TabChip(label, false, Modifier.weight(1f)) { onSaveAttendance(code) }
                }
            }
        }
    }
}
