package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.UserDisciplineProfile
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.ChamberSnapshot
import com.example.ui.viewmodel.UiState

private data class IronLaw(val title: String, val detail: String)

private val QaydIronLaws = listOf(
    IronLaw(
        title = "١. خيار واحد فقط: التصوير الإلزامي",
        detail = "لا تُغلق أي مهمة دون التقاط أو إرفاق صورة تثبت إنجازها الفعلي.",
    ),
    IronLaw(
        title = "٢. تحديد موعد الإنجاز بدقة",
        detail = "تحدد أنت الساعة أو المدة الزمنية لإتمام المهمة قبل بدء العد التنازلي الصارم.",
    ),
    IronLaw(
        title = "٣. العقاب التلقائي والقيود عند انتهاء المدة",
        detail = "انقضاء المدة دون تصوير يطبّق عقاباً تلقائياً فورياً (-٨٠ سيادة و+٣٠ د دين زمني وتصفير السلسلة وتجميد إضافة المهام) حتى تنفّذ كفّارة مصوّرة.",
    ),
    IronLaw(
        title = "٤. الإرسال التلقائي عبر واتساب للشريك الرقيب",
        detail = "يفتح واتساب تلقائياً على محادثة الشخص الذي حددته مع صورة البرهان وتقرير الإنجاز فور توثيق كل مهمة.",
    ),
    IronLaw(
        title = "٥. النفق التسلسلي المغلق",
        detail = "لا يمكنك فتح المهمة الثانية قبل إثبات المهمة الأولى بالصورة، ولا حذف المهام بعد ختم ميثاق اليوم.",
    ),
    IronLaw(
        title = "٦. سجل الصور والشرف الأبدي",
        detail = "كل صورة أضفتها تبقى موثقة في معرض الصور والسجل الدائم لتكون شاهداً حقيقياً على التزامك.",
    ),
)

@Composable
fun ProtocolScreen(
    chamberState: UiState<ChamberSnapshot>,
    userEmail: String,
    onOpenWitnessConfig: () -> Unit,
    onSignOut: () -> Unit,
) {
    val profile = (chamberState as? UiState.Success)?.data?.profile ?: UserDisciplineProfile()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "account_card") {
                Surface(
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, BorderHairline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountCircle,
                                contentDescription = null,
                                tint = BurnishedAmber,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                if (userEmail.isNotBlank()) {
                                    Text(
                                        text = userEmail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            Surface(
                                color = if (profile.restrictedMode) CrimsonContainer else EmeraldContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = if (profile.restrictedMode) "مُقيّد" else "حرّ من القيود",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (profile.restrictedMode) CrimsonText else EmeraldVerified,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            StatBox(
                                title = "مهام مصوّرة",
                                value = "${profile.completedCount}",
                                modifier = Modifier.weight(1f),
                            )
                            StatBox(
                                title = "مخالفات قيود",
                                value = "${profile.breachCount}",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            border = BorderStroke(1.dp, CrimsonBreach.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = CrimsonBreach,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تسجيل الخروج الآمن",
                                color = CrimsonBreach,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            item(key = "whatsapp_witness_card") {
                val hasWitness = !profile.witnessPhone.isNullOrBlank()
                Surface(
                    color = ObsidianSurface,
                    border = BorderStroke(
                        1.dp,
                        if (hasWitness && profile.autoWhatsApp) EmeraldVerified.copy(alpha = 0.65f) else BurnishedAmber.copy(alpha = 0.65f),
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    tint = if (hasWitness && profile.autoWhatsApp) EmeraldVerified else BurnishedAmber,
                                )
                                Text(
                                    text = "الشريك الرقيب على واتساب",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            Surface(
                                color = if (hasWitness && profile.autoWhatsApp) EmeraldContainer else ObsidianElevated,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = when {
                                        !hasWitness -> "غير محدد"
                                        profile.autoWhatsApp -> "تلقائي مفعّل"
                                        else -> "تلقائي متوقف"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (hasWitness && profile.autoWhatsApp) EmeraldVerified else BurnishedAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                )
                            }
                        }

                        Text(
                            text = if (hasWitness) {
                                "الاسم: ${profile.witnessName ?: "المراقب"} • الرقم: ${profile.witnessPhone}"
                            } else {
                                "لم تحدد بعد الشخص الذي تُرسل إليه صور إنجازاتك عبر واتساب."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Button(
                            onClick = onOpenWitnessConfig,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BurnishedAmber,
                                contentColor = ObsidianVoid,
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("protocol_edit_witness_button"),
                        ) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasWitness) "تعديل الشخص المراقب على واتساب" else "تحديد الشخص المراقب الآن",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            item(key = "laws_title") {
                Text(
                    text = "قوانين الميثاق والقيود",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            itemsIndexed(QaydIronLaws, key = { idx, _ -> "law_$idx" }) { _, law ->
                Surface(
                    color = ObsidianSurface,
                    border = BorderStroke(1.dp, BorderHairline),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = law.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = BurnishedAmber,
                        )
                        Text(
                            text = law.detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MonoMetricMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
