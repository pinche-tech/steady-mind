package com.example.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.AiReframeResult
import com.example.data.CognitiveDistortions
import com.example.data.ThoughtLog
import com.example.data.WorryVaultItem
import kotlin.math.roundToInt

@Composable
fun ReframeJournalScreen(
  thoughtLogsState: UiState<List<ThoughtLog>>,
  worryVaultItems: List<WorryVaultItem> = emptyList(),
  onAddWorryToVault: (String, String) -> Unit = { _, _ -> },
  onToggleWorryResolved: (String) -> Unit = {},
  onDeleteWorryFromVault: (String) -> Unit = {},
  onAddThoughtLog: (String, String, String, String, Int, Int) -> Unit,
  onGenerateAiReframe: (String, (AiReframeResult) -> Unit) -> Unit,
  onDeleteThoughtLog: (String) -> Unit,
  showAddDialog: Boolean,
  onDismissAddDialog: () -> Unit,
  onOpenAddDialog: () -> Unit
) {
  var selectedSection by rememberSaveable { mutableStateOf("reframe") }
  var prefilledAnxiousThought by rememberSaveable { mutableStateOf("") }
  var newWorryText by rememberSaveable { mutableStateOf("") }
  var selectedWindow by rememberSaveable { mutableStateOf("Today at 5:30 PM (10-Min Worry Window)") }

  val logs = (thoughtLogsState as? UiState.Success)?.data ?: emptyList()
  val averageDrop = remember(logs) {
    if (logs.isEmpty()) 0 else logs.map { it.anxietyDrop }.average().roundToInt()
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("reframe_journal_screen"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Sub-tab switcher: CBT Thought Deconstructor vs. Scheduled Worry Vault
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        FilterChip(
          selected = selectedSection == "reframe",
          onClick = { selectedSection = "reframe" },
          label = { Text("CBT Reframe Journal (${logs.size})") },
          leadingIcon = {
            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          modifier = Modifier.testTag("section_reframe_chip")
        )
        FilterChip(
          selected = selectedSection == "vault",
          onClick = { selectedSection = "vault" },
          label = { Text("Worry Vault (${worryVaultItems.count { !it.isResolved }})") },
          leadingIcon = {
            Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
          },
          modifier = Modifier.testTag("section_worry_vault_chip")
        )
      }
    }

    if (selectedSection == "vault") {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
          ),
          shape = RoundedCornerShape(24.dp),
          elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Scheduled Worry Vault",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Text(
              text = "Clinical 'Stimulus Control' for anxiety: when an intrusive 'what-if' worry pops up while you're trying to focus, don't fight it and don't spiral. Lock it in the Vault for your 5:30 PM Worry Window.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
              value = newWorryText,
              onValueChange = { newWorryText = it },
              label = { Text("Park an intrusive 'what-if' worry here...") },
              placeholder = { Text("e.g., What if they don't like my presentation next week?") },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("worry_vault_input")
            )

            Button(
              onClick = {
                if (newWorryText.isNotBlank()) {
                  onAddWorryToVault(newWorryText, selectedWindow)
                  newWorryText = ""
                }
              },
              enabled = newWorryText.isNotBlank(),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("lock_worry_button")
            ) {
              Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Lock Worry Until 5:30 PM")
            }
          }
        }
      }

      items(worryVaultItems, key = { it.id }) { item ->
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
          ),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.Top,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier
                  .weight(1f)
                  .clickable { onToggleWorryResolved(item.id) },
                verticalAlignment = Alignment.Top
              ) {
                Icon(
                  imageVector = if (item.isResolved) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (item.isResolved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                  Text(
                    text = item.worryText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isResolved) TextDecoration.LineThrough else TextDecoration.None
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = if (item.isResolved) "Resolved / Faded by Worry Window!" else "Locked for: ${item.scheduledWindow}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }

              IconButton(
                onClick = { onDeleteWorryFromVault(item.id) },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Delete worry",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            if (!item.isResolved) {
              OutlinedButton(
                onClick = {
                  prefilledAnxiousThought = item.worryText
                  onOpenAddDialog()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Deconstruct & Reframe with AI Now")
              }
            }
          }
        }
      }
    } else {
      item {
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f)
          ),
          shape = RoundedCornerShape(24.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Text(
              text = "CBT Thought Deconstructor",
              style = MaterialTheme.typography.headlineSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "Anxious thoughts disguise themselves as facts. Name the distortion, use SteadyMind AI to reframe with self-compassion, and pick one 2-minute step.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            if (logs.isNotEmpty()) {
              Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = "Average Anxiety Relief per Reframe:",
                      style = MaterialTheme.typography.labelLarge
                    )
                  }
                  Text(
                    text = "-$averageDrop%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }

            Button(
              onClick = {
                prefilledAnxiousThought = ""
                onOpenAddDialog()
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("open_reframe_dialog_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Deconstruct an Anxious Thought")
            }
          }
        }
      }

      when (thoughtLogsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 40.dp),
              contentAlignment = Alignment.Center
            ) {
              CircularProgressIndicator()
            }
          }
        }

        is UiState.Error -> {
          item {
            Card(
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
              ),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = thoughtLogsState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
              )
            }
          }
        }

        is UiState.Success -> {
          if (logs.isEmpty()) {
            item {
              Card(
                colors = CardDefaults.cardColors(
                  containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                  )
                  Spacer(modifier = Modifier.height(10.dp))
                  Text(
                    text = "Your Reframe Journal is Clear",
                    style = MaterialTheme.typography.titleLarge
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Whenever a task triggers dread or 'what-if' spirals, log the thought here to separate anxious fiction from grounded reality.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          } else {
            items(logs, key = { it.id }) { entry ->
              ThoughtLogCard(
                entry = entry,
                onDelete = { onDeleteThoughtLog(entry.id) }
              )
            }
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AddThoughtLogDialog(
      initialThought = prefilledAnxiousThought,
      onDismiss = {
        prefilledAnxiousThought = ""
        onDismissAddDialog()
      },
      onGenerateAiReframe = onGenerateAiReframe,
      onConfirm = { thought, distortion, reframe, action, before, after ->
        onAddThoughtLog(thought, distortion, reframe, action, before, after)
        prefilledAnxiousThought = ""
        onDismissAddDialog()
      }
    )
  }
}

@Composable
private fun ThoughtLogCard(
  entry: ThoughtLog,
  onDelete: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ),
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("thought_card_${entry.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = entry.distortion,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = "${entry.anxietyBefore}% → ${entry.anxietyAfter}% (-${entry.anxietyDrop}%)",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }
        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete thought entry",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = "\"${entry.anxiousThought}\"",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.LineThrough
      )

      Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp)) {
          Text(
            text = "COMPASSIONATE REFRAME",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = entry.reframeText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "2-Min Controllable Step: ",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = entry.tinyAction,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
fun AddThoughtLogDialog(
  initialThought: String = "",
  onDismiss: () -> Unit,
  onGenerateAiReframe: (String, (AiReframeResult) -> Unit) -> Unit,
  onConfirm: (String, String, String, String, Int, Int) -> Unit
) {
  var anxiousThought by rememberSaveable(initialThought) { mutableStateOf(initialThought) }
  var selectedDistortion by remember { mutableStateOf(CognitiveDistortions[0]) }
  var reframeText by rememberSaveable { mutableStateOf("") }
  var tinyAction by rememberSaveable { mutableStateOf("") }
  var anxietyBefore by rememberSaveable { mutableFloatStateOf(75f) }
  var anxietyAfter by rememberSaveable { mutableFloatStateOf(35f) }
  var isAiLoading by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Reframe an Anxious Thought",
        style = MaterialTheme.typography.headlineSmall
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = anxiousThought,
          onValueChange = { anxiousThought = it },
          label = { Text("What anxious thought is blocking you?") },
          placeholder = { Text("e.g., It's too late to start and I'll mess it up") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("anxious_thought_input")
        )

        OutlinedButton(
          onClick = {
            if (anxiousThought.isNotBlank() && !isAiLoading) {
              isAiLoading = true
              onGenerateAiReframe(anxiousThought) { aiResult ->
                CognitiveDistortions.firstOrNull {
                  it.name.equals(aiResult.distortion, ignoreCase = true)
                }?.let { selectedDistortion = it }
                reframeText = aiResult.reframeText
                tinyAction = aiResult.tinyAction
                isAiLoading = false
              }
            }
          },
          enabled = anxiousThought.isNotBlank() && !isAiLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_reframe_button")
        ) {
          if (isAiLoading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SteadyMind AI is reframing...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask AI to Help Reframe This")
          }
        }

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Anxiety Intensity Now", style = MaterialTheme.typography.labelLarge)
            Text("${anxietyBefore.roundToInt()}%", color = MaterialTheme.colorScheme.tertiary)
          }
          Slider(
            value = anxietyBefore,
            onValueChange = { anxietyBefore = it },
            valueRange = 0f..100f
          )
        }

        Text(
          text = "Spot the Cognitive Distortion:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          CognitiveDistortions.forEach { option ->
            val isSelected = selectedDistortion.name == option.name
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedDistortion = option }
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = option.name,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = option.summary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Guiding Prompt: ${selectedDistortion.promptQuestion}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(10.dp)
          )
        }

        OutlinedTextField(
          value = reframeText,
          onValueChange = { reframeText = it },
          label = { Text("Grounded, Compassionate Reframe") },
          placeholder = { Text("e.g., Done is better than perfect; 10 minutes of progress is a win.") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("reframe_text_input")
        )

        OutlinedTextField(
          value = tinyAction,
          onValueChange = { tinyAction = it },
          label = { Text("One 2-Minute Action Within Your Control") },
          placeholder = { Text("e.g., Open the file and write 2 rough sentences") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("tiny_action_input")
        )

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Anxiety After Reframing", style = MaterialTheme.typography.labelLarge)
            Text("${anxietyAfter.roundToInt()}%", color = MaterialTheme.colorScheme.primary)
          }
          Slider(
            value = anxietyAfter,
            onValueChange = { anxietyAfter = it },
            valueRange = 0f..100f
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (anxiousThought.isNotBlank() && reframeText.isNotBlank()) {
            onConfirm(
              anxiousThought,
              selectedDistortion.name,
              reframeText,
              tinyAction,
              anxietyBefore.roundToInt(),
              anxietyAfter.roundToInt()
            )
          }
        },
        enabled = anxiousThought.isNotBlank() && reframeText.isNotBlank(),
        modifier = Modifier.testTag("confirm_reframe_button")
      ) {
        Text("Save Reframe")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
