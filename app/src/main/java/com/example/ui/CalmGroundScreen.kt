package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AmbientSoundMode
import kotlinx.coroutines.delay

data class BreathPhase(
  val label: String,
  val instruction: String,
  val durationSeconds: Int,
  val targetScale: Float
)

data class BreathingPattern(
  val id: String,
  val title: String,
  val subtitle: String,
  val phases: List<BreathPhase>
)

private val BreathingPatterns = listOf(
  BreathingPattern(
    id = "sigh",
    title = "Physiological Sigh",
    subtitle = "Double inhale + long slow exhale — fastest biological brake on acute anxiety.",
    phases = listOf(
      BreathPhase("Inhale Deeply", "Fill your lungs ~80% through your nose", 4, 0.85f),
      BreathPhase("Top-Up Sip", "Take one quick second sip of air to pop alveoli open", 2, 1.0f),
      BreathPhase("Long Slow Exhale", "Sigh all the air out slowly through your mouth", 8, 0.48f)
    )
  ),
  BreathingPattern(
    id = "478",
    title = "4-7-8 Calm Reset",
    subtitle = "Slows a racing heart and quiets pre-task dread.",
    phases = listOf(
      BreathPhase("Inhale Quietly", "Breathe in gently through your nose", 4, 0.95f),
      BreathPhase("Hold Softly", "Rest with full lungs — relax your shoulders", 7, 1.0f),
      BreathPhase("Exhale Slowly", "Release tension steadily for 8 seconds", 8, 0.48f)
    )
  ),
  BreathingPattern(
    id = "box",
    title = "4-4-4-4 Box Breath",
    subtitle = "Balances focus and emotional steadiness before deep work.",
    phases = listOf(
      BreathPhase("Inhale", "Steady breath in for 4 seconds", 4, 0.95f),
      BreathPhase("Hold Full", "Soften your jaw and brow", 4, 0.95f),
      BreathPhase("Exhale", "Smooth release out for 4 seconds", 4, 0.50f),
      BreathPhase("Rest Empty", "Calm pause before the next cycle", 4, 0.50f)
    )
  )
)

data class GroundingSenseStep(
  val count: Int,
  val title: String,
  val prompt: String,
  val examples: String
)

private val GroundingSteps = listOf(
  GroundingSenseStep(5, "5 Things You Can See", "Look around and notice 5 physical details in the room.", "e.g., sunlight on the wall, wood grain, a plant leaf, your mug"),
  GroundingSenseStep(4, "4 Things You Can Feel", "Bring awareness to 4 tactile sensations on your body.", "e.g., feet flat on the floor, fabric on your arms, cool air"),
  GroundingSenseStep(3, "3 Things You Can Hear", "Listen past your thoughts for 3 ambient sounds.", "e.g., distant hum, birds outside, your own steady breath"),
  GroundingSenseStep(2, "2 Things You Can Smell", "Notice 2 scents in the air or take a sip of water.", "e.g., coffee/tea, fresh air, soap on your hands"),
  GroundingSenseStep(1, "1 Safe Anchor Truth", "Say aloud: 'Right now, in this moment, I am safe and I only need to do one tiny step.'", "You don't have to solve the whole week right now.")
)

@Composable
fun CalmGroundScreen(
  selectedAmbientSound: AmbientSoundMode = AmbientSoundMode.OFF,
  onSelectAmbientSound: (AmbientSoundMode) -> Unit = {},
  onPlaySingingBowlChime: () -> Unit = {}
) {
  var selectedPattern by remember { mutableStateOf(BreathingPatterns[0]) }
  var isBreathingActive by rememberSaveable { mutableStateOf(false) }
  var phaseIndex by rememberSaveable { mutableIntStateOf(0) }
  var phaseSecondsLeft by rememberSaveable { mutableIntStateOf(BreathingPatterns[0].phases[0].durationSeconds) }
  var completedCycles by rememberSaveable { mutableIntStateOf(0) }

  val checkedGrounding = remember { mutableStateListOf<Int>() }

  val currentPhase = selectedPattern.phases.getOrElse(phaseIndex) { selectedPattern.phases[0] }

  LaunchedEffect(isBreathingActive, phaseIndex, phaseSecondsLeft, selectedPattern) {
    if (isBreathingActive) {
      if (phaseSecondsLeft > 1) {
        delay(1000L)
        phaseSecondsLeft -= 1
      } else {
        delay(1000L)
        val nextIndex = (phaseIndex + 1) % selectedPattern.phases.size
        if (nextIndex == 0) {
          completedCycles += 1
          onPlaySingingBowlChime()
        }
        phaseIndex = nextIndex
        phaseSecondsLeft = selectedPattern.phases[nextIndex].durationSeconds
      }
    }
  }

  val animatedScale by animateFloatAsState(
    targetValue = if (isBreathingActive) currentPhase.targetScale else 0.62f,
    animationSpec = tween(
      durationMillis = if (isBreathingActive) currentPhase.durationSeconds * 900 else 600,
      easing = FastOutSlowInEasing
    ),
    label = "breathOrbScale"
  )

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("calm_ground_screen"),
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
            text = "Somatic Breathing Pacer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = selectedPattern.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
          )

          Spacer(modifier = Modifier.height(14.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BreathingPatterns.forEach { pattern ->
              val isSelected = selectedPattern.id == pattern.id
              Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    isBreathingActive = false
                    selectedPattern = pattern
                    phaseIndex = 0
                    phaseSecondsLeft = pattern.phases[0].durationSeconds
                  }
                  .testTag("breath_pattern_${pattern.id}")
              ) {
                Text(
                  text = pattern.title,
                  style = MaterialTheme.typography.labelMedium,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))

          // Animated Breathing Lotus / Aura Canvas
          val outerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
          val middleColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
          val coreColor = MaterialTheme.colorScheme.primary

          Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
          ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
              val maxR = size.minDimension / 2f
              drawCircle(
                color = outerColor,
                radius = maxR * animatedScale
              )
              drawCircle(
                color = middleColor,
                radius = maxR * animatedScale * 0.78f
              )
              drawCircle(
                color = coreColor,
                radius = maxR * animatedScale * 0.56f
              )
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.padding(16.dp)
            ) {
              Text(
                text = if (isBreathingActive) "${phaseSecondsLeft}s" else "Ready",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary
              )
              Text(
                text = if (isBreathingActive) currentPhase.label else "Tap Start",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = if (isBreathingActive) currentPhase.instruction else "Even 2 cycles of Physiological Sighs can noticeably lower your heart rate.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
          )

          if (completedCycles > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "$completedCycles calming cycle${if (completedCycles == 1) "" else "s"} completed",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { isBreathingActive = !isBreathingActive },
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("breath_start_stop_button")
            ) {
              Icon(
                imageVector = if (isBreathingActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isBreathingActive) "Pause Pacer" else "Begin Calming Breath",
                style = MaterialTheme.typography.titleSmall
              )
            }

            OutlinedButton(
              onClick = onPlaySingingBowlChime,
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .height(52.dp)
                .testTag("play_bowl_chime_button")
            ) {
              Icon(Icons.Default.GraphicEq, contentDescription = "Singing bowl chime")
              Spacer(modifier = Modifier.width(6.dp))
              Text("Chime")
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            AmbientSoundMode.entries.forEach { mode ->
              FilterChip(
                selected = selectedAmbientSound == mode,
                onClick = { onSelectAmbientSound(mode) },
                label = { Text(mode.label) }
              )
            }
          }
        }
      }
    }

    // 5-4-3-2-1 Grounding Section
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
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "5-4-3-2-1 Sensory Grounding",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
              )
              Text(
                text = "Tap each sense as you anchor yourself back into the present room.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            if (checkedGrounding.isNotEmpty()) {
              OutlinedButton(
                onClick = { checkedGrounding.clear() },
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset grounding", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reset")
              }
            }
          }

          GroundingSteps.forEach { step ->
            val isChecked = checkedGrounding.contains(step.count)
            Surface(
              color = if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (isChecked) checkedGrounding.remove(step.count)
                  else {
                    checkedGrounding.add(step.count)
                    if (checkedGrounding.size == GroundingSteps.size) {
                      onPlaySingingBowlChime()
                    }
                  }
                }
                .testTag("grounding_step_${step.count}")
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
              ) {
                Icon(
                  imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                  contentDescription = null,
                  tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                  modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = step.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = step.examples,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          if (checkedGrounding.size == GroundingSteps.size) {
            Surface(
              color = MaterialTheme.colorScheme.tertiaryContainer,
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.Spa,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "All 5 senses anchored! Your nervous system is back in the present. You're ready for one gentle 2-minute step.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }
    }
  }
}
