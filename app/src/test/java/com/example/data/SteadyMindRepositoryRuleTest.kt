package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SteadyMindRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = SteadyMindRepository(firestore)

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(
        title = "Prepare presentation slides",
        blockerType = "perfectionism",
        dreadLevel = 8,
        microSteps = listOf("Open slide deck", "Add title slide")
      )
    }
    assertTrue(createResult.isSuccess)
    val taskId = createResult.getOrThrow()
    assertTrue(taskId.isNotEmpty())
  }

  @Test
  fun getUserTasks_authenticatedOwner_returnsMatchingTasks() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = SteadyMindRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(
        title = "Reply to delayed email",
        blockerType = "fear_of_outcome",
        dreadLevel = 7,
        microSteps = listOf("Draft 2 sentences")
      ).getOrThrow()
    }

    val tasksResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserTasks() }
    assertTrue(tasksResult.isSuccess)
    assertTrue(tasksResult.getOrThrow().map { it.id }.contains(taskId))
  }

  @Test
  fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = SteadyMindRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createTask(
        title = "Organize receipts",
        blockerType = "overwhelmed",
        dreadLevel = 6,
        microSteps = listOf("Put receipts in one pile")
      ).getOrThrow()
    }

    val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(aliceUid).first { list -> list.any { it.id == taskId } }
    }
    assertTrue(emittedTasks.map { it.id }.contains(taskId))
  }

  @Test
  fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = SteadyMindRepository(firestore)
    val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo.createTask(
        title = "Alice private task",
        blockerType = "ambiguous",
        dreadLevel = 5,
        microSteps = listOf("Step 1")
      ).getOrThrow()
    }

    signInTestUser(BOB_EMAIL)
    val bobRepo = SteadyMindRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo.getTaskById(aliceUid, taskId)
    }
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull() as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = SteadyMindRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTasks("unauthenticated_user").first()
      }
      fail("Expected FirebaseFirestoreException.Code.PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  @Test
  fun createWorry_and_observeWorries_authenticatedOwner_emitsWorry() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = SteadyMindRepository(firestore)
    val worryId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createWorry(
        worryText = "What if I forget a point during the meeting?",
        scheduledWindow = "5:30 PM Reflection Window"
      ).getOrThrow()
    }

    val emittedWorries = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeWorries(aliceUid).first { list -> list.any { it.id == worryId } }
    }
    assertTrue(emittedWorries.any { it.id == worryId })
  }

  @Test
  fun createSubscription_and_observeSubscriptions_authenticatedOwner_emitsMpesaSubscription() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val repository = SteadyMindRepository(firestore)
    val subId = withTimeout(DEFAULT_TIMEOUT_MS) {
      repository.createSubscription(
        plan = SubscriptionPlan.MONTHLY,
        senderPhone = "0712345678",
        mpesaReceiptCode = "SJK7Q9M2LP"
      ).getOrThrow()
    }

    val emittedSubs = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeSubscriptions(aliceUid).first { list -> list.any { it.id == subId } }
    }
    val sub = emittedSubs.first { it.id == subId }
    assertEquals("0119137126", sub.recipientMpesaNumber)
    assertEquals("SJK7Q9M2LP", sub.mpesaReceiptCode)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@steadymind.test"
    const val BOB_EMAIL = "bob@steadymind.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
