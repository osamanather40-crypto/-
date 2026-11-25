package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.LedgerEntry
import com.example.data.model.QaydTask
import com.example.data.model.TaskStatus
import com.example.data.model.UserDisciplineProfile
import com.example.data.repository.QaydRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class ChamberSnapshot(
    val profile: UserDisciplineProfile,
    val draftTasks: List<QaydTask>,
    val activeTask: QaydTask?,
    val sealedTasks: List<QaydTask>,
    val finishedTodayTasks: List<QaydTask>,
    val todayDate: String,
    val isContractSealedToday: Boolean,
)

sealed interface WhatsAppDispatchEvent {
    data class ProofPhoto(
        val witnessName: String?,
        val witnessPhone: String,
        val taskTitle: String,
        val proofNote: String,
        val photoPath: String,
    ) : WhatsAppDispatchEvent

    data class PunishmentAlert(
        val witnessName: String?,
        val witnessPhone: String,
        val taskTitle: String,
        val targetTimeLabel: String?,
    ) : WhatsAppDispatchEvent
}

class QaydViewModel(
    private val repository: QaydRepository,
    private val currentUserId: String,
    private val displayName: String,
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _whatsAppEvent = MutableStateFlow<WhatsAppDispatchEvent?>(null)
    val whatsAppEvent: StateFlow<WhatsAppDispatchEvent?> = _whatsAppEvent.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    private val punishedTaskIds = mutableSetOf<String>()

    val nowMillis: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = System.currentTimeMillis(),
    )

    val chamberState: StateFlow<UiState<ChamberSnapshot>> = combine(
        repository.observeProfile(currentUserId),
        repository.observeTasks(currentUserId),
    ) { profileOrNull, allTasks ->
        val today = todayDateKey()
        val profile = profileOrNull ?: UserDisciplineProfile(
            userId = currentUserId,
            displayName = displayName.ifBlank { "مُلتزم" },
        )
        val sortedTasks = allTasks.sortedBy { it.orderIndex }
        val drafts = sortedTasks.filter { it.parsedStatus() == TaskStatus.DRAFT }
        val active = sortedTasks.firstOrNull { it.parsedStatus() == TaskStatus.ACTIVE }
        val sealed = sortedTasks.filter { it.parsedStatus() == TaskStatus.SEALED }
        val finishedToday = sortedTasks.filter {
            it.contractDate == today &&
                (it.parsedStatus() == TaskStatus.COMPLETED || it.parsedStatus() == TaskStatus.BREACHED)
        }
        val contractSealed = profile.sealedContractDate == today ||
            active != null ||
            sealed.isNotEmpty()

        UiState.Success(
            ChamberSnapshot(
                profile = profile,
                draftTasks = drafts,
                activeTask = active,
                sealedTasks = sealed,
                finishedTodayTasks = finishedToday,
                todayDate = today,
                isContractSealedToday = contractSealed,
            )
        ) as UiState<ChamberSnapshot>
    }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing chamber state", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "تعذّر تحميل بيانات غرفة الإلزام."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val ledgerState: StateFlow<UiState<List<LedgerEntry>>> = repository.observeLedger(currentUserId)
        .map<List<LedgerEntry>, UiState<List<LedgerEntry>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing ledger", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "تعذّر تحميل سجل الشرف والصور."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    val photoEntriesState: StateFlow<UiState<List<LedgerEntry>>> = ledgerState
        .map { state ->
            when (state) {
                is UiState.Loading -> UiState.Loading
                is UiState.Error -> state
                is UiState.Success -> UiState.Success(
                    state.data.filter { !it.proofPhotoUri.isNullOrBlank() }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading,
        )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, displayName).onFailure { err ->
                _actionMessage.value = err.localizedMessage ?: "تعذّر تهيئة ملف الانضباط."
            }
        }

        // Automatic timeout punishment observer
        viewModelScope.launch {
            combine(chamberState, nowMillis) { state, now ->
                state to now
            }.collect { (state, now) ->
                val snapshot = (state as? UiState.Success)?.data ?: return@collect
                val active = snapshot.activeTask ?: return@collect
                if (active.deadlineEpochMs > 0L &&
                    now > active.deadlineEpochMs &&
                    !active.penaltyApplied &&
                    punishedTaskIds.add(active.id)
                ) {
                    repository.enforceTimeoutPunishment(
                        userId = currentUserId,
                        activeTask = active,
                        profile = snapshot.profile,
                    ).onSuccess {
                        _actionMessage.value =
                            "انتهى الوقت دون تصوير! نُفّذ العقاب التلقائي (-٨٠ سيادة، +٣٠ د دين، وتجميد الإضافة)."
                        val witnessPhone = snapshot.profile.witnessPhone
                        if (snapshot.profile.autoWhatsApp && !witnessPhone.isNullOrBlank()) {
                            _whatsAppEvent.value = WhatsAppDispatchEvent.PunishmentAlert(
                                witnessName = snapshot.profile.witnessName,
                                witnessPhone = witnessPhone,
                                taskTitle = active.title,
                                targetTimeLabel = active.targetTimeLabel,
                            )
                        }
                    }.onFailure {
                        punishedTaskIds.remove(active.id)
                    }
                }
            }
        }
    }

    fun consumeMessage() {
        _actionMessage.value = null
    }

    fun consumeWhatsAppEvent() {
        _whatsAppEvent.value = null
    }

    fun saveWhatsAppWitness(witnessName: String, witnessPhone: String, autoWhatsApp: Boolean) {
        runMutation {
            repository.updateWhatsAppWitness(
                userId = currentUserId,
                witnessName = witnessName,
                witnessPhone = witnessPhone,
                autoWhatsApp = autoWhatsApp,
            ).onSuccess {
                _actionMessage.value = if (autoWhatsApp) {
                    "تم حفظ الشريك الرقيب وتفعيل الإرسال التلقائي عبر واتساب."
                } else {
                    "تم حفظ بيانات الشريك الرقيب (الإرسال التلقائي متوقف)."
                }
            }
        }
    }

    fun notifyWitnessPunishment(task: QaydTask) {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val witnessPhone = current.profile.witnessPhone
        if (witnessPhone.isNullOrBlank()) {
            _actionMessage.value = "حدّد رقم واتساب الشريك الرقيب أولاً."
            return
        }
        _whatsAppEvent.value = WhatsAppDispatchEvent.PunishmentAlert(
            witnessName = current.profile.witnessName,
            witnessPhone = witnessPhone,
            taskTitle = task.title,
            targetTimeLabel = task.targetTimeLabel,
        )
    }

    fun addTask(
        title: String,
        description: String,
        durationMinutes: Long,
        targetTimeLabel: String,
    ) {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val hasOverdueActive = current.activeTask?.let {
            it.deadlineEpochMs > 0L && System.currentTimeMillis() > it.deadlineEpochMs
        } == true

        if (current.profile.restrictedMode || hasOverdueActive) {
            _actionMessage.value = "ممنوع إضافة مهام جديدة! صوّر المهمة الحالية أو نفّذ مهمة كفّارة لرفع القيود أولاً."
            return
        }

        val nextOrder = (
            current.draftTasks.maxOfOrNull { it.orderIndex }
                ?: current.sealedTasks.maxOfOrNull { it.orderIndex }
                ?: current.activeTask?.orderIndex
                ?: -1L
            ) + 1L

        runMutation {
            repository.createDraftTask(
                userId = currentUserId,
                title = title,
                description = description,
                durationMinutes = durationMinutes,
                targetTimeLabel = targetTimeLabel,
                contractDate = current.todayDate,
                orderIndex = nextOrder,
                alreadySealedToday = current.isContractSealedToday,
                hasActiveOrSealedTasks = current.activeTask != null || current.sealedTasks.isNotEmpty(),
            ).onSuccess {
                _actionMessage.value = if (current.isContractSealedToday) {
                    "أُضيفت المهمة بالموعد المحدد إلى الميثاق المختوم."
                } else {
                    "أُضيفت المهمة بموعد الإنجاز ($targetTimeLabel) إلى مسودة الميثاق."
                }
            }
        }
    }

    fun deleteDraft(taskId: String) {
        runMutation {
            repository.deleteDraftTask(currentUserId, taskId).onSuccess {
                _actionMessage.value = "تم حذف المسودة."
            }
        }
    }

    fun moveDraft(task: QaydTask, moveUp: Boolean) {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val list = current.draftTasks.sortedBy { it.orderIndex }
        val index = list.indexOfFirst { it.id == task.id }
        if (index == -1) return
        val targetIndex = if (moveUp) index - 1 else index + 1
        if (targetIndex !in list.indices) return
        val neighbor = list[targetIndex]
        runMutation {
            repository.swapDraftOrder(currentUserId, task, neighbor)
        }
    }

    fun sealContract() {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        runMutation {
            repository.sealDailyContract(
                userId = currentUserId,
                contractDate = current.todayDate,
                draftTasks = current.draftTasks,
                profile = current.profile,
            ).onSuccess {
                _actionMessage.value = "تم توقيع الميثاق وختم القفل التسلسلي (+١٠ سيادة)."
            }
        }
    }

    fun completeActiveTaskWithPhoto(photoPath: String, proofSummary: String) {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val active = current.activeTask ?: return
        val nextSealed = current.sealedTasks.minByOrNull { it.orderIndex }
        val now = System.currentTimeMillis()
        val overdueMinutes = if (active.deadlineEpochMs > 0L && now > active.deadlineEpochMs) {
            ((now - active.deadlineEpochMs) / 60_000L).coerceAtLeast(1L)
        } else {
            0L
        }

        runMutation {
            repository.completeActiveTaskWithPhoto(
                userId = currentUserId,
                activeTask = active,
                nextSealedTask = nextSealed,
                photoPath = photoPath,
                proofSummary = proofSummary,
                overdueMinutes = overdueMinutes,
                profile = current.profile,
            ).onSuccess {
                _actionMessage.value = when {
                    active.recoveryTask -> "تم توثيق صورة الكفّارة وسداد الدين الزمني (+٣٥ سيادة)."
                    overdueMinutes > 0L -> "سُجّلت الصورة بعد المهلة — القيود والعقاب مسجّلان لانتهاء الوقت ($overdueMinutes د)."
                    else -> "صورة البرهان مقبولة! حُفظت في سجل الصور وفُتح القيد التالي (+٢٥ سيادة)."
                }
                val witnessPhone = current.profile.witnessPhone
                if (current.profile.autoWhatsApp && !witnessPhone.isNullOrBlank()) {
                    _whatsAppEvent.value = WhatsAppDispatchEvent.ProofPhoto(
                        witnessName = current.profile.witnessName,
                        witnessPhone = witnessPhone,
                        taskTitle = active.title,
                        proofNote = proofSummary.trim().ifBlank { "برهان مصور موثق" },
                        photoPath = photoPath,
                    )
                }
            }
        }
    }

    fun breachActiveTask(justification: String, isForceMajeure: Boolean) {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val active = current.activeTask ?: return
        val nextSealed = current.sealedTasks.minByOrNull { it.orderIndex }

        runMutation {
            repository.breachTask(
                userId = currentUserId,
                targetTask = active,
                nextSealedTask = nextSealed,
                justification = justification,
                isForceMajeure = isForceMajeure,
                profile = current.profile,
            ).onSuccess {
                _actionMessage.value = if (isForceMajeure) {
                    "فُرض قيد الاستثناء القاهر لعدم تصوير المهمة (-٣٠ سيادة)."
                } else {
                    "فُرض قيد تجميد الإضافة لعدم تصوير المهمة (-٧٠ سيادة)."
                }
            }
        }
    }

    fun spawnRecoverySprint() {
        val current = (chamberState.value as? UiState.Success)?.data ?: return
        val debt = current.profile.timeDebtMinutes.coerceAtLeast(15L)
        val nextOrder = (
            current.sealedTasks.maxOfOrNull { it.orderIndex }
                ?: current.activeTask?.orderIndex
                ?: current.draftTasks.maxOfOrNull { it.orderIndex }
                ?: -1L
            ) + 1L

        runMutation {
            repository.createRecoverySprintTask(
                userId = currentUserId,
                debtMinutes = debt,
                contractDate = current.todayDate,
                nextOrderIndex = nextOrder,
                hasActiveTask = current.activeTask != null,
            ).onSuccess {
                _actionMessage.value = "أُدرجت مهمة كفّارة مصوّرة لرفع القيود المفروضة."
            }
        }
    }

    private fun runMutation(block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isMutating.value = true
            try {
                val result = block()
                result.onFailure { err ->
                    _actionMessage.value = err.localizedMessage ?: "تعذّر تنفيذ العملية."
                }
            } finally {
                _isMutating.value = false
            }
        }
    }

    companion object {
        private const val TAG = "QaydViewModel"

        fun todayDateKey(): String =
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
}
