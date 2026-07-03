package com.madrastna.teacher.ui.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madrastna.teacher.data.ClassAssignment
import com.madrastna.teacher.data.GRADE_LABELS
import com.madrastna.teacher.data.Teacher
import com.madrastna.teacher.ui.components.BrandCrest
import com.madrastna.teacher.ui.components.CinematicBackground
import com.madrastna.teacher.ui.components.GlassCard
import com.madrastna.teacher.ui.theme.*

@Composable
fun ClassSelectScreen(
    teacher: Teacher,
    classes: List<ClassAssignment>,
    onClassClick: (ClassAssignment) -> Unit,
    onLogout: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        CinematicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandCrest(size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        teacher.nameAr,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        teacher.schoolName,
                        color = Gold400,
                        fontSize = 13.sp,
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "خروج", tint = Rose400)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Greeting + count banner
                item {
                    Text(
                        "فصولك الدراسية",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${classes.size} فصل مُخصّص لك · اختر فصلًا لإدخال الدرجات",
                        color = TextMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(18.dp))
                }

                if (classes.isEmpty()) {
                    item {
                        GlassCard(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    Icons.Default.School,
                                    contentDescription = null,
                                    tint = Gold500,
                                    modifier = Modifier.size(44.dp),
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "لا توجد فصول مخصصة لك حاليًا",
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "تواصل مع إدارة المدرسة لإضافة فصولك",
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                )
                            }
                        }
                    }
                } else {
                    items(classes) { cls ->
                        ClassCard(cls, onClick = { onClassClick(cls) })
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ClassCard(cls: ClassAssignment, onClick: () -> Unit) {
    val gradeLabel = GRADE_LABELS[cls.gradeLevel] ?: "الصف ${cls.gradeLevel}"
    val interaction = remember { MutableInteractionSource() }
    Box(Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) { onClick() },
            cornerRadius = 22.dp,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Subject monogram badge
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(Gold500.copy(alpha = 0.25f), Gold700.copy(alpha = 0.1f)))
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cls.subjectName.take(2), color = Gold300, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        cls.className,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(gradeLabel, color = Gold400, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(cls.subjectName, color = TextMuted, fontSize = 12.sp)
                }

                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Gold500.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Gold400,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
