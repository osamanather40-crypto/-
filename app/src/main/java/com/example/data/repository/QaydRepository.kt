package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.LedgerEntry
import com.example.data.model.LedgerEventType
import com.example.data.model.ProofType
import com.example.data.model.QaydTask
import com.example.data.model.TaskStatus
import com.example.data.model.UserDisciplineProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class QaydRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google قبل الوصول إلى البيانات.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "تغيّر المستخدم المسجّل؛ تم رفض العملية." }
        }
        return uid
    }

    fun observeProfile(userId: String): Flow<UserDisciplineProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        UserDisciplineProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeTasks(userId: String): Flow<List<QaydTask>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("tasks")
        emitAll(
            colRef.orderBy("orderIndex", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QaydTask::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeLedger(userId: String): Flow<List<LedgerEntry>> = flow {
        val uid = requireUserId(userId)
        val colRef = db.collection("users").document(uid).collection("ledger")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LedgerEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(ex, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "مُلتزم" }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "sovereigntyScore" to 500L,
                    "zeroBreachStreak" to 0L,
                    "timeDebtMinutes" to 0L,
                    "completedCount" to 0L,
                    "breachCount" to 0L,
                    "restrictedMode" to false,
                    "autoWhatsApp" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun createDraftTask(
        userId: String,
        title: String,
        description: String,
        durationMinutes: Long,
        targetTimeLabel: String,
        contractDate: String,
        orderIndex: Long,
        alreadySealedToday: Boolean,
        hasActiveOrSealedTasks: Boolean,
    ): Result<Unit> {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        if (cleanTitle.isEmpty() || cleanTitle.length > 120) {
            return Result.failure(IllegalArgumentException("عنوان المهمة يجب أن يكون بين 1 و 120 حرفاً."))
        }
        if (cleanDesc.length > 300) {
            return Result.failure(IllegalArgumentException("الوصف يجب ألا يتجاوز 300 حرف."))
        }
        if (durationMinutes !in 1L..1440L) {
            return Result.failure(IllegalArgumentException("مدة الإنجاز يجب أن تكون بين دقيقة و 24 ساعة."))
        }

        var path = "users/$userId/tasks"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("tasks").document()
            path = docRef.path
            val initialStatus = when {
                !alreadySealedToday -> TaskStatus.DRAFT.wireName
                !hasActiveOrSealedTasks -> TaskStatus.ACTIVE.wireName
                else -> TaskStatus.SEALED.wireName
            }
            val deadlineMs = if (initialStatus == TaskStatus.ACTIVE.wireName) {
                System.currentTimeMillis() + durationMinutes * 60_000L
            } else {
                0L
            }

            val payload = mapOf(
                "userId" to uid,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "orderIndex" to orderIndex.coerceIn(0L, 500L),
                "durationMinutes" to durationMinutes,
                "proofType" to ProofType.PHOTO_PROOF.wireName,
                "status" to initialStatus,
                "contractDate" to contractDate.take(16),
                "targetTimeLabel" to targetTimeLabel.trim().take(32).ifBlank { null },
                "targetEffort" to 1L,
                "deadlineEpochMs" to deadlineMs,
                "debtPenaltyMinutes" to 0L,
                "recoveryTask" to false,
                "penaltyApplied" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDraftTask(userId: String, taskId: String): Result<Unit> {
        val path = "users/$userId/tasks/$taskId"
        return try {
            val uid = requireUserId(userId)
            require(taskId.isNotBlank()) { "معرّف المهمة فارغ." }
            db.collection("users").document(uid).collection("tasks").document(taskId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun swapDraftOrder(
        userId: String,
        firstTask: QaydTask,
        secondTask: QaydTask,
    ): Result<Unit> {
        val path = "users/$userId/tasks"
        return try {
            val uid = requireUserId(userId)
            val tasksCol = db.collection("users").document(uid).collection("tasks")
            val batch = db.batch()
            batch.update(
                tasksCol.document(firstTask.id),
                mapOf("orderIndex" to secondTask.orderIndex, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                tasksCol.document(secondTask.id),
                mapOf("orderIndex" to firstTask.orderIndex, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun sealDailyContract(
        userId: String,
        contractDate: String,
        draftTasks: List<QaydTask>,
        profile: UserDisciplineProfile,
    ): Result<Unit> {
        if (draftTasks.isEmpty()) {
            return Result.failure(IllegalArgumentException("أضف مهمة واحدة على الأقل قبل توقيع ميثاق اليوم."))
        }
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)
            val tasksCol = userDoc.collection("tasks")
            val ledgerRef = userDoc.collection("ledger").document()
            val nowMs = System.currentTimeMillis()

            val batch = db.batch()
            draftTasks.sortedBy { it.orderIndex }.forEachIndexed { idx, task ->
                val isFirst = idx == 0
                val nextStatus = if (isFirst) TaskStatus.ACTIVE.wireName else TaskStatus.SEALED.wireName
                val deadline = if (isFirst) nowMs + task.durationMinutes * 60_000L else 0L
                batch.update(
                    tasksCol.document(task.id),
                    mapOf(
                        "status" to nextStatus,
                        "orderIndex" to idx.toLong(),
                        "deadlineEpochMs" to deadline,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val nextScore = (profile.sovereigntyScore + 10L).coerceIn(0L, 1000L)
            batch.update(
                userDoc,
                mapOf(
                    "sealedContractDate" to contractDate.take(16),
                    "sovereigntyScore" to nextScore,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.set(
                ledgerRef,
                mapOf(
                    "userId" to uid,
                    "taskId" to "contract_${contractDate.replace("-", "")}",
                    "taskTitle" to "ميثاق يوم $contractDate (${draftTasks.size} مهام)",
                    "eventType" to LedgerEventType.CONTRACT_SEALED.wireName,
                    "proofType" to "SYSTEM",
                    "proofNote" to "تم ختم القفل التسلسلي — لا يُفتح التالي إلا بصورة برهان",
                    "scoreDelta" to 10L,
                    "debtMinutesDelta" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun completeActiveTaskWithPhoto(
        userId: String,
        activeTask: QaydTask,
        nextSealedTask: QaydTask?,
        photoPath: String,
        proofSummary: String,
        overdueMinutes: Long,
        profile: UserDisciplineProfile,
    ): Result<Unit> {
        val cleanPhoto = photoPath.trim()
        if (cleanPhoto.isBlank() || cleanPhoto.length > 500) {
            return Result.failure(IllegalArgumentException("يجب إرفاق صورة صالحة كبرهان للمهمة."))
        }
        val cleanSummary = proofSummary.trim().ifBlank { "برهان مصور موثق" }.take(300)
        val safeOverdue = overdueMinutes.coerceIn(0L, 10000L)
        val path = "users/$userId/tasks/${activeTask.id}"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)
            val tasksCol = userDoc.collection("tasks")
            val ledgerRef = userDoc.collection("ledger").document()
            val nowMs = System.currentTimeMillis()

            val batch = db.batch()
            val storedDebtPenalty = maxOf(activeTask.debtPenaltyMinutes, safeOverdue).coerceIn(0L, 10000L)
            batch.update(
                tasksCol.document(activeTask.id),
                mapOf(
                    "status" to TaskStatus.COMPLETED.wireName,
                    "proofSummary" to cleanSummary,
                    "proofPhotoUri" to cleanPhoto,
                    "debtPenaltyMinutes" to storedDebtPenalty,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            if (nextSealedTask != null) {
                val nextDeadline = nowMs + nextSealedTask.durationMinutes * 60_000L
                batch.update(
                    tasksCol.document(nextSealedTask.id),
                    mapOf(
                        "status" to TaskStatus.ACTIVE.wireName,
                        "deadlineEpochMs" to nextDeadline,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val scoreGain: Long
            val debtDelta: Long
            val eventType: LedgerEventType
            val nextRestricted: Boolean
            val nextReason: String

            if (activeTask.recoveryTask) {
                val repaid = minOf(profile.timeDebtMinutes, activeTask.durationMinutes)
                scoreGain = 35L
                debtDelta = -repaid
                eventType = LedgerEventType.DEBT_REPAID
                val remainingDebt = (profile.timeDebtMinutes + debtDelta).coerceAtLeast(0L)
                nextRestricted = remainingDebt > 0L
                nextReason = if (nextRestricted) "لا يزال عليك دين زمني متبقٍ ($remainingDebt د)" else ""
            } else if (activeTask.penaltyApplied) {
                scoreGain = 0L
                debtDelta = 0L
                eventType = LedgerEventType.TASK_COMPLETED
                nextRestricted = true
                nextReason = profile.restrictionReason?.takeIf { it.isNotBlank() }
                    ?: "قيود العقاب نشطة — نفّذ كفّارة مصوّرة لرفع القيد"
            } else if (safeOverdue > 0L) {
                scoreGain = -10L
                debtDelta = safeOverdue
                eventType = LedgerEventType.TASK_COMPLETED
                nextRestricted = true
                nextReason = "قيود مفروضة: تأخر التصوير عن الموعد المحدد بمقدار $safeOverdue دقيقة"
            } else {
                scoreGain = 25L
                debtDelta = 0L
                eventType = LedgerEventType.TASK_COMPLETED
                nextRestricted = profile.timeDebtMinutes > 0L
                nextReason = if (nextRestricted) "قيود نشطة بسبب دين زمني سابق (${profile.timeDebtMinutes} د)" else ""
            }

            val updatedScore = (profile.sovereigntyScore + scoreGain).coerceIn(0L, 1000L)
            val updatedDebt = (profile.timeDebtMinutes + debtDelta).coerceIn(0L, 100000L)
            val updatedStreak = if (safeOverdue > 0L && !activeTask.recoveryTask) 0L else (profile.zeroBreachStreak + 1L).coerceAtMost(10000L)
            val updatedCompleted = (profile.completedCount + 1L).coerceAtMost(1000000L)

            batch.update(
                userDoc,
                mapOf(
                    "sovereigntyScore" to updatedScore,
                    "timeDebtMinutes" to updatedDebt,
                    "zeroBreachStreak" to updatedStreak,
                    "completedCount" to updatedCompleted,
                    "restrictedMode" to nextRestricted,
                    "restrictionReason" to nextReason.take(200),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.set(
                ledgerRef,
                mapOf(
                    "userId" to uid,
                    "taskId" to activeTask.id,
                    "taskTitle" to activeTask.title.take(120),
                    "eventType" to eventType.wireName,
                    "proofType" to ProofType.PHOTO_PROOF.wireName,
                    "proofNote" to cleanSummary,
                    "proofPhotoUri" to cleanPhoto,
                    "scoreDelta" to scoreGain,
                    "debtMinutesDelta" to debtDelta,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun breachTask(
        userId: String,
        targetTask: QaydTask,
        nextSealedTask: QaydTask?,
        justification: String,
        isForceMajeure: Boolean,
        profile: UserDisciplineProfile,
    ): Result<Unit> {
        val cleanJustification = justification.trim()
        if (cleanJustification.length < 6) {
            return Result.failure(IllegalArgumentException("يجب كتابة تبرير صريح (٦ أحرف على الأقل) لتوثيق عدم التصوير."))
        }
        val path = "users/$userId/tasks/${targetTask.id}"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)
            val tasksCol = userDoc.collection("tasks")
            val ledgerRef = userDoc.collection("ledger").document()
            val nowMs = System.currentTimeMillis()

            val penaltyScore = if (isForceMajeure) -30L else -70L
            val debtAdded = targetTask.durationMinutes.coerceIn(1L, 1440L)
            val eventType = if (isForceMajeure) LedgerEventType.FORCE_MAJEURE else LedgerEventType.TASK_BREACHED

            val batch = db.batch()
            batch.update(
                tasksCol.document(targetTask.id),
                mapOf(
                    "status" to TaskStatus.BREACHED.wireName,
                    "proofSummary" to "لم يتم التصوير: ${cleanJustification}".take(300),
                    "debtPenaltyMinutes" to debtAdded,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            if (nextSealedTask != null) {
                batch.update(
                    tasksCol.document(nextSealedTask.id),
                    mapOf(
                        "status" to TaskStatus.ACTIVE.wireName,
                        "deadlineEpochMs" to (nowMs + nextSealedTask.durationMinutes * 60_000L),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val updatedScore = (profile.sovereigntyScore + penaltyScore).coerceIn(0L, 1000L)
            val updatedDebt = (profile.timeDebtMinutes + debtAdded).coerceIn(0L, 100000L)
            val updatedBreaches = (profile.breachCount + 1L).coerceAtMost(1000000L)

            batch.update(
                userDoc,
                mapOf(
                    "sovereigntyScore" to updatedScore,
                    "zeroBreachStreak" to 0L,
                    "timeDebtMinutes" to updatedDebt,
                    "breachCount" to updatedBreaches,
                    "restrictedMode" to true,
                    "restrictionReason" to "قيد إجباري: عدم تصوير مهمة «${targetTask.title.take(40)}» — مطلوب كفّارة مصوّرة",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.set(
                ledgerRef,
                mapOf(
                    "userId" to uid,
                    "taskId" to targetTask.id,
                    "taskTitle" to targetTask.title.take(120),
                    "eventType" to eventType.wireName,
                    "proofType" to ProofType.PHOTO_PROOF.wireName,
                    "proofNote" to "لم تُصوّر المهمة: ${cleanJustification}".take(300),
                    "scoreDelta" to penaltyScore,
                    "debtMinutesDelta" to debtAdded,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun createRecoverySprintTask(
        userId: String,
        debtMinutes: Long,
        contractDate: String,
        nextOrderIndex: Long,
        hasActiveTask: Boolean,
    ): Result<Unit> {
        val sprintDuration = debtMinutes.coerceIn(5L, 60L)
        var path = "users/$userId/tasks"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("tasks").document()
            path = docRef.path
            val initialStatus = if (hasActiveTask) TaskStatus.SEALED.wireName else TaskStatus.ACTIVE.wireName
            val deadlineMs = if (!hasActiveTask) System.currentTimeMillis() + sprintDuration * 60_000L else 0L
            val payload = mapOf(
                "userId" to uid,
                "title" to "مهمة كفّارة مصوّرة: سداد $sprintDuration د لرفع القيود",
                "description" to "أنجز عملاً حقيقياً وصوّره خلال المدة المحددة لفك تجميد الحساب وسداد الدين الزمني.",
                "orderIndex" to nextOrderIndex.coerceIn(0L, 500L),
                "durationMinutes" to sprintDuration,
                "proofType" to ProofType.PHOTO_PROOF.wireName,
                "status" to initialStatus,
                "contractDate" to contractDate.take(16),
                "targetTimeLabel" to "خلال $sprintDuration د",
                "targetEffort" to 1L,
                "deadlineEpochMs" to deadlineMs,
                "debtPenaltyMinutes" to 0L,
                "recoveryTask" to true,
                "penaltyApplied" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateWhatsAppWitness(
        userId: String,
        witnessName: String,
        witnessPhone: String,
        autoWhatsApp: Boolean,
    ): Result<Unit> {
        val cleanName = witnessName.trim().ifBlank { "المراقب" }.take(80)
        val formattedPhone = com.example.media.WhatsAppWitnessHelper.formatStoredPhone(witnessPhone)
        if (formattedPhone.isBlank() || !com.example.media.WhatsAppWitnessHelper.isValidWitnessPhone(formattedPhone)) {
            return Result.failure(IllegalArgumentException("أدخل رقم واتساب صحيحاً مع رمز الدولة (٨ إلى ١٨ رقماً)."))
        }
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "witnessName" to cleanName,
                        "witnessPhone" to formattedPhone.take(24),
                        "autoWhatsApp" to autoWhatsApp,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun enforceTimeoutPunishment(
        userId: String,
        activeTask: QaydTask,
        profile: UserDisciplineProfile,
    ): Result<Unit> {
        if (activeTask.penaltyApplied) return Result.success(Unit)
        val path = "users/$userId/tasks/${activeTask.id}"
        return try {
            val uid = requireUserId(userId)
            val userDoc = db.collection("users").document(uid)
            val taskRef = userDoc.collection("tasks").document(activeTask.id)
            val ledgerRef = userDoc.collection("ledger").document()

            val penaltyScore = -80L
            val penaltyDebt = 30L
            val updatedTaskDebt = (activeTask.debtPenaltyMinutes + penaltyDebt).coerceIn(0L, 10000L)
            val updatedScore = (profile.sovereigntyScore + penaltyScore).coerceIn(0L, 1000L)
            val updatedDebt = (profile.timeDebtMinutes + penaltyDebt).coerceIn(0L, 100000L)
            val updatedBreaches = (profile.breachCount + 1L).coerceAtMost(1000000L)
            val reason = "عقاب تلقائي: انتهى الوقت دون تصوير «${activeTask.title.take(40)}»".take(200)

            val batch = db.batch()
            batch.update(
                taskRef,
                mapOf(
                    "penaltyApplied" to true,
                    "debtPenaltyMinutes" to updatedTaskDebt,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                userDoc,
                mapOf(
                    "sovereigntyScore" to updatedScore,
                    "zeroBreachStreak" to 0L,
                    "timeDebtMinutes" to updatedDebt,
                    "breachCount" to updatedBreaches,
                    "restrictedMode" to true,
                    "restrictionReason" to reason,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                ledgerRef,
                mapOf(
                    "userId" to uid,
                    "taskId" to activeTask.id,
                    "taskTitle" to activeTask.title.take(120),
                    "eventType" to LedgerEventType.TASK_BREACHED.wireName,
                    "proofType" to ProofType.PHOTO_PROOF.wireName,
                    "proofNote" to "عقاب تلقائي: انتهت المهلة دون تصوير البرهان (-٨٠ سيادة، +٣٠ د دين)".take(300),
                    "scoreDelta" to penaltyScore,
                    "debtMinutesDelta" to penaltyDebt,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
