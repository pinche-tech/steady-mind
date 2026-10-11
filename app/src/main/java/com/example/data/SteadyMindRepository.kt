package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SteadyMindRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeTasks(userId: String? = null): Flow<List<MicroTask>> = flow {
    val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid/tasks"
    emitAll(
      db.collection("users").document(uid).collection("tasks")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { MicroTask.fromSnapshot(it) }
            .sortedWith(
              compareBy<MicroTask> { it.isCompleted }
                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
        }
        .catch { error ->
          val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
          val ex = unwrapped ?: (error as? Exception)
          if (ex != null) handleFirestoreError(ex, OperationType.LIST, path)
          throw (unwrapped ?: error)
        }
    )
  }

  fun observeThoughtLogs(userId: String? = null): Flow<List<ThoughtLog>> = flow {
    val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid/thought_logs"
    emitAll(
      db.collection("users").document(uid).collection("thought_logs")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { ThoughtLog.fromSnapshot(it) }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
          val ex = unwrapped ?: (error as? Exception)
          if (ex != null) handleFirestoreError(ex, OperationType.LIST, path)
          throw (unwrapped ?: error)
        }
    )
  }

  fun observeFocusSessions(userId: String? = null): Flow<List<FocusSession>> = flow {
    val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid/focus_sessions"
    emitAll(
      db.collection("users").document(uid).collection("focus_sessions")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { FocusSession.fromSnapshot(it) }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
          val ex = unwrapped ?: (error as? Exception)
          if (ex != null) handleFirestoreError(ex, OperationType.LIST, path)
          throw (unwrapped ?: error)
        }
    )
  }

  fun observeWorries(userId: String? = null): Flow<List<WorryVaultItem>> = flow {
    val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid/worry_vault"
    emitAll(
      db.collection("users").document(uid).collection("worry_vault")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { WorryVaultItem.fromSnapshot(it) }
            .sortedWith(
              compareBy<WorryVaultItem> { it.isResolved }
                .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
            )
        }
        .catch { error ->
          val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
          val ex = unwrapped ?: (error as? Exception)
          if (ex != null) handleFirestoreError(ex, OperationType.LIST, path)
          throw (unwrapped ?: error)
        }
    )
  }

  suspend fun createTask(
    title: String,
    blockerType: String,
    dreadLevel: Int,
    microSteps: List<String>
  ): Result<String> {
    val uid = requireUserId()
    val tasksRef = db.collection("users").document(uid).collection("tasks")
    val docRef = tasksRef.document()
    val sanitizedSteps = microSteps
      .map { it.trim().take(200) }
      .filter { it.isNotEmpty() }
      .take(20)
      .ifEmpty { BlockerType.fromKey(blockerType).defaultMicroSteps }

    val payload = mapOf(
      "userId" to uid,
      "title" to title.trim().take(200),
      "blockerType" to blockerType.trim().take(40),
      "dreadLevel" to dreadLevel.coerceIn(1, 10),
      "microSteps" to sanitizedSteps,
      "completedStepsCount" to 0,
      "status" to "active",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getUserTasks(): Result<List<MicroTask>> {
    val uid = requireUserId()
    val tasksRef = db.collection("users").document(uid).collection("tasks")
    return try {
      val snapshot = tasksRef.whereEqualTo("userId", uid).get().await()
      val items = snapshot.documents.map { MicroTask.fromSnapshot(it) }
      Result.success(items)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, tasksRef.path)
      Result.failure(e)
    }
  }

  suspend fun getTaskById(ownerUserId: String, taskId: String): Result<MicroTask> {
    val docRef = db.collection("users").document(ownerUserId).collection("tasks").document(taskId)
    return try {
      val snapshot = docRef.get().await()
      Result.success(MicroTask.fromSnapshot(snapshot))
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun advanceTaskStep(task: MicroTask): Result<Unit> {
    val uid = requireUserId()
    if (task.isCompleted) return Result.success(Unit)
    val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
    val totalSteps = task.microSteps.size.coerceAtLeast(1)
    val nextCount = (task.completedStepsCount + 1).coerceAtMost(totalSteps)
    val newStatus = if (nextCount >= totalSteps) "completed" else "active"
    val updates = mapOf(
      "completedStepsCount" to nextCount,
      "microSteps" to task.microSteps,
      "status" to newStatus,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun completeTaskNow(task: MicroTask): Result<Unit> {
    val uid = requireUserId()
    if (task.isCompleted) return Result.success(Unit)
    val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
    val totalSteps = task.microSteps.size.coerceAtMost(20)
    val updates = mapOf(
      "completedStepsCount" to totalSteps,
      "microSteps" to task.microSteps,
      "status" to "completed",
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteTask(taskId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createThoughtLog(
    anxiousThought: String,
    distortion: String,
    reframeText: String,
    tinyAction: String,
    anxietyBefore: Int,
    anxietyAfter: Int
  ): Result<String> {
    val uid = requireUserId()
    val logsRef = db.collection("users").document(uid).collection("thought_logs")
    val docRef = logsRef.document()
    val payload = mapOf(
      "userId" to uid,
      "anxiousThought" to anxiousThought.trim().take(500),
      "distortion" to distortion.trim().take(60),
      "reframeText" to reframeText.trim().take(500),
      "tinyAction" to tinyAction.trim().take(200),
      "anxietyBefore" to anxietyBefore.coerceIn(0, 100),
      "anxietyAfter" to anxietyAfter.coerceIn(0, 100),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteThoughtLog(logId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("thought_logs").document(logId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun recordFocusSession(
    modeName: String,
    durationMinutes: Int,
    taskTitle: String,
    reliefRating: Int
  ): Result<String> {
    val uid = requireUserId()
    val sessionsRef = db.collection("users").document(uid).collection("focus_sessions")
    val docRef = sessionsRef.document()
    val payload = mapOf(
      "userId" to uid,
      "modeName" to modeName.trim().take(80),
      "durationMinutes" to durationMinutes.coerceIn(1, 180),
      "taskTitle" to taskTitle.trim().ifEmpty { "General Steady Focus" }.take(200),
      "reliefRating" to reliefRating.coerceIn(1, 10),
      "createdAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createWorry(
    worryText: String,
    scheduledWindow: String
  ): Result<String> {
    val uid = requireUserId()
    val worriesRef = db.collection("users").document(uid).collection("worry_vault")
    val docRef = worriesRef.document()
    val payload = mapOf(
      "userId" to uid,
      "worryText" to worryText.trim().take(500),
      "scheduledWindow" to scheduledWindow.trim().ifEmpty { "Today at 5:30 PM (10-Min Worry Window)" }.take(120),
      "isResolved" to false,
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun toggleWorryResolved(item: WorryVaultItem): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("worry_vault").document(item.id)
    val updates = mapOf(
      "isResolved" to !item.isResolved,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.update(updates).await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteWorry(worryId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("worry_vault").document(worryId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  fun observeSubscriptions(userId: String? = null): Flow<List<MpesaSubscription>> = flow {
    val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
    val path = "users/$uid/subscriptions"
    emitAll(
      db.collection("users").document(uid).collection("subscriptions")
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.documents
            .map { MpesaSubscription.fromSnapshot(it) }
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val unwrapped = (error as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
            ?: (error.cause?.cause as? com.google.firebase.firestore.FirebaseFirestoreException)
          val ex = unwrapped ?: (error as? Exception)
          if (ex != null) handleFirestoreError(ex, OperationType.LIST, path)
          throw (unwrapped ?: error)
        }
    )
  }

  suspend fun createSubscription(
    plan: SubscriptionPlan,
    senderPhone: String,
    mpesaReceiptCode: String
  ): Result<String> {
    val uid = requireUserId()
    val subsRef = db.collection("users").document(uid).collection("subscriptions")
    val docRef = subsRef.document()
    val normalizedPhone = MpesaSubscription.normalizeKenyanPhone(senderPhone).take(24)
    val sanitizedReceipt = mpesaReceiptCode.trim().uppercase().filter { it.isLetterOrDigit() }.take(12)
    val payload = mapOf(
      "userId" to uid,
      "planId" to plan.id,
      "planTitle" to plan.title.take(100),
      "amountKes" to plan.priceKes,
      "recipientMpesaNumber" to MPESA_RECIPIENT_NUMBER,
      "senderPhone" to normalizedPhone,
      "mpesaReceiptCode" to sanitizedReceipt,
      "status" to "active",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    return try {
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }
}
