package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.QaydTask
import com.example.media.WhatsAppWitnessHelper
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String, durationMinutes: Long, targetTimeLabel: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // Mode 0: Specify duration (hours + minutes), Mode 1: Specify exact target clock time (HH:mm)
    var useClockTimeMode by remember { mutableStateOf(false) }

    var durationMinutes by remember { mutableLongStateOf(30L) }

    val initialCal = remember {
        Calendar.getInstance().apply { add(Calendar.MINUTE, 30) }
    }
    var targetHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var targetMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    val computedDurationFromClock: Long = remember(targetHour, targetMinute) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val diffMins = ((target.timeInMillis - now.timeInMillis) / 60_000L).coerceIn(1L, 1440L)
        diffMins
    }

    val effectiveDuration = if (useClockTimeMode) computedDurationFromClock else durationMinutes.coerceIn(1L, 1440L)
    val effectiveEndLabel = remember(effectiveDuration, useClockTimeMode, targetHour, targetMinute) {
        if (useClockTimeMode) {
            "%02d:%02d".format(targetHour, targetMinute)
        } else {
            val endMs = System.currentTimeMillis() + effectiveDuration * 60_000L
            SimpleDateFormat("HH:mm", Locale.US).format(Date(endMs))
        }
    }

    val quickDurationPresets = listOf(15L, 30L, 45L, 60L, 120L, 180L)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "صياغة قيد مصوّر جديد",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("عنوان المهمة الإلزامية") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_task_title_input"),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 300) description = it },
                    label = { Text("ماذا ستصوّر لإثبات الإنجاز؟ (اختياري)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_task_desc_input"),
                )

                // Single mandatory proof notice
                Surface(
                    color = BurnishedAmber.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BurnishedAmber.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = BurnishedAmber,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "طريقة الإثبات الوحيدة: التصوير",
                                style = MaterialTheme.typography.titleMedium,
                                color = BurnishedAmber,
                            )
                            Text(
                                text = "لو انتهى الوقت المحدد ولم تصوّر المهمة تُفرض عليك قيود فورية.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Text(
                    text = "حدد الوقت الذي تريد الإنجاز فيه",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !useClockTimeMode,
                        onClick = { useClockTimeMode = false },
                        label = { Text("حسب المدة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BurnishedAmber,
                            selectedLabelColor = ObsidianVoid,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = useClockTimeMode,
                        onClick = { useClockTimeMode = true },
                        label = { Text("حسب الساعة") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BurnishedAmber,
                            selectedLabelColor = ObsidianVoid,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                if (!useClockTimeMode) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        quickDurationPresets.forEach { preset ->
                            val label = if (preset >= 60L) "${preset / 60} س" else "$preset د"
                            FilterChip(
                                selected = durationMinutes == preset,
                                onClick = { durationMinutes = preset },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BurnishedAmber,
                                    selectedLabelColor = ObsidianVoid,
                                ),
                            )
                        }
                    }

                    // Fine-grained hours & minutes steppers
                    val hours = (durationMinutes / 60L).toInt()
                    val mins = (durationMinutes % 60L).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TimeUnitStepper(
                            label = "ساعات",
                            value = hours,
                            onDecrement = {
                                durationMinutes = (durationMinutes - 60L).coerceAtLeast(1L)
                            },
                            onIncrement = {
                                durationMinutes = (durationMinutes + 60L).coerceAtMost(1440L)
                            },
                            modifier = Modifier.weight(1f),
                        )
                        TimeUnitStepper(
                            label = "دقائق",
                            value = mins,
                            onDecrement = {
                                durationMinutes = (durationMinutes - 5L).coerceAtLeast(1L)
                            },
                            onIncrement = {
                                durationMinutes = (durationMinutes + 5L).coerceAtMost(1440L)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        TimeUnitStepper(
                            label = "الساعة (24)",
                            value = targetHour,
                            onDecrement = { targetHour = (targetHour + 23) % 24 },
                            onIncrement = { targetHour = (targetHour + 1) % 24 },
                            modifier = Modifier.weight(1f),
                        )
                        TimeUnitStepper(
                            label = "الدقيقة",
                            value = targetMinute,
                            onDecrement = { targetMinute = (targetMinute + 55) % 60 },
                            onIncrement = { targetMinute = (targetMinute + 5) % 60 },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Surface(
                    color = ObsidianElevated,
                    border = BorderStroke(1.dp, BorderHairline),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = BurnishedAmber,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "موعد الإنجاز: الساعة $effectiveEndLabel (المهلة: ${formatDurationArabic(effectiveDuration)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(title, description, effectiveDuration, "الساعة $effectiveEndLabel")
                },
                enabled = title.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedAmber,
                    contentColor = ObsidianVoid,
                ),
                modifier = Modifier.testTag("confirm_create_task_button"),
            ) {
                Text("تثبيت القيد", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}

@Composable
private fun TimeUnitStepper(
    label: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onDecrement) {
                    Icon(Icons.Filled.Remove, contentDescription = "إنقاص", tint = BurnishedAmber)
                }
                Text(
                    text = "%02d".format(value),
                    style = MonoMetricMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onIncrement) {
                    Icon(Icons.Filled.Add, contentDescription = "زيادة", tint = BurnishedAmber)
                }
            }
        }
    }
}

private fun formatDurationArabic(totalMinutes: Long): String {
    val h = totalMinutes / 60L
    val m = totalMinutes % 60L
    return when {
        h > 0 && m > 0 -> "$h س و $m د"
        h > 0 -> "$h ساعة"
        else -> "$m دقيقة"
    }
}

@Composable
fun BreachProtocolDialog(
    task: QaydTask,
    onDismiss: () -> Unit,
    onConfirmBreach: (justification: String, isForceMajeure: Boolean) -> Unit,
) {
    var justification by remember { mutableStateOf("") }
    var isForceMajeure by remember { mutableStateOf(false) }
    val penaltyScore = if (isForceMajeure) 30 else 70

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "إنذار: الخروج دون تصوير المهمة",
                style = MaterialTheme.typography.headlineSmall,
                color = CrimsonBreach,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    color = CrimsonContainer,
                    border = BorderStroke(1.dp, CrimsonBreach),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "تحذير: عدم تصوير برهان المهمة سيفرض عليك قيد تجميد إضافة المهام الجديدة، ويخصم $penaltyScore نقطة سيادة، ويضيف ${task.durationMinutes} دقيقة ديناً زمنياً حتى تنفذ كفّارة مصوّرة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonText,
                        modifier = Modifier.padding(12.dp),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ظرف قاهر منع التصوير",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "يُخفّض الخصم من ٧٠ إلى ٣٠ نقطة مع بقاء القيد",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = isForceMajeure,
                        onCheckedChange = { isForceMajeure = it },
                    )
                }

                OutlinedTextField(
                    value = justification,
                    onValueChange = { if (it.length <= 280) justification = it },
                    label = { Text("سبب عدم التصوير للتوثيق الأبدي (٦ أحرف فأكثر)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("breach_justification_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmBreach(justification, isForceMajeure) },
                enabled = justification.trim().length >= 6,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonBreach,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
                modifier = Modifier.testTag("confirm_breach_button"),
            ) {
                Text("تأكيد وفرض القيود")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("تراجع وتصوير المهمة")
            }
        },
    )
}

@Composable
fun WitnessConfigDialog(
    initialName: String?,
    initialPhone: String?,
    initialAutoWhatsApp: Boolean,
    onDismiss: () -> Unit,
    onSave: (witnessName: String, witnessPhone: String, autoWhatsApp: Boolean) -> Unit,
) {
    var witnessName by remember { mutableStateOf(initialName.orEmpty()) }
    var witnessPhone by remember { mutableStateOf(initialPhone.orEmpty()) }
    var autoWhatsApp by remember { mutableStateOf(initialAutoWhatsApp) }
    val phoneValid = WhatsAppWitnessHelper.isValidWitnessPhone(witnessPhone)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        title = {
            Text(
                text = "تحديد الشخص المراقب على واتساب",
                style = MaterialTheme.typography.headlineSmall,
                color = BurnishedAmber,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = EmeraldVerified.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = EmeraldVerified,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "عند إنهاء أي مهمة وتصويرها سيفتح واتساب تلقائياً على محادثة هذا الشخص مع إرفاق الصورة وتقرير الإنجاز جاهزاً للإرسال.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                OutlinedTextField(
                    value = witnessName,
                    onValueChange = { if (it.length <= 80) witnessName = it },
                    label = { Text("اسم الشخص (مثال: أبي، شريكي، المدرب)") },
                    leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("witness_name_input"),
                )

                OutlinedTextField(
                    value = witnessPhone,
                    onValueChange = { if (it.length <= 24) witnessPhone = it },
                    label = { Text("رقم الواتساب مع رمز الدولة (مثال: +962791234567)") },
                    leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    isError = witnessPhone.isNotBlank() && !phoneValid,
                    supportingText = {
                        if (witnessPhone.isNotBlank() && !phoneValid) {
                            Text("أدخل رقماً دولياً صحيحاً (٨ إلى ١٨ رقماً مع رمز الدولة)")
                        } else {
                            Text("لا تضع صفر البداية المحلي بعد رمز الدولة")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("witness_phone_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "فتح واتساب تلقائياً عند كل صورة",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "يشمل أيضاً تنبيه المراقب عند انتهاء المهلة والعقاب",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = autoWhatsApp,
                        onCheckedChange = { autoWhatsApp = it },
                        modifier = Modifier.testTag("witness_auto_switch"),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(witnessName, witnessPhone, autoWhatsApp) },
                enabled = phoneValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BurnishedAmber,
                    contentColor = ObsidianVoid,
                ),
                modifier = Modifier.testTag("save_witness_button"),
            ) {
                Text("حفظ الشريك الرقيب", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
    )
}
