package com.mutabei.workout.data.repository

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import com.mutabei.workout.R
import com.mutabei.workout.data.model.BodyRecord
import com.mutabei.workout.data.model.CoachReportRecord
import com.mutabei.workout.data.model.CustomFood
import com.mutabei.workout.data.model.NutritionDay
import com.mutabei.workout.data.model.UserConfig
import com.mutabei.workout.data.model.WorkoutSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول بحساب Google أولاً.")
        check(uid == expectedUid) { "تغيّر المستخدم المسجّل؛ تم إلغاء العملية." }
        return uid
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeUserConfig(userId: String): Flow<UserConfig> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId)
                .snapshots()
                .map { snapshot -> UserConfig.fromSnapshot(snapshot) ?: UserConfig(userId = userId) }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    fun observeCoachReports(userId: String): Flow<List<CoachReportRecord>> = flow {
        val path = "users/$userId/coachReports"
        emitAll(
            userDoc(userId).collection("coachReports")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { CoachReportRecord.fromSnapshot(it) }
                        .sortedWith(
                            compareByDescending<CoachReportRecord> { it.date }
                                .thenByDescending { it.createdAt?.seconds ?: 0L }
                        )
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeWorkoutSessions(userId: String): Flow<Map<String, WorkoutSession>> = flow {
        val path = "users/$userId/workoutSessions"
        emitAll(
            userDoc(userId).collection("workoutSessions")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { WorkoutSession.fromSnapshot(it) }
                        .associateBy { it.id }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeNutritionDays(userId: String): Flow<Map<String, NutritionDay>> = flow {
        val path = "users/$userId/nutritionDays"
        emitAll(
            userDoc(userId).collection("nutritionDays")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { NutritionDay.fromSnapshot(it) }
                        .associateBy { it.id }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeBodyRecords(userId: String): Flow<List<BodyRecord>> = flow {
        val path = "users/$userId/bodyRecords"
        emitAll(
            userDoc(userId).collection("bodyRecords")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { BodyRecord.fromSnapshot(it) }
                        .sortedBy { it.date }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCustomFoods(userId: String): Flow<List<CustomFood>> = flow {
        val path = "users/$userId/customFoods"
        emitAll(
            userDoc(userId).collection("customFoods")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { CustomFood.fromSnapshot(it) }
                        .sortedByDescending { it.updatedAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    suspend fun saveUserConfig(userId: String, config: UserConfig): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(config.toUpdateMap()).await()
            } else {
                docRef.set(config.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCoachReport(userId: String, record: CoachReportRecord): Result<String> {
        val docId = sanitizeDocId(
            record.id.ifBlank {
                "w${record.weekNumber}_${record.date}_${UUID.randomUUID().toString().replace("-", "").take(6)}"
            }
        )
        val path = "users/$userId/coachReports/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("coachReports").document(docId)
            docRef.set(record.toCreateMap(uid)).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCoachReport(userId: String, reportId: String): Result<Unit> {
        val docId = sanitizeDocId(reportId)
        val path = "users/$userId/coachReports/$docId"
        return try {
            val uid = requireUserId(userId)
            userDoc(uid).collection("coachReports").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWorkoutSession(userId: String, session: WorkoutSession): Result<Unit> {
        val docId = sanitizeDocId(session.id.ifBlank { "${session.date}_${session.dayIndex}" })
        val path = "users/$userId/workoutSessions/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("workoutSessions").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(session.toUpdateMap()).await()
            } else {
                docRef.set(session.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveNutritionDay(userId: String, day: NutritionDay): Result<Unit> {
        val docId = sanitizeDocId(day.date)
        val path = "users/$userId/nutritionDays/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("nutritionDays").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(day.toUpdateMap()).await()
            } else {
                docRef.set(day.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveBodyRecord(userId: String, record: BodyRecord): Result<Unit> {
        val docId = sanitizeDocId(record.date)
        val path = "users/$userId/bodyRecords/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("bodyRecords").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                // Use delete + create or overwrite all optional fields cleanly by replacing via update;
                // since BodyRecord has optional fields that might be cleared on overwrite,
                // delete & re-create keeps schema 100% clean if replacing an existing date record.
                docRef.delete().await()
            }
            docRef.set(record.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteBodyRecord(userId: String, dateId: String): Result<Unit> {
        val docId = sanitizeDocId(dateId)
        val path = "users/$userId/bodyRecords/$docId"
        return try {
            val uid = requireUserId(userId)
            userDoc(uid).collection("bodyRecords").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveCustomFood(userId: String, food: CustomFood): Result<String> {
        val docId = sanitizeDocId(food.id.ifBlank { "f_${UUID.randomUUID().toString().replace("-", "").take(16)}" })
        val path = "users/$userId/customFoods/$docId"
        return try {
            val uid = requireUserId(userId)
            val docRef = userDoc(uid).collection("customFoods").document(docId)
            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(food.toUpdateMap()).await()
            } else {
                docRef.set(food.toCreateMap(uid)).await()
            }
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomFood(userId: String, foodId: String): Result<Unit> {
        val docId = sanitizeDocId(foodId)
        val path = "users/$userId/customFoods/$docId"
        return try {
            val uid = requireUserId(userId)
            userDoc(uid).collection("customFoods").document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfAuthenticated(
        error: Throwable,
        op: OperationType,
        path: String,
        expectedUid: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception) ?: return
        if (auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, op, path)
        }
    }

    private fun sanitizeDocId(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120).ifBlank { "doc" }
}
