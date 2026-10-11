package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

data class GenerateContentRequest(
  val contents: List<GeminiContent>,
  val systemInstruction: GeminiContent? = null,
  val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
  val role: String? = null,
  val parts: List<GeminiPart>
)

data class GeminiPart(
  val text: String
)

data class GeminiGenerationConfig(
  val temperature: Float? = 0.7f,
  val responseMimeType: String? = null
)

data class GenerateContentResponse(
  val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
  val content: GeminiContent? = null
)

interface GeminiApiService {
  @POST("v1beta/models/gemini-3.5-flash:generateContent")
  suspend fun generateContent(
    @Query("key") apiKey: String,
    @Body request: GenerateContentRequest
  ): GenerateContentResponse
}

data class AiTaskBreakdownResult(
  val coachingNote: String,
  val steps: List<String>
)

data class AiReframeResult(
  val distortion: String,
  val reframeText: String,
  val tinyAction: String
)

data class ChatMessage(
  val id: String,
  val isUser: Boolean,
  val text: String,
  val suggestedTaskTitle: String? = null,
  val suggestedMicroSteps: List<String> = emptyList()
)

object GeminiCoachClient {
  private const val BASE_URL = "https://generativelanguage.googleapis.com/"

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val api: GeminiApiService by lazy {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GeminiApiService::class.java)
  }

  private val COACH_SYSTEM_INSTRUCTION = GeminiContent(
    parts = listOf(
      GeminiPart(
        text = """
        You are SteadyMind AI, a deeply empathetic, warm, science-backed coach specializing in helping people overcome procrastination, task paralysis, executive dysfunction, perfectionism, and anxiety.
        Rules for your responses:
        1. Validate emotions first: remind the user that procrastination is an emotional regulation response (protecting them from dread, ambiguity, or fear of failure), NOT laziness.
        2. Keep responses concise, soothing, and easy to scan when someone is overwhelmed (2-3 short paragraphs max).
        3. Always suggest a tiny, Concrete 2-Minute Micro-Step that lowers the bar ("ugly first draft", "just open the tab").
        4. At the end of your message, if the user mentioned a task or goal, include a fenced block formatted exactly like:
        [MICRO_PLAN]
        Title: <short task title>
        Step 1: <2-min step 1>
        Step 2: <2-min step 2>
        Step 3: <2-min step 3>
        [/MICRO_PLAN]
        """.trimIndent()
      )
    )
  )

  private fun isApiKeyConfigured(): Boolean {
    val key = BuildConfig.GEMINI_API_KEY
    return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
  }

  suspend fun sendCoachMessage(
    history: List<ChatMessage>,
    userMessage: String,
    nervousSystemState: String,
    activeTasksSummary: String
  ): ChatMessage = withContext(Dispatchers.IO) {
    if (!isApiKeyConfigured()) {
      return@withContext ChatMessage(
        id = System.currentTimeMillis().toString(),
        isUser = false,
        text = "To enable live Gemini AI coaching, add your `GEMINI_API_KEY` in the **Secrets panel in AI Studio**.\n\nIn the meantime, since you're feeling **$nervousSystemState**, let's shrink the pressure right now: give yourself permission to do an intentionally 'messy' 2-minute start, and stop whenever you want.",
        suggestedTaskTitle = userMessage.take(60).ifBlank { "Gentle 2-Minute Start" },
        suggestedMicroSteps = listOf(
          "Take 2 slow Physiological Sigh breaths and relax your shoulders",
          "Open only the single file or app needed — nothing else yet",
          "Set a 5-minute timer and jot down 3 messy bullet points"
        )
      )
    }

    val recentContents = history.takeLast(6).map { msg ->
      GeminiContent(
        role = if (msg.isUser) "user" else "model",
        parts = listOf(GeminiPart(text = msg.text))
      )
    } + GeminiContent(
      role = "user",
      parts = listOf(
        GeminiPart(
          text = """
          [User's Current Nervous System State: $nervousSystemState]
          [User's Current Upcoming Tasks: $activeTasksSummary]
          User says: $userMessage
          """.trimIndent()
        )
      )
    )

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = recentContents,
          systemInstruction = COACH_SYSTEM_INSTRUCTION,
          generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )
      )
      val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        ?: "I'm right here with you. Let's take one slow breath and pick the smallest 2-minute step together."

      parseCoachResponse(rawText)
    } catch (e: Exception) {
      ChatMessage(
        id = System.currentTimeMillis().toString(),
        isUser = false,
        text = "I couldn't reach Gemini right now (${e.localizedMessage ?: "network error"}), but you don't have to face this alone. Since you're feeling **$nervousSystemState**, let's break this into 3 tiny steps you can do at your own pace:",
        suggestedTaskTitle = userMessage.take(50).ifBlank { "Gentle Micro-Start" },
        suggestedMicroSteps = listOf(
          "Take one deep inhale + top-up sip + slow exhale",
          "Spend just 2 minutes setting up your workspace or opening the document",
          "Write one rough sentence or complete the easiest 5% of the task"
        )
      )
    }
  }

  private fun parseCoachResponse(raw: String): ChatMessage {
    val planRegex = Regex("""\[MICRO_PLAN\](.*?)\[/MICRO_PLAN\]""", RegexOption.DOT_MATCHES_ALL)
    val match = planRegex.find(raw)
    val cleanDisplay = raw.replace(planRegex, "").trim()

    if (match == null) {
      return ChatMessage(
        id = System.currentTimeMillis().toString(),
        isUser = false,
        text = cleanDisplay
      )
    }

    val block = match.groupValues[1]
    var title = "Gentle Action Plan"
    val steps = mutableListOf<String>()
    block.lines().forEach { line ->
      val trimmed = line.trim()
      when {
        trimmed.startsWith("Title:", ignoreCase = true) -> {
          title = trimmed.substringAfter(":").trim().ifBlank { "Gentle Action Plan" }
        }
        trimmed.startsWith("Step", ignoreCase = true) && trimmed.contains(":") -> {
          val step = trimmed.substringAfter(":").trim()
          if (step.isNotEmpty()) steps.add(step)
        }
      }
    }

    return ChatMessage(
      id = System.currentTimeMillis().toString(),
      isUser = false,
      text = cleanDisplay,
      suggestedTaskTitle = if (steps.isNotEmpty()) title else null,
      suggestedMicroSteps = steps.take(3)
    )
  }

  suspend fun generateMicroStepsForTask(
    taskTitle: String,
    blocker: BlockerType,
    dreadLevel: Int
  ): AiTaskBreakdownResult = withContext(Dispatchers.IO) {
    if (!isApiKeyConfigured()) {
      return@withContext AiTaskBreakdownResult(
        coachingNote = blocker.coachingInsight,
        steps = blocker.defaultMicroSteps
      )
    }

    val prompt = """
    Break down this procrastinated task into 3 ultra-gentle 2-minute micro-steps.
    Task: "$taskTitle"
    Why the user is avoiding it: ${blocker.label} (${blocker.subtitle})
    Pre-start dread level: $dreadLevel/10
    Return ONLY a valid JSON object with keys:
    "coachingNote": (1 warm, validating sentence explaining how to approach this without anxiety),
    "step1": (first 2-minute physical setup step),
    "step2": (second low-pressure momentum step),
    "step3": (third gentle wrap-up step)
    """.trimIndent()

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.6f,
            responseMimeType = "application/json"
          )
        )
      )
      val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
      val obj = JSONObject(rawJson)
      val note = obj.optString("coachingNote", blocker.coachingInsight)
      val s1 = obj.optString("step1", blocker.defaultMicroSteps[0])
      val s2 = obj.optString("step2", blocker.defaultMicroSteps[1])
      val s3 = obj.optString("step3", blocker.defaultMicroSteps[2])
      AiTaskBreakdownResult(
        coachingNote = note,
        steps = listOf(s1, s2, s3)
      )
    } catch (e: Exception) {
      AiTaskBreakdownResult(
        coachingNote = blocker.coachingInsight,
        steps = blocker.defaultMicroSteps
      )
    }
  }

  suspend fun generateCompassionateReframe(
    anxiousThought: String
  ): AiReframeResult = withContext(Dispatchers.IO) {
    if (!isApiKeyConfigured()) {
      return@withContext AiReframeResult(
        distortion = "All-or-Nothing Perfectionism",
        reframeText = "Feeling anxious about this doesn't mean I'm failing. A messy 2-minute start is infinitely better than waiting for perfection.",
        tinyAction = "Take 3 slow breaths and do the easiest 2-minute setup step."
      )
    }

    val prompt = """
    The user is experiencing this anxious/procrastination thought: "$anxiousThought"
    Analyze it using Cognitive Behavioral Therapy (CBT) and return ONLY a valid JSON object with keys:
    "distortion": (choose the closest match from: "Catastrophizing", "All-or-Nothing Perfectionism", "Fortune Telling", "Emotional Reasoning", "Mind Reading", "Should Statements"),
    "reframeText": (1-2 sentences of warm, realistic, self-compassionate reframing in first-person "I"),
    "tinyAction": (1 tiny 2-minute action within the user's control right now)
    """.trimIndent()

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.6f,
            responseMimeType = "application/json"
          )
        )
      )
      val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
      val obj = JSONObject(rawJson)
      AiReframeResult(
        distortion = obj.optString("distortion", "All-or-Nothing Perfectionism"),
        reframeText = obj.optString(
          "reframeText",
          "I don't have to finish everything perfectly today; making 2 minutes of gentle progress is enough."
        ),
        tinyAction = obj.optString("tinyAction", "Open the task and spend 2 minutes on the easiest part.")
      )
    } catch (e: Exception) {
      AiReframeResult(
        distortion = "All-or-Nothing Perfectionism",
        reframeText = "I don't have to finish everything perfectly today; making 2 minutes of gentle progress is enough.",
        tinyAction = "Open the task and spend 2 minutes on the easiest part."
      )
    }
  }

  suspend fun sortBrainDumpToCalmPlan(
    rawBrainDump: String
  ): BrainDumpSortedPlan = withContext(Dispatchers.IO) {
    val lines = rawBrainDump.split("\n", ",", ";").map { it.trim() }.filter { it.isNotBlank() }
    val fallbackTitle = lines.firstOrNull() ?: "Clear 1 Top Priority Gently"
    val fallbackPlan = BrainDumpSortedPlan(
      calmingSummary = "You don't have to carry all of this at once. We picked the single easiest starting point for right now and parked the rest safely.",
      doNowTaskTitle = fallbackTitle,
      doNowMicroSteps = listOf(
        "Take one slow breath and open just what you need for '$fallbackTitle'",
        "Spend 2 minutes doing a rough setup or outline",
        "Check it off and give yourself credit for starting"
      ),
      scheduleLaterItems = lines.drop(1).take(3).ifEmpty { listOf("Review remaining non-urgent tasks tomorrow") },
      letGoWorries = listOf("Let go of guilt about not finishing everything in a single hour")
    )

    if (!isApiKeyConfigured()) return@withContext fallbackPlan

    val prompt = """
    The user is overwhelmed and did a messy brain dump of tasks and worries:
    "$rawBrainDump"

    Sort this chaos into a calm, anti-procrastination plan and return ONLY a valid JSON object with keys:
    "calmingSummary": (1 warm, reassuring sentence),
    "doNowTaskTitle": (the single best task to gently start right now),
    "doNowMicroSteps": (array of 3 tiny 2-minute micro-steps for that task),
    "scheduleLaterItems": (array of 1 to 3 other concrete tasks to save for later so they don't crowd working memory),
    "letGoWorries": (array of 1 to 2 anxious 'what-if' worries from the dump to park in the Worry Vault)
    """.trimIndent()

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.6f,
            responseMimeType = "application/json"
          )
        )
      )
      val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
      val obj = JSONObject(rawJson)
      val steps = obj.optJSONArray("doNowMicroSteps").toStringList().ifEmpty { fallbackPlan.doNowMicroSteps }
      val later = obj.optJSONArray("scheduleLaterItems").toStringList().ifEmpty { fallbackPlan.scheduleLaterItems }
      val worries = obj.optJSONArray("letGoWorries").toStringList().ifEmpty { fallbackPlan.letGoWorries }

      BrainDumpSortedPlan(
        calmingSummary = obj.optString("calmingSummary", fallbackPlan.calmingSummary),
        doNowTaskTitle = obj.optString("doNowTaskTitle", fallbackPlan.doNowTaskTitle),
        doNowMicroSteps = steps,
        scheduleLaterItems = later,
        letGoWorries = worries
      )
    } catch (e: Exception) {
      fallbackPlan
    }
  }

  suspend fun generateAvoidanceReplyDraft(
    scenarioType: String,
    recipientContext: String
  ): ReplyDraftResult = withContext(Dispatchers.IO) {
    val fallback = ReplyDraftResult(
      reassuranceNote = "People appreciate brief, honest updates much more than we fear. You don't need a long apology—just warm clarity.",
      subjectOrOpening = "Quick update / following up",
      draftBody = "Hi! Thank you for your patience while I got back to you on ${recipientContext.ifBlank { "this" }}. I'm working through the next steps now and will follow up with a full update shortly. Best regards,"
    )

    if (!isApiKeyConfigured()) return@withContext fallback

    val prompt = """
    The user is procrastinating on sending an anxiety-inducing message.
    Scenario: "$scenarioType"
    Context: "$recipientContext"
    Write a polite, warm, low-stress message that avoids over-apologizing.
    Return ONLY a valid JSON object with keys:
    "reassuranceNote": (1 sentence reassuring the user why sending this is safe),
    "subjectOrOpening": (short subject line or opening line),
    "draftBody": (ready-to-send concise message body)
    """.trimIndent()

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
          generationConfig = GeminiGenerationConfig(
            temperature = 0.65f,
            responseMimeType = "application/json"
          )
        )
      )
      val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
      val obj = JSONObject(rawJson)
      ReplyDraftResult(
        reassuranceNote = obj.optString("reassuranceNote", fallback.reassuranceNote),
        subjectOrOpening = obj.optString("subjectOrOpening", fallback.subjectOrOpening),
        draftBody = obj.optString("draftBody", fallback.draftBody)
      )
    } catch (e: Exception) {
      fallback
    }
  }

  suspend fun generateBodyDoubleBoost(
    taskTitle: String,
    currentMicroStep: String,
    minutesRemaining: Int
  ): String = withContext(Dispatchers.IO) {
    val fallback = "I'm right here body-doubling with you on '$taskTitle'. Keep it messy for the next $minutesRemaining minutes—just focus on '$currentMicroStep'."
    if (!isApiKeyConfigured()) return@withContext fallback

    val prompt = """
    You are a warm, calm 'Body Double' co-working companion sitting quietly with the user while their focus timer runs.
    Task: "$taskTitle"
    Current 2-minute micro-step: "$currentMicroStep"
    Minutes left on timer: $minutesRemaining
    Give a single, soothing, encouraging 1-2 sentence co-working check-in that reminds them perfectionism isn't allowed and you're keeping them company.
    """.trimIndent()

    try {
      val response = api.generateContent(
        apiKey = BuildConfig.GEMINI_API_KEY,
        request = GenerateContentRequest(
          contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
          generationConfig = GeminiGenerationConfig(temperature = 0.7f)
        )
      )
      response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: fallback
    } catch (e: Exception) {
      fallback
    }
  }

  private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    val result = mutableListOf<String>()
    for (i in 0 until length()) {
      val item = optString(i).trim()
      if (item.isNotEmpty()) result.add(item)
    }
    return result
  }
}
