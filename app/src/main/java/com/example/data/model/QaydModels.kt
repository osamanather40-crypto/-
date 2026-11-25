package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ProofType(
    val wireName: String,
    val arabicTitle: String,
    val arabicSubtitle: String,
) {
    PHOTO_PROOF(
        wireName = "PHOTO_PROOF",
        arabicTitle = "التصوير الإلزامي لإثبات الإنجاز",
        arabicSubtitle = "الخيار الوحيد: صوّر نتيجة المهمة قبل انتهاء الوقت المحدد وإلا فُرضت عليك القيود فوراً.",
    );

    companion object {
        fun fromWire(@Suppress("UNUSED_PARAMETER") raw: String): ProofType = PHOTO_PROOF
    }
}

enum class TaskStatus(val wireName: String, val arabicLabel: String) {
    DRAFT("DRAFT", "مسودة قبل الختم"),
    SEALED("SEALED", "مختومة تسلسلياً"),
    ACTIVE("ACTIVE", "قيد التنفيذ المصوّر"),
    COMPLETED("COMPLETED", "مُثبتة بالصورة"),
    BREACHED("BREACHED", "مخروقة القيد (لم تُصوّر)");

    companion object {
        fun fromWire(raw: String): TaskStatus =
            entries.firstOrNull { it.wireName == raw || it.name == raw } ?: DRAFT
    }
}

enum class LedgerEventType(val wireName: String, val arabicLabel: String) {
    CONTRACT_SEALED("CONTRACT_SEALED", "توقيع ميثاق اليوم"),
    TASK_COMPLETED("TASK_COMPLETED", "إنجاز مُثبت بالصورة"),
    TASK_BREACHED("TASK_BREACHED", "خرق: لم تُصوّر المهمة"),
    DEBT_REPAID("DEBT_REPAID", "كفّارة مصوّرة لرفع القيود"),
    FORCE_MAJEURE("FORCE_MAJEURE", "استثناء قاهر موثق");

    companion object {
        fun fromWire(raw: String): LedgerEventType =
            entries.firstOrNull { it.wireName == raw || it.name == raw } ?: TASK_COMPLETED
    }
}

@Keep
@IgnoreExtraProperties
data class UserDisciplineProfile(
    val userId: String = "",
    val displayName: String = "مُلتزم",
    val sovereigntyScore: Long = 500L,
    val zeroBreachStreak: Long = 0L,
    val timeDebtMinutes: Long = 0L,
    val completedCount: Long = 0L,
    val breachCount: Long = 0L,
    val sealedContractDate: String? = null,
    val restrictedMode: Boolean = false,
    val restrictionReason: String? = null,
    val witnessName: String? = null,
    val witnessPhone: String? = null,
    val autoWhatsApp: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class QaydTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val orderIndex: Long = 0L,
    val durationMinutes: Long = 30L,
    val proofType: String = ProofType.PHOTO_PROOF.wireName,
    val status: String = TaskStatus.DRAFT.wireName,
    val contractDate: String = "",
    val targetTimeLabel: String? = null,
    val targetEffort: Long = 1L,
    val deadlineEpochMs: Long = 0L,
    val proofSummary: String? = null,
    val proofPhotoUri: String? = null,
    val debtPenaltyMinutes: Long = 0L,
    val recoveryTask: Boolean = false,
    val penaltyApplied: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun parsedProofType(): ProofType = ProofType.fromWire(proofType)
    fun parsedStatus(): TaskStatus = TaskStatus.fromWire(status)
}

@Keep
@IgnoreExtraProperties
data class LedgerEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val eventType: String = LedgerEventType.TASK_COMPLETED.wireName,
    val proofType: String = ProofType.PHOTO_PROOF.wireName,
    val proofNote: String = "",
    val proofPhotoUri: String? = null,
    val scoreDelta: Long = 0L,
    val debtMinutesDelta: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    fun parsedEventType(): LedgerEventType = LedgerEventType.fromWire(eventType)
}
