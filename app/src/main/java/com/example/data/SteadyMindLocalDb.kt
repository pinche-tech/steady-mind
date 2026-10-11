package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

@Entity(tableName = "local_tasks")
data class LocalTaskEntity(
  @PrimaryKey val id: String,
  val title: String,
  val blockerType: String,
  val dreadLevel: Int,
  val microStepsJson: String,
  val completedStepsCount: Int,
  val status: String,
  val createdAtMillis: Long,
  val updatedAtMillis: Long
) {
  fun toDomain(): MicroTask {
    val steps = mutableListOf<String>()
    try {
      val arr = JSONArray(microStepsJson)
      for (i in 0 until arr.length()) {
        val s = arr.optString(i)
        if (s.isNotBlank()) steps.add(s)
      }
    } catch (_: Exception) {
    }
    return MicroTask(
      id = id,
      userId = "local_user",
      title = title,
      blockerType = blockerType,
      dreadLevel = dreadLevel,
      microSteps = steps,
      completedStepsCount = completedStepsCount,
      status = status,
      createdAt = Timestamp(Date(createdAtMillis)),
      updatedAt = Timestamp(Date(updatedAtMillis))
    )
  }

  companion object {
    fun fromDomain(task: MicroTask): LocalTaskEntity {
      val jsonArr = JSONArray()
      task.microSteps.forEach { jsonArr.put(it) }
      val createdMs = task.createdAt?.toDate()?.time ?: System.currentTimeMillis()
      val updatedMs = task.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
      return LocalTaskEntity(
        id = task.id,
        title = task.title,
        blockerType = task.blockerType,
        dreadLevel = task.dreadLevel,
        microStepsJson = jsonArr.toString(),
        completedStepsCount = task.completedStepsCount,
        status = task.status,
        createdAtMillis = createdMs,
        updatedAtMillis = updatedMs
      )
    }
  }
}

@Entity(tableName = "local_thought_logs")
data class LocalThoughtLogEntity(
  @PrimaryKey val id: String,
  val anxiousThought: String,
  val distortion: String,
  val reframeText: String,
  val tinyAction: String,
  val anxietyBefore: Int,
  val anxietyAfter: Int,
  val createdAtMillis: Long
) {
  fun toDomain(): ThoughtLog = ThoughtLog(
    id = id,
    userId = "local_user",
    anxiousThought = anxiousThought,
    distortion = distortion,
    reframeText = reframeText,
    tinyAction = tinyAction,
    anxietyBefore = anxietyBefore,
    anxietyAfter = anxietyAfter,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(createdAtMillis))
  )

  companion object {
    fun fromDomain(log: ThoughtLog): LocalThoughtLogEntity {
      val createdMs = log.createdAt?.toDate()?.time ?: System.currentTimeMillis()
      return LocalThoughtLogEntity(
        id = log.id,
        anxiousThought = log.anxiousThought,
        distortion = log.distortion,
        reframeText = log.reframeText,
        tinyAction = log.tinyAction,
        anxietyBefore = log.anxietyBefore,
        anxietyAfter = log.anxietyAfter,
        createdAtMillis = createdMs
      )
    }
  }
}

@Entity(tableName = "local_focus_sessions")
data class LocalFocusSessionEntity(
  @PrimaryKey val id: String,
  val modeName: String,
  val durationMinutes: Int,
  val taskTitle: String,
  val reliefRating: Int,
  val createdAtMillis: Long
) {
  fun toDomain(): FocusSession = FocusSession(
    id = id,
    userId = "local_user",
    modeName = modeName,
    durationMinutes = durationMinutes,
    taskTitle = taskTitle,
    reliefRating = reliefRating,
    createdAt = Timestamp(Date(createdAtMillis))
  )

  companion object {
    fun fromDomain(session: FocusSession): LocalFocusSessionEntity {
      val createdMs = session.createdAt?.toDate()?.time ?: System.currentTimeMillis()
      return LocalFocusSessionEntity(
        id = session.id,
        modeName = session.modeName,
        durationMinutes = session.durationMinutes,
        taskTitle = session.taskTitle,
        reliefRating = session.reliefRating,
        createdAtMillis = createdMs
      )
    }
  }
}

@Entity(tableName = "local_worry_vault")
data class LocalWorryEntity(
  @PrimaryKey val id: String,
  val worryText: String,
  val scheduledWindow: String,
  val isResolved: Boolean,
  val createdAtMillis: Long
) {
  fun toDomain(): WorryVaultItem = WorryVaultItem(
    id = id,
    worryText = worryText,
    scheduledWindow = scheduledWindow,
    isResolved = isResolved,
    createdAtMillis = createdAtMillis
  )
}

@Entity(tableName = "local_subscriptions")
data class LocalSubscriptionEntity(
  @PrimaryKey val id: String,
  val planId: String,
  val planTitle: String,
  val amountKes: Int,
  val recipientMpesaNumber: String,
  val senderPhone: String,
  val mpesaReceiptCode: String,
  val status: String,
  val createdAtMillis: Long
) {
  fun toDomain(): MpesaSubscription = MpesaSubscription(
    id = id,
    userId = "local_user",
    planId = planId,
    planTitle = planTitle,
    amountKes = amountKes,
    recipientMpesaNumber = recipientMpesaNumber,
    senderPhone = senderPhone,
    mpesaReceiptCode = mpesaReceiptCode,
    status = status,
    createdAtMillis = createdAtMillis,
    createdAt = Timestamp(Date(createdAtMillis)),
    updatedAt = Timestamp(Date(createdAtMillis))
  )
}

@Dao
interface SteadyMindDao {
  @Query("SELECT * FROM local_tasks ORDER BY CASE WHEN status = 'completed' THEN 1 ELSE 0 END ASC, createdAtMillis DESC")
  fun observeTasks(): Flow<List<LocalTaskEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: LocalTaskEntity)

  @Query("DELETE FROM local_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: String)

  @Query("SELECT * FROM local_thought_logs ORDER BY createdAtMillis DESC")
  fun observeThoughtLogs(): Flow<List<LocalThoughtLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertThoughtLog(log: LocalThoughtLogEntity)

  @Query("DELETE FROM local_thought_logs WHERE id = :id")
  suspend fun deleteThoughtLogById(id: String)

  @Query("SELECT * FROM local_focus_sessions ORDER BY createdAtMillis DESC")
  fun observeFocusSessions(): Flow<List<LocalFocusSessionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFocusSession(session: LocalFocusSessionEntity)

  @Query("SELECT * FROM local_worry_vault ORDER BY isResolved ASC, createdAtMillis DESC")
  fun observeWorries(): Flow<List<LocalWorryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertWorry(worry: LocalWorryEntity)

  @Query("DELETE FROM local_worry_vault WHERE id = :id")
  suspend fun deleteWorryById(id: String)

  @Query("SELECT * FROM local_subscriptions ORDER BY createdAtMillis DESC")
  fun observeSubscriptions(): Flow<List<LocalSubscriptionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSubscription(subscription: LocalSubscriptionEntity)

  @Query("DELETE FROM local_subscriptions WHERE id = :id")
  suspend fun deleteSubscriptionById(id: String)
}

@Database(
  entities = [
    LocalTaskEntity::class,
    LocalThoughtLogEntity::class,
    LocalFocusSessionEntity::class,
    LocalWorryEntity::class,
    LocalSubscriptionEntity::class
  ],
  version = 2,
  exportSchema = false
)
abstract class SteadyMindDatabase : RoomDatabase() {
  abstract fun steadyMindDao(): SteadyMindDao

  companion object {
    @Volatile
    private var INSTANCE: SteadyMindDatabase? = null

    fun getInstance(context: Context): SteadyMindDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SteadyMindDatabase::class.java,
          "steadymind_local.db"
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
        INSTANCE = instance
        instance
      }
    }
  }
}

class LocalSteadyMindRepository(private val dao: SteadyMindDao) {
  val tasksFlow: Flow<List<MicroTask>> = dao.observeTasks().map { list -> list.map { it.toDomain() } }
  val thoughtLogsFlow: Flow<List<ThoughtLog>> = dao.observeThoughtLogs().map { list -> list.map { it.toDomain() } }
  val focusSessionsFlow: Flow<List<FocusSession>> = dao.observeFocusSessions().map { list -> list.map { it.toDomain() } }
  val worriesFlow: Flow<List<WorryVaultItem>> = dao.observeWorries().map { list -> list.map { it.toDomain() } }
  val subscriptionsFlow: Flow<List<MpesaSubscription>> = dao.observeSubscriptions().map { list -> list.map { it.toDomain() } }

  suspend fun saveTask(task: MicroTask) = dao.insertTask(LocalTaskEntity.fromDomain(task))
  suspend fun deleteTask(id: String) = dao.deleteTaskById(id)

  suspend fun saveThoughtLog(log: ThoughtLog) = dao.insertThoughtLog(LocalThoughtLogEntity.fromDomain(log))
  suspend fun deleteThoughtLog(id: String) = dao.deleteThoughtLogById(id)

  suspend fun saveFocusSession(session: FocusSession) = dao.insertFocusSession(LocalFocusSessionEntity.fromDomain(session))

  suspend fun saveWorry(item: WorryVaultItem) = dao.insertWorry(
    LocalWorryEntity(
      id = item.id,
      worryText = item.worryText,
      scheduledWindow = item.scheduledWindow,
      isResolved = item.isResolved,
      createdAtMillis = item.createdAtMillis
    )
  )

  suspend fun deleteWorry(id: String) = dao.deleteWorryById(id)

  suspend fun saveSubscription(sub: MpesaSubscription) = dao.insertSubscription(
    LocalSubscriptionEntity(
      id = sub.id,
      planId = sub.planId,
      planTitle = sub.planTitle,
      amountKes = sub.amountKes,
      recipientMpesaNumber = sub.recipientMpesaNumber,
      senderPhone = sub.senderPhone,
      mpesaReceiptCode = sub.mpesaReceiptCode,
      status = sub.status,
      createdAtMillis = sub.createdAtMillis
    )
  )
}
