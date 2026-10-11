package com.example.data

import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class RelaxingBackgroundScene(
  val id: String,
  val label: String,
  val subtitle: String,
  val drawableRes: Int?
) {
  FOREST_MIST(
    id = "forest_mist",
    label = "Sunlit Forest Mist",
    subtitle = "Morning bamboo & cedar forest bathing",
    drawableRes = R.drawable.img_bg_forest_mist_1791142339328
  ),
  SUNSET_LAKE(
    id = "sunset_lake",
    label = "Twilight Alpine Lake",
    subtitle = "Still water & warm pastel dusk",
    drawableRes = R.drawable.img_bg_sunset_lake_1791142351505
  ),
  ZEN_GARDEN(
    id = "zen_garden",
    label = "Zen Moss & Stream",
    subtitle = "Dappled sunlight over tranquil river stones",
    drawableRes = R.drawable.img_bg_zen_garden_1791142513327
  ),
  SAGE_HILLS(
    id = "sage_hills",
    label = "Golden Sage Hills",
    subtitle = "Soft rolling sunrise horizon",
    drawableRes = R.drawable.img_hero_calm_1791100613242
  ),
  MINIMAL_PARCHMENT(
    id = "minimal",
    label = "Warm Parchment",
    subtitle = "Clean distraction-free canvas",
    drawableRes = null
  )
}

enum class AmbientSoundMode(
  val id: String,
  val label: String,
  val description: String
) {
  OFF("off", "Silent", "Quiet sanctuary"),
  BROWN_NOISE("brown_noise", "Warm Brown Noise", "Deep, low-frequency roar that quiets racing ADHD/anxious thoughts"),
  FOREST_RAIN("forest_rain", "Soft Mist & Rain", "Gentle patter of rain on leaves"),
  ZEN_HUM("zen_hum", "432Hz Warm Drone", "Soothing harmonic singing bowl resonance")
}

data class WorryVaultItem(
  val id: String = "",
  val userId: String = "",
  val worryText: String = "",
  val scheduledWindow: String = "Today at 5:30 PM (10-Min Worry Window)",
  val isResolved: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): WorryVaultItem {
      val createdTs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val updatedTs = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      return WorryVaultItem(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        worryText = doc.getString("worryText") ?: "",
        scheduledWindow = doc.getString("scheduledWindow") ?: "Today at 5:30 PM (10-Min Worry Window)",
        isResolved = doc.getBoolean("isResolved") ?: false,
        createdAtMillis = createdTs?.toDate()?.time ?: System.currentTimeMillis(),
        createdAt = createdTs,
        updatedAt = updatedTs
      )
    }
  }
}

data class BrainDumpSortedPlan(
  val calmingSummary: String,
  val doNowTaskTitle: String,
  val doNowMicroSteps: List<String>,
  val scheduleLaterItems: List<String>,
  val letGoWorries: List<String>
)

data class ReplyDraftResult(
  val reassuranceNote: String,
  val subjectOrOpening: String,
  val draftBody: String
)

enum class BlockerType(
  val key: String,
  val label: String,
  val subtitle: String,
  val coachingInsight: String,
  val defaultMicroSteps: List<String>
) {
  OVERWHELMED(
    key = "overwhelmed",
    label = "Feels Too Huge",
    subtitle = "Task feels massive and exhausting to even think about",
    coachingInsight = "Shrink the horizon: your brain treats a huge project like a physical threat. Only look at the first 2-minute setup step.",
    defaultMicroSteps = listOf(
      "Open the document, app, or folder — nothing else yet",
      "Write down a messy 3-bullet outline of what's needed",
      "Do just the easiest 5-minute piece and stop to breathe"
    )
  ),
  PERFECTIONISM(
    key = "perfectionism",
    label = "Perfectionism",
    subtitle = "Worried it won't be good enough or afraid of making mistakes",
    coachingInsight = "Permission to write garbage: an intentionally 'ugly first draft' bypasses your inner critic and gets momentum flowing.",
    defaultMicroSteps = listOf(
      "Set a 5-min timer to create a deliberately rough version",
      "Fill in the easiest placeholder section without editing",
      "Do one quick pass to clean up just the first paragraph"
    )
  ),
  AMBIGUOUS(
    key = "ambiguous",
    label = "Unclear Start",
    subtitle = "Too vague — not sure what the first concrete step is",
    coachingInsight = "Ambiguity triggers task paralysis. Turn 'figure it out' into a tiny 2-minute reconnaissance mission.",
    defaultMicroSteps = listOf(
      "Write down the single question or unknown blocking you",
      "Spend 3 minutes finding one reference example or link",
      "Define the very first physical action you can take today"
    )
  ),
  LOW_ENERGY(
    key = "low_energy",
    label = "Low Energy",
    subtitle = "Mentally drained, low dopamine, or struggling to focus",
    coachingInsight = "Match the step to your energy. Start with a zero-brainpower mechanical step while taking slow breaths.",
    defaultMicroSteps = listOf(
      "Take 3 slow breaths, grab water, and clear 1 item off your desk",
      "Do the most mechanical 2-minute setup step",
      "Spend 5 gentle minutes making 1% progress, then rest"
    )
  ),
  FEAR_OF_OUTCOME(
    key = "fear_of_outcome",
    label = "Outcome Anxiety",
    subtitle = "Anxious about judgment, feedback, or an uncomfortable reply",
    coachingInsight = "Separate drafting from sending. You are 100% safe while preparing privately — no one is judging a work-in-progress.",
    defaultMicroSteps = listOf(
      "Draft privately in a scratchpad with zero pressure to send",
      "Read it once objectively as if advising a close friend",
      "Complete the final step and immediately do a calming breath"
    )
  );

  companion object {
    fun fromKey(key: String): BlockerType =
      entries.firstOrNull { it.key == key } ?: OVERWHELMED
  }
}

data class MicroTask(
  val id: String = "",
  val userId: String = "",
  val title: String = "",
  val blockerType: String = "overwhelmed",
  val dreadLevel: Int = 5,
  val microSteps: List<String> = emptyList(),
  val completedStepsCount: Int = 0,
  val status: String = "active",
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val isCompleted: Boolean
    get() = status == "completed"

  val nextMicroStep: String
    get() = microSteps.getOrNull(completedStepsCount)
      ?: microSteps.lastOrNull()
      ?: "Spend 2 gentle minutes starting"

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): MicroTask {
      val rawSteps = doc.get("microSteps") as? List<*> ?: emptyList<Any>()
      val steps = rawSteps.mapNotNull { it as? String }
      return MicroTask(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        title = doc.getString("title") ?: "",
        blockerType = doc.getString("blockerType") ?: "overwhelmed",
        dreadLevel = (doc.getLong("dreadLevel") ?: 5L).toInt(),
        microSteps = steps,
        completedStepsCount = (doc.getLong("completedStepsCount") ?: 0L).toInt(),
        status = doc.getString("status") ?: "active",
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class ThoughtLog(
  val id: String = "",
  val userId: String = "",
  val anxiousThought: String = "",
  val distortion: String = "",
  val reframeText: String = "",
  val tinyAction: String = "",
  val anxietyBefore: Int = 70,
  val anxietyAfter: Int = 35,
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val anxietyDrop: Int
    get() = (anxietyBefore - anxietyAfter).coerceAtLeast(0)

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): ThoughtLog {
      return ThoughtLog(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        anxiousThought = doc.getString("anxiousThought") ?: "",
        distortion = doc.getString("distortion") ?: "",
        reframeText = doc.getString("reframeText") ?: "",
        tinyAction = doc.getString("tinyAction") ?: "",
        anxietyBefore = (doc.getLong("anxietyBefore") ?: 70L).toInt(),
        anxietyAfter = (doc.getLong("anxietyAfter") ?: 35L).toInt(),
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
        updatedAt = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class FocusSession(
  val id: String = "",
  val userId: String = "",
  val modeName: String = "",
  val durationMinutes: Int = 5,
  val taskTitle: String = "",
  val reliefRating: Int = 8,
  val createdAt: Timestamp? = null
) {
  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): FocusSession {
      return FocusSession(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        modeName = doc.getString("modeName") ?: "",
        durationMinutes = (doc.getLong("durationMinutes") ?: 5L).toInt(),
        taskTitle = doc.getString("taskTitle") ?: "",
        reliefRating = (doc.getLong("reliefRating") ?: 8L).toInt(),
        createdAt = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      )
    }
  }
}

data class CognitiveDistortionOption(
  val name: String,
  val summary: String,
  val promptQuestion: String
)

val CognitiveDistortions = listOf(
  CognitiveDistortionOption(
    name = "Catastrophizing",
    summary = "Jumping straight to the worst-case disaster scenario",
    promptQuestion = "What is the most realistic, ordinary outcome rather than the worst-case scenario?"
  ),
  CognitiveDistortionOption(
    name = "All-or-Nothing Perfectionism",
    summary = "Believing if it isn't 100% flawless, it's a total failure",
    promptQuestion = "What does a 'good enough' 70% version look like that moves you forward today?"
  ),
  CognitiveDistortionOption(
    name = "Fortune Telling",
    summary = "Predicting that a task will go badly before even starting",
    promptQuestion = "Think of a past time you dreaded a task — did it turn out easier once you started?"
  ),
  CognitiveDistortionOption(
    name = "Emotional Reasoning",
    summary = "'I feel anxious and overwhelmed, so this must be impossible'",
    promptQuestion = "Feelings are real, but they aren't facts. What objective skills do you already have?"
  ),
  CognitiveDistortionOption(
    name = "Mind Reading",
    summary = "Assuming others will judge you harshly for asking or trying",
    promptQuestion = "Would you judge a friend harshly for being in your exact situation right now?"
  ),
  CognitiveDistortionOption(
    name = "Should Statements",
    summary = "Beating yourself up with 'I should have finished this already'",
    promptQuestion = "If you let go of past guilt, what is one kind, practical thing you can do in the next 2 minutes?"
  )
)

const val MPESA_RECIPIENT_NUMBER = "0119137126"
const val MPESA_RECIPIENT_DISPLAY = "0119 137 126"

enum class SubscriptionPlan(
  val id: String,
  val title: String,
  val priceKes: Int,
  val billingCycle: String,
  val badge: String?,
  val perks: List<String>
) {
  WEEKLY(
    id = "weekly",
    title = "Weekly Unstick Pass",
    priceKes = 99,
    billingCycle = "per week",
    badge = null,
    perks = listOf(
      "Unlimited AI Brain-Dump Sorter & 2-Min Micro-Steps",
      "Scary Reply Draft Helper for avoided texts & emails",
      "432Hz & Brown Noise Focus Soundscapes"
    )
  ),
  MONTHLY(
    id = "monthly",
    title = "SteadyMind Plus Monthly",
    priceKes = 299,
    billingCycle = "per month",
    badge = "MOST POPULAR",
    perks = listOf(
      "Unlimited 24/7 Compassionate AI Coach & Body-Doubling",
      "Full Cloud Firestore Sync across phone & Chrome",
      "All 5 Relaxing Nature Sanctuary Backgrounds",
      "Unlimited CBT Reframe Journal & Worry Vault"
    )
  ),
  ANNUAL(
    id = "annual",
    title = "Annual Sanctuary Patron",
    priceKes = 2499,
    billingCycle = "per year",
    badge = "SAVE 30%",
    perks = listOf(
      "Full year of SteadyMind Plus with zero interruptions",
      "Priority AI Coaching, Chaos Sorter & Scary Reply Helper",
      "Encrypted multi-device backup & lifetime progress stats"
    )
  );

  companion object {
    fun fromId(id: String): SubscriptionPlan =
      entries.firstOrNull { it.id == id } ?: MONTHLY
  }
}

data class MpesaSubscription(
  val id: String = "",
  val userId: String = "",
  val planId: String = "monthly",
  val planTitle: String = "SteadyMind Plus Monthly",
  val amountKes: Int = 299,
  val recipientMpesaNumber: String = MPESA_RECIPIENT_NUMBER,
  val senderPhone: String = "",
  val mpesaReceiptCode: String = "",
  val status: String = "active",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val createdAt: Timestamp? = null,
  val updatedAt: Timestamp? = null
) {
  val isActive: Boolean
    get() = status == "active"

  companion object {
    fun fromSnapshot(doc: DocumentSnapshot): MpesaSubscription {
      val createdTs = doc.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      val updatedTs = doc.getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      return MpesaSubscription(
        id = doc.id,
        userId = doc.getString("userId") ?: "",
        planId = doc.getString("planId") ?: "monthly",
        planTitle = doc.getString("planTitle") ?: "SteadyMind Plus Monthly",
        amountKes = (doc.getLong("amountKes") ?: 299L).toInt(),
        recipientMpesaNumber = doc.getString("recipientMpesaNumber") ?: MPESA_RECIPIENT_NUMBER,
        senderPhone = doc.getString("senderPhone") ?: "",
        mpesaReceiptCode = doc.getString("mpesaReceiptCode") ?: "",
        status = doc.getString("status") ?: "active",
        createdAtMillis = createdTs?.toDate()?.time ?: System.currentTimeMillis(),
        createdAt = createdTs,
        updatedAt = updatedTs
      )
    }

    fun isValidMpesaReceipt(code: String): Boolean {
      val cleaned = code.trim().uppercase()
      return cleaned.length in 8..12 && cleaned.all { it.isLetterOrDigit() } && cleaned.any { it.isDigit() } && cleaned.any { it.isLetter() }
    }

    fun normalizeKenyanPhone(phone: String): String {
      val digits = phone.filter { it.isDigit() }
      return when {
        digits.startsWith("254") && digits.length == 12 -> "0${digits.drop(3)}"
        digits.startsWith("0") && digits.length == 10 -> digits
        digits.length == 9 && (digits.startsWith("7") || digits.startsWith("1")) -> "0$digits"
        else -> phone.trim()
      }
    }
  }
}

