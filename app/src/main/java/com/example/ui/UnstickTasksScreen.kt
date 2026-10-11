package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PsychologyAlt
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AiTaskBreakdownResult
import com.example.data.BlockerType
import com.example.data.BrainDumpSortedPlan
import com.example.data.FocusSession
import com.example.data.MicroTask
import kotlin.math.roundToInt

@Composable
fun UnstickTasksScreen(
  tasksState: UiState<List<MicroTask>>,
  sessionsState: UiState<List<FocusSession>>,
  brainDumpPlan: BrainDumpSortedPlan? = null,
  isBrainDumpLoading: Boolean = false,
  onSortBrainDump: (String) -> Unit = {},
  onApplyBrainDumpPlan: (BrainDumpSortedPlan) -> Unit = {},
  onDismissBrainDumpPlan: () -> Unit = {},
  onAddTask: (String, BlockerType, Int, List<String>) -> Unit,
  onAdvanceStep: (MicroTask) -> Unit,
  onCompleteTask: (MicroTask) -> Unit,
  onDeleteTask: (String) -> Unit,
  onStartFiveMinFocus: (MicroTask) -> Unit,
  onQuickCalmBreath: () -> Unit,
  onOpenAiCoach: () -> Unit,
  onGenerateAiBreakdown: (String, BlockerType, Int, (AiTaskBreakdownResult) -> Unit) -> Unit,
  onSeedSampleTask: () -> Unit,
  showAddDialog: Boolean,
  onDismissAddDialog: () -> Unit,
  onOpenAddDialog: () -> Unit
) {
  var selectedFilter by rememberSaveable { mutableStateOf("active") }
  var showBrainDumpModal by rememberSaveable { mutableStateOf(false) }

  val allTasks = (tasksState as? UiState.Success)?.data ?: emptyList()
  val activeTasks = remember(allTasks) { allTasks.filter { !it.isCompleted } }
  val completedTasks = remember(allTasks) { allTasks.filter { it.isCompleted } }
  val displayedTasks = remember(allTasks, selectedFilter) {
    when (selectedFilter) {
      "active" -> activeTasks
      "completed" -> completedTasks
      else -> allTasks
    }
  }

  val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
  val totalFocusMinutes = remember(sessions) { sessions.sumOf { it.durationMinutes } }
  val averageRelief = remember(sessions) {
    if (sessions.isEmpty()) 8 else sessions.map { it.reliefRating }.average().roundToInt()
  }

  if (allTasks.isEmpty()) {
    // Centered placeholder area to display upcoming tasks
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .testTag("upcoming_tasks_placeholder_container"),
      contentAlignment = Alignment.Center
    ) {
      CenteredUpcomingTasksPlaceholder(
        onOpenAddDialog = onOpenAddDialog,
        onOpenBrainDump = { showBrainDumpModal = true },
        onOpenAiCoach = onOpenAiCoach,
        onSeedSampleTask = onSeedSampleTask,
        onQuickCalmBreath = onQuickCalmBreath
      )
    }
  } else {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .testTag("unstick_tasks_list"),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        UpcomingTasksOverviewHeader(
          activeCount = activeTasks.size,
          completedCount = completedTasks.size,
          totalFocusMinutes = totalFocusMinutes,
          averageRelief = averageRelief,
          easiestTask = activeTasks.firstOrNull(),
          onStartEasiest = { task -> onStartFiveMinFocus(task) },
          onOpenBrainDump = { showBrainDumpModal = true },
          onOpenAiCoach = onOpenAiCoach,
          onQuickCalmBreath = onQuickCalmBreath
        )
      }

      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          FilterChip(
            selected = selectedFilter == "active",
            onClick = { selectedFilter = "active" },
            label = { Text("Upcoming (${activeTasks.size})") },
            modifier = Modifier.testTag("filter_active_chip")
          )
          FilterChip(
            selected = selectedFilter == "completed",
            onClick = { selectedFilter = "completed" },
            label = { Text("Completed (${completedTasks.size})") },
            modifier = Modifier.testTag("filter_completed_chip")
          )
          FilterChip(
            selected = selectedFilter == "all",
            onClick = { selectedFilter = "all" },
            label = { Text("All (${allTasks.size})") },
            modifier = Modifier.testTag("filter_all_chip")
          )
        }
      }

      if (displayedTasks.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
          ) {
            CenteredUpcomingTasksPlaceholder(
              onOpenAddDialog = onOpenAddDialog,
              onOpenBrainDump = { showBrainDumpModal = true },
              onOpenAiCoach = onOpenAiCoach,
              onSeedSampleTask = onSeedSampleTask,
              onQuickCalmBreath = onQuickCalmBreath
            )
          }
        }
      } else {
        items(displayedTasks, key = { it.id }) { task ->
          MicroTaskCard(
            task = task,
            averageRelief = averageRelief,
            onAdvanceStep = { onAdvanceStep(task) },
            onCompleteTask = { onCompleteTask(task) },
            onDeleteTask = { onDeleteTask(task.id) },
            onStartFiveMinFocus = { onStartFiveMinFocus(task) }
          )
        }
      }
    }
  }

  if (showAddDialog) {
    AddMicroTaskDialog(
      onDismiss = onDismissAddDialog,
      onGenerateAiBreakdown = onGenerateAiBreakdown,
      onConfirm = { title, blocker, dread, steps ->
        onAddTask(title, blocker, dread, steps)
        onDismissAddDialog()
      }
    )
  }

  if (showBrainDumpModal) {
    BrainDumpSorterDialog(
      plan = brainDumpPlan,
      isLoading = isBrainDumpLoading,
      onSortBrainDump = onSortBrainDump,
      onApplyPlan = { plan ->
        onApplyBrainDumpPlan(plan)
        showBrainDumpModal = false
      },
      onDismiss = {
        onDismissBrainDumpPlan()
        showBrainDumpModal = false
      }
    )
  }
}

@Composable
private fun CenteredUpcomingTasksPlaceholder(
  onOpenAddDialog: () -> Unit,
  onOpenBrainDump: () -> Unit,
  onOpenAiCoach: () -> Unit,
  onSeedSampleTask: () -> Unit,
  onQuickCalmBreath: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ),
    shape = RoundedCornerShape(28.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    modifier = Modifier
      .widthIn(max = 480.dp)
      .fillMaxWidth()
      .testTag("upcoming_tasks_placeholder")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(22.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.EventNote,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = stringResource(R.string.upcoming_tasks_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = stringResource(R.string.upcoming_tasks_placeholder_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(14.dp))

      Image(
        painter = painterResource(id = R.drawable.img_empty_tasks_1791100630772),
        contentDescription = stringResource(R.string.cd_empty_tasks),
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .fillMaxWidth()
          .height(118.dp)
          .clip(RoundedCornerShape(18.dp))
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = onOpenAddDialog,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("placeholder_add_task_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Add Upcoming Task")
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = onOpenBrainDump,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.secondary,
          contentColor = MaterialTheme.colorScheme.onSecondary
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("placeholder_brain_dump_button")
      ) {
        Icon(Icons.Default.PsychologyAlt, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Overwhelmed? AI Brain-Dump Sorter")
      }

      Spacer(modifier = Modifier.height(8.dp))

      Button(
        onClick = onOpenAiCoach,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.tertiary,
          contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("placeholder_ai_coach_button")
      ) {
        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Talk to SteadyMind AI Coach")
      }

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = onSeedSampleTask,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("seed_sample_task_button")
        ) {
          Text("Try Sample Task")
        }
        OutlinedButton(
          onClick = onQuickCalmBreath,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("placeholder_calm_breath_button")
        ) {
          Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Calm Breath")
        }
      }
    }
  }
}

@Composable
private fun UpcomingTasksOverviewHeader(
  activeCount: Int,
  completedCount: Int,
  totalFocusMinutes: Int,
  averageRelief: Int,
  easiestTask: MicroTask?,
  onStartEasiest: (MicroTask) -> Unit,
  onOpenBrainDump: () -> Unit,
  onOpenAiCoach: () -> Unit,
  onQuickCalmBreath: () -> Unit
) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ),
    shape = RoundedCornerShape(24.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.upcoming_tasks_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Focus only on the next 2-minute micro-step.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        OutlinedButton(
          onClick = onOpenBrainDump,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.testTag("header_brain_dump_button")
        ) {
          Icon(Icons.Default.PsychologyAlt, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Brain-Dump")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        MetricPill(
          value = "$activeCount",
          label = "Upcoming",
          modifier = Modifier.weight(1f)
        )
        MetricPill(
          value = "$completedCount",
          label = "Completed",
          modifier = Modifier.weight(1f)
        )
        MetricPill(
          value = "${totalFocusMinutes}m",
          label = "Focused",
          modifier = Modifier.weight(1f)
        )
        MetricPill(
          value = "$averageRelief/10",
          label = "Avg Relief",
          modifier = Modifier.weight(1f)
        )
      }

      if (easiestTask != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = { onStartEasiest(easiestTask) },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.tertiary,
              contentColor = MaterialTheme.colorScheme.onTertiary
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("hero_just_5_min_button")
          ) {
            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start 5-Min Icebreaker")
          }
          OutlinedButton(
            onClick = onQuickCalmBreath,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("hero_sos_calm_button")
          ) {
            Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Calm")
          }
        }
      }
    }
  }
}

@Composable
private fun MetricPill(
  value: String,
  label: String,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
    shape = RoundedCornerShape(14.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
      )
    }
  }
}

@Composable
private fun MicroTaskCard(
  task: MicroTask,
  averageRelief: Int,
  onAdvanceStep: () -> Unit,
  onCompleteTask: () -> Unit,
  onDeleteTask: () -> Unit,
  onStartFiveMinFocus: () -> Unit
) {
  val blocker = remember(task.blockerType) { BlockerType.fromKey(task.blockerType) }
  var showAllSteps by rememberSaveable(task.id) { mutableStateOf(false) }
  val totalSteps = task.microSteps.size.coerceAtLeast(1)
  val progress = (task.completedStepsCount.toFloat() / totalSteps.toFloat()).coerceIn(0f, 1f)

  Card(
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
    ),
    shape = RoundedCornerShape(22.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("task_card_${task.id}")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = blocker.label,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(50)
          ) {
            Text(
              text = if (task.isCompleted) {
                "Pre-Dread ${task.dreadLevel}/10 → Relief $averageRelief/10"
              } else {
                "Dread ${task.dreadLevel}/10"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
          }
        }

        IconButton(
          onClick = onDeleteTask,
          modifier = Modifier
            .size(36.dp)
            .testTag("delete_task_${task.id}")
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Delete task",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }

      Text(
        text = task.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = if (task.isCompleted) {
              "All ${task.microSteps.size} micro-steps completed!"
            } else {
              "Micro-step ${task.completedStepsCount + 1} of ${task.microSteps.size}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
          )
          Text(
            text = "${(progress * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50)),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
      }

      if (!task.isCompleted) {
        Surface(
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAllSteps = !showAllSteps }
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "NEXT 2-MINUTE MICRO-STEP (TAP TO ${if (showAllSteps) "HIDE" else "VIEW ALL"})",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = task.nextMicroStep,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = blocker.coachingInsight,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
            )
          }
        }
      } else {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Reality Check: Your brain predicted ${task.dreadLevel}/10 dread, but taking small steps proved it was manageable.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(12.dp)
          )
        }
      }

      AnimatedVisibility(visible = showAllSteps || task.isCompleted) {
        Column(
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(top = 4.dp)
        ) {
          task.microSteps.forEachIndexed { index, stepText ->
            val isStepDone = task.isCompleted || index < task.completedStepsCount
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(
                imageVector = if (isStepDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isStepDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stepText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isStepDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isStepDone) TextDecoration.LineThrough else TextDecoration.None
              )
            }
          }
        }
      }

      if (!task.isCompleted) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onAdvanceStep,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("complete_step_${task.id}")
          ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Did This Step")
          }
          OutlinedButton(
            onClick = onStartFiveMinFocus,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag("start_focus_${task.id}")
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("5-Min Timer")
          }
        }
      }
    }
  }
}

@Composable
fun BrainDumpSorterDialog(
  plan: BrainDumpSortedPlan?,
  isLoading: Boolean,
  onSortBrainDump: (String) -> Unit,
  onApplyPlan: (BrainDumpSortedPlan) -> Unit,
  onDismiss: () -> Unit
) {
  var dumpText by rememberSaveable {
    mutableStateOf(
      "Reply to landlord email, finish project slides, worried I'm behind on bills, clean messy desk, call dentist"
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.PsychologyAlt,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Overwhelm Brain-Dump Sorter",
          style = MaterialTheme.typography.headlineSmall
        )
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "When everything feels urgent at once, your brain freezes. Dump all messy tasks and worries below—SteadyMind AI will pick 1 gentle starting step and park the rest.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
          value = dumpText,
          onValueChange = { dumpText = it },
          label = { Text("Everything crowding your head right now") },
          minLines = 3,
          maxLines = 5,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("brain_dump_input")
        )

        Button(
          onClick = { onSortBrainDump(dumpText) },
          enabled = dumpText.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("run_brain_dump_sort_button")
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sorting chaos into calm...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sort My Chaos with AI")
          }
        }

        if (plan != null) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Text(
                text = plan.calmingSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "1. DO NOW (2-MIN START): ${plan.doNowTaskTitle}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
              )
              plan.doNowMicroSteps.forEachIndexed { i, step ->
                Text(
                  text = "• Step ${i + 1}: $step",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                )
              }

              if (plan.scheduleLaterItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "2. PARKED FOR LATER (OFF YOUR MIND NOW):",
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.LockClock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "3. LOCKED IN 5:30 PM WORRY VAULT:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                  )
                }
                plan.letGoWorries.forEach { worry ->
                  Text(
                    text = "• $worry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      if (plan != null) {
        Button(
          onClick = { onApplyPlan(plan) },
          modifier = Modifier.testTag("apply_brain_dump_plan_button")
        ) {
          Text("Apply Calm Plan")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun AddMicroTaskDialog(
  onDismiss: () -> Unit,
  onGenerateAiBreakdown: (String, BlockerType, Int, (AiTaskBreakdownResult) -> Unit) -> Unit,
  onConfirm: (String, BlockerType, Int, List<String>) -> Unit
) {
  var title by rememberSaveable { mutableStateOf("") }
  var selectedBlocker by remember { mutableStateOf(BlockerType.OVERWHELMED) }
  var dreadSlider by rememberSaveable { mutableFloatStateOf(7f) }
  var coachingNote by rememberSaveable { mutableStateOf(BlockerType.OVERWHELMED.coachingInsight) }
  var step1 by rememberSaveable { mutableStateOf(BlockerType.OVERWHELMED.defaultMicroSteps[0]) }
  var step2 by rememberSaveable { mutableStateOf(BlockerType.OVERWHELMED.defaultMicroSteps[1]) }
  var step3 by rememberSaveable { mutableStateOf(BlockerType.OVERWHELMED.defaultMicroSteps[2]) }
  var isGeneratingAi by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Add Upcoming Task",
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
          value = title,
          onValueChange = { title = it },
          label = { Text("What task are you putting off?") },
          placeholder = { Text("e.g., Study for Chapter 4 exam") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_title_input")
        )

        Text(
          text = "Why does your brain want to avoid it?",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          BlockerType.entries.forEach { blocker ->
            val isSelected = selectedBlocker == blocker
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  selectedBlocker = blocker
                  coachingNote = blocker.coachingInsight
                  step1 = blocker.defaultMicroSteps.getOrElse(0) { "" }
                  step2 = blocker.defaultMicroSteps.getOrElse(1) { "" }
                  step3 = blocker.defaultMicroSteps.getOrElse(2) { "" }
                }
                .testTag("blocker_option_${blocker.key}")
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = blocker.label,
                  style = MaterialTheme.typography.titleSmall,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = blocker.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }

        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Pre-Start Dread Level",
              style = MaterialTheme.typography.labelLarge
            )
            Text(
              text = "${dreadSlider.roundToInt()}/10",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.tertiary
            )
          }
          Slider(
            value = dreadSlider,
            onValueChange = { dreadSlider = it },
            valueRange = 1f..10f,
            steps = 8
          )
        }

        // AI Magic Breakdown Button
        OutlinedButton(
          onClick = {
            if (title.isNotBlank() && !isGeneratingAi) {
              isGeneratingAi = true
              onGenerateAiBreakdown(title, selectedBlocker, dreadSlider.roundToInt()) { aiResult ->
                coachingNote = aiResult.coachingNote
                step1 = aiResult.steps.getOrElse(0) { step1 }
                step2 = aiResult.steps.getOrElse(1) { step2 }
                step3 = aiResult.steps.getOrElse(2) { step3 }
                isGeneratingAi = false
              }
            }
          },
          enabled = title.isNotBlank() && !isGeneratingAi,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_magic_breakdown_button")
        ) {
          if (isGeneratingAi) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("SteadyMind AI is tailoring micro-steps...")
          } else {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ask AI to Tailor 2-Min Micro-Steps")
          }
        }

        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top
          ) {
            Icon(
              imageVector = Icons.Default.Lightbulb,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = coachingNote,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onTertiaryContainer
            )
          }
        }

        Text(
          text = "2-Minute Micro-Steps (Editable)",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
          value = step1,
          onValueChange = { step1 = it },
          label = { Text("Micro-Step 1 (Just cross the starting line)") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = step2,
          onValueChange = { step2 = it },
          label = { Text("Micro-Step 2 (Low-pressure momentum)") },
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = step3,
          onValueChange = { step3 = it },
          label = { Text("Micro-Step 3 (Wrap up gently)") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onConfirm(
              title,
              selectedBlocker,
              dreadSlider.roundToInt(),
              listOf(step1, step2, step3)
            )
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("confirm_add_task_button")
      ) {
        Text("Save Task")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
