package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AmbientSoundMode
import com.example.data.FocusSession
import com.example.data.MicroTask
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

data class FocusPreset(
  val name: String,
  val minutes: Int,
  val tagline: String
)

private val FocusPresets = listOf(
  FocusPreset(
    name = "5-Min Icebreaker",
    minutes = 5,
    tagline = "Permission to stop after 5 minutes. Just cross the starting threshold."
  ),
  FocusPreset(
    name = "10-Min Quick Tidy",
    minutes = 10,
    tagline = "Great for replies, small errands, or setting up your workspace."
  ),
  FocusPreset(
    name = "15-Min Gentle Sprint",
    minutes = 15,
    tagline = "Short enough to feel safe, long enough to build real momentum."
  ),
  FocusPreset(
    name = "25-Min Deep Steady",
    minutes = 25,
    tagline = "Classic focused rhythm with zero perfectionism allowed."
  )
)

@Composable
fun FocusSanctuaryScreen(
  selectedTask: MicroTask?,
  activeTasks: List<MicroTask>,
  sessionsState: UiState<List<FocusSession>>,
  selectedAmbientSound: AmbientSoundMode = AmbientSoundMode.OFF,
  onSelectAmbientSound: (AmbientSoundMode) -> Unit = {},
  bodyDoubleMessage: String = "I'm sitting right here with you. Perfectionism is off the table—let's just make a messy 2-minute start together.",
  isBodyDoubleLoading: Boolean = false,
  onRequestBodyDoubleBoost: (String, String, Int) -> Unit = { _, _, _ -> },
  onSelectTask: (MicroTask?) -> Unit,
  onAdvanceTaskStep: (MicroTask) -> Unit,
  onLogFocusSession: (String, Int, String, Int) -> Unit
) {
  var selectedPreset by remember { mutableStateOf(FocusPresets[0]) }
  var remainingSeconds by rememberSaveable { mutableIntStateOf(FocusPresets[0].minutes * 60) }
  var totalSeconds by rememberSaveable { mutableIntStateOf(FocusPresets[0].minutes * 60) }
  var isRunning by rememberSaveable { mutableStateOf(false) }
  var uglyDraftPledge by rememberSaveable { mutableStateOf(true) }
  var showCompletionModal by rememberSaveable { mutableStateOf(false) }
  var reliefSlider by rememberSaveable { mutableFloatStateOf(8f) }

  LaunchedEffect(isRunning, remainingSeconds) {
    if (isRunning && remainingSeconds > 0) {
      delay(1000L)
      remainingSeconds -= 1
      if (remainingSeconds == 0) {
        isRunning = false
        showCompletionModal = true
      }
    }
  }

  val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("focus_sanctuary_screen"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "Anti-Perfectionism Timer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = selectedPreset.tagline,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Preset Chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FocusPresets.forEach { preset ->
              val isSelected = selectedPreset.name == preset.name
              Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    isRunning = false
                    selectedPreset = preset
                    totalSeconds = preset.minutes * 60
                    remainingSeconds = preset.minutes * 60
                  }
                  .testTag("preset_${preset.minutes}m")
              ) {
                Column(
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
                ) {
                  Text(
                    text = "${preset.minutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  Text(
                    text = preset.name.substringAfter(" "),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))

          // Radial Timer Ring
          FocusTimerDial(
            remainingSeconds = remainingSeconds,
            totalSeconds = totalSeconds,
            isRunning = isRunning,
            presetName = selectedPreset.name
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Button(
              onClick = { isRunning = !isRunning },
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
              ),
              modifier = Modifier
                .height(54.dp)
                .weight(1.3f)
                .testTag("timer_play_pause_button")
            ) {
              Icon(
                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isRunning) "Pause" else "Start Gentle Timer",
                style = MaterialTheme.typography.titleSmall
              )
            }

            OutlinedButton(
              onClick = {
                isRunning = false
                remainingSeconds = totalSeconds
              },
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .height(54.dp)
                .testTag("timer_reset_button")
            ) {
              Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
            }

            OutlinedButton(
              onClick = {
                isRunning = false
                showCompletionModal = true
              },
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .height(54.dp)
                .testTag("timer_complete_button")
            ) {
              Icon(Icons.Default.Check, contentDescription = "Log session")
            }
          }
        }
      }
    }

    // Calming Ambient Soundscape Synthesizer Card
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = if (selectedAmbientSound == AmbientSoundMode.OFF) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Calming Focus Soundscape",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }
            Text(
              text = selectedAmbientSound.label,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold
            )
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            AmbientSoundMode.entries.forEach { mode ->
              FilterChip(
                selected = selectedAmbientSound == mode,
                onClick = { onSelectAmbientSound(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("ambient_sound_${mode.id}")
              )
            }
          }

          Text(
            text = selectedAmbientSound.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // AI Body-Doubling Co-Working Companion Card
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "AI Body-Doubling Companion",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Text(
            text = "\"$bodyDoubleMessage\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          OutlinedButton(
            onClick = {
              onRequestBodyDoubleBoost(
                selectedTask?.title ?: "Gentle Focus Session",
                selectedTask?.nextMicroStep ?: "Do the easiest 2-minute piece",
                (remainingSeconds / 60).coerceAtLeast(1)
              )
            },
            enabled = !isBodyDoubleLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("body_double_checkin_button")
          ) {
            if (isBodyDoubleLoading) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
              Spacer(modifier = Modifier.width(8.dp))
              Text("Checking in with you...")
            } else {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Ask AI Body Double for a Gentle Check-In")
            }
          }
        }
      }
    }

    // Active Task Focus Card
    item {
      Card(
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.90f)
        ),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Text(
            text = "ANCHORED TASK & NEXT 2-MINUTE STEP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
          )
          if (selectedTask != null) {
            Text(
              text = selectedTask.title,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
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
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Current Micro-Step:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                  Text(
                    text = selectedTask.nextMicroStep,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = { onAdvanceTaskStep(selectedTask) },
                  shape = RoundedCornerShape(12.dp)
                ) {
                  Text("Done")
                }
              }
            }
          } else if (activeTasks.isNotEmpty()) {
            Text(
              text = "Tap an active task below to anchor your timer:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              activeTasks.take(3).forEach { task ->
                FilterChip(
                  selected = false,
                  onClick = { onSelectTask(task) },
                  label = { Text(task.title, maxLines = 1) }
                )
              }
            }
          } else {
            Text(
              text = "Working on general steady momentum. Add a task in the Tasks tab anytime to anchor micro-steps here.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Ugly First Draft Pledge",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
              )
              Text(
                text = "I give myself permission to work messily without self-criticism.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
              )
            }
            Switch(
              checked = uglyDraftPledge,
              onCheckedChange = { uglyDraftPledge = it }
            )
          }
        }
      }
    }

    if (sessions.isNotEmpty()) {
      item {
        Text(
          text = "Recent Focus Sessions (${sessions.size})",
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
          modifier = Modifier.padding(top = 6.dp)
        )
      }
      items(sessions.take(8), key = { it.id }) { session ->
        Card(
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
          ),
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Timer,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = session.taskTitle,
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "${session.modeName} • ${session.durationMinutes} min",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = RoundedCornerShape(50)
            ) {
              Text(
                text = "Relief ${session.reliefRating}/10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }
        }
      }
    }
  }

  if (showCompletionModal) {
    AlertDialog(
      onDismissRequest = { showCompletionModal = false },
      title = {
        Text(
          text = "You Crossed the Starting Line!",
          style = MaterialTheme.typography.headlineSmall
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            text = "Starting is the hardest part of overcoming procrastination. How much lighter or calmer do you feel now compared to before starting?",
            style = MaterialTheme.typography.bodyMedium
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Post-Session Relief", style = MaterialTheme.typography.labelLarge)
            Text(
              text = "${reliefSlider.roundToInt()}/10",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Slider(
            value = reliefSlider,
            onValueChange = { reliefSlider = it },
            valueRange = 1f..10f,
            steps = 8
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onLogFocusSession(
              selectedPreset.name,
              selectedPreset.minutes,
              selectedTask?.title ?: "Steady Focus Block",
              reliefSlider.roundToInt()
            )
            remainingSeconds = totalSeconds
            showCompletionModal = false
          },
          modifier = Modifier.testTag("confirm_log_session_button")
        ) {
          Text("Save Momentum")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCompletionModal = false }) {
          Text("Dismiss")
        }
      }
    )
  }
}

@Composable
private fun FocusTimerDial(
  remainingSeconds: Int,
  totalSeconds: Int,
  isRunning: Boolean,
  presetName: String
) {
  val progress = if (totalSeconds > 0) {
    remainingSeconds.toFloat() / totalSeconds.toFloat()
  } else 1f

  val infiniteTransition = rememberInfiniteTransition(label = "focusPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.94f,
    targetValue = 1.04f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  val primaryColor = MaterialTheme.colorScheme.primary
  val trackColor = MaterialTheme.colorScheme.surfaceVariant
  val glowColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isRunning) 0.55f else 0.25f)

  val mins = remainingSeconds / 60
  val secs = remainingSeconds % 60
  val formattedTime = "%02d:%02d".format(mins, secs)

  Box(
    modifier = Modifier.size(230.dp),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val strokeWidth = 14.dp.toPx()
      val radius = (size.minDimension - strokeWidth * 2) / 2f

      drawCircle(
        color = glowColor,
        radius = radius * (if (isRunning) pulseScale else 0.92f)
      )

      drawCircle(
        color = trackColor,
        radius = radius,
        style = Stroke(width = strokeWidth)
      )

      drawArc(
        color = primaryColor,
        startAngle = -90f,
        sweepAngle = 360f * progress,
        useCenter = false,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = formattedTime,
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (isRunning) "Breathe & go gently" else presetName,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}
