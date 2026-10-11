package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BlockerType
import com.example.data.MPESA_RECIPIENT_NUMBER
import com.example.data.RelaxingBackgroundScene
import com.example.data.SteadyMindRepository
import com.example.ui.AiCoachScreen
import com.example.ui.CalmGroundScreen
import com.example.ui.FocusSanctuaryScreen
import com.example.ui.MpesaSubscriptionDialog
import com.example.ui.ReframeJournalScreen
import com.example.ui.SteadyMindAuthScreen
import com.example.ui.SteadyMindViewModel
import com.example.ui.UiState
import com.example.ui.UnstickTasksScreen
import com.example.ui.signOut
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.roundToInt

enum class SteadyDestination(
  val route: String,
  val labelRes: Int,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector
) {
  UNSTICK("unstick", R.string.nav_unstick, Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
  COACH("coach", R.string.nav_coach, Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
  FOCUS("focus", R.string.nav_focus, Icons.Filled.Timer, Icons.Outlined.Timer),
  CALM("calm", R.string.nav_calm, Icons.Filled.Spa, Icons.Outlined.Spa),
  REFRAME("reframe", R.string.nav_reframe, Icons.Filled.Psychology, Icons.Outlined.Psychology)
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        SteadyMindAppRoot()
      }
    }
  }
}

@Composable
fun SteadyMindAppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    SteadyMindAuthScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      }
    )
  } else {
    val uid = user.uid
    val viewModel: SteadyMindViewModel = viewModel(
      key = uid,
      factory = viewModelFactory {
        initializer {
          val app = checkNotNull(this[APPLICATION_KEY]) {
            "APPLICATION_KEY missing from CreationExtras"
          }
          val databaseId = app.getString(R.string.firestore_database_id)
          val db = FirebaseFirestore.getInstance(databaseId)
          SteadyMindViewModel(
            repository = SteadyMindRepository(db),
            currentUserId = uid
          )
        }
      }
    )

    SteadyMindMainScreen(
      viewModel = viewModel,
      userDisplayName = user.displayName ?: user.email ?: "Signed In User",
      userEmail = user.email ?: "Google Account",
      onSignOut = {
        signOut(
          context = context,
          credentialManager = credentialManager,
          onSignOutComplete = {
            currentUser = null
          },
          scope = scope
        )
      }
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SteadyMindMainScreen(
  viewModel: SteadyMindViewModel = viewModel(),
  userDisplayName: String = "Signed In User",
  userEmail: String = "",
  onSignOut: () -> Unit = {}
) {
  val context = LocalContext.current
  LaunchedEffect(Unit) {
    viewModel.attachLocalDatabaseIfNeeded(context)
  }

  val tasksState by viewModel.tasksUiState.collectAsStateWithLifecycle()
  val thoughtLogsState by viewModel.thoughtLogsState.collectAsStateWithLifecycle()
  val focusSessionsState by viewModel.focusSessionsState.collectAsStateWithLifecycle()
  val worryVaultItems by viewModel.worryVaultItems.collectAsStateWithLifecycle()
  val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
  val selectedTaskForFocus by viewModel.selectedTaskForFocus.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.actionBannerMessage.collectAsStateWithLifecycle()
  val coachMessages by viewModel.coachMessages.collectAsStateWithLifecycle()
  val isAiCoachLoading by viewModel.isAiCoachLoading.collectAsStateWithLifecycle()
  val nervousSystemState by viewModel.nervousSystemState.collectAsStateWithLifecycle()

  val selectedBackgroundScene by viewModel.selectedBackgroundScene.collectAsStateWithLifecycle()
  val backgroundScrimAlpha by viewModel.backgroundScrimAlpha.collectAsStateWithLifecycle()
  val selectedAmbientSound by viewModel.selectedAmbientSound.collectAsStateWithLifecycle()

  val brainDumpPlan by viewModel.brainDumpPlan.collectAsStateWithLifecycle()
  val isBrainDumpLoading by viewModel.isBrainDumpLoading.collectAsStateWithLifecycle()
  val replyDraftResult by viewModel.replyDraftResult.collectAsStateWithLifecycle()
  val isReplyDraftLoading by viewModel.isReplyDraftLoading.collectAsStateWithLifecycle()
  val bodyDoubleMessage by viewModel.bodyDoubleMessage.collectAsStateWithLifecycle()
  val isBodyDoubleLoading by viewModel.isBodyDoubleLoading.collectAsStateWithLifecycle()

  var currentDestination by rememberSaveable { mutableStateOf(SteadyDestination.UNSTICK) }
  var showAddTaskDialog by rememberSaveable { mutableStateOf(false) }
  var showAddReframeDialog by rememberSaveable { mutableStateOf(false) }
  var showBackgroundPicker by rememberSaveable { mutableStateOf(false) }
  var showAccountDialog by rememberSaveable { mutableStateOf(false) }
  var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }

  val activeSubscription = subscriptions.firstOrNull { it.isActive }

  val snackbarHostState = remember { SnackbarHostState() }

  if (currentDestination != SteadyDestination.UNSTICK) {
    BackHandler {
      currentDestination = SteadyDestination.UNSTICK
    }
  }

  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (msg != null) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBannerMessage()
    }
  }

  val activeTasks = (tasksState as? UiState.Success)?.data?.filter { !it.isCompleted } ?: emptyList()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Full-screen Relaxing Nature Photography Background Layer
    Crossfade(
      targetState = selectedBackgroundScene,
      label = "relaxingBackgroundCrossfade"
    ) { scene ->
      val resId = scene.drawableRes
      if (resId != null) {
        Box(modifier = Modifier.fillMaxSize()) {
          Image(
            painter = painterResource(id = resId),
            contentDescription = scene.label,
            contentScale = ContentScale.Crop,
            modifier = Modifier
              .fillMaxSize()
              .testTag("relaxing_background_image")
          )
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.background.copy(alpha = backgroundScrimAlpha))
          )
        }
      } else {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        )
      }
    }

    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .testTag("main_scaffold"),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        TopAppBar(
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                shape = RoundedCornerShape(50)
              ) {
                Text(
                  text = nervousSystemState,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }
          },
          actions = {
            OutlinedButton(
              onClick = { showSubscriptionDialog = true },
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .padding(end = 4.dp)
                .testTag("subscription_top_button")
            ) {
              Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = "SteadyMind Plus M-Pesa Subscription",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (activeSubscription != null) "Plus Active" else "Plus",
                style = MaterialTheme.typography.labelMedium
              )
            }

            OutlinedButton(
              onClick = { showBackgroundPicker = true },
              shape = RoundedCornerShape(50),
              modifier = Modifier
                .padding(end = 4.dp)
                .testTag("background_scene_picker_button")
            ) {
              Icon(
                imageVector = Icons.Default.Landscape,
                contentDescription = "Choose relaxing background picture",
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Scene",
                style = MaterialTheme.typography.labelMedium
              )
            }

            IconButton(
              onClick = { showAccountDialog = true },
              modifier = Modifier.testTag("account_profile_button")
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Account & Cloud Sync",
                tint = MaterialTheme.colorScheme.primary
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
          )
        )
      },
      floatingActionButton = {
        when (currentDestination) {
          SteadyDestination.UNSTICK -> {
            ExtendedFloatingActionButton(
              onClick = { showAddTaskDialog = true },
              icon = { Icon(Icons.Default.Add, contentDescription = null) },
              text = { Text(stringResource(R.string.add_task_fab)) },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_add_task")
            )
          }

          SteadyDestination.REFRAME -> {
            ExtendedFloatingActionButton(
              onClick = { showAddReframeDialog = true },
              icon = { Icon(Icons.Default.Add, contentDescription = null) },
              text = { Text(stringResource(R.string.new_reframe_fab)) },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.testTag("fab_add_reframe")
            )
          }

          else -> {}
        }
      },
      bottomBar = {
        NavigationBar(
          containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
          modifier = Modifier.testTag("bottom_navigation_bar")
        ) {
          SteadyDestination.entries.forEach { destination ->
            val selected = currentDestination == destination
            NavigationBarItem(
              selected = selected,
              onClick = { currentDestination = destination },
              icon = {
                Icon(
                  imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                  contentDescription = stringResource(destination.labelRes),
                  modifier = Modifier.size(22.dp)
                )
              },
              label = {
                Text(
                  text = stringResource(destination.labelRes),
                  style = MaterialTheme.typography.labelMedium
                )
              },
              modifier = Modifier.testTag("nav_${destination.route}")
            )
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.Center
      ) {
        when (currentDestination) {
          SteadyDestination.UNSTICK -> {
            UnstickTasksScreen(
              tasksState = tasksState,
              sessionsState = focusSessionsState,
              brainDumpPlan = brainDumpPlan,
              isBrainDumpLoading = isBrainDumpLoading,
              onSortBrainDump = { viewModel.sortBrainDump(it) },
              onApplyBrainDumpPlan = { viewModel.applyBrainDumpPlan(it) },
              onDismissBrainDumpPlan = { viewModel.dismissBrainDumpPlan() },
              onAddTask = { title, blocker, dread, steps ->
                viewModel.addTask(title, blocker, dread, steps)
              },
              onAdvanceStep = { task ->
                viewModel.advanceMicroStep(task)
              },
              onCompleteTask = { task ->
                viewModel.completeTaskImmediately(task)
              },
              onDeleteTask = { id ->
                viewModel.deleteTask(id)
              },
              onStartFiveMinFocus = { task ->
                viewModel.selectTaskForFocus(task)
                currentDestination = SteadyDestination.FOCUS
              },
              onQuickCalmBreath = {
                currentDestination = SteadyDestination.CALM
              },
              onOpenAiCoach = {
                currentDestination = SteadyDestination.COACH
              },
              onGenerateAiBreakdown = { title, blocker, dread, callback ->
                viewModel.generateAiTaskBreakdown(title, blocker, dread, callback)
              },
              onSeedSampleTask = {
                viewModel.seedStarterTemplateIfNeeded()
              },
              showAddDialog = showAddTaskDialog,
              onDismissAddDialog = { showAddTaskDialog = false },
              onOpenAddDialog = { showAddTaskDialog = true }
            )
          }

          SteadyDestination.COACH -> {
            AiCoachScreen(
              messages = coachMessages,
              isLoading = isAiCoachLoading,
              selectedNervousSystemState = nervousSystemState,
              onSelectNervousSystemState = { viewModel.setNervousSystemState(it) },
              onSendMessage = { viewModel.sendCoachMessage(it) },
              onAddSuggestedPlanToTasks = { title, steps ->
                viewModel.addTask(
                  title = title,
                  blockerType = BlockerType.OVERWHELMED,
                  dreadLevel = 6,
                  customSteps = steps
                )
              },
              onStartFocusWithPlan = { title, steps ->
                val created = viewModel.addTask(
                  title = title,
                  blockerType = BlockerType.OVERWHELMED,
                  dreadLevel = 6,
                  customSteps = steps
                )
                viewModel.selectTaskForFocus(created)
                currentDestination = SteadyDestination.FOCUS
              },
              replyDraftResult = replyDraftResult,
              isReplyDraftLoading = isReplyDraftLoading,
              onGenerateReplyDraft = { scenario, ctx ->
                viewModel.generateScaryReplyDraft(scenario, ctx)
              },
              brainDumpPlan = brainDumpPlan,
              isBrainDumpLoading = isBrainDumpLoading,
              onSortBrainDump = { viewModel.sortBrainDump(it) },
              onApplyBrainDumpPlan = { viewModel.applyBrainDumpPlan(it) }
            )
          }

          SteadyDestination.FOCUS -> {
            FocusSanctuaryScreen(
              selectedTask = selectedTaskForFocus ?: activeTasks.firstOrNull(),
              activeTasks = activeTasks,
              sessionsState = focusSessionsState,
              selectedAmbientSound = selectedAmbientSound,
              onSelectAmbientSound = { viewModel.selectAmbientSound(it) },
              bodyDoubleMessage = bodyDoubleMessage,
              isBodyDoubleLoading = isBodyDoubleLoading,
              onRequestBodyDoubleBoost = { title, step, mins ->
                viewModel.requestBodyDoubleBoost(title, step, mins)
              },
              onSelectTask = { viewModel.selectTaskForFocus(it) },
              onAdvanceTaskStep = { viewModel.advanceMicroStep(it) },
              onLogFocusSession = { modeName, mins, taskTitle, relief ->
                viewModel.logCompletedFocusSession(modeName, mins, taskTitle, relief)
              }
            )
          }

          SteadyDestination.CALM -> {
            CalmGroundScreen(
              selectedAmbientSound = selectedAmbientSound,
              onSelectAmbientSound = { viewModel.selectAmbientSound(it) },
              onPlaySingingBowlChime = { viewModel.playCompletionChime() }
            )
          }

          SteadyDestination.REFRAME -> {
            ReframeJournalScreen(
              thoughtLogsState = thoughtLogsState,
              worryVaultItems = worryVaultItems,
              onAddWorryToVault = { worry, window ->
                viewModel.addWorryToVault(worry, window)
              },
              onToggleWorryResolved = { id ->
                viewModel.toggleWorryResolved(id)
              },
              onDeleteWorryFromVault = { id ->
                viewModel.deleteWorryFromVault(id)
              },
              onAddThoughtLog = { thought, distortion, reframe, action, before, after ->
                viewModel.addThoughtLog(thought, distortion, reframe, action, before, after)
              },
              onGenerateAiReframe = { thought, callback ->
                viewModel.generateAiReframe(thought, callback)
              },
              onDeleteThoughtLog = { id ->
                viewModel.deleteThoughtLog(id)
              },
              showAddDialog = showAddReframeDialog,
              onDismissAddDialog = { showAddReframeDialog = false },
              onOpenAddDialog = { showAddReframeDialog = true }
            )
          }
        }
      }
    }
  }

  if (showBackgroundPicker) {
    RelaxingBackgroundPickerDialog(
      selectedScene = selectedBackgroundScene,
      scrimAlpha = backgroundScrimAlpha,
      onSelectScene = { scene ->
        viewModel.setBackgroundScene(scene)
      },
      onChangeScrimAlpha = { alpha ->
        viewModel.setBackgroundScrimAlpha(alpha)
      },
      onDismiss = { showBackgroundPicker = false }
    )
  }

  if (showAccountDialog) {
    AlertDialog(
      onDismissRequest = { showAccountDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.CloudDone,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Account & Cloud Sync",
            style = MaterialTheme.typography.headlineSmall
          )
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = userDisplayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
          )
          if (userEmail.isNotBlank()) {
            Text(
              text = userEmail,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Your upcoming tasks, 2-minute micro-steps, focus sessions, CBT reframes, and Worry Vault items are securely synced with Cloud Firestore.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(12.dp)
            )
          }
          OutlinedButton(
            onClick = {
              showAccountDialog = false
              showSubscriptionDialog = true
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("open_subscription_from_account_button")
          ) {
            Icon(
              imageVector = Icons.Default.WorkspacePremium,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              if (activeSubscription != null) {
                "SteadyMind Plus Active (#${activeSubscription.mpesaReceiptCode})"
              } else {
                "Subscribe via M-Pesa ($MPESA_RECIPIENT_NUMBER)"
              }
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showAccountDialog = false
            onSignOut()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("sign_out_button")
        ) {
          Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Sign Out")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAccountDialog = false }) {
          Text("Close")
        }
      }
    )
  }

  if (showSubscriptionDialog) {
    MpesaSubscriptionDialog(
      subscriptions = subscriptions,
      onActivateSubscription = { plan, senderPhone, receiptCode, onResult ->
        viewModel.activateMpesaSubscription(plan, senderPhone, receiptCode, onResult)
      },
      onDismiss = { showSubscriptionDialog = false }
    )
  }
}

@Composable
private fun RelaxingBackgroundPickerDialog(
  selectedScene: RelaxingBackgroundScene,
  scrimAlpha: Float,
  onSelectScene: (RelaxingBackgroundScene) -> Unit,
  onChangeScrimAlpha: (Float) -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Landscape,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Relaxing Sanctuary Backgrounds",
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
          text = "Choose a calming nature backdrop to soften visual stress while you work:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RelaxingBackgroundScene.entries.forEach { scene ->
          val isSelected = selectedScene == scene
          Surface(
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectScene(scene) }
              .testTag("scene_option_${scene.id}")
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              if (scene.drawableRes != null) {
                Image(
                  painter = painterResource(id = scene.drawableRes),
                  contentDescription = scene.label,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = scene.label,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = scene.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              if (isSelected) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Selected",
                  tint = MaterialTheme.colorScheme.primary
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
              text = "Background Softness Veil",
              style = MaterialTheme.typography.labelLarge
            )
            Text(
              text = "${(scrimAlpha * 100).roundToInt()}%",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary
            )
          }
          Slider(
            value = scrimAlpha,
            onValueChange = onChangeScrimAlpha,
            valueRange = 0.35f..0.90f
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Done")
      }
    }
  )
}
