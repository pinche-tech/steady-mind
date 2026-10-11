package com.example.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiReframeResult
import com.example.data.AiTaskBreakdownResult
import com.example.data.AmbientSoundEngine
import com.example.data.AmbientSoundMode
import com.example.data.BlockerType
import com.example.data.BrainDumpSortedPlan
import com.example.data.ChatMessage
import com.example.data.FocusSession
import com.example.data.GeminiCoachClient
import com.example.data.LocalSteadyMindRepository
import com.example.data.MPESA_RECIPIENT_NUMBER
import com.example.data.MicroTask
import com.example.data.MpesaSubscription
import com.example.data.RelaxingBackgroundScene
import com.example.data.ReplyDraftResult
import com.example.data.SteadyMindDatabase
import com.example.data.SteadyMindRepository
import com.example.data.SubscriptionPlan
import com.example.data.ThoughtLog
import com.example.data.WorryVaultItem
import com.google.firebase.Timestamp
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

class SteadyMindViewModel(
  private val repository: SteadyMindRepository? = null,
  private val currentUserId: String? = null
) : ViewModel() {

  private var localRepo: LocalSteadyMindRepository? = null
  private var isLocalDbAttached = false

  private val _tasks = MutableStateFlow<List<MicroTask>>(emptyList())

  private val _tasksUiState = MutableStateFlow<UiState<List<MicroTask>>>(UiState.Success(emptyList()))
  val tasksUiState: StateFlow<UiState<List<MicroTask>>> = _tasksUiState.asStateFlow()

  private val _thoughtLogsUiState = MutableStateFlow<UiState<List<ThoughtLog>>>(UiState.Success(emptyList()))
  val thoughtLogsState: StateFlow<UiState<List<ThoughtLog>>> = _thoughtLogsUiState.asStateFlow()

  private val _focusSessionsUiState = MutableStateFlow<UiState<List<FocusSession>>>(UiState.Success(emptyList()))
  val focusSessionsState: StateFlow<UiState<List<FocusSession>>> = _focusSessionsUiState.asStateFlow()

  private val _worryVaultItems = MutableStateFlow<List<WorryVaultItem>>(
    listOf(
      WorryVaultItem(
        id = "starter_worry_1",
        worryText = "What if I'm already behind and can't catch up fast enough?",
        scheduledWindow = "Today at 5:30 PM (10-Min Worry Window)",
        isResolved = false
      )
    )
  )
  val worryVaultItems: StateFlow<List<WorryVaultItem>> = _worryVaultItems.asStateFlow()

  private val _subscriptions = MutableStateFlow<List<MpesaSubscription>>(emptyList())
  val subscriptions: StateFlow<List<MpesaSubscription>> = _subscriptions.asStateFlow()

  private val _selectedTaskForFocus = MutableStateFlow<MicroTask?>(null)
  val selectedTaskForFocus: StateFlow<MicroTask?> = _selectedTaskForFocus.asStateFlow()

  private val _actionBannerMessage = MutableStateFlow<String?>(null)
  val actionBannerMessage: StateFlow<String?> = _actionBannerMessage.asStateFlow()

  private val _nervousSystemState = MutableStateFlow("Frozen & Overwhelmed")
  val nervousSystemState: StateFlow<String> = _nervousSystemState.asStateFlow()

  // Relaxing Background Picture State
  private val _selectedBackgroundScene = MutableStateFlow(RelaxingBackgroundScene.FOREST_MIST)
  val selectedBackgroundScene: StateFlow<RelaxingBackgroundScene> = _selectedBackgroundScene.asStateFlow()

  private val _backgroundScrimAlpha = MutableStateFlow(0.68f)
  val backgroundScrimAlpha: StateFlow<Float> = _backgroundScrimAlpha.asStateFlow()

  // Ambient Soundscape State
  private val _selectedAmbientSound = MutableStateFlow(AmbientSoundMode.OFF)
  val selectedAmbientSound: StateFlow<AmbientSoundMode> = _selectedAmbientSound.asStateFlow()

  // AI Brain-Dump Sorter State
  private val _brainDumpPlan = MutableStateFlow<BrainDumpSortedPlan?>(null)
  val brainDumpPlan: StateFlow<BrainDumpSortedPlan?> = _brainDumpPlan.asStateFlow()

  private val _isBrainDumpLoading = MutableStateFlow(false)
  val isBrainDumpLoading: StateFlow<Boolean> = _isBrainDumpLoading.asStateFlow()

  // AI Scary Reply Draft Helper State
  private val _replyDraftResult = MutableStateFlow<ReplyDraftResult?>(null)
  val replyDraftResult: StateFlow<ReplyDraftResult?> = _replyDraftResult.asStateFlow()

  private val _isReplyDraftLoading = MutableStateFlow(false)
  val isReplyDraftLoading: StateFlow<Boolean> = _isReplyDraftLoading.asStateFlow()

  // AI Body-Doubling Companion State
  private val _bodyDoubleMessage = MutableStateFlow(
    "I'm sitting right here with you. Perfectionism is off the table—let's just make a messy 2-minute start together."
  )
  val bodyDoubleMessage: StateFlow<String> = _bodyDoubleMessage.asStateFlow()

  private val _isBodyDoubleLoading = MutableStateFlow(false)
  val isBodyDoubleLoading: StateFlow<Boolean> = _isBodyDoubleLoading.asStateFlow()

  private val _coachMessages = MutableStateFlow(
    listOf(
      ChatMessage(
        id = "welcome_msg",
        isUser = false,
        text = "Hi, I'm your SteadyMind AI Coach. Procrastination isn't a character flaw—it's your nervous system trying to protect you from stress, ambiguity, or perfectionism.\n\nTell me what task you're putting off or how you're feeling, and we'll shrink it into tiny 2-minute steps together.",
        suggestedTaskTitle = "2-Minute Gentle Unstick",
        suggestedMicroSteps = listOf(
          "Take one deep inhale + top-up sip + slow sigh out",
          "Pick just 1 thing on your mind and open the file or tab",
          "Spend 2 minutes making a messy start with zero pressure"
        )
      )
    )
  )
  val coachMessages: StateFlow<List<ChatMessage>> = _coachMessages.asStateFlow()

  private val _isAiCoachLoading = MutableStateFlow(false)
  val isAiCoachLoading: StateFlow<Boolean> = _isAiCoachLoading.asStateFlow()

  init {
    if (repository != null && !currentUserId.isNullOrBlank()) {
      observeCloudData(repository, currentUserId)
    }
  }

  private fun observeCloudData(repo: SteadyMindRepository, uid: String) {
    viewModelScope.launch {
      repo.observeTasks(uid)
        .map<List<MicroTask>, UiState<List<MicroTask>>> { UiState.Success(it) }
        .catch { error ->
          Log.w("SteadyMindVM", "Error observing tasks", error)
          emit(UiState.Error(error.message ?: "Failed to observe tasks"))
        }
        .collect { state ->
          if (state is UiState.Success) {
            _tasks.value = state.data
            _tasksUiState.value = state
            val selectedId = _selectedTaskForFocus.value?.id
            if (selectedId != null) {
              _selectedTaskForFocus.value = state.data.firstOrNull { it.id == selectedId }
            }
          } else if (_tasks.value.isEmpty()) {
            _tasksUiState.value = state
          }
        }
    }

    viewModelScope.launch {
      repo.observeThoughtLogs(uid)
        .map<List<ThoughtLog>, UiState<List<ThoughtLog>>> { UiState.Success(it) }
        .catch { error ->
          Log.w("SteadyMindVM", "Error observing thought logs", error)
          emit(UiState.Error(error.message ?: "Failed to observe thought logs"))
        }
        .collect { state ->
          if (state is UiState.Success) {
            _thoughtLogsUiState.value = state
          }
        }
    }

    viewModelScope.launch {
      repo.observeFocusSessions(uid)
        .map<List<FocusSession>, UiState<List<FocusSession>>> { UiState.Success(it) }
        .catch { error ->
          Log.w("SteadyMindVM", "Error observing focus sessions", error)
          emit(UiState.Error(error.message ?: "Failed to observe focus sessions"))
        }
        .collect { state ->
          if (state is UiState.Success) {
            _focusSessionsUiState.value = state
          }
        }
    }

    viewModelScope.launch {
      repo.observeWorries(uid)
        .catch { error ->
          Log.w("SteadyMindVM", "Error observing worry vault", error)
        }
        .collect { worries ->
          if (worries.isNotEmpty()) {
            _worryVaultItems.value = worries
          }
        }
    }

    viewModelScope.launch {
      repo.observeSubscriptions(uid)
        .catch { error ->
          Log.w("SteadyMindVM", "Error observing subscriptions", error)
        }
        .collect { subs ->
          if (subs.isNotEmpty()) {
            _subscriptions.value = subs
          }
        }
    }
  }

  fun attachLocalDatabaseIfNeeded(context: Context) {
    if (isLocalDbAttached) return
    isLocalDbAttached = true
    try {
      val db = SteadyMindDatabase.getInstance(context)
      val repo = LocalSteadyMindRepository(db.steadyMindDao())
      localRepo = repo

      if (repository == null) {
        viewModelScope.launch {
          repo.tasksFlow.collect { dbTasks ->
            if (dbTasks.isNotEmpty() || _tasks.value.isEmpty()) {
              _tasks.value = dbTasks
              _tasksUiState.value = UiState.Success(dbTasks)
            }
          }
        }
        viewModelScope.launch {
          repo.thoughtLogsFlow.collect { dbLogs ->
            val current = (_thoughtLogsUiState.value as? UiState.Success)?.data ?: emptyList()
            if (dbLogs.isNotEmpty() || current.isEmpty()) {
              _thoughtLogsUiState.value = UiState.Success(dbLogs)
            }
          }
        }
        viewModelScope.launch {
          repo.focusSessionsFlow.collect { dbSessions ->
            val current = (_focusSessionsUiState.value as? UiState.Success)?.data ?: emptyList()
            if (dbSessions.isNotEmpty() || current.isEmpty()) {
              _focusSessionsUiState.value = UiState.Success(dbSessions)
            }
          }
        }
        viewModelScope.launch {
          repo.worriesFlow.collect { dbWorries ->
            if (dbWorries.isNotEmpty()) {
              _worryVaultItems.value = dbWorries
            }
          }
        }
        viewModelScope.launch {
          repo.subscriptionsFlow.collect { dbSubs ->
            if (dbSubs.isNotEmpty()) {
              _subscriptions.value = dbSubs
            }
          }
        }
      }
    } catch (_: Exception) {
    }
  }

  fun setBackgroundScene(scene: RelaxingBackgroundScene) {
    _selectedBackgroundScene.value = scene
    _actionBannerMessage.value = "Background set to ${scene.label}"
  }

  fun setBackgroundScrimAlpha(alpha: Float) {
    _backgroundScrimAlpha.value = alpha.coerceIn(0.35f, 0.92f)
  }

  fun selectAmbientSound(mode: AmbientSoundMode) {
    _selectedAmbientSound.value = mode
    AmbientSoundEngine.startMode(mode)
    if (mode != AmbientSoundMode.OFF) {
      _actionBannerMessage.value = "Playing ${mode.label}"
    }
  }

  fun playCompletionChime() {
    AmbientSoundEngine.playSingingBowlChime()
  }

  override fun onCleared() {
    super.onCleared()
    AmbientSoundEngine.stop()
  }

  fun clearBannerMessage() {
    _actionBannerMessage.value = null
  }

  fun setNervousSystemState(state: String) {
    _nervousSystemState.value = state
  }

  fun selectTaskForFocus(task: MicroTask?) {
    _selectedTaskForFocus.value = task
  }

  fun sendCoachMessage(userText: String) {
    if (userText.isBlank() || _isAiCoachLoading.value) return
    val userMsg = ChatMessage(
      id = UUID.randomUUID().toString(),
      isUser = true,
      text = userText.trim()
    )
    val priorHistory = _coachMessages.value
    _coachMessages.update { it + userMsg }
    _isAiCoachLoading.value = true

    viewModelScope.launch {
      val activeSummary = _tasks.value
        .filter { !it.isCompleted }
        .joinToString("; ") { "${it.title} (next step: ${it.nextMicroStep})" }
        .ifBlank { "No active tasks yet" }

      val reply = GeminiCoachClient.sendCoachMessage(
        history = priorHistory,
        userMessage = userText.trim(),
        nervousSystemState = _nervousSystemState.value,
        activeTasksSummary = activeSummary
      )
      _coachMessages.update { it + reply }
      _isAiCoachLoading.value = false
    }
  }

  fun sortBrainDump(rawText: String) {
    if (rawText.isBlank() || _isBrainDumpLoading.value) return
    _isBrainDumpLoading.value = true
    viewModelScope.launch {
      val plan = GeminiCoachClient.sortBrainDumpToCalmPlan(rawText.trim())
      _brainDumpPlan.value = plan
      _isBrainDumpLoading.value = false
    }
  }

  fun dismissBrainDumpPlan() {
    _brainDumpPlan.value = null
  }

  fun applyBrainDumpPlan(plan: BrainDumpSortedPlan) {
    addTask(
      title = plan.doNowTaskTitle,
      blockerType = BlockerType.OVERWHELMED,
      dreadLevel = 6,
      customSteps = plan.doNowMicroSteps
    )
    plan.scheduleLaterItems.forEach { laterTitle ->
      if (laterTitle.isNotBlank()) {
        addTask(
          title = laterTitle,
          blockerType = BlockerType.LOW_ENERGY,
          dreadLevel = 4,
          customSteps = BlockerType.LOW_ENERGY.defaultMicroSteps
        )
      }
    }
    plan.letGoWorries.forEach { worry ->
      if (worry.isNotBlank()) {
        addWorryToVault(worry, "Today at 5:30 PM (10-Min Worry Window)")
      }
    }
    _brainDumpPlan.value = null
    _actionBannerMessage.value = "Chaos sorted! Added your 1 Do-Now task and locked worries in the Worry Vault."
  }

  fun generateScaryReplyDraft(scenarioType: String, recipientContext: String) {
    if (_isReplyDraftLoading.value) return
    _isReplyDraftLoading.value = true
    viewModelScope.launch {
      val result = GeminiCoachClient.generateAvoidanceReplyDraft(scenarioType, recipientContext)
      _replyDraftResult.value = result
      _isReplyDraftLoading.value = false
    }
  }

  fun requestBodyDoubleBoost(taskTitle: String, currentMicroStep: String, minutesRemaining: Int) {
    if (_isBodyDoubleLoading.value) return
    _isBodyDoubleLoading.value = true
    viewModelScope.launch {
      val boost = GeminiCoachClient.generateBodyDoubleBoost(
        taskTitle = taskTitle.ifBlank { "Steady Momentum Session" },
        currentMicroStep = currentMicroStep.ifBlank { "Make 2 minutes of rough progress" },
        minutesRemaining = minutesRemaining.coerceAtLeast(1)
      )
      _bodyDoubleMessage.value = boost
      _isBodyDoubleLoading.value = false
    }
  }

  fun generateAiTaskBreakdown(
    taskTitle: String,
    blocker: BlockerType,
    dreadLevel: Int,
    onResult: (AiTaskBreakdownResult) -> Unit
  ) {
    if (taskTitle.isBlank()) return
    viewModelScope.launch {
      val result = GeminiCoachClient.generateMicroStepsForTask(
        taskTitle = taskTitle,
        blocker = blocker,
        dreadLevel = dreadLevel
      )
      onResult(result)
    }
  }

  fun generateAiReframe(
    anxiousThought: String,
    onResult: (AiReframeResult) -> Unit
  ) {
    if (anxiousThought.isBlank()) return
    viewModelScope.launch {
      val result = GeminiCoachClient.generateCompassionateReframe(anxiousThought)
      onResult(result)
    }
  }

  fun addTask(
    title: String,
    blockerType: BlockerType,
    dreadLevel: Int,
    customSteps: List<String>
  ): MicroTask {
    val steps = customSteps.map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { blockerType.defaultMicroSteps }
    val newTask = MicroTask(
      id = UUID.randomUUID().toString(),
      userId = currentUserId ?: "local_user",
      title = title.trim(),
      blockerType = blockerType.key,
      dreadLevel = dreadLevel.coerceIn(1, 10),
      microSteps = steps,
      completedStepsCount = 0,
      status = "active",
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now()
    )
    _tasks.update { current -> listOf(newTask) + current }
    _tasksUiState.value = UiState.Success(_tasks.value)
    viewModelScope.launch {
      localRepo?.saveTask(newTask)
      repository?.createTask(
        title = title.trim(),
        blockerType = blockerType.key,
        dreadLevel = dreadLevel.coerceIn(1, 10),
        microSteps = steps
      )
    }
    _actionBannerMessage.value = "Added to Upcoming Tasks with ${steps.size} gentle micro-steps."
    return newTask
  }

  fun advanceMicroStep(task: MicroTask) {
    val totalSteps = task.microSteps.size.coerceAtLeast(1)
    val nextCount = (task.completedStepsCount + 1).coerceAtMost(totalSteps)
    val nextStatus = if (nextCount >= totalSteps) "completed" else "active"
    var updatedTask: MicroTask? = null
    _tasks.update { list ->
      list.map { item ->
        if (item.id == task.id) {
          item.copy(
            completedStepsCount = nextCount,
            status = nextStatus,
            updatedAt = Timestamp.now()
          ).also { updatedTask = it }
        } else {
          item
        }
      }
    }
    _tasksUiState.value = UiState.Success(_tasks.value)
    viewModelScope.launch {
      updatedTask?.let { t -> localRepo?.saveTask(t) }
      repository?.advanceTaskStep(task)
    }
    if (_selectedTaskForFocus.value?.id == task.id) {
      _selectedTaskForFocus.value = _tasks.value.firstOrNull { it.id == task.id }
    }
    _actionBannerMessage.value = if (nextStatus == "completed") {
      "Task completed! Notice how starting was the hardest part."
    } else {
      "Micro-step complete! Keep going gently."
    }
  }

  fun completeTaskImmediately(task: MicroTask) {
    var updatedTask: MicroTask? = null
    _tasks.update { list ->
      list.map { item ->
        if (item.id == task.id) {
          item.copy(
            completedStepsCount = item.microSteps.size,
            status = "completed",
            updatedAt = Timestamp.now()
          ).also { updatedTask = it }
        } else {
          item
        }
      }
    }
    _tasksUiState.value = UiState.Success(_tasks.value)
    viewModelScope.launch {
      updatedTask?.let { t -> localRepo?.saveTask(t) }
      repository?.completeTaskNow(task)
    }
    _actionBannerMessage.value = "Task marked complete!"
  }

  fun deleteTask(taskId: String) {
    _tasks.update { list -> list.filterNot { it.id == taskId } }
    _tasksUiState.value = UiState.Success(_tasks.value)
    viewModelScope.launch {
      localRepo?.deleteTask(taskId)
      repository?.deleteTask(taskId)
    }
    if (_selectedTaskForFocus.value?.id == taskId) {
      _selectedTaskForFocus.value = null
    }
  }

  fun addThoughtLog(
    anxiousThought: String,
    distortion: String,
    reframeText: String,
    tinyAction: String,
    anxietyBefore: Int,
    anxietyAfter: Int
  ) {
    if (anxiousThought.isBlank() || reframeText.isBlank()) return
    val cleanAction = tinyAction.ifBlank { "Take 3 slow breaths and do 2 minutes of the easiest step" }
    val currentLogs = (_thoughtLogsUiState.value as? UiState.Success)?.data ?: emptyList()
    val newEntry = ThoughtLog(
      id = UUID.randomUUID().toString(),
      userId = currentUserId ?: "local_user",
      anxiousThought = anxiousThought.trim(),
      distortion = distortion,
      reframeText = reframeText.trim(),
      tinyAction = cleanAction,
      anxietyBefore = anxietyBefore.coerceIn(0, 100),
      anxietyAfter = anxietyAfter.coerceIn(0, 100),
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now()
    )
    _thoughtLogsUiState.value = UiState.Success(listOf(newEntry) + currentLogs)
    viewModelScope.launch {
      localRepo?.saveThoughtLog(newEntry)
      repository?.createThoughtLog(
        anxiousThought = anxiousThought.trim(),
        distortion = distortion,
        reframeText = reframeText.trim(),
        tinyAction = cleanAction,
        anxietyBefore = anxietyBefore.coerceIn(0, 100),
        anxietyAfter = anxietyAfter.coerceIn(0, 100)
      )
    }
    val drop = (anxietyBefore - anxietyAfter).coerceAtLeast(0)
    _actionBannerMessage.value = if (drop > 0) {
      "Reframe saved! Anxiety dropped by $drop%."
    } else {
      "Cognitive reframe saved."
    }
  }

  fun deleteThoughtLog(logId: String) {
    val currentLogs = (_thoughtLogsUiState.value as? UiState.Success)?.data ?: emptyList()
    _thoughtLogsUiState.value = UiState.Success(currentLogs.filterNot { it.id == logId })
    viewModelScope.launch {
      localRepo?.deleteThoughtLog(logId)
      repository?.deleteThoughtLog(logId)
    }
  }

  fun addWorryToVault(worryText: String, scheduledWindow: String) {
    if (worryText.isBlank()) return
    val windowText = scheduledWindow.ifBlank { "Today at 5:30 PM (10-Min Worry Window)" }
    val newItem = WorryVaultItem(
      id = UUID.randomUUID().toString(),
      userId = currentUserId ?: "local_user",
      worryText = worryText.trim(),
      scheduledWindow = windowText,
      isResolved = false,
      createdAtMillis = System.currentTimeMillis()
    )
    _worryVaultItems.update { listOf(newItem) + it }
    viewModelScope.launch {
      localRepo?.saveWorry(newItem)
      repository?.createWorry(worryText.trim(), windowText)
    }
    _actionBannerMessage.value = "Worry safely parked in your Worry Vault for later."
  }

  fun toggleWorryResolved(worryId: String) {
    val target = _worryVaultItems.value.firstOrNull { it.id == worryId } ?: return
    var updated: WorryVaultItem? = null
    _worryVaultItems.update { list ->
      list.map { item ->
        if (item.id == worryId) {
          item.copy(isResolved = !item.isResolved).also { updated = it }
        } else {
          item
        }
      }
    }
    viewModelScope.launch {
      updated?.let { item -> localRepo?.saveWorry(item) }
      repository?.toggleWorryResolved(target)
    }
  }

  fun deleteWorryFromVault(worryId: String) {
    _worryVaultItems.update { list -> list.filterNot { it.id == worryId } }
    viewModelScope.launch {
      localRepo?.deleteWorry(worryId)
      repository?.deleteWorry(worryId)
    }
  }

  fun logCompletedFocusSession(
    modeName: String,
    durationMinutes: Int,
    taskTitle: String,
    reliefRating: Int
  ) {
    val cleanTitle = taskTitle.ifBlank { "Gentle Focus Session" }
    val currentSessions = (_focusSessionsUiState.value as? UiState.Success)?.data ?: emptyList()
    val newSession = FocusSession(
      id = UUID.randomUUID().toString(),
      userId = currentUserId ?: "local_user",
      modeName = modeName,
      durationMinutes = durationMinutes,
      taskTitle = cleanTitle,
      reliefRating = reliefRating.coerceIn(1, 10),
      createdAt = Timestamp.now()
    )
    _focusSessionsUiState.value = UiState.Success(listOf(newSession) + currentSessions)
    viewModelScope.launch {
      localRepo?.saveFocusSession(newSession)
      repository?.recordFocusSession(
        modeName = modeName,
        durationMinutes = durationMinutes,
        taskTitle = cleanTitle,
        reliefRating = reliefRating.coerceIn(1, 10)
      )
    }
    AmbientSoundEngine.playSingingBowlChime()
    _actionBannerMessage.value = "$durationMinutes min focus session logged!"
  }

  fun seedStarterTemplateIfNeeded() {
    addTask(
      title = "Reply to the 1 message I've been putting off",
      blockerType = BlockerType.FEAR_OF_OUTCOME,
      dreadLevel = 7,
      customSteps = listOf(
        "Open messages and find just that 1 thread",
        "Draft a rough 2-sentence reply in a note with zero pressure",
        "Send and take one slow Physiological Sigh breath"
      )
    )
  }

  fun activateMpesaSubscription(
    plan: SubscriptionPlan,
    senderPhone: String,
    mpesaReceiptCode: String,
    onResult: (Boolean, String) -> Unit = { _, _ -> }
  ) {
    val normalizedPhone = MpesaSubscription.normalizeKenyanPhone(senderPhone)
    val cleanedReceipt = mpesaReceiptCode.trim().uppercase().filter { it.isLetterOrDigit() }

    if (normalizedPhone.length < 9) {
      onResult(false, "Please enter a valid M-Pesa phone number (e.g., 0712345678 or 0112345678).")
      return
    }
    if (!MpesaSubscription.isValidMpesaReceipt(cleanedReceipt)) {
      onResult(false, "Please enter a valid 10-character M-Pesa Transaction Code from your SMS (e.g., SJK7Q9M2LP).")
      return
    }

    val newSub = MpesaSubscription(
      id = UUID.randomUUID().toString(),
      userId = currentUserId ?: "local_user",
      planId = plan.id,
      planTitle = plan.title,
      amountKes = plan.priceKes,
      recipientMpesaNumber = MPESA_RECIPIENT_NUMBER,
      senderPhone = normalizedPhone,
      mpesaReceiptCode = cleanedReceipt,
      status = "active",
      createdAtMillis = System.currentTimeMillis(),
      createdAt = Timestamp.now(),
      updatedAt = Timestamp.now()
    )

    _subscriptions.update { listOf(newSub) + it }
    viewModelScope.launch {
      localRepo?.saveSubscription(newSub)
      repository?.createSubscription(
        plan = plan,
        senderPhone = normalizedPhone,
        mpesaReceiptCode = cleanedReceipt
      )
    }
    AmbientSoundEngine.playSingingBowlChime()
    val msg = "${plan.title} activated! M-Pesa receipt #$cleanedReceipt to $MPESA_RECIPIENT_NUMBER verified."
    _actionBannerMessage.value = msg
    onResult(true, msg)
  }
}
