package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.QaydTask
import com.example.data.model.TaskStatus
import com.example.data.model.UserDisciplineProfile
import com.example.ui.components.BreachProtocolDialog
import com.example.ui.components.PhysicalProofGateConsole
import com.example.ui.theme.BorderHairline
import com.example.ui.theme.BurnishedAmber
import com.example.ui.theme.CrimsonBreach
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonText
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.viewmodel.ChamberSnapshot
import com.example.ui.viewmodel.UiState
import kotlin.math.abs
import kotlinx.coroutines.delay

@Composable
fun ChamberScreen(
    state: UiState<ChamberSnapshot>,
    nowMillis: Long,
    isMutating: Boolean,
    onOpenCreateTask: () -> Unit,
    onDeleteDraft: (String) -> Unit,
    onMoveDraft: (QaydTask, Boolean) -> Unit,
    onSealContract: () -> Unit,
    onCompleteActiveTaskWithPhoto: (String, String) -> Unit,
    onBreachActiveTask: (String, Boolean) -> Unit,
    onSpawnRecoverySprint: () -> Unit,
    onOpenWitnessConfig: () -> Unit,
    onNotifyWitnessPunishment: (QaydTask) -> Unit,
) {
    var breachDialogTask by remember { mutableStateOf<QaydTask?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (state) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BurnishedAmber)
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = CrimsonContainer,
                        border = BorderStroke(1.dp, CrimsonBreach),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CrimsonText,
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            }

            is UiState.Success -> {
                val snapshot = state.data
                val activeOverdue = snapshot.activeTask?.let {
                    it.deadlineEpochMs > 0L && nowMillis > it.deadlineEpochMs
                } == true
                val isRestricted = snapshot.profile.restrictedMode || activeOverdue

                LazyColumn(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 104.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "telemetry_header") {
                        SovereigntyTelemetryCard(
                            profile = snapshot.profile,
                            activeOverdue = activeOverdue,
                            isRestricted = isRestricted,
                            hasActiveRecovery = snapshot.activeTask?.recoveryTask == true ||
                                snapshot.sealedTasks.any { it.recoveryTask },
                            isBusy = isMutating,
                            onSpawnRecoverySprint = onSpawnRecoverySprint,
                        )
                    }

                    item(key = "whatsapp_witness_banner") {
                        WhatsAppWitnessBannerCard(
                            profile = snapshot.profile,
                            onOpenWitnessConfig = onOpenWitnessConfig,
                        )
                    }

                    if (snapshot.activeTask != null) {
                        item(key = "active_spotlight_${snapshot.activeTask.id}") {
                            ActiveTaskSpotlightCard(
                                task = snapshot.activeTask,
                                nowMillis = nowMillis,
                                isBusy = isMutating,
                                witnessName = snapshot.profile.witnessName,
                                witnessPhone = snapshot.profile.witnessPhone,
                                autoWhatsApp = snapshot.profile.autoWhatsApp,
                                onPhotoProofVerified = onCompleteActiveTaskWithPhoto,
                                onRequestBreach = { breachDialogTask = snapshot.activeTask },
                                onNotifyWitnessPunishment = {
                                    onNotifyWitnessPunishment(snapshot.activeTask)
                                },
                            )
                        }
                    }

                    if (snapshot.sealedTasks.isNotEmpty()) {
                        item(key = "sealed_header") {
                            Text(
                                text = "القائمة المختومة تسلسلياً (${snapshot.sealedTasks.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        itemsIndexed(
                            items = snapshot.sealedTasks,
                            key = { _, task -> "sealed_${task.id}" },
                        ) { idx, task ->
                            SealedVaultTaskCard(
                                position = idx + 2,
                                durationMinutes = task.durationMinutes,
                                targetTimeLabel = task.targetTimeLabel,
                                isRecovery = task.recoveryTask,
                            )
                        }
                    }

                    if (snapshot.draftTasks.isNotEmpty()) {
                        item(key = "draft_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "مسودة ميثاق اليوم (${snapshot.draftTasks.size})",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = "رتّب الأولويات قبل الختم",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BurnishedAmber,
                                )
                            }
                        }

                        itemsIndexed(
                            items = snapshot.draftTasks,
                            key = { _, task -> "draft_${task.id}" },
                        ) { idx, task ->
                            DraftTaskCard(
                                index = idx,
                                totalCount = snapshot.draftTasks.size,
                                task = task,
                                isBusy = isMutating,
                                onMoveUp = { onMoveDraft(task, true) },
                                onMoveDown = { onMoveDraft(task, false) },
                                onDelete = { onDeleteDraft(task.id) },
                            )
                        }

                        item(key = "seal_contract_button") {
                            HoldToSealContractPad(
                                taskCount = snapshot.draftTasks.size,
                                isBusy = isMutating,
                                onSealed = onSealContract,
                            )
                        }
                    }

                    if (snapshot.activeTask == null &&
                        snapshot.sealedTasks.isEmpty() &&
                        snapshot.draftTasks.isEmpty()
                    ) {
                        item(key = "empty_chamber") {
                            EmptyChamberCard(
                                isSealedToday = snapshot.isContractSealedToday,
                                isRestricted = isRestricted,
                                finishedCount = snapshot.finishedTodayTasks.size,
                                onOpenCreateTask = onOpenCreateTask,
                                onSpawnRecoverySprint = onSpawnRecoverySprint,
                            )
                        }
                    }

                    if (snapshot.finishedTodayTasks.isNotEmpty()) {
                        item(key = "finished_header") {
                            Text(
                                text = "محصلة اليوم (${snapshot.finishedTodayTasks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        items(
                            items = snapshot.finishedTodayTasks,
                            key = { "done_${it.id}" },
                        ) { task ->
                            ResolvedTaskRow(task = task)
                        }
                    }
                }
            }
        }
    }

    val dialogTarget = breachDialogTask
    if (dialogTarget != null) {
        BreachProtocolDialog(
            task = dialogTarget,
            onDismiss = { breachDialogTask = null },
            onConfirmBreach = { justification, isForceMajeure ->
                breachDialogTask = null
                onBreachActiveTask(justification, isForceMajeure)
            },
        )
    }
}

@Composable
private fun SovereigntyTelemetryCard(
    profile: UserDisciplineProfile,
    activeOverdue: Boolean,
    isRestricted: Boolean,
    hasActiveRecovery: Boolean,
    isBusy: Boolean,
    onSpawnRecoverySprint: () -> Unit,
) {
    val scoreFraction = (profile.sovereigntyScore.toFloat() / 1000f).coerceIn(0f, 1f)
    val hasDebt = profile.timeDebtMinutes > 0L

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, if (isRestricted || hasDebt) CrimsonBreach.copy(alpha = 0.7f) else BorderHairline),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "مؤشر السيادة والالتزام",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${profile.sovereigntyScore}",
                            style = MonoMetricLarge,
                            color = if (isRestricted) CrimsonBreach else BurnishedAmber,
                        )
                        Text(
                            text = " / 1000",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TelemetryMiniBadge(
                        label = "سلسلة الصور",
                        value = "${profile.zeroBreachStreak}",
                        accent = EmeraldVerified,
                    )
                    TelemetryMiniBadge(
                        label = "دين زمني",
                        value = "${profile.timeDebtMinutes}د",
                        accent = if (hasDebt) CrimsonBreach else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { scoreFraction },
                color = if (isRestricted) CrimsonBreach else BurnishedAmber,
                trackColor = ObsidianVoid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )

            if (isRestricted || hasDebt) {
                Surface(
                    color = CrimsonContainer,
                    border = BorderStroke(1.dp, CrimsonBreach.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                Icons.Filled.Block,
                                contentDescription = null,
                                tint = CrimsonBreach,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = when {
                                    activeOverdue -> "قيود فورية نشطة: انتهى الوقت ولم تُصوّر المهمة! إضافة مهام جديدة مجمّدة."
                                    !profile.restrictionReason.isNullOrBlank() -> profile.restrictionReason
                                    else -> "قيود نشطة بسبب دين زمني (${profile.timeDebtMinutes} د). نفّذ كفّارة مصوّرة لرفع القيد."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonText,
                            )
                        }
                        if (!hasActiveRecovery && !activeOverdue) {
                            TextButton(
                                onClick = onSpawnRecoverySprint,
                                enabled = !isBusy,
                                modifier = Modifier.testTag("spawn_recovery_button"),
                            ) {
                                Text("بدء مهمة كفّارة مصوّرة لرفع القيود", color = BurnishedAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryMiniBadge(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    Surface(
        color = ObsidianElevated,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MonoMetricMedium,
                color = accent,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WhatsAppWitnessBannerCard(
    profile: UserDisciplineProfile,
    onOpenWitnessConfig: () -> Unit,
) {
    val hasWitness = !profile.witnessPhone.isNullOrBlank()
    val borderColor = when {
        !hasWitness -> BurnishedAmber.copy(alpha = 0.75f)
        profile.autoWhatsApp -> EmeraldVerified.copy(alpha = 0.65f)
        else -> BorderHairline
    }

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("whatsapp_witness_banner"),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = if (hasWitness && profile.autoWhatsApp) EmeraldContainer else BurnishedAmber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = null,
                        tint = if (hasWitness && profile.autoWhatsApp) EmeraldVerified else BurnishedAmber,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (hasWitness) {
                        "المراقب على واتساب: ${profile.witnessName ?: "الشريك"}"
                    } else {
                        "حدّد شخصاً لإرسال الصور له عبر واتساب"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = when {
                        !hasWitness -> "عند التصوير يفتح واتساب تلقائياً على محادثة الشخص المحدد."
                        profile.autoWhatsApp -> "تلقائي مفعّل • ${profile.witnessPhone}"
                        else -> "التشغيل التلقائي متوقف • ${profile.witnessPhone}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasWitness && profile.autoWhatsApp) EmeraldVerified else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedButton(
                onClick = onOpenWitnessConfig,
                border = BorderStroke(1.dp, if (hasWitness) BorderHairline else BurnishedAmber),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("configure_witness_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = BurnishedAmber,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (hasWitness) "تعديل" else "تحديد",
                    style = MaterialTheme.typography.labelLarge,
                    color = BurnishedAmber,
                )
            }
        }
    }
}

@Composable
private fun ActiveTaskSpotlightCard(
    task: QaydTask,
    nowMillis: Long,
    isBusy: Boolean,
    witnessName: String?,
    witnessPhone: String?,
    autoWhatsApp: Boolean,
    onPhotoProofVerified: (String, String) -> Unit,
    onRequestBreach: () -> Unit,
    onNotifyWitnessPunishment: () -> Unit,
) {
    val remainingMs = if (task.deadlineEpochMs > 0L) task.deadlineEpochMs - nowMillis else task.durationMinutes * 60_000L
    val isOverdue = remainingMs < 0L
    val isPunished = isOverdue || task.penaltyApplied
    val absSeconds = abs(remainingMs) / 1000L
    val hours = absSeconds / 3600L
    val mins = (absSeconds % 3600L) / 60L
    val secs = absSeconds % 60L
    val formattedClock = if (hours > 0L) {
        "${if (isOverdue) "-" else ""}%02d:%02d:%02d".format(hours, mins, secs)
    } else {
        "${if (isOverdue) "-" else ""}%02d:%02d".format(mins, secs)
    }

    Surface(
        color = ObsidianSurface,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isPunished) CrimsonBreach else BurnishedAmber,
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_task_card"),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = if (isPunished) CrimsonContainer else BurnishedAmber.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = when {
                            task.penaltyApplied -> "عقاب تلقائي نافذ!"
                            isOverdue -> "مُقيّد — تأخر التصوير!"
                            task.recoveryTask -> "كفّارة مصوّرة لرفع القيود"
                            else -> "المهمة النشطة الوحيدة"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isPunished) CrimsonText else BurnishedAmber,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = if (isPunished) CrimsonBreach else BurnishedAmber,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedClock,
                        style = MonoMetricMedium,
                        color = if (isPunished) CrimsonBreach else BurnishedAmber,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!task.targetTimeLabel.isNullOrBlank()) {
                    Text(
                        text = "الموعد المحدد للتصوير: ${task.targetTimeLabel} (${task.durationMinutes} د)",
                        style = MaterialTheme.typography.labelMedium,
                        color = BurnishedAmber,
                    )
                }
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isPunished) {
                Surface(
                    color = CrimsonContainer,
                    border = BorderStroke(1.dp, CrimsonBreach),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("punishment_active_banner"),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = CrimsonBreach)
                            Text(
                                text = if (task.penaltyApplied) {
                                    "تم تنفيذ العقاب التلقائي لانتهاء المهلة دون تصوير: خصم ٨٠ سيادة + ٣٠ د دين زمني + تصفير السلسلة + تجميد المهام. صوّر المهمة الآن للمتابعة."
                                } else {
                                    "انتهى الوقت دون تصوير! جارٍ تطبيق العقاب التلقائي — صوّر المهمة فوراً."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrimsonText,
                            )
                        }
                        if (!witnessPhone.isNullOrBlank()) {
                            Button(
                                onClick = onNotifyWitnessPunishment,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CrimsonBreach,
                                    contentColor = MaterialTheme.colorScheme.onError,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("notify_witness_punishment_button"),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إبلاغ ${witnessName ?: witnessPhone} بالعقاب عبر واتساب",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }

            if (autoWhatsApp && !witnessPhone.isNullOrBlank()) {
                Surface(
                    color = EmeraldContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, EmeraldVerified.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = null,
                            tint = EmeraldVerified,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "سيفتح واتساب تلقائياً لإرسال الصورة إلى ${witnessName ?: witnessPhone} فور التوثيق.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldVerified,
                        )
                    }
                }
            }

            PhysicalProofGateConsole(
                isOverdue = isOverdue,
                isBusy = isBusy,
                onPhotoProofVerified = onPhotoProofVerified,
            )

            OutlinedButton(
                onClick = onRequestBreach,
                enabled = !isBusy,
                border = BorderStroke(1.dp, CrimsonBreach.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("breach_protocol_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Gavel,
                    contentDescription = null,
                    tint = CrimsonBreach,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "لم أستطع التصوير (تفعيل القيود)",
                    style = MaterialTheme.typography.labelLarge,
                    color = CrimsonBreach,
                )
            }
        }
    }
}

@Composable
private fun SealedVaultTaskCard(
    position: Int,
    durationMinutes: Long,
    targetTimeLabel: String?,
    isRecovery: Boolean,
) {
    Surface(
        color = ObsidianSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sealed_task_card_$position"),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = stringResource(id = R.string.cd_seal_lock),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRecovery) "كفّارة مصوّرة مختومة #$position" else "مهمة مختومة تسلسلياً #$position",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "تُفتح تلقائياً فور تصوير المهمة الحالية${if (!targetTimeLabel.isNullOrBlank()) " • $targetTimeLabel" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                )
            }
            Text(
                text = "${durationMinutes}د",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DraftTaskCard(
    index: Int,
    totalCount: Int,
    task: QaydTask,
    isBusy: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, BorderHairline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("draft_task_card_${task.id}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = ObsidianElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BurnishedAmber,
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "الموعد: ${task.targetTimeLabel ?: "${task.durationMinutes} د"} (${task.durationMinutes} د) • إثبات بالتصوير",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isBusy && index > 0,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = "رفع الأولوية",
                        tint = if (index > 0) BurnishedAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isBusy && index < totalCount - 1,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = "خفض الأولوية",
                        tint = if (index < totalCount - 1) BurnishedAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = !isBusy,
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف المسودة",
                        tint = CrimsonBreach,
                    )
                }
            }
        }
    }
}

@Composable
private fun HoldToSealContractPad(
    taskCount: Int,
    isBusy: Boolean,
    onSealed: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var isHolding by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val animProgress by animateFloatAsState(targetValue = progress, label = "seal_contract_progress")

    LaunchedEffect(isHolding) {
        if (isHolding && !isBusy) {
            val totalMs = 1800L
            var elapsed = 0L
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            while (isHolding && elapsed < totalMs) {
                delay(40L)
                elapsed += 40L
                progress = (elapsed.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
            }
            if (isHolding && elapsed >= totalMs) {
                isHolding = false
                progress = 0f
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSealed()
            }
        } else {
            progress = 0f
        }
    }

    Surface(
        color = if (isHolding) BurnishedAmber.copy(alpha = 0.22f) else BurnishedAmber.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, BurnishedAmber),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("seal_contract_pad")
            .pointerInput(isBusy) {
                detectTapGestures(
                    onPress = {
                        if (isBusy) return@detectTapGestures
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                    }
                )
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(34.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.size(32.dp)) {
                    drawArc(
                        color = BorderHairline,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = BurnishedAmber,
                        startAngle = -90f,
                        sweepAngle = 360f * animProgress,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = BurnishedAmber,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isHolding) {
                    "جارٍ توقيع وختم الميثاق..."
                } else {
                    "اضغط مطوّلاً لختم ميثاق اليوم ($taskCount مهام — بلا تراجع)"
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = BurnishedAmber,
            )
        }
    }
}

@Composable
private fun EmptyChamberCard(
    isSealedToday: Boolean,
    isRestricted: Boolean,
    finishedCount: Int,
    onOpenCreateTask: () -> Unit,
    onSpawnRecoverySprint: () -> Unit,
) {
    Surface(
        color = ObsidianSurface,
        border = BorderStroke(1.dp, if (isRestricted) CrimsonBreach else BorderHairline),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_empty_vault),
                contentDescription = stringResource(id = R.string.cd_empty_vault),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Text(
                text = when {
                    isRestricted -> "حسابك في حالة تقييد إجباري"
                    isSealedToday && finishedCount > 0 -> "تم تصوير وإثبات ميثاق اليوم"
                    else -> "غرفة الإلزام جاهزة لصياغة ميثاقك"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (isRestricted) CrimsonBreach else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = when {
                    isRestricted -> "إضافة المهام العادية محظورة لأنك لم تصوّر مهمة سابقة في وقتها. نفّذ كفّارة مصوّرة لرفع القيد."
                    isSealedToday && finishedCount > 0 -> "أتممت $finishedCount مهام بالصور الموثقة. كل الصور محفوظة في سجل الصور."
                    else -> "أضف مهامك وحدد وقت الإنجاز الدقيق لكل منها. البرهان الوحيد هو التصوير قبل انقضاء الوقت."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (isRestricted) {
                Button(
                    onClick = onSpawnRecoverySprint,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CrimsonBreach,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_recovery_button"),
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("بدء مهمة كفّارة مصوّرة لرفع القيود", style = MaterialTheme.typography.labelLarge)
                }
            } else {
                Button(
                    onClick = onOpenCreateTask,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurnishedAmber,
                        contentColor = ObsidianVoid,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_add_task_button"),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة مهمة بوقت محدد", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun ResolvedTaskRow(task: QaydTask) {
    val isCompleted = task.parsedStatus() == TaskStatus.COMPLETED
    Surface(
        color = if (isCompleted) EmeraldContainer.copy(alpha = 0.45f) else CrimsonContainer.copy(alpha = 0.45f),
        border = BorderStroke(
            1.dp,
            if (isCompleted) EmeraldVerified.copy(alpha = 0.4f) else CrimsonBreach.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (isCompleted) EmeraldVerified else CrimsonBreach,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!task.proofSummary.isNullOrBlank()) {
                    Text(
                        text = task.proofSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
