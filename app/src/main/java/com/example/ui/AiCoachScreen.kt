package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BrainDumpSortedPlan
import com.example.data.ChatMessage
import com.example.data.ReplyDraftResult

private val NervousSystemStates = listOf(
  "Frozen & Overwhelmed",
  "Anxious & Racing",
  "Perfectionist Block",
  "Low Energy / Drained",
  "Guilty About Delaying"
)

private val QuickCoachPrompts = listOf(
  "I keep avoiding a task and feel stuck",
  "Help me do an 'ugly first draft' for 2 mins",
  "I feel guilty for wasting time today",
  "My chest feels tight thinking about my to-do list"
)

private val ReplyScenarios = listOf(
  "Replying Late Without Over-Apologizing",
  "Asking for a Gentle Deadline Extension",
  "Saying 'No' Kindly When Overloaded",
  "Asking for Clarification When Confused"
)

@Composable
fun AiCoachScreen(
  messages: List<ChatMessage>,
  isLoading: Boolean,
  selectedNervousSystemState: String,
  onSelectNervousSystemState: (String) -> Unit,
  onSendMessage: (String) -> Unit,
  onAddSuggestedPlanToTasks: (String, List<String>) -> Unit,
  onStartFocusWithPlan: (String, List<String>) -> Unit,
  replyDraftResult: ReplyDraftResult? = null,
  isReplyDraftLoading: Boolean = false,
  onGenerateReplyDraft: (String, String) -> Unit = { _, _ -> },
  brainDumpPlan: BrainDumpSortedPlan? = null,
  isBrainDumpLoading: Boolean = false,
  onSortBrainDump: (String) -> Unit = {},
  onApplyBrainDumpPlan: (BrainDumpSortedPlan) -> Unit = {}
) {
  var selectedSubTab by rememberSaveable { mutableStateOf("chat") }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .testTag("ai_coach_screen")
  ) {
    // Top AI Toolkit Mode Selector + Nervous System State
    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
      tonalElevation = 2.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedSubTab == "chat",
            onClick = { selectedSubTab = "chat" },
            label = { Text("Empathy Coach") },
            leadingIcon = {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("coach_tab_chat")
          )
          FilterChip(
            selected = selectedSubTab == "reply",
            onClick = { selectedSubTab = "reply" },
            label = { Text("Scary Reply Helper") },
            leadingIcon = {
              Icon(Icons.Default.MarkChatRead, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("coach_tab_reply")
          )
          FilterChip(
            selected = selectedSubTab == "brain_dump",
            onClick = { selectedSubTab = "brain_dump" },
            label = { Text("Chaos Sorter") },
            leadingIcon = {
              Icon(Icons.Default.PsychologyAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("coach_tab_brain_dump")
          )
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          NervousSystemStates.forEach { stateLabel ->
            FilterChip(
              selected = selectedNervousSystemState == stateLabel,
              onClick = { onSelectNervousSystemState(stateLabel) },
              label = { Text(stateLabel) }
            )
          }
        }
      }
    }

    when (selectedSubTab) {
      "chat" -> {
        EmpathyChatView(
          messages = messages,
          isLoading = isLoading,
          onSendMessage = onSendMessage,
          onAddSuggestedPlanToTasks = onAddSuggestedPlanToTasks,
          onStartFocusWithPlan = onStartFocusWithPlan,
          modifier = Modifier.weight(1f)
        )
      }

      "reply" -> {
        ScaryReplyHelperView(
          replyDraftResult = replyDraftResult,
          isLoading = isReplyDraftLoading,
          onGenerateReplyDraft = onGenerateReplyDraft,
          onAddReplyTask = { title ->
            onAddSuggestedPlanToTasks(
              title,
              listOf(
                "Open the messaging or email app to that thread",
                "Paste your SteadyMind AI draft and adjust 1 detail",
                "Hit send and take one deep Physiological Sigh breath"
              )
            )
          },
          modifier = Modifier.weight(1f)
        )
      }

      "brain_dump" -> {
        InlineBrainDumpView(
          plan = brainDumpPlan,
          isLoading = isBrainDumpLoading,
          onSortBrainDump = onSortBrainDump,
          onApplyBrainDumpPlan = onApplyBrainDumpPlan,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun EmpathyChatView(
  messages: List<ChatMessage>,
  isLoading: Boolean,
  onSendMessage: (String) -> Unit,
  onAddSuggestedPlanToTasks: (String, List<String>) -> Unit,
  onStartFocusWithPlan: (String, List<String>) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by rememberSaveable { mutableStateOf("") }
  val listState = rememberLazyListState()

  LaunchedEffect(messages.size, isLoading) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f)
          ),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text(
              text = "Tap a thought or tell SteadyMind AI what you're avoiding:",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            QuickCoachPrompts.forEach { prompt ->
              Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable(enabled = !isLoading) {
                    onSendMessage(prompt)
                  }
              ) {
                Text(
                  text = "\"$prompt\"",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                )
              }
            }
          }
        }
      }

      items(messages, key = { it.id }) { message ->
        ChatBubbleItem(
          message = message,
          onAddPlanToTasks = onAddSuggestedPlanToTasks,
          onStartFocusWithPlan = onStartFocusWithPlan
        )
      }

      if (isLoading) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(20.dp),
              strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "SteadyMind AI is crafting a gentle plan...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }

    Surface(
      color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        OutlinedTextField(
          value = inputText,
          onValueChange = { inputText = it },
          placeholder = { Text("Tell me what feels overwhelming...") },
          shape = RoundedCornerShape(20.dp),
          maxLines = 3,
          modifier = Modifier
            .weight(1f)
            .testTag("ai_coach_input")
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = {
            if (inputText.isNotBlank() && !isLoading) {
              val toSend = inputText
              inputText = ""
              onSendMessage(toSend)
            }
          },
          enabled = inputText.isNotBlank() && !isLoading,
          modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(
              if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.surfaceVariant
            )
            .testTag("ai_coach_send_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send to AI Coach",
            tint = if (inputText.isNotBlank() && !isLoading) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun ScaryReplyHelperView(
  replyDraftResult: ReplyDraftResult?,
  isLoading: Boolean,
  onGenerateReplyDraft: (String, String) -> Unit,
  onAddReplyTask: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var selectedScenario by rememberSaveable { mutableStateOf(ReplyScenarios[0]) }
  var recipientContext by rememberSaveable {
    mutableStateOf("Replying to my professor/manager about the draft I meant to send 3 days ago")
  }
  var copiedFeedback by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "\"Scary Message\" Reply Drafter",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Putting off a text or email because of guilt makes the anxiety snowball. Pick your situation and let AI write a warm, low-stress draft so you only have to copy and send.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
          text = "Select Situation:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )

        ReplyScenarios.forEach { scenario ->
          val selected = selectedScenario == scenario
          Surface(
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { selectedScenario = scenario }
          ) {
            Text(
              text = scenario,
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
              color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        OutlinedTextField(
          value = recipientContext,
          onValueChange = { recipientContext = it },
          label = { Text("Who is it for & what's the quick context?") },
          minLines = 2,
          maxLines = 4,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("scary_reply_context_input")
        )

        Button(
          onClick = {
            copiedFeedback = false
            onGenerateReplyDraft(selectedScenario, recipientContext)
          },
          enabled = !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("generate_scary_reply_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Drafting warm, guilt-free message...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Draft Polite, Low-Stress Reply")
          }
        }
      }
    }

    if (replyDraftResult != null) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "REASSURANCE NOTE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = replyDraftResult.reassuranceNote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "Subject / Opening: ${replyDraftResult.subjectOrOpening}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = replyDraftResult.draftBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText("SteadyMind Draft", replyDraftResult.draftBody))
                copiedFeedback = true
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(if (copiedFeedback) "Copied!" else "Copy Draft")
            }

            OutlinedButton(
              onClick = {
                onAddReplyTask("Send reply: ${replyDraftResult.subjectOrOpening}")
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Add as Task")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun InlineBrainDumpView(
  plan: BrainDumpSortedPlan?,
  isLoading: Boolean,
  onSortBrainDump: (String) -> Unit,
  onApplyBrainDumpPlan: (BrainDumpSortedPlan) -> Unit,
  modifier: Modifier = Modifier
) {
  var rawDump by rememberSaveable {
    mutableStateOf("Finish presentation slides, reply to Sarah's email, worried I'll blank out during tomorrow's meeting, pay electricity bill")
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    Card(
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
      ),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Overwhelm Brain-Dump Sorter",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Write down everything tangling your mind—tasks, errands, and 'what-if' worries. SteadyMind AI separates what to do right now from what to park for later.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = rawDump,
          onValueChange = { rawDump = it },
          label = { Text("Messy thoughts, tasks, and worries") },
          minLines = 3,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = { onSortBrainDump(rawDump) },
          enabled = rawDump.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sorting into a calm action plan...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Turn Chaos into 1 Gentle Step")
          }
        }
      }
    }

    if (plan != null) {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = plan.calmingSummary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "DO NOW (1 GENTLE TASK): ${plan.doNowTaskTitle}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          plan.doNowMicroSteps.forEachIndexed { idx, s ->
            Text(
              text = "${idx + 1}. $s",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }

          if (plan.scheduleLaterItems.isNotEmpty()) {
            Text(
              text = "PARKED FOR LATER:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            plan.scheduleLaterItems.forEach { item ->
              Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          if (plan.letGoWorries.isNotEmpty()) {
            Text(
              text = "WORRIES TO LOCK IN YOUR 5:30 PM VAULT:",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.tertiary,
              fontWeight = FontWeight.Bold
            )
            plan.letGoWorries.forEach { w ->
              Text(
                text = "• $w",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }

          Button(
            onClick = { onApplyBrainDumpPlan(plan) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Save Tasks & Lock Worries in Vault")
          }
        }
      }
    }
  }
}

@Composable
private fun ChatBubbleItem(
  message: ChatMessage,
  onAddPlanToTasks: (String, List<String>) -> Unit,
  onStartFocusWithPlan: (String, List<String>) -> Unit
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
  ) {
    Surface(
      color = if (message.isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
      shape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (message.isUser) 20.dp else 6.dp,
        bottomEnd = if (message.isUser) 6.dp else 20.dp
      ),
      tonalElevation = if (message.isUser) 0.dp else 2.dp,
      modifier = Modifier.widthIn(max = 340.dp)
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        if (!message.isUser) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.SelfImprovement,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "SteadyMind AI Coach",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
          text = message.text,
          style = MaterialTheme.typography.bodyMedium,
          color = if (message.isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )

        if (!message.isUser && !message.suggestedTaskTitle.isNullOrBlank() && message.suggestedMicroSteps.isNotEmpty()) {
          Spacer(modifier = Modifier.height(12.dp))
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = "SUGGESTED 2-MIN MICRO-PLAN",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = message.suggestedTaskTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              message.suggestedMicroSteps.forEachIndexed { idx, step ->
                Row(verticalAlignment = Alignment.Top) {
                  Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                      .size(16.dp)
                      .padding(top = 2.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "${idx + 1}. $step",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }

              Spacer(modifier = Modifier.height(4.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Button(
                  onClick = {
                    onAddPlanToTasks(message.suggestedTaskTitle, message.suggestedMicroSteps)
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.AddTask, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add Task", style = MaterialTheme.typography.labelMedium)
                }
                OutlinedButton(
                  onClick = {
                    onStartFocusWithPlan(message.suggestedTaskTitle, message.suggestedMicroSteps)
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("5m Focus", style = MaterialTheme.typography.labelMedium)
                }
              }
            }
          }
        }
      }
    }
  }
}
